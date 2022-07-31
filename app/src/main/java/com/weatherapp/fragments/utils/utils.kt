package com.weatherapp.fragments.utils

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.models.entities.SimpleWeatherForCity
import com.weatherapp.models.entities.WeatherNow
import com.weatherapp.models.entities.WeatherOnDay
import com.weatherapp.providers.ResourceProvider

@SuppressLint("UseCompatLoadingForDrawables")
fun getDrawable(name: String, resourceProvider: ResourceProvider): Drawable {
    val resourceId =
        resourceProvider.resources.getIdentifier(name, "drawable", resourceProvider.packageName)
    return resourceProvider.resources.getDrawable(resourceId)
}

fun toSimpleCity(city: DatabaseCity, pair: Pair<WeatherNow, List<WeatherOnDay>>) =
    SimpleWeatherForCity(
        city.cityName,
        city.cityId,
        city.timezone,
        pair.first.temp,
        pair.first.text,
        pair.second[0].tempMax,
        pair.second[0].tempMin,
        pair.second,
        pair.second[0].uvIndex,
        pair.second[0].sunset,
        pair.first.windSpeed,
        pair.first.windDir,
        pair.first.humidity,
        pair.first.visibility,
        pair.first.precipitation
    )