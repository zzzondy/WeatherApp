package com.weatherapp.models.entities

import kotlinx.serialization.Serializable

@Serializable
data class WeatherOnHourResponse(
    val code: String,
    val updateTime: String,
    val fxLink: String,
    val hourly: List<WeatherOnHour>,
    val refer: References
)
