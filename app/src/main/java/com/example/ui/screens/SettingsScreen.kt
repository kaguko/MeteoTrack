package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.components.CenteredContent
import com.example.ui.components.InfoBanner
import com.example.ui.openAppSettings
import com.example.ui.openNotificationSettings
import com.example.ui.rememberNotificationPermission
import com.example.ui.viewmodel.WeatherViewModel

private const val PRIVACY_POLICY_URL = "https://kaguko.github.io/MeteoTrack/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hasNotificationPermission by rememberNotificationPermission()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Cài đặt", style = MaterialTheme.typography.titleLarge) }) }
    ) { innerPadding ->
        CenteredContent(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsCard(icon = Icons.Default.Thermostat, title = "Đơn vị") {
                    ChoiceRow(
                        label = "Nhiệt độ",
                        options = listOf("C" to "°C", "F" to "°F"),
                        selected = settings.tempUnit,
                        onSelect = viewModel::setTempUnit,
                        tagPrefix = "unit_temp"
                    )
                    ChoiceRow(
                        label = "Tốc độ gió",
                        options = listOf("kmh" to "km/h", "ms" to "m/s", "mph" to "mph"),
                        selected = settings.windUnit,
                        onSelect = viewModel::setWindUnit
                    )
                }

                SettingsCard(icon = Icons.Default.Navigation, title = "Tự làm mới khi di chuyển") {
                    SwitchRow(
                        title = "Cập nhật thời tiết khi tôi di chuyển",
                        subtitle = "Dự báo mới được tải khi bạn đi xa hoặc sau một khoảng thời gian.",
                        checked = settings.autoRefreshOnMove,
                        onCheckedChange = viewModel::setAutoRefresh,
                        modifier = Modifier.testTag("settings_auto_refresh_switch")
                    )
                    ChoiceRow(
                        label = "Khi đi quá",
                        options = listOf(1.0 to "1 km", 3.0 to "3 km", 5.0 to "5 km", 10.0 to "10 km"),
                        selected = settings.minDistanceKm,
                        onSelect = viewModel::setMinDistance,
                        enabled = settings.autoRefreshOnMove
                    )
                    ChoiceRow(
                        label = "Hoặc sau",
                        options = listOf(15 to "15 phút", 30 to "30 phút", 60 to "60 phút"),
                        selected = settings.minTimeMinutes,
                        onSelect = viewModel::setMinTime,
                        enabled = settings.autoRefreshOnMove
                    )
                }

                SettingsCard(icon = Icons.Default.Notifications, title = "Thông báo") {
                    if (!hasNotificationPermission) {
                        InfoBanner(
                            text = "Thông báo đang bị tắt trên thiết bị. Bật lại để nhận cảnh báo thời tiết.",
                            icon = Icons.Default.NotificationsOff,
                            actionLabel = "Mở cài đặt",
                            onAction = { context.openNotificationSettings() },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    SwitchRow(
                        title = "Cảnh báo thời tiết",
                        subtitle = "Nhận thông báo khi dự báo vượt các ngưỡng bạn đã đặt ở tab Cảnh báo.",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = viewModel::setNotifications
                    )
                }

                SettingsCard(icon = Icons.Default.PrivacyTip, title = "Quyền riêng tư") {
                    Text(
                        text = "Vị trí của bạn chỉ được dùng để lấy dự báo: tọa độ gần đúng được gửi tới Open-Meteo " +
                            "và không gắn với tài khoản nào. Nhật ký di chuyển, địa điểm đã lưu và cảnh báo " +
                            "chỉ nằm trên thiết bị này.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { context.openAppSettings() },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Quản lý quyền của ứng dụng") }
                    OutlinedButton(
                        onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Chính sách quyền riêng tư") }
                }

                SettingsCard(icon = Icons.Default.Info, title = "Về MeteoTrack") {
                    Text(
                        text = "Phiên bản ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Dữ liệu thời tiết và tìm kiếm địa điểm do Open-Meteo.com cung cấp " +
                            "theo giấy phép CC BY 4.0.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://open-meteo.com/") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Mở open-meteo.com") }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            }
            content()
        }
    }
}

/** A whole-row switch: the label, the description and the control are one 48dp+ touch target. */
@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, modifier = modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tagPrefix: String? = null
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, text) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    modifier = if (tagPrefix != null) Modifier.testTag("${tagPrefix}_${text.trim('°').lowercase()}") else Modifier
                ) { Text(text, maxLines = 1) }
            }
        }
    }
}
