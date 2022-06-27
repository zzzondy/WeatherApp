package com.weatherapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CityWeatherEntity::class], version = 2)
abstract class CityWeatherDatabase : RoomDatabase() {
    abstract val cityWeatherDao: CityWeatherDao

    companion object {
        fun create(context: Context): CityWeatherDatabase {
            return Room.databaseBuilder(
                context,
                CityWeatherDatabase::class.java,
                CityWeatherDatabaseContract.DATABASE_NAME
            ).fallbackToDestructiveMigration().build()
        }
    }
}