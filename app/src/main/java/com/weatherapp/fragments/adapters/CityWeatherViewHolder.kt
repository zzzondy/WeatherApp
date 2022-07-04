package com.weatherapp.fragments.adapters

import android.annotation.SuppressLint
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.BuildConfig
import com.weatherapp.R
import com.weatherapp.databinding.ViewHolderCityWeatherBinding
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.models.entities.SimpleWeatherForCity
import com.weatherapp.models.entities.WeatherNow
import com.weatherapp.models.entities.WeatherOnDay
import com.weatherapp.models.network.WeatherApiModule
import com.weatherapp.navigation.CityWeatherListener
import com.weatherapp.providers.ResourceProvider
import com.weatherapp.receivers.NetworkChangeListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

class CityWeatherViewHolder(private val itemBinding: ViewHolderCityWeatherBinding) :
    RecyclerView.ViewHolder(itemBinding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(
        city: DatabaseCity,
        resourceProvider: ResourceProvider,
        subscriptions: CompositeDisposable,
        cityWeatherCallback: CityWeatherListener,
        networkChangeListener: NetworkChangeListener
    ) {
        val weatherApiModule = WeatherApiModule.getInstance()
        itemBinding.nameCity.text = city.cityName
        val cityCalendar = Calendar.getInstance(TimeZone.getTimeZone(city.timezone))
        val cityHour = cityCalendar.get(Calendar.HOUR_OF_DAY)
        itemBinding.timeCity.text = "${cityHour}:${cityCalendar.get(Calendar.MINUTE)}"
        when (cityHour) {
            in 0..3 -> itemBinding.root.setBackgroundColor(resourceProvider.resources.getColor(R.color.night))
            in 22..23 -> itemBinding.root.setBackgroundColor(resourceProvider.resources.getColor(R.color.night))
            in 4..10 -> itemBinding.root.setBackgroundColor(resourceProvider.resources.getColor(R.color.morning))
            in 11..17 -> itemBinding.root.setBackgroundColor(resourceProvider.resources.getColor(R.color.clearSky))
            in 18..21 -> itemBinding.root.setBackgroundColor(resourceProvider.resources.getColor(R.color.evening))
        }
        val cityFromPreferences = resourceProvider.getFromPref(city.cityId)
        val passedTime = resourceProvider.getUpdateTime()
        if (cityFromPreferences != null && passedTime != null && (Calendar.getInstance().time.time - passedTime.time) / (60 * 1000) <= 5) {
            itemBinding.weatherTemp.text =
                "${cityFromPreferences.tempNow} ${resourceProvider.resources.getString(R.string.celsius)}"
            itemBinding.weatherTextCity.text = cityFromPreferences.textWeather
            itemBinding.minTemp.text =
                "${resourceProvider.resources.getString(R.string.min)}: ${cityFromPreferences.tempMin}"
            itemBinding.maxTemp.text =
                "${resourceProvider.resources.getString(R.string.max)}: ${cityFromPreferences.tempMax}"
            itemBinding.root.setOnClickListener {
                cityWeatherCallback.openCityWeather(city, itemBinding.root, cityFromPreferences)
            }
        } else {
            weatherApiModule.weatherService.getWeatherNow(
                city.cityId,
                resourceProvider.language,
                BuildConfig.API_KEY
            )
                .subscribeOn(Schedulers.io())
                .zipWith(
                    weatherApiModule.weatherService.getWeatherOnDays(
                        city.cityId,
                        resourceProvider.language,
                        BuildConfig.API_KEY
                    )
                        .subscribeOn(Schedulers.io())
                ).observeOn(Schedulers.io())
                .map { pair -> pair.first.now to pair.second.daily }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onSuccess = { pair ->
                    itemBinding.weatherTemp.text =
                        "${pair.first.temp} ${resourceProvider.resources.getString(R.string.celsius)}"
                    itemBinding.weatherTextCity.text = pair.first.text
                    itemBinding.maxTemp.text =
                        "${resourceProvider.resources.getString(R.string.max)}: ${pair.second[0].tempMax}"
                    itemBinding.minTemp.text =
                        "${resourceProvider.resources.getString(R.string.min)}: ${pair.second[0].tempMin}"
                    resourceProvider.updateCache(city.cityId, toSimpleCity(city, pair))
                    itemBinding.root.setOnClickListener {
                        cityWeatherCallback.openCityWeather(
                            city,
                            itemBinding.root,
                            toSimpleCity(city, pair)
                        )
                    }

                }, onError = {
                    networkChangeListener.setNetworkReceiver()
                    itemBinding.root.setOnClickListener {
                        cityWeatherCallback.openCityWeather(
                            city,
                            itemBinding.root
                        )
                    }
                })
                .addTo(subscriptions)
        }

    }

    private fun toSimpleCity(city: DatabaseCity, pair: Pair<WeatherNow, List<WeatherOnDay>>) =
        SimpleWeatherForCity(
            city.cityName,
            city.cityId,
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
}