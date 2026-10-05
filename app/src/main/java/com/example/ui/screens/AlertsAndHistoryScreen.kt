package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TravelHistoryEntity
import com.example.data.local.WeatherAlertEntity
import com.example.data.model.WeatherCodeMapper
import com.example.ui.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsAndHistoryScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val alerts by viewModel.weatherAlerts.collectAsState()
    val history by viewModel.travelHistory.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cảnh báo & Nhật ký di chuyển",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Ngưỡng cảnh báo (${alerts.size})") },
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Nhật ký di chuyển (${history.size})") },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedTab == 0) {
                // Tab 1: Weather Alerts
                AlertsTabContent(
                    alerts = alerts,
                    onToggle = { alert, enabled -> viewModel.toggleAlertEnabled(alert, enabled) },
                    onDelete = { alert -> viewModel.deleteAlert(alert) },
                    onAddNew = { showAddDialog = true },
                    onTestNotification = {
                        viewModel.testAlertNotification(
                            "⚠️ Cảnh báo thời tiết thử nghiệm",
                            "MeteoTrack: Thông báo cảnh báo thời tiết hoạt động bình thường!"
                        )
                    }
                )
            } else {
                // Tab 2: Travel History
                TravelHistoryTabContent(
                    history = history,
                    onClearAll = { viewModel.clearAllHistory() }
                )
            }
        }
    }

    if (showAddDialog) {
        AddNewAlertDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, threshold ->
                viewModel.addNewAlert(name, type, threshold)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AlertsTabContent(
    alerts: List<WeatherAlertEntity>,
    onToggle: (WeatherAlertEntity, Boolean) -> Unit,
    onDelete: (WeatherAlertEntity) -> Unit,
    onAddNew: () -> Unit,
    onTestNotification: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAddNew,
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_new_alert_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm cảnh báo", fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onTestNotification,
                modifier = Modifier
                    .weight(1f)
                    .testTag("test_notification_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thử thông báo", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có ngưỡng cảnh báo nào. Bấm 'Thêm cảnh báo' để tạo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(alerts, key = { it.id }) { alert ->
                    AlertItemCard(
                        alert = alert,
                        onToggle = { onToggle(alert, it) },
                        onDelete = { onDelete(alert) }
                    )
                }
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
    val icon = when (alert.type) {
        "TEMP_HIGH" -> Icons.Default.Thermostat
        "TEMP_LOW" -> Icons.Default.Thermostat
        "RAIN_CHANCE" -> Icons.Default.WaterDrop
        "WIND_HIGH" -> Icons.Default.Air
        else -> Icons.Default.NotificationsActive
    }

    val iconColor = when (alert.type) {
        "TEMP_HIGH" -> Color(0xFFEF4444)
        "TEMP_LOW" -> Color(0xFF3B82F6)
        "RAIN_CHANCE" -> Color(0xFF0284C7)
        "WIND_HIGH" -> Color(0xFF0D9488)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alert_card_${alert.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = alert.name,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (alert.type) {
                        "TEMP_HIGH" -> "Báo động khi nhiệt độ ≥ ${alert.threshold.toInt()}°C"
                        "TEMP_LOW" -> "Báo động khi nhiệt độ ≤ ${alert.threshold.toInt()}°C"
                        "RAIN_CHANCE" -> "Báo động khi khả năng mưa ≥ ${alert.threshold.toInt()}%"
                        "WIND_HIGH" -> "Báo động khi gió ≥ ${alert.threshold.toInt()} km/h"
                        else -> "Ngưỡng: ${alert.threshold}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (alert.isTriggered) {
                    Text(
                        text = "⚠️ Điều kiện đang thỏa mãn!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFDC2626)
                    )
                }
            }

            Switch(
                checked = alert.isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("alert_switch_${alert.id}")
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_alert_${alert.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TravelHistoryTabContent(
    history: List<TravelHistoryEntity>,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Lịch sử điểm đến",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tự động ghi lại thời tiết mỗi khi bạn di chuyển",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (history.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Xóa tất cả", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Xóa hết")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Trống",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Chưa có hành trình nào được ghi lại",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Khi bạn di chuyển >5km, app sẽ tự động ghi lại tại đây",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    HistoryItemCard(item = item)
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(item: TravelHistoryEntity) {
    val weatherInfo = WeatherCodeMapper.getInfo(item.weatherCode)
    val timeStr = formatHistoryTime(item.recordedAt)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = weatherInfo.iconEmoji,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.placeName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.weatherDescription} • ${item.temperature.toInt()}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = timeStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.distanceFromPreviousKm > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+%.1f km".format(item.distanceFromPreviousKm),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun AddNewAlertDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, threshold: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("TEMP_HIGH") }
    var thresholdText by remember { mutableStateOf("35") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm cảnh báo mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên cảnh báo") },
                    placeholder = { Text("VD: Nắng gắt trên 35°C") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Loại cảnh báo:", style = MaterialTheme.typography.bodySmall)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TypeSelectButton("Nhiệt cao", "TEMP_HIGH", type) {
                        type = it
                        if (name.isEmpty()) name = "Nắng gắt"
                        thresholdText = "35"
                    }
                    TypeSelectButton("Nhiệt thấp", "TEMP_LOW", type) {
                        type = it
                        if (name.isEmpty()) name = "Rét buốt"
                        thresholdText = "16"
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TypeSelectButton("Mưa to (%)", "RAIN_CHANCE", type) {
                        type = it
                        if (name.isEmpty()) name = "Mưa lớn"
                        thresholdText = "70"
                    }
                    TypeSelectButton("Gió mạnh", "WIND_HIGH", type) {
                        type = it
                        if (name.isEmpty()) name = "Gió giật"
                        thresholdText = "30"
                    }
                }

                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it },
                    label = { Text("Ngưỡng kích hoạt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val threshold = thresholdText.toDoubleOrNull() ?: 30.0
                    val finalName = if (name.isBlank()) "Cảnh báo cá nhân" else name
                    onConfirm(finalName, type, threshold)
                }
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun TypeSelectButton(
    label: String,
    typeKey: String,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    val isSelected = selectedKey == typeKey
    Button(
        onClick = { onSelect(typeKey) },
        shape = RoundedCornerShape(10.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(label, fontSize = 11.sp)
    }
}

private fun formatHistoryTime(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale("vi", "VN"))
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}
