package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.FavoritePlaceEntity
import com.example.data.local.MeteoDatabase
import com.example.data.local.TravelHistoryEntity
import com.example.data.local.WeatherAlertEntity
import com.example.data.model.GeocodingLocation
import com.example.data.model.WeatherCodeMapper
import com.example.data.model.WeatherResponse
import com.example.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class WeatherRepository(
    private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val openMeteoApi = ApiClient.openMeteoApi
    private val geocodingApi = ApiClient.geocodingApi
    private val database = MeteoDatabase.getInstance(context)
    private val notificationHelper = NotificationHelper(context)

    val favoritePlaces: Flow<List<FavoritePlaceEntity>> =
        database.favoritePlaceDao().getAll()

    val travelHistory: Flow<List<TravelHistoryEntity>> =
        database.travelHistoryDao().getAll()

    val weatherAlerts: Flow<List<WeatherAlertEntity>> =
        database.weatherAlertDao().getAll()

    suspend fun getForecast(lat: Double, lng: Double): Result<WeatherResponse> = withContext(Dispatchers.IO) {
        try {
            val response = openMeteoApi.getForecast(latitude = lat, longitude = lng)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Failed to fetch forecast for ($lat, $lng)", e)
            Result.failure(e)
        }
    }

    suspend fun searchLocations(query: String): List<GeocodingLocation> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val response = geocodingApi.searchLocations(name = query.trim())
            response.results ?: emptyList()
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Failed searching locations", e)
            emptyList()
        }
    }

    suspend fun checkAlertsAndNotify(
        weatherResponse: WeatherResponse,
        locationName: String
    ) = withContext(Dispatchers.IO) {
        val current = weatherResponse.current ?: return@withContext
        val alerts = database.weatherAlertDao().getAll().first()
        val settings = preferencesRepository.settingsFlow.first()

        if (!settings.notificationsEnabled) return@withContext

        val currentTemp = current.temperature2m
        val rainProb = weatherResponse.daily?.precipitationProbabilityMax?.firstOrNull()
            ?: if (current.precipitation > 0) 100 else 0
        val windSpeed = current.windSpeed10m

        for (alert in alerts) {
            if (!alert.isEnabled) continue

            var isConditionMet = false
            var alertMessage = ""

            when (alert.type) {
                "TEMP_HIGH" -> {
                    if (currentTemp >= alert.threshold) {
                        isConditionMet = true
                        alertMessage = "Nhiệt độ tại $locationName đã đạt %.1f°C (vượt ngưỡng cảnh báo %.1f°C)".format(currentTemp, alert.threshold)
                    }
                }
                "TEMP_LOW" -> {
                    if (currentTemp <= alert.threshold) {
                        isConditionMet = true
                        alertMessage = "Nhiệt độ tại $locationName hạ xuống %.1f°C (dưới ngưỡng cảnh báo %.1f°C)".format(currentTemp, alert.threshold)
                    }
                }
                "RAIN_CHANCE" -> {
                    if (rainProb >= alert.threshold) {
                        isConditionMet = true
                        alertMessage = "Khả năng mưa tại $locationName lên tới $rainProb%% (vượt ngưỡng %d%%)".format(alert.threshold.toInt())
                    }
                }
                "WIND_HIGH" -> {
                    if (windSpeed >= alert.threshold) {
                        isConditionMet = true
                        alertMessage = "Gió tại $locationName giật mạnh %.1f km/h (vượt ngưỡng %.1f km/h)".format(windSpeed, alert.threshold)
                    }
                }
            }

            if (isConditionMet) {
                val now = System.currentTimeMillis()
                // Avoid notifying too repeatedly within 1 hour for the same alert
                val lastTriggered = alert.lastTriggeredAt ?: 0L
                if (now - lastTriggered > 60 * 60 * 1000L) {
                    notificationHelper.showWeatherAlertNotification(
                        title = "⚠️ " + alert.name,
                        message = alertMessage
                    )
                    database.weatherAlertDao().setTriggered(alert.id, true, now)
                }
            } else if (alert.isTriggered) {
                // Reset triggered status if condition is no longer met
                database.weatherAlertDao().setTriggered(alert.id, false, alert.lastTriggeredAt)
            }
        }
    }

    suspend fun saveTravelHistory(
        locationName: String,
        latitude: Double,
        longitude: Double,
        temperature: Double,
        weatherCode: Int,
        distanceMovedKm: Double
    ) = withContext(Dispatchers.IO) {
        val weatherInfo = WeatherCodeMapper.getInfo(weatherCode)
        database.travelHistoryDao().insert(
            TravelHistoryEntity(
                placeName = locationName,
                latitude = latitude,
                longitude = longitude,
                temperature = temperature,
                weatherCode = weatherCode,
                weatherDescription = weatherInfo.title,
                recordedAt = System.currentTimeMillis(),
                distanceFromPreviousKm = distanceMovedKm
            )
        )
    }

    suspend fun clearTravelHistory() = withContext(Dispatchers.IO) {
        database.travelHistoryDao().clearAll()
    }

    suspend fun addFavoritePlace(name: String, address: String, lat: Double, lng: Double, category: String = "CUSTOM") = withContext(Dispatchers.IO) {
        database.favoritePlaceDao().insert(
            FavoritePlaceEntity(
                name = name,
                address = address,
                latitude = lat,
                longitude = lng,
                category = category
            )
        )
    }

    suspend fun removeFavoritePlace(place: FavoritePlaceEntity) = withContext(Dispatchers.IO) {
        database.favoritePlaceDao().delete(place)
    }

    suspend fun addWeatherAlert(alert: WeatherAlertEntity) = withContext(Dispatchers.IO) {
        database.weatherAlertDao().insert(alert)
    }

    suspend fun updateWeatherAlert(alert: WeatherAlertEntity) = withContext(Dispatchers.IO) {
        database.weatherAlertDao().update(alert)
    }

    suspend fun deleteWeatherAlert(alert: WeatherAlertEntity) = withContext(Dispatchers.IO) {
        database.weatherAlertDao().delete(alert)
    }

    fun testSendNotification(title: String, message: String) {
        notificationHelper.showWeatherAlertNotification(title, message)
    }
}
