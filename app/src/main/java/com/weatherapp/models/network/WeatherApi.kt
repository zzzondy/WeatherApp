package com.weatherapp.models.network

import com.weatherapp.models.entities.WeatherNowResponse
import com.weatherapp.models.entities.WeatherOnDaysResponse
import com.weatherapp.models.entities.WeatherOnHourResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("weather/now?")
    fun getWeatherNow(
        @Query("location") location: String,
        @Query("lang") language: String,
        @Query("key") key: String
    ): Single<WeatherNowResponse>

    @GET("weather/3d?")
    fun getWeatherOnDays(
        @Query("location") location: String,
        @Query("lang") language: String,
        @Query("key") key: String
    ): Single<WeatherOnDaysResponse>

    @GET("weather/24h?")
    fun getHourlyWeather(
        @Query("location") location: String,
        @Query("lang") language: String,
        @Query("key") key: String
    ): Single<WeatherOnHourResponse>
}