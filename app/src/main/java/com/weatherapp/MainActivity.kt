package com.weatherapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.weatherapp.providers.ResourceProvider

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onDestroy() {
        super.onDestroy()
        val resourceProvider = ResourceProvider(applicationContext)
        resourceProvider.deleteCache()
    }
}