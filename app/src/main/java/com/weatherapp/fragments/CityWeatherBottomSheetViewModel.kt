package com.weatherapp.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherapp.BuildConfig
import com.weatherapp.R
import com.weatherapp.database.CityWeatherRepository
import com.weatherapp.fragments.states.BackgroundState
import com.weatherapp.fragments.states.LoadingState
import com.weatherapp.fragments.states.ResultState
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.models.entities.WeatherOnDay
import com.weatherapp.models.network.WeatherApiModule
import com.weatherapp.providers.ResourceProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import java.util.*

class CityWeatherBottomSheetViewModel(
    resourceProvider: ResourceProvider,
    private val cityWeatherRepository: CityWeatherRepository
) : ViewModel() {
    private val weatherApiModule = WeatherApiModule.getInstance()
    private val language = resourceProvider.language
    private val resources = resourceProvider.resources

    private val subscriptions = CompositeDisposable()


    private val mutableAddButtonState = MutableLiveData<Boolean>()
    val addButtonState: LiveData<Boolean> get() = mutableAddButtonState

    private val mutableLoadingState = MutableLiveData<LoadingState>()
    val loadingState: LiveData<LoadingState> get() = mutableLoadingState

    private val mutableResultState = MutableLiveData<ResultState>()
    val resultState: LiveData<ResultState> get() = mutableResultState

    private val mutableBackgroundState = MutableLiveData<BackgroundState>()
    val backgroundState: LiveData<BackgroundState> get() = mutableBackgroundState

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

    fun getWeatherForCity(city: DatabaseCity) {
        checkCityAtDatabase(city)
        mutableLoadingState.value = LoadingState.LOADING
        mutableCityNameLiveData.value = city.cityName
        mutableBackgroundState.value = getBackgroundState(city.timezone)
        weatherApiModule.weatherService.getWeatherNow(city.cityId, language, BuildConfig.API_KEY)
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
            .subscribeBy(
                onSuccess = { pair ->
                    mutableLoadingState.value = LoadingState.READY
                    mutableResultState.value = ResultState.SUCCESS
                    mutable3DaysForecastLiveData.value = pair.second
                    mutableSunsetLiveData.value = pair.second[0].sunset
                    mutableUvIndexLiveData.value = pair.second[0].uvIndex
                    mutableMaxTempLiveData.value = pair.second[0].tempMax
                    mutableMinTempLiveData.value = pair.second[0].tempMin
                    mutableUvIndexTextLiveData.value = when (pair.second[0].uvIndex.toInt()) {
                        in 0..2 -> resources.getString(R.string.low)
                        in 3..5 -> resources.getString(R.string.middle)
                        in 6..7 -> resources.getString(R.string.high)
                        in 8..10 -> resources.getString(R.string.very_high)
                        else -> resources.getString(R.string.extremal)
                    }

                    mutableWeatherTextLiveData.value = pair.first.text
                    mutableTempNowLiveData.value = pair.first.temp
                    mutablePrecipitationLiveData.value = pair.first.precipitation
                    if (pair.first.precipitation.toFloat() in 0.1..2.0)
                        mutableBackgroundState.value = BackgroundState.LIGHT_RAIN
                    else if (pair.first.precipitation.toFloat() > 2)
                        mutableBackgroundState.value = BackgroundState.HEAVY_RAIN
                    mutableVisibilityLiveData.value = pair.first.visibility
                    mutableWindDirectionLiveData.value = pair.first.windDir
                    mutableWindSpeedLiveData.value = pair.first.windSpeed
                    mutableHumidityLiveData.value = pair.first.humidity
                },
                onError = {
                    mutableLoadingState.value = LoadingState.READY
                    mutableResultState.value = ResultState.ERROR
                }
            )
    }

    fun addCityToDatabase(city: DatabaseCity) {
        subscriptions.add(
            cityWeatherRepository.getNumberOfCities()
                .subscribeOn(Schedulers.io())
                .flatMap {
                    cityWeatherRepository.addNewCity(
                        DatabaseCity(
                            city.cityName,
                            city.cityId,
                            city.timezone,
                            it
                        )
                    )
                        .subscribeOn(Schedulers.io())
                }
                .subscribeBy()
        )
    }

    private fun getBackgroundState(timezone: String) =
        when (Calendar.getInstance(TimeZone.getTimeZone(timezone)).get(Calendar.HOUR_OF_DAY)) {
            in 0..3 -> BackgroundState.NIGHT
            in 22..23 -> BackgroundState.NIGHT
            in 4..10 -> BackgroundState.MORNING
            in 11..17 -> BackgroundState.NOON
            else -> BackgroundState.EVENING
        }

    private fun checkCityAtDatabase(city: DatabaseCity) {
        cityWeatherRepository.getCityById(city.cityId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { mutableAddButtonState.value = false },
                onComplete = { mutableAddButtonState.value = true },
                onError = {})
            .addTo(subscriptions)
    }

    fun onClear() {
        subscriptions.dispose()
    }
}
