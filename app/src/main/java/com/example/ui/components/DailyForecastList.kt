package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyWeather
import com.example.data.model.Units
import com.example.data.model.WeatherCodeMapper
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DailyForecastList(
    daily: DailyWeather,
    tempUnit: String,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = daily.time.size.coerceAtMost(7)
    val weekMin = remember(daily) { daily.temperature2mMin.take(count).minOrNull() ?: 0.0 }
    val weekMax = remember(daily) { daily.temperature2mMax.take(count).maxOrNull() ?: 0.0 }

    GlassCard(
        modifier = modifier.testTag("daily_forecast_card"),
        contentPadding = 0.dp
    ) {
        SectionHeader(
            icon = Icons.Default.CalendarMonth,
            title = "7 ngày tới",
            trailing = "Chạm để xem chi tiết",
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 4.dp)
        )

        for (i in 0 until count) {
            val label = dayLabel(daily.time.getOrNull(i).orEmpty(), isToday = i == 0)
            val info = WeatherCodeMapper.getInfo(daily.weatherCode.getOrNull(i) ?: 0)
            val min = daily.temperature2mMin.getOrNull(i) ?: 0.0
            val max = daily.temperature2mMax.getOrNull(i) ?: 0.0
            val rain = daily.precipitationProbabilityMax?.getOrNull(i) ?: 0

            DailyItemRow(
                dayLabel = label,
                emoji = info.iconEmoji,
                title = info.title,
                min = min,
                max = max,
                weekMin = weekMin,
                weekMax = weekMax,
                tempUnit = tempUnit,
                rainProb = rain,
                onClick = { onDayClick(i) }
            )
            if (i < count - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    color = Color.White.copy(alpha = 0.12f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun DailyItemRow(
    dayLabel: String,
    emoji: String,
    title: String,
    min: Double,
    max: Double,
    weekMin: Double,
    weekMax: Double,
    tempUnit: String,
    rainProb: Int,
    onClick: () -> Unit
) {
    val spoken = buildString {
        append(dayLabel).append(", ").append(title)
        append(", thấp nhất ${Math.round(Units.temp(min, tempUnit))} độ")
        append(", cao nhất ${Math.round(Units.temp(max, tempUnit))} độ")
        if (rainProb > 0) append(", khả năng mưa $rainProb%")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClickLabel = "Xem chi tiết", onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.1f)) {
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.titleSmall,
                color = Glass.OnGlass,
                modifier = Modifier.clearAndSetSemantics { }
            )
            if (rainProb > 0) {
                Text(
                    text = "💧 $rainProb%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Glass.OnGlassMuted,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }
        }

        Text(
            text = emoji,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .clearAndSetSemantics { }
        )

        Text(
            text = Units.tempShort(min, tempUnit),
            style = MaterialTheme.typography.bodyMedium,
            color = Glass.OnGlassMuted,
            modifier = Modifier
                .width(36.dp)
                .clearAndSetSemantics { },
            maxLines = 1
        )
        TemperatureRangeBar(
            min = min, max = max, weekMin = weekMin, weekMax = weekMax,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
        )
        Text(
            text = Units.tempShort(max, tempUnit),
            style = MaterialTheme.typography.titleSmall,
            color = Glass.OnGlass,
            modifier = Modifier
                .width(36.dp)
                .clearAndSetSemantics { },
            maxLines = 1
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Glass.OnGlassMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Where today's min–max sits inside the week's range. */
@Composable
private fun TemperatureRangeBar(
    min: Double,
    max: Double,
    weekMin: Double,
    weekMax: Double,
    modifier: Modifier = Modifier
) {
    val span = (weekMax - weekMin).coerceAtLeast(1.0)
    val start = ((min - weekMin) / span).toFloat().coerceIn(0f, 1f)
    val end = ((max - weekMin) / span).toFloat().coerceIn(start, 1f)

    Box(
        modifier = modifier
            .height(6.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .clearAndSetSemantics { }
    ) {
        Row(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
            if (start > 0f) Spacer(modifier = Modifier.weight(start))
            Box(
                modifier = Modifier
                    .weight((end - start).coerceAtLeast(0.08f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF7DD3FC), Glass.Accent)))
            )
            if (end < 1f) Spacer(modifier = Modifier.weight(1f - end))
        }
    }
}

internal fun dayLabel(isoDate: String, isToday: Boolean): String {
    if (isToday) return "Hôm nay"
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate) ?: return isoDate
        SimpleDateFormat("EEEE", Locale.forLanguageTag("vi-VN")).format(date).replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        isoDate
    }
}
