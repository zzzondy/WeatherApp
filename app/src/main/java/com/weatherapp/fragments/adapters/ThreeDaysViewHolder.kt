package com.weatherapp.fragments.adapters

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.weatherapp.R
import com.weatherapp.databinding.ViewHolderWeatherOnDayBinding
import com.weatherapp.models.entities.WeatherOnDay
import com.weatherapp.providers.ResourceProvider
import java.util.*

class ThreeDaysViewHolder(private val itemBinding: ViewHolderWeatherOnDayBinding) :
    RecyclerView.ViewHolder(itemBinding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(weatherForDay: WeatherOnDay, resourceProvider: ResourceProvider) {
        itemBinding.dayText.text = getWeekDayByDate(weatherForDay.fxDate, resourceProvider)
        itemBinding.dayIcon.load(getDrawable("ic_${weatherForDay.iconDay}", resourceProvider))
        itemBinding.dayTempMax.text =
            "${resourceProvider.resources.getString(R.string.max_temp)}: ${weatherForDay.tempMax}"
        itemBinding.dayTempMin.text =
            "${resourceProvider.resources.getString(R.string.min_temp)}: ${weatherForDay.tempMin}"
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun getDrawable(name: String, resourceProvider: ResourceProvider): Drawable {
        val resourceId =
            resourceProvider.resources.getIdentifier(name, "drawable", resourceProvider.packageName)
        return resourceProvider.resources.getDrawable(resourceId)
    }

    private fun getWeekDayByDate(date: String, resourceProvider: ResourceProvider): String {
        val calendar = Calendar.getInstance()
        val splittedDate = date.split("-").map { it.toInt() }
        calendar.set(Calendar.YEAR, splittedDate[0])
        calendar.set(Calendar.DAY_OF_MONTH, splittedDate[2])
        calendar.set(Calendar.MONTH, splittedDate[1] - 1)
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            1 -> resourceProvider.resources.getString(R.string.sunday)
            2 -> resourceProvider.resources.getString(R.string.monday)
            3 -> resourceProvider.resources.getString(R.string.tuesday)
            4 -> resourceProvider.resources.getString(R.string.wednesday)
            5 -> resourceProvider.resources.getString(R.string.thursday)
            6 -> resourceProvider.resources.getString(R.string.friday)
            else -> resourceProvider.resources.getString(R.string.saturday)
        }
    }
}