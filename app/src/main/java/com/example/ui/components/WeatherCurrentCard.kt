package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrentWeather
import com.example.data.model.Units
import com.example.data.model.WeatherCodeMapper
import java.util.Locale

/**
 * Hero block of the home screen: the big temperature, a one-line summary and the key readings.
 *
 * @param todayHigh / [todayLow] today's range in °C, shown next to the feels-like value.
 */
@Composable
fun WeatherCurrentCard(
    current: CurrentWeather,
    todayHigh: Double?,
    todayLow: Double?,
    tempUnit: String,
    windUnit: String,
    onAiInsightsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val info = WeatherCodeMapper.getInfo(current.weatherCode, current.isDay == 1)
    val temp = Math.round(Units.temp(current.temperature2m, tempUnit))
    val feelsLike = Units.tempWithUnit(current.apparentTemperature, tempUnit)

    val range = if (todayHigh != null && todayLow != null) {
        "Cao ${Units.tempShort(todayHigh, tempUnit)} · Thấp ${Units.tempShort(todayLow, tempUnit)}"
    } else null

    val summary = "${info.title}, $temp độ ${if (tempUnit == "F") "F" else "C"}. " +
        "Cảm giác như ${Math.round(Units.temp(current.apparentTemperature, tempUnit))} độ."

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weather_current_card"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The whole headline reads as one sentence to screen readers; the emoji is purely decorative.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = summary },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = info.iconEmoji,
                fontSize = 56.sp,
                modifier = Modifier.clearAndSetSemantics { }
            )
            Text(
                text = buildAnnotatedString {
                    append("$temp")
                    withStyle(SpanStyle(fontSize = 32.sp, fontWeight = FontWeight.Normal)) {
                        append("°$tempUnit")
                    }
                },
                style = MaterialTheme.typography.displayLarge,
                color = Glass.OnGlass,
                modifier = Modifier.clearAndSetSemantics { }
            )
            Text(
                text = info.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Glass.OnGlass,
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics { }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = listOfNotNull(range, "Cảm giác $feelsLike").joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = Glass.OnGlassMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics { }
            )
            Text(
                text = info.description,
                style = MaterialTheme.typography.bodySmall,
                color = Glass.OnGlassMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clearAndSetSemantics { }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        GlassCard(contentPadding = 14.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Metric(Icons.Default.WaterDrop, "Độ ẩm", "${current.relativeHumidity2m}%", Modifier.weight(1f))
                    Metric(
                        Icons.Default.Air, "Gió",
                        buildString {
                            append(Units.wind(current.windSpeed10m, windUnit))
                            current.windDirection10m?.let { append(" · ").append(Units.windDirection(it)) }
                        },
                        Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Metric(
                        Icons.Default.Compress, "Áp suất",
                        current.surfacePressure?.let { "${it.toInt()} hPa" } ?: "—",
                        Modifier.weight(1f)
                    )
                    Metric(
                        Icons.Outlined.Umbrella, "Lượng mưa",
                        "%.1f mm".format(Locale.US, current.precipitation),
                        Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onAiInsightsClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Glass.FillStrong,
                contentColor = Glass.OnGlass
            ),
            modifier = Modifier.testTag("ai_insights_button")
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Glass.Accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Gợi ý cho hôm nay", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun Metric(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Glass.OnGlassMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Glass.OnGlassMuted)
            Text(text = value, style = MaterialTheme.typography.titleSmall, color = Glass.OnGlass)
        }
    }
}
