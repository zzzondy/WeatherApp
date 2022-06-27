package com.weatherapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.reactivex.Single

@Dao
interface CityWeatherDao {
    @Query("SELECT * FROM city_weather ORDER BY id ASC")
    fun getAll(): Single<List<CityWeatherEntity>>

    @Insert
    fun insertCityWeather(cityWeatherEntity: CityWeatherEntity): Long

    @Query("DELETE FROM city_weather WHERE city_id = :id")
    fun deleteCityWeatherById(id: String)
}