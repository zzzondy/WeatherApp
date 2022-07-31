package com.weatherapp.models.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.weatherapp.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory

class WeatherApiModule {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()

    private val json = Json {
        ignoreUnknownKeys = false
    }

    val weatherService = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_WEATHER_URL)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .client(client)
        .build()
        .create(WeatherApi::class.java)

    val cityService = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_GEO_URL)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .client(client)
        .build()
        .create(CityApi::class.java)


    companion object {
        fun getInstance() = WeatherApiModule()
    }
}