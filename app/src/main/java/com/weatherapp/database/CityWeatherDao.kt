package com.weatherapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.weatherapp.models.entities.DatabaseCity
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface CityWeatherDao {
    @Query("SELECT * FROM city_weather ORDER BY id ASC")
    fun getAll(): Single<List<CityWeatherEntity>>

    @Insert
    fun insertCityWeather(cityWeatherEntity: CityWeatherEntity): Long

    @Query("DELETE FROM city_weather WHERE city_id = :id")
    fun deleteCityWeatherById(id: String)

    @Query("UPDATE city_weather SET id_at_list = :idAtList WHERE city_id = :id")
    fun updateCity(id: String, idAtList: Int)

    @Query("SELECT COUNT(id) FROM city_weather")
    fun getNumberOfCities(): Int

    @Query("SELECT * FROM city_weather WHERE city_id = :id")
    fun getCityById(id: String): Maybe<CityWeatherEntity>
}