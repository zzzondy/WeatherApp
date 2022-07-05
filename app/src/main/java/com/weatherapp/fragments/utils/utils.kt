package com.weatherapp.fragments.utils

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import com.weatherapp.providers.ResourceProvider

@SuppressLint("UseCompatLoadingForDrawables")
fun getDrawable(name: String, resourceProvider: ResourceProvider): Drawable {
    val resourceId =
        resourceProvider.resources.getIdentifier(name, "drawable", resourceProvider.packageName)
    return resourceProvider.resources.getDrawable(resourceId)
}