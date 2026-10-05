package com.example.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HourlyWeather

@Composable
fun TemperatureChart(
    hourly: HourlyWeather,
    tempUnit: String,
    modifier: Modifier = Modifier
) {
    // Take 12 intervals (every 2 hours for a clean chart)
    val pointsCount = hourly.time.size.coerceAtMost(24)
    val step = (pointsCount / 8).coerceAtLeast(1)
    val selectedIndices = (0 until pointsCount step step).take(8)

    val temps = remember(hourly, tempUnit) {
        selectedIndices.map { idx ->
            val raw = hourly.temperature2m.getOrNull(idx) ?: 0.0
            if (tempUnit == "F") (raw * 9.0 / 5.0) + 32.0 else raw
        }
    }

    val labels = remember(hourly) {
        selectedIndices.mapIndexed { i, idx ->
            val rawTime = hourly.time.getOrNull(idx) ?: ""
            if (i == 0) "Hiện tại"
            else {
                val parts = rawTime.split("T")
                if (parts.size >= 2) parts[1].take(5) else rawTime
            }
        }
    }

    if (temps.isEmpty()) return

    val minTemp = (temps.minOrNull() ?: 20.0).toFloat()
    val maxTemp = (temps.maxOrNull() ?: 30.0).toFloat()
    val tempRange = (maxTemp - minTemp).coerceAtLeast(2f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("temperature_chart_card"),
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
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = "Biểu đồ nhiệt độ",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Biểu đồ biến thiên nhiệt độ (°$tempUnit)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .testTag("temperature_chart_canvas")
            ) {
                val width = size.width
                val height = size.height
                val topPadding = 30f
                val bottomPadding = 40f
                val availableHeight = height - topPadding - bottomPadding

                val stepX = width / (temps.size - 1).coerceAtLeast(1)

                val points = temps.mapIndexed { index, temp ->
                    val x = index * stepX
                    val normalizedY = (temp.toFloat() - minTemp) / tempRange
                    val y = height - bottomPadding - (normalizedY * availableHeight)
                    Pair(x, y)
                }

                // Draw filled gradient area below the curve
                val fillPath = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points[0].first, height - bottomPadding)
                        lineTo(points[0].first, points[0].second)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlX1 = p0.first + (p1.first - p0.first) / 2
                            val controlY1 = p0.second
                            val controlX2 = controlX1
                            val controlY2 = p1.second
                            cubicTo(controlX1, controlY1, controlX2, controlY2, p1.first, p1.second)
                        }

                        lineTo(points.last().first, height - bottomPadding)
                        close()
                    }
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        startY = topPadding,
                        endY = height - bottomPadding
                    )
                )

                // Draw smooth curve stroke
                val strokePath = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points[0].first, points[0].second)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlX1 = p0.first + (p1.first - p0.first) / 2
                            val controlY1 = p0.second
                            val controlX2 = controlX1
                            val controlY2 = p1.second
                            cubicTo(controlX1, controlY1, controlX2, controlY2, p1.first, p1.second)
                        }
                    }
                }

                drawPath(
                    path = strokePath,
                    color = Color.White,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw points and labels
                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 11.sp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                val subTextPaint = Paint().apply {
                    color = android.graphics.Color.argb(200, 255, 255, 255)
                    textSize = 10.sp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                points.forEachIndexed { index, point ->
                    // Point circle
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(point.first, point.second)
                    )
                    drawCircle(
                        color = Color(0xFF0284C7),
                        radius = 2.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(point.first, point.second)
                    )

                    // Draw temp value text above point
                    val tempStr = "${temps[index].toInt()}°"
                    drawContext.canvas.nativeCanvas.drawText(
                        tempStr,
                        point.first,
                        point.second - 10f,
                        textPaint
                    )

                    // Draw hour label text below bottom padding
                    val labelStr = labels.getOrElse(index) { "" }
                    drawContext.canvas.nativeCanvas.drawText(
                        labelStr,
                        point.first,
                        height - 8f,
                        subTextPaint
                    )
                }
            }
        }
    }
}
