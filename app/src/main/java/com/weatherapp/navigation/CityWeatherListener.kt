package com.weatherapp.navigation

import android.view.View
import com.weatherapp.models.entities.DatabaseCity

interface CityWeatherListener {
    fun openCityWeather(city: DatabaseCity, originView: View)
}