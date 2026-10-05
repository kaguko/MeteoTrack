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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Units
import com.example.data.model.WeatherCodeMapper
import com.example.data.model.hourLabel
import com.example.ui.components.CenteredContent
import com.example.ui.components.Glass
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.viewmodel.WeatherUiState
import com.example.ui.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    viewModel: WeatherViewModel,
    dayIndex: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val location by viewModel.activeLocation.collectAsStateWithLifecycle()

    val daily = (uiState as? WeatherUiState.Success)?.data?.daily
    val valid = daily != null && dayIndex in daily.time.indices
    val info = WeatherCodeMapper.getInfo(if (valid) daily!!.weatherCode[dayIndex] else 0)

    Box(modifier = modifier.fillMaxSize().background(info.backgroundBrush)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Chi tiết ngày", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("day_detail_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Glass.OnGlass,
                        navigationIconContentColor = Glass.OnGlass
                    )
                )
            }
        ) { innerPadding ->
            if (!valid) {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy dữ liệu của ngày đã chọn.", color = Glass.OnGlass)
                }
                return@Scaffold
            }
            daily!!

            val unit = settings.tempUnit
            val min = daily.temperature2mMin[dayIndex]
            val max = daily.temperature2mMax[dayIndex]
            val uv = daily.uvIndexMax?.getOrNull(dayIndex)
            val rainMm = daily.precipitationSum?.getOrNull(dayIndex)
            val rainProb = daily.precipitationProbabilityMax?.getOrNull(dayIndex)
            val windMax = daily.windSpeed10mMax?.getOrNull(dayIndex)
            val sunrise = hourLabel(daily.sunrise?.getOrNull(dayIndex))
            val sunset = hourLabel(daily.sunset?.getOrNull(dayIndex))

            CenteredContent(modifier = Modifier.padding(innerPadding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {
                                contentDescription = "${formatFullDate(daily.time[dayIndex])} tại ${location.displayName}. " +
                                    "${info.title}, thấp nhất ${Math.round(Units.temp(min, unit))} độ, " +
                                    "cao nhất ${Math.round(Units.temp(max, unit))} độ."
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = formatFullDate(daily.time[dayIndex]),
                            style = MaterialTheme.typography.titleLarge,
                            color = Glass.OnGlass,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clearAndSetSemantics { }
                        )
                        Text(
                            text = location.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Glass.OnGlassMuted,
                            modifier = Modifier.clearAndSetSemantics { }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = info.iconEmoji, fontSize = 64.sp, modifier = Modifier.clearAndSetSemantics { })
                        Text(
                            text = info.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Glass.OnGlass,
                            modifier = Modifier.clearAndSetSemantics { }
                        )
                        Text(
                            text = "${Units.tempShort(min, unit)}  —  ${Units.tempShort(max, unit)}",
                            style = MaterialTheme.typography.displayMedium,
                            color = Glass.OnGlass,
                            modifier = Modifier.clearAndSetSemantics { }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailTile(Icons.Default.WbSunny, "Bình minh", sunrise ?: "—", null, Modifier.weight(1f))
                        DetailTile(Icons.Default.WbTwilight, "Hoàng hôn", sunset ?: "—", null, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailTile(
                            Icons.Outlined.Umbrella, "Lượng mưa",
                            rainMm?.let { "%.1f mm".format(Locale.US, it) } ?: "—",
                            rainProb?.let { "Khả năng mưa $it%" },
                            Modifier.weight(1f)
                        )
                        DetailTile(
                            Icons.Default.Air, "Gió tối đa",
                            windMax?.let { Units.wind(it, settings.windUnit) } ?: "—",
                            windMax?.let { if (it > 30) "Gió mạnh, cẩn thận khi di chuyển" else "Gió nhẹ" },
                            Modifier.weight(1f)
                        )
                    }
                    DetailTile(
                        Icons.Outlined.WbSunny, "Chỉ số UV cao nhất",
                        uv?.let { "%.1f".format(Locale.US, it) } ?: "—",
                        uv?.let { Units.uvAdvice(it) },
                        Modifier.fillMaxWidth()
                    )

                    GlassCard {
                        SectionHeader(icon = Icons.Default.Lightbulb, title = "Lời khuyên cho ngày này")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = advice(info.isRaining, info.isSevere, uv, Units.temp(max, "C")),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Glass.OnGlass
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailTile(
    icon: ImageVector,
    title: String,
    value: String,
    note: String?,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$title: $value${note?.let { ". $it" } ?: ""}"
        },
        contentPadding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Glass.OnGlassMuted, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = Glass.OnGlassMuted)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = Glass.OnGlass)
        if (note != null) {
            Text(note, style = MaterialTheme.typography.bodySmall, color = Glass.OnGlassMuted)
        }
    }
}

private fun advice(isRaining: Boolean, isSevere: Boolean, uv: Double?, maxTempC: Double): String {
    val items = mutableListOf<String>()
    when {
        isSevere -> items += "• Thời tiết nguy hiểm: hạn chế ra ngoài và theo dõi cảnh báo của cơ quan khí tượng."
        isRaining -> {
            items += "• Mang theo áo mưa hoặc ô."
            items += "• Giảm tốc độ khi đi trên đường ướt."
        }
        else -> items += "• Thời tiết thuận lợi cho các hoạt động ngoài trời."
    }
    if (uv != null && uv >= 6) items += "• Đội mũ, đeo kính râm và bôi kem chống nắng từ 10h đến 16h."
    if (maxTempC >= 34) items += "• Trời nóng: uống đủ nước và tránh ở ngoài trời quá lâu."
    if (maxTempC <= 15) items += "• Trời lạnh: mặc ấm, nhất là buổi sáng và tối."
    return items.joinToString("\n")
}

private fun formatFullDate(isoDate: String): String = try {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate)!!
    SimpleDateFormat("EEEE, 'ngày' d 'tháng' M", Locale.forLanguageTag("vi-VN")).format(date)
        .replaceFirstChar { it.uppercase() }
} catch (e: Exception) {
    isoDate
}
