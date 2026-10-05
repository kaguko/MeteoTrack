package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TravelHistoryEntity
import com.example.data.local.WeatherAlertEntity
import com.example.data.model.Units
import com.example.data.model.WeatherCodeMapper
import com.example.ui.components.CenteredContent
import com.example.ui.components.EmptyState
import com.example.ui.components.InfoBanner
import com.example.ui.openNotificationSettings
import com.example.ui.rememberNotificationPermission
import com.example.ui.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AlertType(
    val key: String,
    val chipLabel: String,
    val defaultName: String,
    val defaultThreshold: String,
    val unit: String,
    val min: Double,
    val max: Double,
    val icon: ImageVector
) {
    TEMP_HIGH("TEMP_HIGH", "Nóng", "Nắng nóng", "35", "°C", -50.0, 60.0, Icons.Default.Thermostat),
    TEMP_LOW("TEMP_LOW", "Lạnh", "Trời lạnh", "16", "°C", -50.0, 60.0, Icons.Default.Thermostat),
    RAIN_CHANCE("RAIN_CHANCE", "Mưa", "Khả năng mưa cao", "70", "%", 0.0, 100.0, Icons.Default.WaterDrop),
    WIND_HIGH("WIND_HIGH", "Gió", "Gió mạnh", "30", "km/h", 1.0, 200.0, Icons.Default.Air);

    companion object {
        fun from(key: String) = entries.firstOrNull { it.key == key }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsAndHistoryScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showClearConfirm by rememberSaveable { mutableStateOf(false) }

    val alerts by viewModel.weatherAlerts.collectAsStateWithLifecycle()
    val history by viewModel.travelHistory.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Cảnh báo & nhật ký", style = MaterialTheme.typography.titleLarge) }) }
    ) { innerPadding ->
        CenteredContent(modifier = Modifier.padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Cảnh báo (${alerts.size})") },
                        icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Nhật ký (${history.size})") },
                        icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                }

                if (selectedTab == 0) {
                    AlertsTab(
                        alerts = alerts,
                        notificationsEnabled = settings.notificationsEnabled,
                        onToggle = { alert, enabled -> viewModel.toggleAlertEnabled(alert, enabled) },
                        onDelete = { viewModel.deleteAlert(it) },
                        onAddNew = { showAddDialog = true },
                        onTestNotification = {
                            viewModel.testAlertNotification(
                                "Thông báo thử nghiệm",
                                "Thông báo cảnh báo thời tiết đang hoạt động bình thường."
                            )
                        }
                    )
                } else {
                    HistoryTab(
                        history = history,
                        tempUnit = settings.tempUnit,
                        thresholdKm = settings.minDistanceKm,
                        onClearAll = { showClearConfirm = true }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddAlertDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, threshold ->
                viewModel.addNewAlert(name, type, threshold)
                showAddDialog = false
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Xóa toàn bộ nhật ký?") },
            text = { Text("${history.size} điểm đến đã ghi sẽ bị xóa và không thể khôi phục.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_clear_history")
                ) { Text("Xóa hết", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Hủy") } }
        )
    }
}

// ------------------------------------------------------------------------------------ alerts

@Composable
private fun AlertsTab(
    alerts: List<WeatherAlertEntity>,
    notificationsEnabled: Boolean,
    onToggle: (WeatherAlertEntity, Boolean) -> Unit,
    onDelete: (WeatherAlertEntity) -> Unit,
    onAddNew: () -> Unit,
    onTestNotification: () -> Unit
) {
    val context = LocalContext.current
    val hasPermission by rememberNotificationPermission()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!hasPermission || !notificationsEnabled) {
            item {
                InfoBanner(
                    text = if (!hasPermission) "Thông báo đang bị tắt trên thiết bị nên bạn sẽ không nhận được cảnh báo."
                    else "Bạn đã tắt thông báo thời tiết trong Cài đặt.",
                    icon = Icons.Default.NotificationsOff,
                    actionLabel = if (!hasPermission) "Bật" else null,
                    onAction = if (!hasPermission) ({ context.openNotificationSettings() }) else null,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onAddNew,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("add_new_alert_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Thêm cảnh báo")
                }
                OutlinedButton(
                    onClick = onTestNotification,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("test_notification_button")
                ) { Text("Gửi thử") }
            }
        }

        if (alerts.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.NotificationsActive,
                    title = "Chưa có cảnh báo nào",
                    message = "Đặt ngưỡng nhiệt độ, mưa hoặc gió, MeteoTrack sẽ báo ngay khi dự báo vượt ngưỡng."
                )
            }
        } else {
            items(alerts, key = { it.id }) { alert ->
                AlertItemCard(alert = alert, onToggle = { onToggle(alert, it) }, onDelete = { onDelete(alert) })
            }
        }
    }
}

