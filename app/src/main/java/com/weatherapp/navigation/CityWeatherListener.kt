package com.weatherapp.navigation

import com.weatherapp.models.entities.DatabaseCity

interface CityWeatherListener {
    fun openCityWeather(city: DatabaseCity)
}