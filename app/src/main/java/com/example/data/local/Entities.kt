package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_places")
data class FavoritePlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: String = "CUSTOM", // HOME, WORK, TRAVEL, CUSTOM
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "travel_history")
data class TravelHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val weatherCode: Int,
    val weatherDescription: String,
    val recordedAt: Long = System.currentTimeMillis(),
    val distanceFromPreviousKm: Double = 0.0
)

@Entity(tableName = "weather_alerts")
data class WeatherAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // TEMP_HIGH, TEMP_LOW, RAIN_CHANCE, WIND_HIGH
    val threshold: Double,
    val isEnabled: Boolean = true,
    val isTriggered: Boolean = false,
    val lastTriggeredAt: Long? = null
)
