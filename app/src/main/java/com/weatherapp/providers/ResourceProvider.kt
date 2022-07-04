package com.weatherapp.providers

import android.content.Context
import android.content.res.Resources
import com.google.gson.Gson
import com.weatherapp.models.entities.SimpleWeatherForCity
import java.util.*

class ResourceProvider(val context: Context) {
    val language = context.resources.configuration.locale.toLanguageTag().toString().split("-")[0]
    val resources: Resources = context.resources
    val packageName: String = context.packageName
    private val sharedPreferences = context.getSharedPreferences(TEMP_DATA, Context.MODE_PRIVATE)

    fun updateCache(cityTag: String, city: SimpleWeatherForCity) {
        sharedPreferences.edit().apply {
            putString(cityTag, toJson(city))
            putString(UPDATE_TIME, toJson(Calendar.getInstance().time))
            apply()
        }
    }

    fun deleteCache() {
        sharedPreferences.edit().apply {
            clear()
            apply()
        }
    }

    fun getFromPref(cityTag: String) = fromJson(sharedPreferences.getString(cityTag, null))

    fun getUpdateTime() = dateFromJson(sharedPreferences.getString(UPDATE_TIME, null))

    private fun toJson(city: SimpleWeatherForCity) = Gson().toJson(city)

    private fun toJson(date: Date) = Gson().toJson(date)

    private fun fromJson(city: String?): SimpleWeatherForCity? {
        return Gson().fromJson(city, SimpleWeatherForCity::class.java)
    }

    private fun dateFromJson(date: String?): Date? = Gson().fromJson(date, Date::class.java)

    companion object {
        const val TEMP_DATA = "TEMP_DATA"
        const val UPDATE_TIME = "UPDATE_TIME"
    }
}