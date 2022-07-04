package com.weatherapp.database

object CityWeatherDatabaseContract {
    const val DATABASE_NAME = "CityWeather.db"
    object CityWeather {
        const val TABLE_NAME = "city_weather"

        const val COLUMN_NAME_ID = "id"
        const val COLUMN_NAME_CITY_NAME = "city_name"
        const val COLUMN_NAME_CITY_ID = "city_id"
        const val COLUMN_NAME_ID_AT_LIST = "id_at_list"
        const val COLUMN_NAME_TIMEZONE = "timezone"
    }
}