package com.weatherapp.fragments.states

import com.weatherapp.models.entities.City

sealed class SearchResult
object EmptyResult: SearchResult()
object EmptyQuery: SearchResult()
data class ErrorResult(val e: Throwable): SearchResult()
object TerminalError: SearchResult()
data class ValidResult(val result: List<City>): SearchResult()
