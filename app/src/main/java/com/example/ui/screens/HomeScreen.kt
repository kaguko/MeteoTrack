package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.model.WeatherCodeMapper
import com.example.ui.components.AiWeatherInsightsDialog
import com.example.ui.components.CappedFontScale
import com.example.ui.components.CenteredContent
import com.example.ui.components.DailyForecastList
import com.example.ui.components.Glass
import com.example.ui.components.GlassCard
import com.example.ui.components.HomeSkeleton
import com.example.ui.components.HourlyForecastRow
import com.example.ui.components.InfoBanner
import com.example.ui.components.MovementTrackerCard
import com.example.ui.components.TemperatureChart
import com.example.ui.components.TrackingStatus
import com.example.ui.components.WeatherCurrentCard
import com.example.ui.viewmodel.LocationMode
import com.example.ui.viewmodel.WeatherUiState
import com.example.ui.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: WeatherViewModel,
    onNavigateToDayDetail: (Int) -> Unit,
    onEnableLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeLocation by viewModel.activeLocation.collectAsStateWithLifecycle()
    val mode by viewModel.locationMode.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val distanceMoved by viewModel.distanceSinceLastFetchKm.collectAsStateWithLifecycle()
    val isTrackingOn by viewModel.isMovementTrackingActive.collectAsStateWithLifecycle()
    val aiInsights by viewModel.aiInsights.collectAsStateWithLifecycle()
    val isLoadingAi by viewModel.isLoadingAi.collectAsStateWithLifecycle()

    var showAiDialog by remember { mutableStateOf(false) }

    val success = uiState as? WeatherUiState.Success
    val isRefreshing = success?.isRefreshing == true

    // The background follows the current weather and cross-fades when it changes.
    val current = success?.data?.current
    val info = WeatherCodeMapper.getInfo(current?.weatherCode ?: 0, current?.isDay != 0)
    val c0 by animateColorAsState(info.gradient[0], tween(900), label = "bg0")
    val c1 by animateColorAsState(info.gradient[1], tween(900), label = "bg1")
    val c2 by animateColorAsState(info.gradient[2], tween(900), label = "bg2")

    Column(modifier = modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c0, c1, c2)))) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        CappedFontScale { Column {
                            Text(
                                text = activeLocation.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitle(mode, success),
                                style = MaterialTheme.typography.bodySmall,
                                color = Glass.OnGlassMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = Glass.OnGlass,
                        actionIconContentColor = Glass.OnGlass
                    ),
                    actions = {
                        if (mode != LocationMode.GPS && hasPermission) {
                            IconButton(
                                onClick = { viewModel.switchToGpsLocation() },
                                modifier = Modifier.testTag("switch_to_gps_button")
                            ) {
                                Icon(Icons.Default.GpsFixed, contentDescription = "Dùng vị trí hiện tại của tôi")
                            }
                        }
                        IconButton(
                            onClick = { viewModel.refreshCurrentWeather() },
                            enabled = !isRefreshing,
                            modifier = Modifier.testTag("refresh_weather_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (isRefreshing) "Đang làm mới" else "Làm mới thời tiết",
                                modifier = Modifier.rotate(if (isRefreshing) spinAngle() else 0f)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshCurrentWeather() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                CenteredContent {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // --- notices -------------------------------------------------------
                        if (mode == LocationMode.DEFAULT) {
                            if (!hasPermission) {
                                GlassBanner(
                                    text = "Chưa bật vị trí nên đang hiển thị Hà Nội. Bật vị trí để xem thời tiết nơi bạn đứng.",
                                    icon = Icons.Default.LocationOff,
                                    actionLabel = "Bật vị trí",
                                    onAction = onEnableLocation
                                )
                            } else {
                                GlassBanner(
                                    text = "Chưa xác định được vị trí của bạn. Hãy bật GPS rồi thử lại.",
                                    icon = Icons.Default.LocationOff,
                                    actionLabel = "Thử lại",
                                    onAction = { viewModel.switchToGpsLocation() }
                                )
                            }
                        }
                        success?.refreshError?.let { message ->
                            GlassBanner(
                                text = "$message Đang hiển thị dữ liệu lúc ${clock(success.lastUpdated)}.",
                                icon = Icons.Default.CloudOff,
                                actionLabel = "Thử lại",
                                onAction = { viewModel.refreshCurrentWeather() }
                            )
                        }

                        // --- content -------------------------------------------------------
                        when (val state = uiState) {
                            is WeatherUiState.Idle, is WeatherUiState.Loading -> {
                                HomeSkeleton(
                                    modifier = Modifier.semantics {
                                        liveRegion = LiveRegionMode.Polite
                                        contentDescription = "Đang tải dữ liệu thời tiết"
                                    }
                                )
                            }

                            is WeatherUiState.Error -> ErrorCard(
                                message = state.message,
                                isOffline = state.isOffline,
                                onRetry = { viewModel.refreshCurrentWeather() }
                            )

                            is WeatherUiState.Success -> {
                                val data = state.data
                                val now = data.current
                                if (now != null) {
                                    WeatherCurrentCard(
                                        current = now,
                                        todayHigh = data.daily?.temperature2mMax?.firstOrNull(),
                                        todayLow = data.daily?.temperature2mMin?.firstOrNull(),
                                        tempUnit = settings.tempUnit,
                                        windUnit = settings.windUnit,
                                        onAiInsightsClick = {
                                            showAiDialog = true
                                            viewModel.fetchAiInsights()
                                        }
                                    )

                                    MovementTrackerCard(
                                        distanceKm = distanceMoved,
                                        thresholdKm = settings.minDistanceKm,
                                        status = when {
                                            !hasPermission -> TrackingStatus.NEEDS_PERMISSION
                                            mode != LocationMode.GPS -> TrackingStatus.VIEWING_OTHER_PLACE
                                            isTrackingOn -> TrackingStatus.ACTIVE
                                            else -> TrackingStatus.PAUSED
                                        },
                                        isOn = isTrackingOn && hasPermission,
                                        onToggleTracking = { viewModel.toggleMovementTracking() },
                                        onSimulateMove = if (BuildConfig.DEBUG) {
                                            { viewModel.simulateMovementTest(5.5) }
                                        } else null
                                    )

                                    data.hourly?.let {
                                        HourlyForecastRow(it, now.time, settings.tempUnit)
                                    }
                                    data.daily?.let { daily ->
                                        DailyForecastList(
                                            daily = daily,
                                            tempUnit = settings.tempUnit,
                                            onDayClick = { index ->
                                                viewModel.selectDayDetail(index)
                                                onNavigateToDayDetail(index)
                                            }
                                        )
                                    }
                                    data.hourly?.let {
                                        TemperatureChart(it, now.time, settings.tempUnit)
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Dữ liệu thời tiết: Open-Meteo.com",
                            style = MaterialTheme.typography.labelSmall,
                            color = Glass.OnGlassMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAiDialog) {
        AiWeatherInsightsDialog(
            locationName = activeLocation.displayName,
            insights = aiInsights,
            isLoading = isLoadingAi,
            onDismiss = {
                showAiDialog = false
                viewModel.clearAiInsights()
            }
        )
    }
}

@Composable
private fun spinAngle(): Float {
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    return angle
}

@Composable
private fun GlassBanner(
    text: String,
    icon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit
) {
    InfoBanner(
        text = text,
        icon = icon,
        actionLabel = actionLabel,
        onAction = onAction,
        containerColor = Glass.FillStrong,
        contentColor = Glass.OnGlass
    )
}

@Composable
private fun ErrorCard(message: String, isOffline: Boolean, onRetry: () -> Unit) {
    GlassCard(modifier = Modifier.padding(top = 24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.Warning,
                contentDescription = null,
                tint = Glass.Accent,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = if (isOffline) "Không có kết nối mạng" else "Không thể tải thời tiết",
                style = MaterialTheme.typography.titleMedium,
                color = Glass.OnGlass
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Glass.OnGlassMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0F172A)),
                modifier = Modifier.testTag("retry_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Thử lại")
            }
        }
    }
}

private fun subtitle(mode: LocationMode, success: WeatherUiState.Success?): String {
    val label = when (mode) {
        LocationMode.GPS -> "Vị trí hiện tại"
        LocationMode.SELECTED -> "Địa điểm đã chọn"
        LocationMode.DEFAULT -> "Vị trí mặc định"
    }
    return when {
        success == null -> label
        success.isFromCache && success.isRefreshing -> "Đang cập nhật · lần trước ${clock(success.lastUpdated)}"
        else -> "$label · Cập nhật ${clock(success.lastUpdated)}"
    }
}

private fun clock(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
