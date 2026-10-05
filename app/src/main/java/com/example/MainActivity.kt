package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AlertsAndHistoryScreen
import com.example.ui.screens.DayDetailScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MeteoTrackTheme
import com.example.ui.viewmodel.WeatherViewModel

enum class ScreenTab {
    HOME, FAVORITES, ALERTS_HISTORY, SETTINGS
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

@Composable
fun MainApp(viewModel: WeatherViewModel) {
    var currentTab by remember { mutableStateOf(ScreenTab.HOME) }
    var selectedDayDetailIndex by remember { mutableStateOf<Int?>(null) }

    // Request permissions on first launch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.startGpsTracking()
            viewModel.switchToGpsLocation()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    // Back handling when in DayDetailScreen
    if (selectedDayDetailIndex != null) {
        BackHandler {
            selectedDayDetailIndex = null
        }

        DayDetailScreen(
            viewModel = viewModel,
            dayIndex = selectedDayDetailIndex!!,
            onBack = { selectedDayDetailIndex = null }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A).copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = currentTab == ScreenTab.HOME,
                    onClick = { currentTab = ScreenTab.HOME },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == ScreenTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Trang chủ",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Thời tiết", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0284C7),
                        selectedTextColor = Color(0xFF38BDF8),
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f),
                        indicatorColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_home")
                )

                NavigationBarItem(
                    selected = currentTab == ScreenTab.FAVORITES,
                    onClick = { currentTab = ScreenTab.FAVORITES },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == ScreenTab.FAVORITES) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Yêu thích",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Yêu thích", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFF59E0B),
                        selectedTextColor = Color(0xFFFBBF24),
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f),
                        indicatorColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_favorites")
                )

                NavigationBarItem(
                    selected = currentTab == ScreenTab.ALERTS_HISTORY,
                    onClick = { currentTab = ScreenTab.ALERTS_HISTORY },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == ScreenTab.ALERTS_HISTORY) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
                            contentDescription = "Cảnh báo & Lịch sử",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Cảnh báo", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFEF4444),
                        selectedTextColor = Color(0xFFF87171),
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f),
                        indicatorColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_alerts")
                )

                NavigationBarItem(
                    selected = currentTab == ScreenTab.SETTINGS,
                    onClick = { currentTab = ScreenTab.SETTINGS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == ScreenTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Cài đặt",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Cài đặt", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF10B981),
                        selectedTextColor = Color(0xFF34D399),
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f),
                        indicatorColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            ScreenTab.HOME -> {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToFavorites = { currentTab = ScreenTab.FAVORITES },
                    onNavigateToDayDetail = { dayIdx -> selectedDayDetailIndex = dayIdx },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenTab.FAVORITES -> {
                FavoritesScreen(
                    viewModel = viewModel,
                    onLocationSelected = { currentTab = ScreenTab.HOME },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenTab.ALERTS_HISTORY -> {
                AlertsAndHistoryScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenTab.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
