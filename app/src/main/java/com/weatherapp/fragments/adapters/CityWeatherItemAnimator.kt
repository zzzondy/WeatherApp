package com.weatherapp.fragments.adapters

import android.animation.ObjectAnimator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.R

class CityWeatherItemAnimator: DefaultItemAnimator() {
    override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
        return true
    }

    override fun animateMove(
        holder: RecyclerView.ViewHolder?,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ): Boolean {
        dispatchMoveStarting(holder)
        val newHolder = holder as CityWeatherViewHolder
        ObjectAnimator.ofFloat(newHolder.itemView.findViewById(R.id.constraionLayoutCity), "y", fromY.toFloat(), toY.toFloat())
            .apply {
                duration = 300
                start()
            }
        dispatchMoveFinished(holder)
        return true
    }
}