package com.weatherapp.fragments.adapters

import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.databinding.ViewHolderCityBinding
import com.weatherapp.models.entities.City

class SearchCityViewHolder(private val itemBinding: ViewHolderCityBinding) :
    RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(city: City) {
            itemBinding.cityNameText.text = city.name
        }
}