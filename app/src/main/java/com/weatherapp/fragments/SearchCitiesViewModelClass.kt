package com.weatherapp.fragments

import io.reactivex.Observer
import androidx.lifecycle.ViewModel

internal abstract class SearchCitiesViewModelClass: ViewModel() {
    abstract val queryInput: Observer<String>
}