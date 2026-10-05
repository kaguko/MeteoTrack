package com.example.data.model

import java.util.Locale

/** Unit conversion helpers shared by every screen. Source data is always °C and km/h. */
object Units {
    fun temp(celsius: Double, unit: String): Double =
        if (unit == "F") celsius * 9.0 / 5.0 + 32.0 else celsius

    /** "24°" – rounded, with the degree sign only. */
    fun tempShort(celsius: Double, unit: String): String =
        "${Math.round(temp(celsius, unit))}°"

    /** "24°C" – rounded, with the unit letter. */
    fun tempWithUnit(celsius: Double, unit: String): String =
        "${Math.round(temp(celsius, unit))}°$unit"

    fun wind(kmh: Double, unit: String): String = when (unit) {
        "ms" -> "%.1f m/s".format(Locale.US, kmh / 3.6)
        "mph" -> "%.1f mph".format(Locale.US, kmh * 0.621371)
        else -> "%.0f km/h".format(Locale.US, kmh)
    }

    fun uvAdvice(uv: Double): String = when {
        uv < 3 -> "Thấp — an toàn khi ra ngoài"
        uv < 6 -> "Trung bình — nên đội mũ, bôi kem chống nắng"
        uv < 8 -> "Cao — hạn chế ra nắng từ 10h đến 15h"
        uv < 11 -> "Rất cao — tránh nắng, che chắn kỹ"
        else -> "Cực cao — hạn chế tối đa ra ngoài"
    }

    /** Compass label (Vietnamese) for a wind direction in degrees. */
    fun windDirection(degrees: Double): String {
        val names = listOf("B", "ĐB", "Đ", "ĐN", "N", "TN", "T", "TB")
        val index = (((degrees % 360 + 360) % 360 + 22.5) / 45).toInt() % 8
        return names[index]
    }
}

/**
 * Open-Meteo returns hourly data from 00:00 of *today*, so the "next 24 hours" must start at the
 * current hour, not at index 0. Times are ISO local strings ("2026-10-05T14:00"), so a prefix
 * comparison on the hour is enough.
 */
fun HourlyWeather.indexOfHour(currentTime: String?): Int {
    if (currentTime == null) return 0
    val hour = currentTime.take(13)
    return time.indexOfFirst { it.take(13) >= hour }.coerceAtLeast(0)
}

/** "14:00" from an ISO local timestamp, or null when it does not contain a time. */
fun hourLabel(iso: String?): String? = iso?.substringAfter('T', "")?.take(5)?.takeIf { it.length == 5 }
