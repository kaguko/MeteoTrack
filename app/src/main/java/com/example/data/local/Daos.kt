package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePlaceDao {
    @Query("SELECT * FROM favorite_places ORDER BY createdAt DESC")
    fun getAll(): Flow<List<FavoritePlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: FavoritePlaceEntity): Long

    @Delete
    suspend fun delete(place: FavoritePlaceEntity)

    @Query("DELETE FROM favorite_places WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface TravelHistoryDao {
    @Query("SELECT * FROM travel_history ORDER BY recordedAt DESC LIMIT 100")
    fun getAll(): Flow<List<TravelHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TravelHistoryEntity): Long

    @Query("DELETE FROM travel_history")
    suspend fun clearAll()
}

@Dao
interface WeatherAlertDao {
    @Query("SELECT * FROM weather_alerts ORDER BY id ASC")
    fun getAll(): Flow<List<WeatherAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: WeatherAlertEntity): Long

    @Update
    suspend fun update(alert: WeatherAlertEntity)

    @Delete
    suspend fun delete(alert: WeatherAlertEntity)

    @Query("UPDATE weather_alerts SET isTriggered = :triggered, lastTriggeredAt = :timestamp WHERE id = :id")
    suspend fun setTriggered(id: Long, triggered: Boolean, timestamp: Long?)
}
