package com.weatherapp.models.network

import com.weatherapp.models.entities.CityResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface CityApi {
    @GET("city/lookup?")
    fun getCity(
        @Query("location") location: String,
        @Query("lang") language: String,
        @Query("key") key: String
    ): Single<CityResponse>
}