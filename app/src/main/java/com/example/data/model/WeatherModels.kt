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

/** Last successful forecast, stored on disk for instant / offline start-up. */
@JsonClass(generateAdapter = true)
data class CachedForecast(
    @Json(name = "locationName") val locationName: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "savedAt") val savedAt: Long,
    @Json(name = "data") val data: WeatherResponse
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

/**
 * Presentation info for a WMO weather code.
 *
 * Every gradient stop is dark enough for white text to reach WCAG AA (4.5:1);
 * this is enforced by `WeatherCodeMapperTest`.
 */
data class WeatherInfo(
    val title: String,
    val description: String,
    val iconEmoji: String,
    val isRaining: Boolean,
    val isSevere: Boolean,
    val gradient: List<Color>
) {
    val backgroundBrush: Brush get() = Brush.verticalGradient(gradient)
}

object WeatherCodeMapper {
    private val ClearDay = listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8), Color(0xFF2563EB))
    private val PartlyCloudy = listOf(Color(0xFF1E3A8A), Color(0xFF075985), Color(0xFF0369A1))
    private val Overcast = listOf(Color(0xFF334155), Color(0xFF475569), Color(0xFF64748B))
    private val Fog = listOf(Color(0xFF3F4856), Color(0xFF59616F), Color(0xFF6B7280))
    private val Drizzle = listOf(Color(0xFF0F3D55), Color(0xFF155E75), Color(0xFF0E7490))
    private val Rain = listOf(Color(0xFF1E293B), Color(0xFF1E3A5F), Color(0xFF1D4E89))
    private val HeavyRain = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
    private val Snow = listOf(Color(0xFF1E3A8A), Color(0xFF2B5C9E), Color(0xFF3A6EAE))
    private val Showers = listOf(Color(0xFF1E293B), Color(0xFF1E40AF), Color(0xFF2563EB))
    private val Thunder = listOf(Color(0xFF090D16), Color(0xFF1E1B4B), Color(0xFF3B0764))
    private val Night = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF311042))

    /** All gradients, exposed so tests can verify contrast. */
    val allGradients: List<List<Color>> = listOf(
        ClearDay, PartlyCloudy, Overcast, Fog, Drizzle, Rain, HeavyRain, Snow, Showers, Thunder, Night
    )

    fun getInfo(code: Int, isDay: Boolean = true): WeatherInfo {
        if (!isDay && (code == 0 || code == 1)) {
            return WeatherInfo(
                title = "Đêm trời quang",
                description = "Trời quang mây, thời tiết dịu mát về đêm",
                iconEmoji = "🌙",
                isRaining = false,
                isSevere = false,
                gradient = Night
            )
        }

        return when (code) {
            0 -> info("Trời quang đãng", "Nắng đẹp, trời quang không mây", "☀️", ClearDay)
            1, 2 -> info("Ít mây", "Thời tiết dễ chịu, có nắng xen kẽ mây", "⛅", PartlyCloudy)
            3 -> info("Nhiều mây", "Bầu trời u ám, râm mát", "☁️", Overcast)
            45, 48 -> info("Sương mù", "Tầm nhìn giảm, di chuyển cẩn thận", "🌫️", Fog)
            51, 53, 55 -> info("Mưa phùn", "Mưa phùn lất phất, ẩm ướt nhẹ", "🌦️", Drizzle, raining = true)
            56, 57 -> info(
                "Mưa phùn băng giá", "Mưa phùn đóng băng, đường có thể trơn trượt", "🌦️", Drizzle,
                raining = true, severe = true
            )
            61, 63 -> info("Mưa vừa", "Nên mang theo áo mưa hoặc ô", "🌧️", Rain, raining = true)
            65 -> info(
                "Mưa to", "Mưa nặng hạt, chú ý an toàn khi di chuyển", "🌧️", HeavyRain,
                raining = true, severe = true
            )
            66, 67 -> info(
                "Mưa băng", "Mưa đóng băng, đường rất trơn — hạn chế di chuyển", "🌧️", HeavyRain,
                raining = true, severe = true
            )
            71, 73, 75, 77, 85, 86 -> info("Tuyết rơi", "Trời lạnh, hãy giữ ấm cơ thể", "❄️", Snow, raining = true)
            80, 81, 82 -> info("Mưa rào", "Mưa rào ngắt quãng, có thể rất to trong thời gian ngắn", "🌧️", Showers, raining = true)
            95, 96, 99 -> info(
                "Dông sét", "Dông sét nguy hiểm, hạn chế ra ngoài", "⛈️", Thunder,
                raining = true, severe = true
            )
            else -> info("Thời tiết ổn định", "Không có hiện tượng đặc biệt", "🌤️", ClearDay)
        }
    }

    private fun info(
        title: String,
        description: String,
        emoji: String,
        gradient: List<Color>,
        raining: Boolean = false,
        severe: Boolean = false
    ) = WeatherInfo(title, description, emoji, raining, severe, gradient)
}
