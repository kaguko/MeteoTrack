package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class AppSettings(
    val tempUnit: String = "C", // "C" or "F"
    val windUnit: String = "kmh", // "kmh", "ms", "mph"
    val autoRefreshOnMove: Boolean = true,
    val minDistanceKm: Double = 5.0, // 1.0, 3.0, 5.0, 10.0
    val minTimeMinutes: Int = 30, // 15, 30, 60
    val notificationsEnabled: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {
    companion object {
        private val KEY_TEMP_UNIT = stringPreferencesKey("temp_unit")
        private val KEY_WIND_UNIT = stringPreferencesKey("wind_unit")
        private val KEY_AUTO_REFRESH = booleanPreferencesKey("auto_refresh")
        private val KEY_MIN_DISTANCE = doublePreferencesKey("min_distance")
        private val KEY_MIN_TIME = intPreferencesKey("min_time")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    /** `null` until DataStore has been read, so the UI can avoid flashing the welcome screen. */
    val onboardingDoneFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            tempUnit = preferences[KEY_TEMP_UNIT] ?: "C",
            windUnit = preferences[KEY_WIND_UNIT] ?: "kmh",
            autoRefreshOnMove = preferences[KEY_AUTO_REFRESH] ?: true,
            minDistanceKm = preferences[KEY_MIN_DISTANCE] ?: 5.0,
            minTimeMinutes = preferences[KEY_MIN_TIME] ?: 30,
            notificationsEnabled = preferences[KEY_NOTIFICATIONS] ?: true
        )
    }

    suspend fun setTempUnit(unit: String) {
        context.dataStore.edit { it[KEY_TEMP_UNIT] = unit }
    }

    suspend fun setWindUnit(unit: String) {
        context.dataStore.edit { it[KEY_WIND_UNIT] = unit }
    }

    suspend fun setAutoRefresh(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_REFRESH] = enabled }
    }

    suspend fun setMinDistance(distanceKm: Double) {
        context.dataStore.edit { it[KEY_MIN_DISTANCE] = distanceKm }
    }

    suspend fun setMinTime(minutes: Int) {
        context.dataStore.edit { it[KEY_MIN_TIME] = minutes }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }
}
