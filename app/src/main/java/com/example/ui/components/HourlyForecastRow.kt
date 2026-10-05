package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.HourlyWeather
import com.example.data.model.Units
import com.example.data.model.WeatherCodeMapper
import com.example.data.model.hourLabel
import com.example.data.model.indexOfHour

private data class HourItem(
    val label: String,
    val emoji: String,
    val tempText: String,
    val spokenTemp: String,
    val rainProb: Int,
    val isNow: Boolean
)

@Composable
fun HourlyForecastRow(
    hourly: HourlyWeather,
    currentTime: String?,
    tempUnit: String,
    modifier: Modifier = Modifier
) {
    val items = remember(hourly, currentTime, tempUnit) {
        val start = hourly.indexOfHour(currentTime)
        val end = (start + 24).coerceAtMost(hourly.time.size)
        (start until end).map { index ->
            val temp = hourly.temperature2m.getOrNull(index) ?: 0.0
            HourItem(
                label = if (index == start) "Bây giờ" else hourLabel(hourly.time[index]) ?: "",
                emoji = WeatherCodeMapper.getInfo(hourly.weatherCode.getOrNull(index) ?: 0).iconEmoji,
                tempText = Units.tempShort(temp, tempUnit),
                spokenTemp = "${Math.round(Units.temp(temp, tempUnit))} độ",
                rainProb = hourly.precipitationProbability?.getOrNull(index) ?: 0,
                isNow = index == start
            )
        }
    }

    GlassCard(
        modifier = modifier.testTag("hourly_forecast_card"),
        contentPadding = 0.dp
    ) {
        SectionHeader(
            icon = Icons.Default.Schedule,
            title = "24 giờ tới",
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { HourlyItemCard(it) }
        }
    }
}

@Composable
private fun HourlyItemCard(item: HourItem) {
    val spoken = buildString {
        append(item.label).append(", ").append(item.spokenTemp)
        if (item.rainProb > 0) append(", khả năng mưa ${item.rainProb}%")
    }
    Column(
        modifier = Modifier
            .widthIn(min = 64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (item.isNow) Color(0x40FFFFFF) else Color(0x14FFFFFF))
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (item.isNow) FontWeight.Bold else FontWeight.Medium,
            color = Glass.OnGlass,
            modifier = Modifier.clearAndSetSemantics { }
        )
        Text(
            text = item.emoji,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.clearAndSetSemantics { }
        )
        Text(
            text = item.tempText,
            style = MaterialTheme.typography.titleMedium,
            color = Glass.OnGlass,
            modifier = Modifier.clearAndSetSemantics { }
        )
        // Reserve the line even when dry so every tile has the same height.
        Text(
            text = if (item.rainProb > 0) "💧${item.rainProb}%" else " ",
            style = MaterialTheme.typography.labelSmall,
            color = Glass.OnGlassMuted,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}
