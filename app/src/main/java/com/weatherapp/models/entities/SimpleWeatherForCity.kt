package com.weatherapp.models.entities

data class SimpleWeatherForCity(
    val cityName: String,
    val cityId: String,
    val tempNow: String,
    val textWeather: String,
    val tempMax: String,
    val tempMin: String,
    val daysForecast: List<WeatherOnDay>,
    val uvIndex: String,
    val sunset: String,
    val windSpeed: String,
    val windDirection: String,
    val humidity: String,
    val visibility: String,
    val precipitation: String
)
