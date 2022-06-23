package com.weatherapp.models.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherNow(
    val obsTime: String,
    val temp: String,
    val feelsLike: String,
    val icon: String,
    val text: String,
    val wind360: String,
    val windDir: String,
    val windScale: String,
    val windSpeed: String,
    val humidity: String,
    @SerialName("precip")
    val precipitation: String,
    val pressure: String,
    @SerialName("vis")
    val visibility: String,
    val cloud: String?,
    val dew: String?
)
