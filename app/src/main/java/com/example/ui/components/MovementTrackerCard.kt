package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Why the card is (not) tracking, so the subtitle can be honest about it. */
enum class TrackingStatus { ACTIVE, PAUSED, NEEDS_PERMISSION, VIEWING_OTHER_PLACE }

@Composable
fun MovementTrackerCard(
    distanceKm: Double,
    thresholdKm: Double,
    status: TrackingStatus,
    /** Whether tracking is switched on (it may still be inactive, e.g. while viewing another city). */
    isOn: Boolean,
    onToggleTracking: () -> Unit,
    modifier: Modifier = Modifier,
    /** Debug-only shortcut; pass null in release builds. */
    onSimulateMove: (() -> Unit)? = null
) {
    val isActive = status == TrackingStatus.ACTIVE
    val progress = (distanceKm / thresholdKm).coerceIn(0.0, 1.0).toFloat()
    val threshold = "%.0f".format(Locale.US, thresholdKm)

    val subtitle = when (status) {
        TrackingStatus.ACTIVE -> "Cập nhật khi bạn đi quá $threshold km"
        TrackingStatus.PAUSED -> "Đang tạm dừng"
        TrackingStatus.NEEDS_PERMISSION -> "Cần quyền vị trí để hoạt động"
        TrackingStatus.VIEWING_OTHER_PLACE -> "Chỉ áp dụng khi xem vị trí GPS của bạn"
    }
    val trackingEnabled = status != TrackingStatus.NEEDS_PERMISSION

    GlassCard(modifier = modifier.testTag("movement_tracker_card")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = isOn,
                    enabled = trackingEnabled,
                    role = Role.Switch,
                    onValueChange = { onToggleTracking() }
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = "Tự làm mới khi di chuyển. $subtitle"
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color(0x3322C55E) else Color(0x24FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = if (isActive) Color(0xFF86EFAC) else Glass.OnGlass,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tự làm mới khi di chuyển",
                    style = MaterialTheme.typography.titleSmall,
                    color = Glass.OnGlass
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Glass.OnGlassMuted
                )
            }
            Switch(
                checked = isOn,
                onCheckedChange = null,
                enabled = trackingEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF16A34A),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0x4DFFFFFF),
                    uncheckedBorderColor = Color(0x80FFFFFF)
                )
            )
        }

        if (isActive) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Đã di chuyển %.1f km".format(Locale.US, distanceKm),
                    style = MaterialTheme.typography.labelMedium,
                    color = Glass.OnGlass
                )
                Text(
                    text = "Ngưỡng $threshold km",
                    style = MaterialTheme.typography.labelMedium,
                    color = Glass.OnGlassMuted
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .semantics { contentDescription = "Đã đi ${(progress * 100).toInt()}% quãng đường để làm mới" },
                color = if (progress >= 1f) Glass.Accent else Color(0xFF7DD3FC),
                trackColor = Color(0x33FFFFFF),
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }

        if (onSimulateMove != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSimulateMove,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("simulate_move_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Glass.OnGlass)
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Thử đi +5,5 km (chỉ bản debug)")
            }
        }
    }
}
