package com.weatherapp.fragments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.weatherapp.databinding.ViewHolderWeatherOnDayBinding
import com.weatherapp.models.entities.WeatherOnDay
import com.weatherapp.providers.ResourceProvider

class ThreeDaysAdapter(private val resourceProvider: ResourceProvider) : ListAdapter<WeatherOnDay, ThreeDaysViewHolder>(WeatherDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreeDaysViewHolder {
        val itemBinding = ViewHolderWeatherOnDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThreeDaysViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ThreeDaysViewHolder, position: Int) {
        val weatherForDay = getItem(position)
        holder.bind(weatherForDay, resourceProvider)
    }
}

private class WeatherDiffCallback : DiffUtil.ItemCallback<WeatherOnDay>() {
    private val payload = Any()

    override fun areItemsTheSame(oldItem: WeatherOnDay, newItem: WeatherOnDay): Boolean =
        (oldItem.fxDate == newItem.fxDate)

    override fun areContentsTheSame(oldItem: WeatherOnDay, newItem: WeatherOnDay): Boolean =
        (oldItem == newItem)

    override fun getChangePayload(oldItem: WeatherOnDay, newItem: WeatherOnDay): Any = payload
}