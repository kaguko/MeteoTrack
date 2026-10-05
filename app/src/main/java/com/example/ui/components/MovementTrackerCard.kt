package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import java.util.Locale

@Composable
fun MovementTrackerCard(
    distanceKm: Double,
    thresholdKm: Double,
    isTrackingActive: Boolean,
    onToggleTracking: () -> Unit,
    onSimulateMove: (Double) -> Unit,
    onManualRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (distanceKm / thresholdKm).coerceIn(0.0, 1.0).toFloat()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("movement_tracker_card"),
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isTrackingActive) Color(0xFF10B981).copy(alpha = 0.3f)
                                else Color.White.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Movement Tracker",
                            tint = if (isTrackingActive) Color(0xFF10B981) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Tự động làm mới khi di chuyển",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isTrackingActive) "Đang theo dõi GPS (ngưỡng >${thresholdKm.toInt()}km)" else "Đang tạm dừng theo dõi",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Switch(
                    checked = isTrackingActive,
                    onCheckedChange = { onToggleTracking() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF10B981),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("toggle_tracking_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar showing distance moved towards threshold
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Khoảng cách đã đi: %.2f km".format(Locale.US, distanceKm),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Mục tiêu: %.1f km".format(Locale.US, thresholdKm),
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (progress >= 1f) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )

                if (distanceKm >= thresholdKm) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✓ Đã đủ ngưỡng di chuyển! Thời tiết tự động làm mới.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFDE047)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions: Simulate Move & Manual Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onManualRefresh,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manual_refresh_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Làm mới",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cập nhật ngay", fontSize = 12.sp)
                }

                Button(
                    onClick = { onSimulateMove(5.5) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_move_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.28f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Mô phỏng",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFD54F)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thử đi +5.5km", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
