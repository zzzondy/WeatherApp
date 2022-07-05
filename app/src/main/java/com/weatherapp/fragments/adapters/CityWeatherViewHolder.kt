package com.weatherapp.fragments.adapters

import android.annotation.SuppressLint
import com.weatherapp.fragments.utils.getDrawable
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

        if (cityCalendar.get(Calendar.MINUTE) in 0..9)
            itemBinding.timeCity.text = "${cityHour}:0${cityCalendar.get(Calendar.MINUTE)}"
        else
            itemBinding.timeCity.text = "${cityHour}:${cityCalendar.get(Calendar.MINUTE)}"

        when (cityHour) {
            in 0..3 -> itemBinding.root.background = getDrawable("night_gradient", resourceProvider)
            in 22..23 -> itemBinding.root.background =
                getDrawable("night_gradient", resourceProvider)
            in 4..10 -> itemBinding.root.background =
                getDrawable("morning_gradient", resourceProvider)
            in 11..17 -> itemBinding.root.background =
                getDrawable("noon_gradient", resourceProvider)
            in 18..21 -> itemBinding.root.background =
                getDrawable("evening_gradient", resourceProvider)
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
            if (cityFromPreferences.precipitation.toFloat() > 1 && cityFromPreferences.precipitation.toFloat() < 10)
                itemBinding.root.background = getDrawable("light_rain_gradient", resourceProvider)
            else if (cityFromPreferences.precipitation.toFloat() > 10)
                itemBinding.root.background = getDrawable("heavy_rain_gradient", resourceProvider)
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