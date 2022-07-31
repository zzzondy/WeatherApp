package com.weatherapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager

class NetworkStateReceiver(private val callback: NetworkChangeListener) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val status = isNetworkAvailable(context!!)
        callback.onNetworkChanged(status)
    }

    private fun isNetworkAvailable(context: Context): Boolean = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = cm.activeNetworkInfo
        activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    } catch (e: NullPointerException) {
        false
    }

    companion object {
        fun getInstance(callback: NetworkChangeListener) = NetworkStateReceiver(callback)
    }
}