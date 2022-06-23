package com.weatherapp.models.entities

import kotlinx.serialization.Serializable

@Serializable
data class WeatherOnDaysResponse(
    val code: String,
    val updateTime: String,
    val fxLink: String,
    val daily: List<WeatherOnDay>,
    val refer: References
)