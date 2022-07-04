package com.weatherapp.receivers

interface NetworkChangeListener {
    fun onNetworkChanged(status: Boolean)
    fun setNetworkReceiver()
}