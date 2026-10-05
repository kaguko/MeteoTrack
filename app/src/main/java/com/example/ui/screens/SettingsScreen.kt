package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cài đặt ứng dụng",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Unit settings card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SettingSectionTitle(
                        icon = Icons.Default.Thermostat,
                        title = "Đơn vị đo lường"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Nhiệt độ:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = settings.tempUnit == "C",
                            onClick = { viewModel.setTempUnit("C") },
                            label = { Text("Độ C (°C)") },
                            modifier = Modifier.testTag("unit_temp_c")
                        )
                        FilterChip(
                            selected = settings.tempUnit == "F",
                            onClick = { viewModel.setTempUnit("F") },
                            label = { Text("Độ F (°F)") },
                            modifier = Modifier.testTag("unit_temp_f")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Tốc độ gió:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = settings.windUnit == "kmh",
                            onClick = { viewModel.setWindUnit("kmh") },
                            label = { Text("km/h") }
                        )
                        FilterChip(
                            selected = settings.windUnit == "ms",
                            onClick = { viewModel.setWindUnit("ms") },
                            label = { Text("m/s") }
                        )
                        FilterChip(
                            selected = settings.windUnit == "mph",
                            onClick = { viewModel.setWindUnit("mph") },
                            label = { Text("mph") }
                        )
                    }
                }
            }

            // Movement & Auto-refresh settings card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SettingSectionTitle(
                        icon = Icons.Default.Navigation,
                        title = "Theo dõi di chuyển (Core Feature)"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tự động làm mới khi di chuyển",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Cập nhật thời tiết khi đi vượt ngưỡng khoảng cách",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = settings.autoRefreshOnMove,
                            onCheckedChange = { viewModel.setAutoRefresh(it) },
                            modifier = Modifier.testTag("settings_auto_refresh_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Khoảng cách tối thiểu để cập nhật:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1.0, 3.0, 5.0, 10.0).forEach { km ->
                            FilterChip(
                                selected = settings.minDistanceKm == km,
                                onClick = { viewModel.setMinDistance(km) },
                                label = { Text("${km.toInt()} km") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Thời gian tối thiểu giữa các lần làm mới:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 60).forEach { mins ->
                            FilterChip(
                                selected = settings.minTimeMinutes == mins,
                                onClick = { viewModel.setMinTime(mins) },
                                label = { Text("$mins phút") }
                            )
                        }
                    }
                }
            }

            // Notification settings card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SettingSectionTitle(
                        icon = Icons.Default.Notifications,
                        title = "Thông báo cảnh báo"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bật thông báo thời tiết",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Nhận thông báo khi thời tiết vượt ngưỡng đã cài đặt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = settings.notificationsEnabled,
                            onCheckedChange = { viewModel.setNotifications(it) }
                        )
                    }
                }
            }

            // About app card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SettingSectionTitle(
                        icon = Icons.Default.Info,
                        title = "Về ứng dụng MeteoTrack"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Dữ liệu thời tiết chính xác theo thời gian thực từ Open-Meteo API (chuẩn quốc tế WMO).\n" +
                                "• Gợi ý địa điểm & Google Maps grounding thông minh qua mô hình Gemini 2.5 Flash.\n" +
                                "• Tự động tính toán khoảng cách di chuyển GPS bằng giải thuật Haversine trên Android.\n" +
                                "• Phiên bản 1.0 (Bản hoàn thiện đầy đủ tính năng).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingSectionTitle(
    icon: ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
