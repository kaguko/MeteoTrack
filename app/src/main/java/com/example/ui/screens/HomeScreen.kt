package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.WeatherCodeMapper
import com.example.ui.components.AiWeatherInsightsDialog
import com.example.ui.components.DailyForecastList
import com.example.ui.components.HourlyForecastRow
import com.example.ui.components.MovementTrackerCard
import com.example.ui.components.TemperatureChart
import com.example.ui.components.WeatherCurrentCard
import com.example.ui.viewmodel.WeatherUiState
import com.example.ui.viewmodel.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: WeatherViewModel,
    onNavigateToFavorites: () -> Unit,
    onNavigateToDayDetail: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeLocation by viewModel.activeLocation.collectAsState()
    val isShowingGps by viewModel.isShowingGpsLocation.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val distanceMoved by viewModel.distanceSinceLastFetchKm.collectAsState()
    val isTrackingActive by viewModel.isMovementTrackingActive.collectAsState()
    val aiInsights by viewModel.aiInsights.collectAsState()
    val isLoadingAi by viewModel.isLoadingAi.collectAsState()

    var showAiDialog by remember { mutableStateOf(false) }

    // Dynamic gradient based on current weather code
    val currentCode = (uiState as? WeatherUiState.Success)?.data?.current?.weatherCode ?: 0
    val isDay = (uiState as? WeatherUiState.Success)?.data?.current?.isDay != 0
    val weatherInfo = WeatherCodeMapper.getInfo(currentCode, isDay)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(weatherInfo.backgroundBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MeteoTrack",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = weatherInfo.iconEmoji,
                                    fontSize = 18.sp
                                )
                            }
                            Text(
                                text = if (isShowingGps) "📍 Định vị GPS tự động" else "⭐ Địa điểm đã chọn",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        if (!isShowingGps) {
                            IconButton(
                                onClick = { viewModel.switchToGpsLocation() },
                                modifier = Modifier.testTag("switch_to_gps_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = "Quay lại GPS",
                                    tint = Color.White
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.refreshCurrentWeather() },
                            modifier = Modifier.testTag("refresh_weather_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Làm mới thời tiết",
                                tint = Color.White
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (val state = uiState) {
                    is WeatherUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Đang tải dữ liệu thời tiết & GPS...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    is WeatherUiState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Lỗi",
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Không thể tải thời tiết",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { viewModel.refreshCurrentWeather() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF0F172A)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Thử lại")
                                }
                            }
                        }
                    }

                    is WeatherUiState.Success -> {
                        val current = state.data.current
                        if (current != null) {
                            // 1. Current Weather Main Card
                            WeatherCurrentCard(
                                locationName = activeLocation.displayName,
                                isGps = isShowingGps,
                                current = current,
                                tempUnit = settings.tempUnit,
                                windUnit = settings.windUnit,
                                onAiInsightsClick = {
                                    showAiDialog = true
                                    viewModel.fetchAiInsights()
                                }
                            )

                            // 2. Core Movement Tracker Card
                            MovementTrackerCard(
                                distanceKm = distanceMoved,
                                thresholdKm = settings.minDistanceKm,
                                isTrackingActive = isTrackingActive,
                                onToggleTracking = { viewModel.toggleMovementTracking() },
                                onSimulateMove = { delta -> viewModel.simulateMovementTest(delta) },
                                onManualRefresh = { viewModel.refreshCurrentWeather() }
                            )

                            // 3. Hourly Forecast Row (24h)
                            state.data.hourly?.let { hourly ->
                                HourlyForecastRow(
                                    hourly = hourly,
                                    tempUnit = settings.tempUnit
                                )

                                // 4. Temperature Trend Canvas Chart
                                TemperatureChart(
                                    hourly = hourly,
                                    tempUnit = settings.tempUnit
                                )
                            }

                            // 5. Daily Forecast 7 Days
                            state.data.daily?.let { daily ->
                                DailyForecastList(
                                    daily = daily,
                                    tempUnit = settings.tempUnit,
                                    onDayClick = { dayIndex ->
                                        viewModel.selectDayDetail(dayIndex)
                                        onNavigateToDayDetail(dayIndex)
                                    }
                                )
                            }

                            // Decorative visual banner
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = painterResource(id = R.drawable.weather_hero_banner),
                                        contentDescription = "Scenic Weather Illustration",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                                )
                                            )
                                    )
                                    Text(
                                        text = "MeteoTrack • Dự báo thời tiết & Di chuyển chuẩn xác",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    WeatherUiState.Idle -> {
                        // Empty idle
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAiDialog) {
        AiWeatherInsightsDialog(
            locationName = activeLocation.displayName,
            insightsText = aiInsights,
            isLoading = isLoadingAi,
            onDismiss = {
                showAiDialog = false
                viewModel.clearAiInsights()
            }
        )
    }
}
