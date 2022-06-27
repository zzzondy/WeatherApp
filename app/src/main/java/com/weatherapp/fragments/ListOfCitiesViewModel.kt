package com.weatherapp.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherapp.database.CityWeatherRepository
import com.weatherapp.models.entities.DatabaseCity
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

class ListOfCitiesViewModel(
    private val cityWeatherRepository: CityWeatherRepository
) :
    ViewModel() {
    private val mutableListOfCitiesLiveData = MutableLiveData<List<DatabaseCity>>()
    val listOfCitiesLiveData: LiveData<List<DatabaseCity>> get() = mutableListOfCitiesLiveData


    private val subscriptions = CompositeDisposable()

    init {
        getCities()
    }

    fun getCities() {
        cityWeatherRepository.getAllCities()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { it.map { cityWeatherRepository.toCity(it) } }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { mutableListOfCitiesLiveData.value = it })
            .addTo(subscriptions)
    }

    fun deleteCity(city: DatabaseCity) {
        subscriptions.add(Single.fromCallable { cityWeatherRepository.deleteById(city.cityId) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { getCities() }))
    }

    fun addCity(city: DatabaseCity) {
        subscriptions.add(
            cityWeatherRepository.addNewCity(city)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onSuccess = { getCities() })
        )
    }

    fun onClear() {
        subscriptions.dispose()
    }

}