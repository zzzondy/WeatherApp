package com.weatherapp.models.entities

data class DatabaseCity(
    val cityName: String,
    val cityId: String,
    val timezone: String,
    var idAtList: Int?
)