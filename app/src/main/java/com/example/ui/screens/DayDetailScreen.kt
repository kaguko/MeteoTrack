package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherCodeMapper
import com.example.ui.viewmodel.WeatherUiState
import com.example.ui.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    viewModel: WeatherViewModel,
    dayIndex: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val activeLoc by viewModel.activeLocation.collectAsState()

    val daily = (uiState as? WeatherUiState.Success)?.data?.daily

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chi tiết dự báo thời tiết",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("day_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (daily == null || dayIndex !in daily.time.indices) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Không tìm thấy dữ liệu ngày đã chọn.")
            }
            return@Scaffold
        }

        val rawDate = daily.time[dayIndex]
        val formattedDate = formatFullDate(rawDate)
        val code = daily.weatherCode[dayIndex]
        val weatherInfo = WeatherCodeMapper.getInfo(code)

        val rawMin = daily.temperature2mMin[dayIndex]
        val rawMax = daily.temperature2mMax[dayIndex]
        val displayMin = if (settings.tempUnit == "F") (rawMin * 9.0 / 5.0) + 32.0 else rawMin
        val displayMax = if (settings.tempUnit == "F") (rawMax * 9.0 / 5.0) + 32.0 else rawMax

        val uvMax = daily.uvIndexMax?.getOrNull(dayIndex) ?: 5.0
        val precipSum = daily.precipitationSum?.getOrNull(dayIndex) ?: 0.0
        val precipProb = daily.precipitationProbabilityMax?.getOrNull(dayIndex) ?: 0
        val windMax = daily.windSpeed10mMax?.getOrNull(dayIndex) ?: 12.0
        val sunrise = daily.sunrise?.getOrNull(dayIndex)?.split("T")?.getOrNull(1)?.take(5) ?: "06:00"
        val sunset = daily.sunset?.getOrNull(dayIndex)?.split("T")?.getOrNull(1)?.take(5) ?: "18:00"

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = activeLoc.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = weatherInfo.iconEmoji,
                        fontSize = 54.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = weatherInfo.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${displayMin.toInt()}°${settings.tempUnit}  —  ${displayMax.toInt()}°${settings.tempUnit}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Key Metrics Grid
            Text(
                text = "Các chỉ số chi tiết trong ngày",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricDetailCard(
                    title = "Bình minh",
                    value = sunrise,
                    subtitle = "Mặt trời mọc",
                    icon = Icons.Default.WbSunny,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                MetricDetailCard(
                    title = "Hoàng hôn",
                    value = sunset,
                    subtitle = "Mặt trời lặn",
                    icon = Icons.Default.WbTwilight,
                    tint = Color(0xFFEA580C),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricDetailCard(
                    title = "Chỉ số UV cực đại",
                    value = "%.1f".format(uvMax),
                    subtitle = getUvAdvice(uvMax),
                    icon = Icons.Default.Thermostat,
                    tint = if (uvMax >= 7) Color(0xFFDC2626) else Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                MetricDetailCard(
                    title = "Lượng mưa & Xác suất",
                    value = "%.1f mm".format(precipSum),
                    subtitle = "Xác suất mưa: $precipProb%",
                    icon = Icons.Default.Thermostat,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
            }

            MetricDetailCard(
                title = "Tốc độ gió giật tối đa",
                value = "%.1f km/h".format(windMax),
                subtitle = if (windMax > 30) "Gió mạnh, lái xe cẩn trọng" else "Gió thoảng dễ chịu",
                icon = Icons.Default.Air,
                tint = Color(0xFF0D9488),
                modifier = Modifier.fillMaxWidth()
            )

            // Advice card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 Lời khuyên di chuyển & sinh hoạt:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = buildAdviceText(weatherInfo.isRaining, uvMax, displayMax),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetricDetailCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getUvAdvice(uv: Double): String {
    return when {
        uv < 3 -> "Mức thấp • An toàn"
        uv < 6 -> "Trung bình • Cần mũ nón"
        uv < 8 -> "Cao • Bôi kem chống nắng"
        else -> "Rất nguy hiểm • Tránh nắng gắt"
    }
}

private fun buildAdviceText(isRaining: Boolean, uv: Double, maxTemp: Double): String {
    val items = mutableListOf<String>()
    if (isRaining) {
        items.add("• Mang theo áo mưa bộ hoặc ô dù.")
        items.add("• Giảm tốc độ khi lái xe trên đường ướt.")
    } else {
        items.add("• Thời tiết thích hợp cho các hoạt động ngoài trời.")
    }

    if (uv >= 6) {
        items.add("• Đeo kính râm, che chắn chống tia UV từ 10h - 16h.")
    }

    if (maxTemp >= 34) {
        items.add("• Nhiệt độ cao, hãy uống đủ nước và tránh ở ngoài trời quá lâu.")
    }

    return items.joinToString("\n")
}

private fun formatFullDate(isoDate: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = parser.parse(isoDate) ?: return isoDate
        val formatter = SimpleDateFormat("EEEE, 'ngày' dd 'tháng' MM", Locale("vi", "VN"))
        formatter.format(date).replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        isoDate
    }
}
