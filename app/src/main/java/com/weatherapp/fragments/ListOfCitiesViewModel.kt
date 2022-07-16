package com.weatherapp.fragments

import android.Manifest
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherapp.BuildConfig
import com.weatherapp.database.CityWeatherRepository
import com.weatherapp.fragments.states.BackgroundState
import com.weatherapp.fragments.states.ListState
import com.weatherapp.fragments.utils.toSimpleCity
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.models.entities.SimpleWeatherForCity
import com.weatherapp.models.network.WeatherApiModule
import com.weatherapp.providers.GetUserLocationResult
import com.weatherapp.providers.LocationProvider
import com.weatherapp.providers.ResourceProvider
import com.weatherapp.receivers.NetworkChangeListener
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import java.util.*

class ListOfCitiesViewModel(
    private val resourceProvider: ResourceProvider,
    private val cityWeatherRepository: CityWeatherRepository,
    private val locationProvider: LocationProvider,
    private val networkChangeListener: NetworkChangeListener
) :
    ViewModel() {
    private val weatherApiModule = WeatherApiModule.getInstance()

    private val language = resourceProvider.language

    private var locationResult: Location? = null

    private val mutableListOfCitiesLiveData = MutableLiveData<List<DatabaseCity>>()
    val listOfCitiesLiveData: LiveData<List<DatabaseCity>> get() = mutableListOfCitiesLiveData

    private val mutableResultState = MutableLiveData<ListState>()
    val resultState: LiveData<ListState> get() = mutableResultState

    private val mutableLocationLiveData = MutableLiveData<GetUserLocationResult>()
    val locationLiveData: LiveData<GetUserLocationResult> get() = mutableLocationLiveData

    private val mutableBackgroundLiveData = MutableLiveData<BackgroundState>()
    val backgroundLiveData: LiveData<BackgroundState> get() = mutableBackgroundLiveData

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
                if (listOfCities.isEmpty())
                    mutableResultState.value = ListState.EMPTY
                else
                    mutableResultState.value = ListState.NOT_EMPTY
                mutableListOfCitiesLiveData.value = listOfCities.sortedBy { it.idAtList }
            })
            .addTo(subscriptions)
    }

    fun deleteCity(city: DatabaseCity) {
        subscriptions.add(Single.fromCallable { cityWeatherRepository.deleteById(city.cityId) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { getCities() }))
    }

    fun updatePosition(city: DatabaseCity, newIdAtList: Int) {
        subscriptions.add(
            Single.fromCallable { cityWeatherRepository.updateCity(city.cityId, newIdAtList) }
                .subscribeOn(Schedulers.io())
                .subscribeBy()
        )
    }

    fun requestLocation() {
        val weatherFromLocation = getWeatherForLocationFromPref()
        if (weatherFromLocation != null) {
            mutableBackgroundLiveData.value = getBackgroundTime(weatherFromLocation.timeZone)
            mutableLocationLiveData.value = GetUserLocationResult.Success(weatherFromLocation)
            return
        }
        locationProvider.requestLocation()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { location ->
                    locationResult = location
                    val longitude = Math.round(location.longitude * 100) / 100.0
                    val latitude = Math.round(location.latitude * 100) / 100.0
                    getWeatherForLocation("${longitude},${latitude}")
                },
                onError = {
                    mutableLocationLiveData.value = GetUserLocationResult.Failed.OtherFailure
                },
                onComplete = {
                    if (locationResult == null)
                        mutableLocationLiveData.value = GetUserLocationResult.Failed.OtherFailure
                }
            )
            .addTo(subscriptions)

    }

    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    private fun getWeatherForLocation(location: String) {
        weatherApiModule.cityService.getCity(location, language, BuildConfig.API_KEY)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { city ->
                DatabaseCity(
                    city.location[0].name,
                    city.location[0].id,
                    city.location[0].timezone,
                    null
                )
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { city ->
                mutableBackgroundLiveData.value = getBackgroundTime(city.timezone)
                weatherApiModule.weatherService.getWeatherNow(
                    city.cityId,
                    language,
                    BuildConfig.API_KEY
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .zipWith(
                        weatherApiModule.weatherService.getWeatherOnDays(
                            city.cityId,
                            language,
                            BuildConfig.API_KEY
                        )
                            .subscribeOn(Schedulers.io())
                    )
                    .map { pair -> pair.first.now to pair.second.daily }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onSuccess = { pair ->
                        mutableLocationLiveData.value =
                            GetUserLocationResult.Success(toSimpleCity(city, pair))
                        resourceProvider.updateCacheWithLocationWeather(toSimpleCity(city, pair))
                    }, onError = { networkChangeListener.setNetworkReceiver() })
                    .addTo(subscriptions)
            }, onError = { networkChangeListener.setNetworkReceiver() })
            .addTo(subscriptions)
    }

    private fun getWeatherForLocationFromPref(): SimpleWeatherForCity? {
        val weatherFromPref = resourceProvider.getLocationFromPref()
        return if (checkPassedTime() && weatherFromPref != null)
            weatherFromPref
        else
            null
    }

    private fun checkPassedTime(): Boolean {
        val passedTime = resourceProvider.getUpdateTime()
        return (passedTime != null && (Calendar.getInstance().time.time - passedTime.time) / (60 * 1000) <= 5)
    }

    private fun getBackgroundTime(timezone: String): BackgroundState {
        return when (Calendar.getInstance(TimeZone.getTimeZone(timezone))
            .get(Calendar.HOUR_OF_DAY)) {
            in 0..3 -> BackgroundState.NIGHT
            in 22..23 -> BackgroundState.NIGHT
            in 4..10 -> BackgroundState.MORNING
            in 11..17 -> BackgroundState.NOON
            else -> BackgroundState.EVENING
        }
    }


    fun onClear() {
        subscriptions.dispose()
    }

}