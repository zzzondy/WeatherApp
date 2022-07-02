package com.weatherapp.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(tableName = CityWeatherDatabaseContract.CityWeather.TABLE_NAME, indices = [Index(CityWeatherDatabaseContract.CityWeather.COLUMN_NAME_ID)])
data class CityWeatherEntity(
    @ColumnInfo(name = CityWeatherDatabaseContract.CityWeather.COLUMN_NAME_ID)
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = CityWeatherDatabaseContract.CityWeather.COLUMN_NAME_CITY_NAME)
    val cityName: String,

    @ColumnInfo(name = CityWeatherDatabaseContract.CityWeather.COLUMN_NAME_CITY_ID)
    val cityId: String,

    @ColumnInfo(name = CityWeatherDatabaseContract.CityWeather.COLUMN_NAME_ID_AT_LIST)
    var idAtList: Int?
)
