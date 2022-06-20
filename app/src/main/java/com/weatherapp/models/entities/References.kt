package com.weatherapp.models.entities

import kotlinx.serialization.Serializable

@Serializable
data class References(
    val sources: List<String>,
    val license: List<String>
)
