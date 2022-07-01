package com.weatherapp.models.entities

import com.weatherapp.fragments.states.SearchResult
import kotlinx.serialization.Serializable

@Serializable
data class CityResponse(
    val code: String,
    val location: List<City>,
    val refer: References
)
