package com.weatherapp.providers

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import com.weatherapp.models.entities.SimpleWeatherForCity
import io.reactivex.Observable
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider


interface LocationProvider {

    @RequiresPermission(ACCESS_COARSE_LOCATION)
    fun requestLocation(): Observable<Location>
}

sealed class GetUserLocationResult {
    data class Success(val weatherCity: SimpleWeatherForCity) : GetUserLocationResult()
    sealed class Failed : GetUserLocationResult() {
        object OtherFailure : Failed()
    }
}

class FusedLocationProvider(private val context: Context) : LocationProvider {


    @RequiresPermission(ACCESS_COARSE_LOCATION)
    override fun requestLocation(): Observable<Location> {
        return ReactiveLocationProvider(context)
            .lastKnownLocation
    }
}