@Composable
private fun AlertItemCard(
    alert: WeatherAlertEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val type = AlertType.from(alert.type)
    val dark = isSystemInDarkTheme()
    val tint = when (type) {
        AlertType.TEMP_HIGH -> if (dark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
        AlertType.TEMP_LOW -> if (dark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
        AlertType.RAIN_CHANCE -> MaterialTheme.colorScheme.primary
        AlertType.WIND_HIGH -> MaterialTheme.colorScheme.secondary
        null -> MaterialTheme.colorScheme.primary
    }
    val condition = when (type) {
        AlertType.TEMP_HIGH -> "Khi nhiệt độ từ ${alert.threshold.toInt()}°C trở lên"
        AlertType.TEMP_LOW -> "Khi nhiệt độ từ ${alert.threshold.toInt()}°C trở xuống"
        AlertType.RAIN_CHANCE -> "Khi khả năng mưa từ ${alert.threshold.toInt()}% trở lên"
        AlertType.WIND_HIGH -> "Khi gió từ ${alert.threshold.toInt()} km/h trở lên"
        null -> "Ngưỡng ${alert.threshold}"
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("alert_card_${alert.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .toggleable(value = alert.isEnabled, role = Role.Switch, onValueChange = onToggle)
                    .heightIn(min = 56.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${alert.name}. $condition. ${if (alert.isEnabled) "Đang bật" else "Đang tắt"}"
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(type?.icon ?: Icons.Default.NotificationsActive, null, tint = tint, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(alert.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(condition, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (alert.isTriggered) {
                        Text(
                            "Đang vượt ngưỡng",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Switch(
                    checked = alert.isEnabled,
                    onCheckedChange = null,
                    modifier = Modifier.testTag("alert_switch_${alert.id}")
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_alert_${alert.id}")) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Xóa cảnh báo ${alert.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddAlertDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, threshold: Double) -> Unit
) {
    var type by rememberSaveable { mutableStateOf(AlertType.TEMP_HIGH) }
    var name by rememberSaveable { mutableStateOf("") }
    var thresholdText by rememberSaveable { mutableStateOf(AlertType.TEMP_HIGH.defaultThreshold) }

    val threshold = thresholdText.replace(',', '.').toDoubleOrNull()
    val valid = threshold != null && threshold in type.min..type.max

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm cảnh báo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Báo cho tôi khi", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlertType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = {
                                type = option
                                thresholdText = option.defaultThreshold
                            },
                            label = { Text(option.chipLabel) },
                            leadingIcon = { Icon(option.icon, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it.take(6) },
                    label = { Text(if (type == AlertType.TEMP_LOW) "Từ mức này trở xuống" else "Từ mức này trở lên") },
                    suffix = { Text(type.unit) },
                    singleLine = true,
                    isError = !valid,
                    supportingText = {
                        if (!valid) Text("Nhập số từ ${type.min.toInt()} đến ${type.max.toInt()}")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(40)
                    },
                    label = { Text("Tên cảnh báo (không bắt buộc)") },
                    placeholder = { Text(type.defaultName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { onConfirm(name.trim().ifBlank { type.defaultName }, type.key, threshold!!) }
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

// ------------------------------------------------------------------------------------ history

@Composable
private fun HistoryTab(
    history: List<TravelHistoryEntity>,
    tempUnit: String,
    thresholdKm: Double,
    onClearAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Nơi bạn đã đi qua", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Mỗi lần bạn di chuyển đủ xa, thời tiết tại điểm mới được ghi lại. Dữ liệu chỉ lưu trên máy này.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearAll, modifier = Modifier.testTag("clear_history_button")) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Xóa hết")
                    }
                }
            }
        }

        if (history.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Navigation,
                    title = "Chưa có hành trình nào",
                    message = "Khi bạn di chuyển hơn ${thresholdKm.toInt()} km, ứng dụng sẽ tự ghi lại thời tiết tại điểm mới."
                )
            }
        } else {
            items(history, key = { it.id }) { HistoryItemCard(it, tempUnit) }
        }
    }
}

@Composable
private fun HistoryItemCard(item: TravelHistoryEntity, tempUnit: String) {
    val info = WeatherCodeMapper.getInfo(item.weatherCode)
    val time = remember(item.recordedAt) {
        SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).format(Date(item.recordedAt))
    }
    val temp = Units.tempWithUnit(item.temperature, tempUnit)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "${item.placeName}, ${info.title}, $temp, $time" +
                        if (item.distanceFromPreviousKm > 0) ", cách điểm trước %.1f ki-lô-mét".format(Locale.US, item.distanceFromPreviousKm) else ""
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(info.iconEmoji, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.clearAndSetSemantics { })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.placeName, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${info.title} · $temp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.distanceFromPreviousKm > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "+%.1f km".format(Locale.US, item.distanceFromPreviousKm),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
