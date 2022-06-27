package com.weatherapp.fragments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.weatherapp.databinding.ViewHolderCityWeatherBinding
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.navigation.CityWeatherListener
import com.weatherapp.providers.ResourceProvider
import io.reactivex.disposables.CompositeDisposable

class CityWeatherAdapter(private val resourceProvider: ResourceProvider, private val cityWeatherCallBack: CityWeatherListener) :
    ListAdapter<DatabaseCity, CityWeatherViewHolder>(CityWeatherDiffCallback()) {

    private val subscriptions = CompositeDisposable()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityWeatherViewHolder {
        val itemBinding =
            ViewHolderCityWeatherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CityWeatherViewHolder((itemBinding))
    }

    override fun onBindViewHolder(holder: CityWeatherViewHolder, position: Int) {
        val city = getItem(position)
        holder.bind(city, resourceProvider, subscriptions, cityWeatherCallBack)
    }

    fun getCityWithId(position: Int): DatabaseCity = getItem(position)

    fun onClear() {
        subscriptions.dispose()
    }

}

private class CityWeatherDiffCallback : DiffUtil.ItemCallback<DatabaseCity>() {
    private val payload = Any()

    override fun areItemsTheSame(oldItem: DatabaseCity, newItem: DatabaseCity): Boolean =
        (oldItem.cityId == newItem.cityId)

    override fun areContentsTheSame(oldItem: DatabaseCity, newItem: DatabaseCity): Boolean =
        (oldItem == newItem)

    override fun getChangePayload(oldItem: DatabaseCity, newItem: DatabaseCity): Any = payload
}