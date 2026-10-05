package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyWeather
import com.example.data.model.WeatherCodeMapper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailyForecastList(
    daily: DailyWeather,
    tempUnit: String,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = daily.time.size.coerceAtMost(7)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_forecast_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.16f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Dự báo 7 ngày tới",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Dự báo 7 ngày tới (chạm để xem chi tiết)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 0 until count) {
                    val rawDate = daily.time.getOrNull(i) ?: ""
                    val dayLabel = formatDayLabel(rawDate, i == 0)
                    val code = daily.weatherCode.getOrNull(i) ?: 0
                    val weatherInfo = WeatherCodeMapper.getInfo(code)

                    val rawMin = daily.temperature2mMin.getOrNull(i) ?: 0.0
                    val rawMax = daily.temperature2mMax.getOrNull(i) ?: 0.0
                    val displayMin = if (tempUnit == "F") (rawMin * 9.0 / 5.0) + 32.0 else rawMin
                    val displayMax = if (tempUnit == "F") (rawMax * 9.0 / 5.0) + 32.0 else rawMax

                    val rainProb = daily.precipitationProbabilityMax?.getOrNull(i) ?: 0

                    DailyItemRow(
                        dayLabel = dayLabel,
                        emoji = weatherInfo.iconEmoji,
                        title = weatherInfo.title,
                        minTemp = "${displayMin.toInt()}°",
                        maxTemp = "${displayMax.toInt()}°",
                        rainProb = rainProb,
                        onClick = { onDayClick(i) }
                    )

                    if (i < count - 1) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.1f),
                            thickness = 0.8.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyItemRow(
    dayLabel: String,
    emoji: String,
    title: String,
    minTemp: String,
    maxTemp: String,
    rainProb: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.width(90.dp)
        )

        Text(
            text = emoji,
            fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1
            )
            if (rainProb > 0) {
                Text(
                    text = "💧 Khả năng mưa: $rainProb%",
                    fontSize = 11.sp,
                    color = Color(0xFFBAE6FD)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = minTemp,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = " / ",
                color = Color.White.copy(alpha = 0.4f)
            )
            Text(
                text = maxTemp,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Chi tiết",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

private fun formatDayLabel(isoDate: String, isFirst: Boolean): String {
    if (isFirst) return "Hôm nay"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = parser.parse(isoDate) ?: return isoDate
        val formatter = SimpleDateFormat("EEEE", Locale("vi", "VN"))
        formatter.format(date).replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        isoDate
    }
}
