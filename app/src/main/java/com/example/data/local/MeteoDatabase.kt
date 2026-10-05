package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FavoritePlaceEntity::class,
        TravelHistoryEntity::class,
        WeatherAlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MeteoDatabase : RoomDatabase() {
    abstract fun favoritePlaceDao(): FavoritePlaceDao
    abstract fun travelHistoryDao(): TravelHistoryDao
    abstract fun weatherAlertDao(): WeatherAlertDao

    companion object {
        @Volatile
        private var INSTANCE: MeteoDatabase? = null

        fun getInstance(context: Context): MeteoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeteoDatabase::class.java,
                    "meteo_track.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Ship a few sensible alert thresholds; users can edit or delete them.
                            // Raw SQL on the supplied `db` avoids re-entering getInstance() while it is still being built.
                            listOf(
                                Triple("Nắng nóng (≥ 35°C)", "TEMP_HIGH", 35.0),
                                Triple("Khả năng mưa to (≥ 70%)", "RAIN_CHANCE", 70.0),
                                Triple("Gió mạnh (≥ 30 km/h)", "WIND_HIGH", 30.0)
                            ).forEach { (name, type, threshold) ->
                                db.execSQL(
                                    "INSERT INTO weather_alerts (name, type, threshold, isEnabled, isTriggered) VALUES (?, ?, ?, 1, 0)",
                                    arrayOf<Any>(name, type, threshold)
                                )
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
