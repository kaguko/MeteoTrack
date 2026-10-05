package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.model.HourlyWeather
import com.example.data.model.WeatherCodeMapper

@Composable
fun HourlyForecastRow(
    hourly: HourlyWeather,
    tempUnit: String,
    modifier: Modifier = Modifier
) {
    // Show next 24 items
    val count = hourly.time.size.coerceAtMost(24)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hourly_forecast_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.16f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Dự báo theo giờ",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Dự báo 24 giờ tới",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(count) { index ->
                    val rawTime = hourly.time.getOrNull(index) ?: ""
                    val hourStr = formatHourString(rawTime, index == 0)
                    val rawTemp = hourly.temperature2m.getOrNull(index) ?: 0.0
                    val displayTemp = if (tempUnit == "F") (rawTemp * 9.0 / 5.0) + 32.0 else rawTemp
                    val weatherCode = hourly.weatherCode.getOrNull(index) ?: 0
                    val weatherInfo = WeatherCodeMapper.getInfo(weatherCode)
                    val rainProb = hourly.precipitationProbability?.getOrNull(index) ?: 0

                    HourlyItemCard(
                        hour = hourStr,
                        emoji = weatherInfo.iconEmoji,
                        temp = "${displayTemp.toInt()}°",
                        rainProb = rainProb,
                        isNow = index == 0
                    )
                }
            }
        }
    }
}

@Composable
private fun HourlyItemCard(
    hour: String,
    emoji: String,
    temp: String,
    rainProb: Int,
    isNow: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isNow) Color.White.copy(alpha = 0.32f)
                else Color.White.copy(alpha = 0.12f)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = hour,
                fontSize = 12.sp,
                fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = emoji,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = temp,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (rainProb > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💧$rainProb%",
                    fontSize = 10.sp,
                    color = Color(0xFFBAE6FD),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatHourString(isoString: String, isFirst: Boolean): String {
    if (isFirst) return "Bây giờ"
    return try {
        // Typically ISO like "2026-10-05T14:00"
        val parts = isoString.split("T")
        if (parts.size >= 2) {
            parts[1].take(5)
        } else {
            isoString
        }
    } catch (e: Exception) {
        isoString
    }
}
