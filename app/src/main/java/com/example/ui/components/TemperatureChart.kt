package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HourlyWeather
import com.example.data.model.Units
import com.example.data.model.hourLabel
import com.example.data.model.indexOfHour

private data class ChartPoint(val label: String, val temp: Double)

@Composable
fun TemperatureChart(
    hourly: HourlyWeather,
    currentTime: String?,
    tempUnit: String,
    modifier: Modifier = Modifier
) {
    val points = remember(hourly, currentTime, tempUnit) {
        val start = hourly.indexOfHour(currentTime)
        val end = (start + 24).coerceAtMost(hourly.time.size)
        // One sample every 3 hours keeps labels readable even at large font sizes.
        (start until end step 3).map { idx ->
            ChartPoint(
                label = if (idx == start) "Bây giờ" else hourLabel(hourly.time[idx]).orEmpty(),
                temp = Units.temp(hourly.temperature2m.getOrNull(idx) ?: 0.0, tempUnit)
            )
        }
    }
    if (points.size < 2) return

    val minTemp = points.minOf { it.temp }
    val maxTemp = points.maxOf { it.temp }
    val range = (maxTemp - minTemp).coerceAtLeast(2.0)

    val summary = "Biểu đồ nhiệt độ 24 giờ tới: thấp nhất ${Math.round(minTemp)} độ, " +
        "cao nhất ${Math.round(maxTemp)} độ."

    val textMeasurer = rememberTextMeasurer()
    val valueStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    val labelStyle = TextStyle(color = Glass.OnGlassMuted, fontSize = 12.sp)

    GlassCard(modifier = modifier.testTag("temperature_chart_card")) {
        SectionHeader(icon = Icons.AutoMirrored.Filled.ShowChart, title = "Xu hướng nhiệt độ (°$tempUnit)")
        Spacer(modifier = Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .semantics { contentDescription = summary }
                .testTag("temperature_chart_canvas")
        ) {
            val sidePad = 22.dp.toPx()
            val topPad = 26.dp.toPx()
            val bottomPad = 26.dp.toPx()
            val plotW = size.width - sidePad * 2
            val plotH = size.height - topPad - bottomPad
            val stepX = plotW / (points.size - 1)

            val xy = points.mapIndexed { i, p ->
                Offset(
                    x = sidePad + i * stepX,
                    y = topPad + plotH - ((p.temp - minTemp) / range).toFloat() * plotH
                )
            }

            fun Path.smoothThrough() {
                moveTo(xy.first().x, xy.first().y)
                for (i in 0 until xy.size - 1) {
                    val midX = (xy[i].x + xy[i + 1].x) / 2
                    cubicTo(midX, xy[i].y, midX, xy[i + 1].y, xy[i + 1].x, xy[i + 1].y)
                }
            }

            val area = Path().apply {
                smoothThrough()
                lineTo(xy.last().x, topPad + plotH)
                lineTo(xy.first().x, topPad + plotH)
                close()
            }
            drawPath(
                area,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.32f), Color.Transparent),
                    startY = topPad,
                    endY = topPad + plotH
                )
            )
            drawPath(
                Path().apply { smoothThrough() },
                color = Color.White,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            xy.forEachIndexed { i, pt ->
                drawCircle(Color.White, radius = 5.dp.toPx(), center = pt)
                drawCircle(Color(0xFF0C4A6E), radius = 2.5.dp.toPx(), center = pt)

                val value = textMeasurer.measure("${Math.round(points[i].temp)}°", valueStyle)
                drawText(
                    value,
                    topLeft = Offset(pt.x - value.size.width / 2f, pt.y - value.size.height - 8.dp.toPx())
                )
                val label = textMeasurer.measure(points[i].label, labelStyle)
                val labelX = (pt.x - label.size.width / 2f).coerceIn(0f, size.width - label.size.width)
                drawText(label, topLeft = Offset(labelX, size.height - label.size.height))
            }
        }
    }
}
