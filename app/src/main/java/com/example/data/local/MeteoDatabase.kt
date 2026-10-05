package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                            // Pre-seed some default favorites and alerts
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                database.favoritePlaceDao().insert(
                                    FavoritePlaceEntity(
                                        name = "Hà Nội",
                                        address = "Hoàn Kiếm, Hà Nội, Việt Nam",
                                        latitude = 21.0285,
                                        longitude = 105.8542,
                                        category = "HOME"
                                    )
                                )
                                database.favoritePlaceDao().insert(
                                    FavoritePlaceEntity(
                                        name = "TP. Hồ Chí Minh",
                                        address = "Quận 1, TP. Hồ Chí Minh, Việt Nam",
                                        latitude = 10.8231,
                                        longitude = 106.6297,
                                        category = "WORK"
                                    )
                                )
                                database.favoritePlaceDao().insert(
                                    FavoritePlaceEntity(
                                        name = "Đà Lạt",
                                        address = "Lâm Đồng, Việt Nam",
                                        latitude = 11.9404,
                                        longitude = 108.4583,
                                        category = "TRAVEL"
                                    )
                                )

                                database.weatherAlertDao().insert(
                                    WeatherAlertEntity(
                                        name = "Cảnh báo nắng nóng (>35°C)",
                                        type = "TEMP_HIGH",
                                        threshold = 35.0,
                                        isEnabled = true
                                    )
                                )
                                database.weatherAlertDao().insert(
                                    WeatherAlertEntity(
                                        name = "Cảnh báo mưa to (>70%)",
                                        type = "RAIN_CHANCE",
                                        threshold = 70.0,
                                        isEnabled = true
                                    )
                                )
                                database.weatherAlertDao().insert(
                                    WeatherAlertEntity(
                                        name = "Cảnh báo gió mạnh (>30 km/h)",
                                        type = "WIND_HIGH",
                                        threshold = 30.0,
                                        isEnabled = true
                                    )
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
