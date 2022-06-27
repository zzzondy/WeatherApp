package com.weatherapp.fragments.adapters

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.BuildConfig
import com.weatherapp.R
import com.weatherapp.databinding.ViewHolderCityWeatherBinding
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.models.network.WeatherApiModule
import com.weatherapp.navigation.CityWeatherListener
import com.weatherapp.providers.ResourceProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

class CityWeatherViewHolder(private val itemBinding: ViewHolderCityWeatherBinding) :
    RecyclerView.ViewHolder(itemBinding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(
        city: DatabaseCity,
        resourceProvider: ResourceProvider,
        subscriptions: CompositeDisposable,
        cityWeatherCallback: CityWeatherListener
    ) {
        itemBinding.root.setOnClickListener {
            cityWeatherCallback.openCityWeather(city)
        }
        val weatherApiModule = WeatherApiModule()
        itemBinding.nameCity.text = city.cityName
        weatherApiModule.weatherService.getWeatherNow(
            city.cityId,
            resourceProvider.language,
            BuildConfig.API_KEY
        )
            .map { it.now }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { weatherNow ->
                itemBinding.weatherTemp.text = "${weatherNow.temp} ${
                    resourceProvider.resources.getString(
                        R.string.celsius
                    )
                }"
                itemBinding.weatherTextCity.text = weatherNow.text
            })
            .addTo(subscriptions)

        weatherApiModule.weatherService.getWeatherOnDays(
            city.cityId,
            resourceProvider.language,
            BuildConfig.API_KEY
        )
            .map { it.daily[0] }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { weatherOnDay ->
                itemBinding.maxTemp.text =
                    "${resourceProvider.resources.getString(R.string.max)}: ${weatherOnDay.tempMax}"
                itemBinding.minTemp.text =
                    "${resourceProvider.resources.getString(R.string.min)}: ${weatherOnDay.tempMin}"
            })
            .addTo(subscriptions)

    }
}