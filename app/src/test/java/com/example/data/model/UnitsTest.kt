package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {

    @Test
    fun `celsius to fahrenheit`() {
        assertEquals(32.0, Units.temp(0.0, "F"), 0.001)
        assertEquals(98.6, Units.temp(37.0, "F"), 0.001)
        assertEquals(25.0, Units.temp(25.0, "C"), 0.001)
    }

    @Test
    fun `temperature text rounds instead of truncating`() {
        assertEquals("25°", Units.tempShort(24.6, "C"))
        assertEquals("25°C", Units.tempWithUnit(24.6, "C"))
        assertEquals("77°F", Units.tempWithUnit(25.0, "F"))
    }

    @Test
    fun `wind unit conversion`() {
        assertEquals("36 km/h", Units.wind(36.0, "kmh"))
        assertEquals("10.0 m/s", Units.wind(36.0, "ms"))
        assertEquals("22.4 mph", Units.wind(36.0, "mph"))
    }

    @Test
    fun `wind direction compass`() {
        assertEquals("B", Units.windDirection(0.0))
        assertEquals("B", Units.windDirection(359.0))
        assertEquals("Đ", Units.windDirection(90.0))
        assertEquals("N", Units.windDirection(180.0))
        assertEquals("TB", Units.windDirection(315.0))
    }

    @Test
    fun `uv advice grows with the index`() {
        assertEquals(true, Units.uvAdvice(1.0).startsWith("Thấp"))
        assertEquals(true, Units.uvAdvice(12.0).startsWith("Cực cao"))
    }

    @Test
    fun `hourly series starts at the current hour, not at midnight`() {
        val times = (0..23).map { "2026-10-05T%02d:00".format(it) }
        val hourly = HourlyWeather(
            time = times,
            temperature2m = List(24) { 20.0 },
            weatherCode = List(24) { 0 }
        )
        assertEquals(14, hourly.indexOfHour("2026-10-05T14:15"))
        assertEquals(0, hourly.indexOfHour("2026-10-05T00:00"))
        assertEquals(0, hourly.indexOfHour(null))
    }

    @Test
    fun `hour label extracts HH mm`() {
        assertEquals("06:12", hourLabel("2026-10-05T06:12"))
        assertEquals(null, hourLabel("2026-10-05"))
        assertEquals(null, hourLabel(null))
    }
}
