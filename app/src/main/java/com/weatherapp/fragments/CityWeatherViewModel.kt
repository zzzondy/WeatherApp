package com.weatherapp.fragments

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherapp.BuildConfig
import com.weatherapp.R
import com.weatherapp.models.entities.WeatherOnDay
import com.weatherapp.models.entities.WeatherOnHour
import com.weatherapp.models.network.WeatherApiModule
import com.weatherapp.providers.ResourceProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers


class CityWeatherViewModel(private val cityName: String, resourceProvider: ResourceProvider) :
    ViewModel() {

    private val weatherApiModule = WeatherApiModule.getInstance()
    private val context = resourceProvider.context

    private val resources = resourceProvider.resources
    private val language = resourceProvider.language

    private val subscriptions = CompositeDisposable()

    private val mutableCityNameLiveData = MutableLiveData<String>()
    val cityNameLiveData: LiveData<String> get() = mutableCityNameLiveData

    private val mutableTempNowLiveData = MutableLiveData<String>()
    val tempNowLiveData: LiveData<String> get() = mutableTempNowLiveData

    private val mutableWeatherTextLiveData = MutableLiveData<String>()
    val weatherTextLiveData: LiveData<String> get() = mutableWeatherTextLiveData

    private val mutableMaxTempLiveData = MutableLiveData<String>()
    val maxTempLiveData: LiveData<String> get() = mutableMaxTempLiveData

    private val mutableMinTempLiveData = MutableLiveData<String>()
    val minTempLiveData: LiveData<String> get() = mutableMinTempLiveData

    private val mutableHourlyForecastLiveData = MutableLiveData<List<WeatherOnHour>>()
    val hourlyForecastLiveData: LiveData<List<WeatherOnHour>> get() = mutableHourlyForecastLiveData

    private val mutable3DaysForecastLiveData = MutableLiveData<List<WeatherOnDay>>()
    val days3ForecastLiveData: LiveData<List<WeatherOnDay>> get() = mutable3DaysForecastLiveData

    private val mutableUvIndexLiveData = MutableLiveData<String>()
    val uvIndexLiveData: LiveData<String> get() = mutableUvIndexLiveData

    private val mutableUvIndexTextLiveData = MutableLiveData<String>()
    val uvIndexTextLiveData: LiveData<String> get() = mutableUvIndexTextLiveData

    private val mutableWindSpeedLiveData = MutableLiveData<String>()
    val windSpeedLiveData: LiveData<String> get() = mutableWindSpeedLiveData

    private val mutableWindDirectionLiveData = MutableLiveData<String>()
    val windDirectionLiveData: LiveData<String> get() = mutableWindDirectionLiveData

    private val mutableSunsetLiveData = MutableLiveData<String>()
    val sunsetLiveData: LiveData<String> get() = mutableSunsetLiveData

    private val mutableHumidityLiveData = MutableLiveData<String>()
    val humidityLiveData: LiveData<String> get() = mutableHumidityLiveData

    private val mutableVisibilityLiveData = MutableLiveData<String>()
    val visibilityLiveData: LiveData<String> get() = mutableVisibilityLiveData

    private val mutablePrecipitationLiveData = MutableLiveData<String>()
    val precipitationLiveData: LiveData<String> get() = mutablePrecipitationLiveData

    init {
        mutableCityNameLiveData.value = cityName
        getWeatherForCity()
    }

    private fun getWeatherForNow(location: String) {
        weatherApiModule.weatherService.getWeatherNow(location, language, BuildConfig.API_KEY)
            .map { it.now }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { weatherNow ->
                mutableWeatherTextLiveData.value = weatherNow.text
                mutableTempNowLiveData.value = weatherNow.temp
                mutablePrecipitationLiveData.value = weatherNow.precipitation
                mutableVisibilityLiveData.value = weatherNow.visibility
                mutableWindDirectionLiveData.value = weatherNow.windDir
                mutableWindSpeedLiveData.value = weatherNow.windSpeed
                mutableHumidityLiveData.value = weatherNow.humidity
            }, onError = {
                showExceptionToast(it)
            })
            .addTo(subscriptions)
    }

    private fun getWeatherOnDays(location: String) {
        weatherApiModule.weatherService.getWeatherOnDays(location, language, BuildConfig.API_KEY)
            .map { it.daily }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { weatherOnDays ->
                mutable3DaysForecastLiveData.value = weatherOnDays
                mutableSunsetLiveData.value = weatherOnDays[0].sunset
                mutableUvIndexLiveData.value = weatherOnDays[0].uvIndex
                mutableMaxTempLiveData.value = weatherOnDays[0].tempMax
                mutableMinTempLiveData.value = weatherOnDays[0].tempMin
                mutableUvIndexTextLiveData.value = when (weatherOnDays[0].uvIndex.toInt()) {
                    in 0..2 -> resources.getString(R.string.low)
                    in 3..5 -> resources.getString(R.string.middle)
                    in 6..7 -> resources.getString(R.string.high)
                    in 8..10 -> resources.getString(R.string.very_high)
                    else -> resources.getString(R.string.extremal)
                }
            }, onError = { showExceptionToast(it) })
            .addTo(subscriptions)
    }

    private fun getWeatherForCity() {
        weatherApiModule.cityService.getCity(cityName, language, BuildConfig.API_KEY)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { cityResponse ->
                val cityId = cityResponse.location[0].id
                getWeatherForNow(cityId)
                getWeatherOnDays(cityId)
            }, onError = { showExceptionToast(it) })
            .addTo(subscriptions)

    }

    private fun showExceptionToast(e: Throwable) {
        Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
        Log.println(Log.ASSERT, "exception", e.toString())
    }

    fun onClear() {
        subscriptions.dispose()
    }


}