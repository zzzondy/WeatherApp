package com.weatherapp.models.entities

import kotlinx.serialization.Serializable

@Serializable
data class CityResponse(
    val code: String,
    val location: List<City>,
    val refer: References
)
