package com.example.data.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "elevation") val elevation: Double? = null,
    @Json(name = "current") val current: CurrentWeather? = null,
    @Json(name = "hourly") val hourly: HourlyWeather? = null,
    @Json(name = "daily") val daily: DailyWeather? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @Json(name = "time") val time: String,
    @Json(name = "temperature_2m") val temperature2m: Double,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int,
    @Json(name = "apparent_temperature") val apparentTemperature: Double,
    @Json(name = "is_day") val isDay: Int,
    @Json(name = "precipitation") val precipitation: Double,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "surface_pressure") val surfacePressure: Double? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double,
    @Json(name = "wind_direction_10m") val windDirection10m: Double? = null
)

@JsonClass(generateAdapter = true)
data class HourlyWeather(
    @Json(name = "time") val time: List<String>,
    @Json(name = "temperature_2m") val temperature2m: List<Double>,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Int>? = null,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>? = null,
    @Json(name = "weather_code") val weatherCode: List<Int>,
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double>? = null
)

@JsonClass(generateAdapter = true)
data class DailyWeather(
    @Json(name = "time") val time: List<String>,
    @Json(name = "weather_code") val weatherCode: List<Int>,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double>,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double>,
    @Json(name = "apparent_temperature_max") val apparentTemperatureMax: List<Double>? = null,
    @Json(name = "apparent_temperature_min") val apparentTemperatureMin: List<Double>? = null,
    @Json(name = "sunrise") val sunrise: List<String>? = null,
    @Json(name = "sunset") val sunset: List<String>? = null,
    @Json(name = "uv_index_max") val uvIndexMax: List<Double>? = null,
    @Json(name = "precipitation_sum") val precipitationSum: List<Double>? = null,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>? = null,
    @Json(name = "wind_speed_10m_max") val windSpeed10mMax: List<Double>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    @Json(name = "results") val results: List<GeocodingLocation>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingLocation(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "elevation") val elevation: Double? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "admin1") val admin1: String? = null,
    @Json(name = "admin2") val admin2: String? = null
) {
    val fullDisplayName: String
        get() = buildString {
            append(name)
            if (!admin1.isNullOrBlank()) append(", $admin1")
            if (!country.isNullOrBlank()) append(", $country")
        }
}

data class WeatherInfo(
    val title: String,
    val description: String,
    val iconEmoji: String,
    val isRaining: Boolean,
    val isSevere: Boolean,
    val backgroundBrush: Brush,
    val cardColor: Color,
    val textColor: Color
)

object WeatherCodeMapper {
    fun getInfo(code: Int, isDay: Boolean = true): WeatherInfo {
        if (!isDay && (code == 0 || code == 1)) {
            return WeatherInfo(
                title = "Đêm trời quang",
                description = "Trời quang mây, thời tiết dịu mát về đêm",
                iconEmoji = "🌙",
                isRaining = false,
                isSevere = false,
                backgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF311042))
                ),
                cardColor = Color(0x331E293B),
                textColor = Color.White
            )
        }

        return when (code) {
            0 -> WeatherInfo(
                title = "Trời quang đãng",
                description = "Nắng vàng rực rỡ, trời quang đãng không mây",
                iconEmoji = "☀️",
                isRaining = false,
                isSevere = false,
                backgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xFF2563EB), Color(0xFF38BDF8), Color(0xFF93C5FD))
                ),
                cardColor = Color(0x2BFFFFFF),
                textColor = Color.White
            )
            1, 2 -> WeatherInfo(
                title = "Ít mây / Mây rải rác",
                description = "Thời tiết dễ chịu, có nắng xen kẽ mây",
                iconEmoji = "⛅",
                isRaining = false,
                isSevere = false,
                backgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFFBAE6FD))
                ),
                cardColor = Color(0x2BFFFFFF),
                textColor = Color.White
            )
            3 -> WeatherInfo(
                title = "Nhiều mây / U ám",
                description = "Bầu trời u ám nhiều mây, râm mát",
                iconEmoji = "☁️",
                isRaining = false,
                isSevere = false,
                backgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xFF475569), Color(0xFF64748B), Color(0xFF94A3B8))
                ),
                cardColor = Color(0x2BFFFFFF),
                textColor = Color.White
            )
            45, 48 -> WeatherInfo(
                title = "Sương mù",
                description = "Tầm nhìn giảm, lái xe cẩn thận",
                iconEmoji = "🌫️",
                isRaining = false,
                isSevere = false,
                backgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xFF4B5563), Color(0xFF6B7280), Color(0xFF9CA3AF))
                ),
                cardColor = Color(0x2BFFFFFF),
                textColor = Color.White
            )
            51, 53, 55 -> WeatherInfo(
                title = "Mưa phùn",
                description = "Mưa phùn lất phất, ẩm ướt nhẹ",
                iconEmoji = "🌦️",
                isRaining = true,
                isSevere = false,
                backgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xFF334155), Color(0xFF475569), Color(0xFF64748B))
                ),
                cardColor = Color(0x2BFFFFFF),
                textColor = Color.White
            )
            61, 63 -> WeatherInfo(
                title = "Mưa vừa",
                description = "Mưa rải rác, nên mang theo áo mưa hoặc ô",
                iconEmoji = "🌧️",
                isRaining = true,
                isSevere = false,
                backgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569))
                ),
                cardColor = Color(0x2BFFFFFF),
                textColor = Color.White
            )
            65 -> WeatherInfo(
                title = "Mưa to diện rộng",
                description = "Mưa nặng hạt, chú ý an toàn khi di chuyển",
                iconEmoji = "🌧️",
                isRaining = true,
                isSevere = true,
                backgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
                ),
                cardColor = Color(0x2BFFFFFF),
                textColor = Color.White
            )
            71, 73, 75, 77, 85, 86 -> WeatherInfo(
                title = "Tuyết rơi",
                description = "Tuyết rơi lạnh giá, giữ ấm cơ thể",
                iconEmoji = "❄️",
                isRaining = true,
                isSevere = false,
                backgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xFF60A5FA), Color(0xFF93C5FD), Color(0xFFE0F2FE))
                ),
                cardColor = Color(0x2BFFFFFF),
                textColor = Color.White
            )
            80, 81, 82 -> WeatherInfo(
                title = "Mưa rào từng đợt",
                description = "Mưa rào xối xả ngắt quãng",
                iconEmoji = "🌧️",
                isRaining = true,
                isSevere = false,
                backgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xFF1E293B), Color(0xFF3B82F6), Color(0xFF60A5FA))
                ),
                cardColor = Color(0x2BFFFFFF),
                textColor = Color.White
            )
            95, 96, 99 -> WeatherInfo(
                title = "Dông sét bão to",
                description = "Dông sét nguy hiểm, hạn chế ra ngoài!",
                iconEmoji = "⛈️",
                isRaining = true,
                isSevere = true,
                backgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xFF090D16), Color(0xFF1E1B4B), Color(0xFF3B0764))
                ),
                cardColor = Color(0x2BFFFFFF),
                textColor = Color.White
            )
            else -> WeatherInfo(
                title = "Thời tiết ổn định",
                description = "Thời tiết bình thường",
                iconEmoji = "🌤️",
                isRaining = false,
                isSevere = false,
                backgroundBrush = Brush.verticalGradient(
                    listOf(Color(0xFF2563EB), Color(0xFF38BDF8), Color(0xFFBAE6FD))
                ),
                cardColor = Color(0x2BFFFFFF),
                textColor = Color.White
            )
        }
    }
}
