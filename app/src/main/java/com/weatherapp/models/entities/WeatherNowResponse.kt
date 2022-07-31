package com.weatherapp.models.entities

import kotlinx.serialization.Serializable

@Serializable
data class WeatherNowResponse(
    val code: String,
    val updateTime: String,
    val fxLink: String,
    val now: WeatherNow,
    val refer: References
)
