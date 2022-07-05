package com.weatherapp.fragments

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherapp.database.CityWeatherRepository
import com.weatherapp.models.entities.DatabaseCity
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
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
            .subscribeBy(onSuccess = { listOfCities ->
                mutableListOfCitiesLiveData.value = listOfCities.sortedBy { it.idAtList }
            },
                onError = { Log.e("error", it.toString()) })
            .addTo(subscriptions)
    }

    fun deleteCity(city: DatabaseCity) {
        subscriptions.add(Single.fromCallable { cityWeatherRepository.deleteById(city.cityId) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { getCities() }, onError = { Log.e("error", it.toString()) }))
    }

    fun updatePosition(city: DatabaseCity, newIdAtList: Int) {
        subscriptions.add(
            Single.fromCallable { cityWeatherRepository.updateCity(city.cityId, newIdAtList) }
                .subscribeOn(Schedulers.io())
                .subscribeBy(onError = { Log.e("error", it.toString()) })
        )
    }

    fun onClear() {
        subscriptions.dispose()
    }

}