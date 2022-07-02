package com.weatherapp.fragments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.weatherapp.databinding.ViewHolderCityBinding
import com.weatherapp.models.entities.City
import com.weatherapp.navigation.CityWeatherListener

class SearchCityAdapter(private val cityWeatherCallback: CityWeatherListener) : ListAdapter<City, SearchCityViewHolder>(SearchCityDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchCityViewHolder {
        val itemBinding =
            ViewHolderCityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchCityViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: SearchCityViewHolder, position: Int) {
        val city = getItem(position)
        holder.bind(city, cityWeatherCallback)
    }
}

private class SearchCityDiffCallback : DiffUtil.ItemCallback<City>() {
    private val payload = Any()

    override fun areItemsTheSame(oldItem: City, newItem: City): Boolean =
        (oldItem.id == newItem.id)

    override fun areContentsTheSame(oldItem: City, newItem: City): Boolean =
        (oldItem == newItem)

    override fun getChangePayload(oldItem: City, newItem: City): Any = payload
}