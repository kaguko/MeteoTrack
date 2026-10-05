package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherCodeMapperTest {

    private fun contrastWithWhite(background: Color): Double = 1.05 / (background.luminance() + 0.05)

    @Test
    fun `white text reaches WCAG AA on every gradient stop`() {
        WeatherCodeMapper.allGradients.forEachIndexed { g, stops ->
            stops.forEachIndexed { s, color ->
                val ratio = contrastWithWhite(color)
                assertTrue("gradient #$g stop #$s has contrast %.2f (< 4.5)".format(ratio), ratio >= 4.5)
            }
        }
    }

    @Test
    fun `every WMO code has a specific description`() {
        val generic = WeatherCodeMapper.getInfo(-1).title
        val codes = listOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99)
        codes.forEach { code ->
            assertNotEquals("code $code fell through to the generic case", generic, WeatherCodeMapper.getInfo(code).title)
        }
    }

    @Test
    fun `freezing rain is flagged as severe`() {
        assertTrue(WeatherCodeMapper.getInfo(66).isSevere)
        assertTrue(WeatherCodeMapper.getInfo(67).isRaining)
    }

    @Test
    fun `clear sky at night uses the night variant`() {
        assertEquals("Đêm trời quang", WeatherCodeMapper.getInfo(0, isDay = false).title)
        assertEquals("Trời quang đãng", WeatherCodeMapper.getInfo(0, isDay = true).title)
        assertFalse(WeatherCodeMapper.getInfo(0, isDay = false).isRaining)
    }

    @Test
    fun `thunderstorm is severe`() {
        assertTrue(WeatherCodeMapper.getInfo(95).isSevere)
    }
}
