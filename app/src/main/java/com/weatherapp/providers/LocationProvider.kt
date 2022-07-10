package com.weatherapp.providers

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.weatherapp.models.entities.SimpleWeatherForCity
import io.reactivex.Single

interface LocationProvider {
    fun checkLocationManager(): Boolean

    @RequiresPermission(ACCESS_COARSE_LOCATION)
    fun requestLastKnownLocation(): Task<Location>?
}

sealed class GetUserLocationResult {
    data class Success(val weatherCity: SimpleWeatherForCity) : GetUserLocationResult()
    sealed class Failed : GetUserLocationResult() {
        object OtherFailure : Failed()
    }
}

class FusedLocationProvider(private val context: Context) : LocationProvider {
    private val TAG = "FusedLocationProvider"

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    override fun checkLocationManager(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            ?: return false
        if (!isLocationEnabled(locationManager))
            return false
        return true
    }

    @RequiresPermission(ACCESS_COARSE_LOCATION)
    override fun requestLastKnownLocation(): Task<Location>? {
        val lastLocation = fusedLocationClient.lastLocation
        return if (checkLocationManager())
            lastLocation
        else null
    }

    private fun isLocationEnabled(lm: LocationManager): Boolean {
        var gpsEnabled = false
        var networkEnabled = false

        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
        }

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
        }

        return gpsEnabled || networkEnabled
    }
}