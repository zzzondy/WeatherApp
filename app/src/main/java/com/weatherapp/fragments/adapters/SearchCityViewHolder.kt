package com.weatherapp.fragments.adapters

import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.databinding.ViewHolderCityBinding
import com.weatherapp.models.entities.City
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.navigation.CityWeatherListener

class SearchCityViewHolder(private val itemBinding: ViewHolderCityBinding) :
    RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(city: City, cityWeatherCallback: CityWeatherListener) {
            itemBinding.cityNameText.text = city.name
            itemBinding.root.setOnClickListener {
                cityWeatherCallback.openCityWeather(DatabaseCity(city.name, city.id, null), it)
            }
        }
}