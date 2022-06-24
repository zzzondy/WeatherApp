package com.weatherapp.providers

import android.content.Context
import android.content.res.Resources

class ResourceProvider(val context: Context) {
    val language = context.resources.configuration.locale.toLanguageTag().toString().split("-")[0]
    val resources: Resources = context.resources
    val packageName = context.packageName
}