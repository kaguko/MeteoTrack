package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherCodeMapper
import com.example.ui.components.CenteredContent
import com.example.ui.components.Glass

/**
 * First-run welcome. Explains *why* the permissions are needed before the system dialog appears,
 * and works fine if the user skips it (the app then falls back to a default city).
 */
@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val info = WeatherCodeMapper.getInfo(code = 1)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(info.backgroundBrush)
            .safeDrawingPadding()
    ) {
        CenteredContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.size(24.dp))
                Text("🌤️", fontSize = 72.sp, modifier = Modifier.clearAndSetSemantics { })
                Spacer(Modifier.size(12.dp))
                Text(
                    "Chào mừng đến với MeteoTrack",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Glass.OnGlass,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Thời tiết luôn đúng nơi bạn đang đứng.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Glass.OnGlassMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.size(32.dp))

                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Feature(
                        Icons.Default.WbSunny,
                        "Dự báo chính xác",
                        "Xem thời tiết hiện tại, 24 giờ và 7 ngày tới tại vị trí của bạn."
                    )
                    Feature(
                        Icons.Default.Navigation,
                        "Tự cập nhật khi bạn di chuyển",
                        "Đi xa hơn vài km, dự báo sẽ tự làm mới — không cần mở lại ứng dụng."
                    )
                    Feature(
                        Icons.Default.NotificationsActive,
                        "Cảnh báo theo ý bạn",
                        "Đặt ngưỡng nóng, lạnh, mưa, gió và nhận thông báo kịp thời."
                    )
                }

                Spacer(Modifier.size(32.dp))

                Text(
                    "Để làm được điều này, MeteoTrack cần quyền Vị trí và Thông báo. " +
                        "Vị trí chỉ dùng để lấy dự báo và không gắn với tài khoản nào.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Glass.OnGlassMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.size(20.dp))

                Button(
                    onClick = onContinue,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0B2A5B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("onboarding_continue")
                ) { Text("Tiếp tục", style = MaterialTheme.typography.titleMedium) }

                TextButton(
                    onClick = onSkip,
                    colors = ButtonDefaults.textButtonColors(contentColor = Glass.OnGlass),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("onboarding_skip")
                ) { Text("Để sau — dùng thử với Hà Nội", textAlign = TextAlign.Center) }
            }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$title. $body" },
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = Glass.Accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Glass.OnGlass)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Glass.OnGlassMuted)
        }
    }
}
