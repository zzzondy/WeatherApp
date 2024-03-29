package com.weatherapp.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.weatherapp.BuildConfig
import com.weatherapp.fragments.states.*
import com.weatherapp.models.network.WeatherApiModule
import com.weatherapp.providers.ResourceProvider
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

private const val DEBOUNCE_DELAY_TIME = 500L

internal class SearchCitiesViewModel(resourceProvider: ResourceProvider) :
    SearchCitiesViewModelClass() {
    override val queryInput = PublishSubject.create<String>()

    private val mutableResultOutput = MutableLiveData<SearchResult>()
    val resultOutput: LiveData<SearchResult> get() = mutableResultOutput

    private val mutableSearchStateOutput = MutableLiveData<LoadingState>()
    val searchStateOutput: LiveData<LoadingState> get() = mutableSearchStateOutput

    private val weatherApiModule = WeatherApiModule.getInstance()

    private val subscriptions = CompositeDisposable()

    private val language = resourceProvider.language

    init {
        queryInput
            .debounce(DEBOUNCE_DELAY_TIME, TimeUnit.MILLISECONDS, Schedulers.computation())
            .doOnEach { mutableSearchStateOutput.postValue(LoadingState.LOADING) }
            .switchMap(::searchCitiesByQuery)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnEach { mutableSearchStateOutput.value = LoadingState.READY }
            .subscribeBy(
                onNext = { searchResult ->
                    mutableResultOutput.value = searchResult
                }, onError = {}
            )
            .addTo(subscriptions)

    }

    private fun searchCitiesByQuery(query: String): Observable<SearchResult> {
        return if (query.isEmpty()) {
            Observable.just(EmptyQuery)
        } else {
            weatherApiModule.cityService.getCity(query, language, BuildConfig.API_KEY)
                .toObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map { result ->
                    if (result.location.isEmpty()) {
                        EmptyResult
                    } else {
                        ValidResult(result.location)
                    }
                }
                .onErrorReturn {
                    when (it.message.toString()) {
                        "Field 'location' is required, but it was missing" -> EmptyResult
                        else -> ErrorResult(it)
                    }
                }
        }
    }

    fun onClear() {
        subscriptions.dispose()
    }
}