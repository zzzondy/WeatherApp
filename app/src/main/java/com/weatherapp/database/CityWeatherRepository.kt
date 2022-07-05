package com.weatherapp.database

import android.content.Context
import android.util.Log
import com.weatherapp.models.entities.DatabaseCity
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable

class CityWeatherRepository(applicationContext: Context) {
    private val database = CityWeatherDatabase.create(applicationContext)

    private val subscriptions = CompositeDisposable()

    private fun toEntity(city: DatabaseCity) = CityWeatherEntity(
        cityName = city.cityName,
        cityId = city.cityId,
        timezone = city.timezone,
        idAtList = city.idAtList
    )

    fun toCity(entity: CityWeatherEntity) = DatabaseCity(
        cityName = entity.cityName,
        cityId = entity.cityId,
        timezone = entity.timezone,
        idAtList = entity.idAtList
    )

    fun getAllCities(): Single<List<CityWeatherEntity>> = database.cityWeatherDao.getAll()

    fun addNewCity(city: DatabaseCity): Single<Long> {
        return Single.fromCallable {
            database.cityWeatherDao.insertCityWeather(toEntity(city))
        }
    }

    fun deleteById(id: String) {
        database.cityWeatherDao.deleteCityWeatherById(id)
    }

    fun updateCity(id: String, idAtList: Int) {
        database.cityWeatherDao.updateCity(id, idAtList)
        Log.println(Log.ASSERT, "update", "yes")
    }

    fun getNumberOfCities(): Single<Int> {
        return Single.fromCallable { database.cityWeatherDao.getNumberOfCities() }
    }

    fun getCityById(id: String) = database.cityWeatherDao.getCityById(id)

    fun onClear() {
        subscriptions.dispose()
    }
}