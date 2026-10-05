package com.example

import android.Manifest
import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CappedFontScale
import com.example.ui.openAppSettings
import com.example.ui.screens.AlertsAndHistoryScreen
import com.example.ui.screens.DayDetailScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MeteoTrackTheme
import com.example.ui.viewmodel.UiEvent
import com.example.ui.viewmodel.WeatherViewModel
import kotlinx.coroutines.launch

enum class ScreenTab(val label: String) {
    HOME("Thời tiết"),
    FAVORITES("Địa điểm"),
    ALERTS_HISTORY("Cảnh báo"),
    SETTINGS("Cài đặt")
}

class MainActivity : ComponentActivity() {
    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MeteoTrackTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

private val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

@Composable
fun MainApp(viewModel: WeatherViewModel) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()

    var currentTab by rememberSaveable { mutableStateOf(ScreenTab.HOME) }
    var dayDetailIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- permissions -----------------------------------------------------------------------
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.refreshPermissionState()
        val locationDenied = LocationPermissions.none { result[it] == true } && !viewModel.hasLocationPermission.value
        // After a second denial Android no longer shows the dialog; send the user to settings instead.
        val permanentlyDenied = locationDenied && result.keys.any { it in LocationPermissions } &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permanentlyDenied) {
            viewModel.postEvent(
                UiEvent("Quyền vị trí đang bị tắt. Hãy bật lại trong cài đặt của ứng dụng.", "Mở cài đặt") {
                    context.openAppSettings()
                }
            )
        }
    }

    fun requestLocation() = permissionLauncher.launch(LocationPermissions)

    fun requestAll() {
        val permissions = LocationPermissions.toMutableList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions += Manifest.permission.POST_NOTIFICATIONS
        permissionLauncher.launch(permissions.toTypedArray())
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissionState()
        viewModel.onAppResumed()
    }

    // --- snackbars (undo, confirmations) ---------------------------------------------------
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel,
                    withDismissAction = event.actionLabel == null
                )
                if (result == SnackbarResult.ActionPerformed) event.onAction?.invoke()
            }
        }
    }

    // --- system bars: keep icons readable on both the weather gradient and plain surfaces ----
    val darkTheme = isSystemInDarkTheme()
    val onGradient = onboardingDone == false || dayDetailIndex != null || currentTab == ScreenTab.HOME
    DisposableEffect(onGradient, darkTheme) {
        val transparent = AndroidColor.TRANSPARENT
        val statusBar = if (onGradient) SystemBarStyle.dark(transparent)
        else SystemBarStyle.auto(transparent, transparent) { darkTheme }
        val navBar = if (onboardingDone == false || dayDetailIndex != null) SystemBarStyle.dark(transparent)
        else SystemBarStyle.auto(transparent, transparent) { darkTheme }
        (activity as ComponentActivity).enableEdgeToEdge(statusBarStyle = statusBar, navigationBarStyle = navBar)
        onDispose { }
    }

    // --- first run ------------------------------------------------------------------------
    when (onboardingDone) {
        null -> {
            // DataStore is still loading: show nothing (the window background is already themed).
            Box(Modifier.fillMaxSize())
            return
        }

        false -> {
            OnboardingScreen(
                onContinue = {
                    viewModel.completeOnboarding()
                    requestAll()
                },
                onSkip = { viewModel.completeOnboarding() }
            )
            return
        }

        true -> Unit
    }

    // --- navigation -----------------------------------------------------------------------
    BackHandler(enabled = dayDetailIndex != null) { dayDetailIndex = null }
    BackHandler(enabled = dayDetailIndex == null && currentTab != ScreenTab.HOME) { currentTab = ScreenTab.HOME }

    dayDetailIndex?.let { index ->
        DayDetailScreen(viewModel = viewModel, dayIndex = index, onBack = { dayDetailIndex = null })
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            CappedFontScale {
            NavigationBar(modifier = Modifier.testTag("main_bottom_nav")) {
                ScreenTab.entries.forEach { tab ->
                    val (selectedIcon, unselectedIcon) = tab.icons()
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == tab) selectedIcon else unselectedIcon,
                                contentDescription = null
                            )
                        },
                        label = { Text(tab.label, maxLines = 1) },
                        modifier = Modifier.testTag(tab.testTag())
                    )
                }
            }
            }
        }
    ) { innerPadding ->
        Crossfade(targetState = currentTab, label = "tab") { tab ->
            val tabModifier = Modifier.padding(innerPadding)
            when (tab) {
                ScreenTab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDayDetail = { dayDetailIndex = it },
                    onEnableLocation = ::requestLocation,
                    modifier = tabModifier
                )

                ScreenTab.FAVORITES -> FavoritesScreen(
                    viewModel = viewModel,
                    onLocationSelected = { currentTab = ScreenTab.HOME },
                    modifier = tabModifier
                )

                ScreenTab.ALERTS_HISTORY -> AlertsAndHistoryScreen(viewModel = viewModel, modifier = tabModifier)
                ScreenTab.SETTINGS -> SettingsScreen(viewModel = viewModel, modifier = tabModifier)
            }
        }
    }
}

private fun ScreenTab.icons(): Pair<ImageVector, ImageVector> = when (this) {
    ScreenTab.HOME -> Icons.Filled.Home to Icons.Outlined.Home
    ScreenTab.FAVORITES -> Icons.Filled.Place to Icons.Outlined.Place
    ScreenTab.ALERTS_HISTORY -> Icons.Filled.NotificationsActive to Icons.Outlined.Notifications
    ScreenTab.SETTINGS -> Icons.Filled.Settings to Icons.Outlined.Settings
}

private fun ScreenTab.testTag(): String = when (this) {
    ScreenTab.HOME -> "nav_tab_home"
    ScreenTab.FAVORITES -> "nav_tab_favorites"
    ScreenTab.ALERTS_HISTORY -> "nav_tab_alerts"
    ScreenTab.SETTINGS -> "nav_tab_settings"
}
