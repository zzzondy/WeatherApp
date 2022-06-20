package com.weatherapp.models.entities

import kotlinx.serialization.Serializable

@Serializable
data class WeatherOnDayResponse(
    val code: String,
    val updateTime: String,
    val fxLink: String,
    val daily: List<WeatherOnDay>
)