package com.weatherapp.navigation

import android.view.View
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.models.entities.SimpleWeatherForCity

interface CityWeatherListener {
    fun openCityWeather(
        city: DatabaseCity,
        originView: View,
        cityWeather: SimpleWeatherForCity? = null
    )
}