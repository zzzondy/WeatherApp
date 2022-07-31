package com.weatherapp.models.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherOnHour(
    val fxTime: String,
    val temp: String,
    val icon: String,
    val text: String,
    val wind360: String,
    val windDir: String,
    val windScale: String,
    val windSpeed: String,
    val humidity: String,
    @SerialName("precip")
    val precipitation: String,
    val pop: String?,
    val pressure: String,
    val cloud: String?,
    val dew: String?
)
