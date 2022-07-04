package com.weatherapp.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialSharedAxis
import com.google.gson.Gson
import com.weatherapp.R
import com.weatherapp.databinding.FragmentCityWeatherBinding
import com.weatherapp.fragments.adapters.ThreeDaysAdapter
import com.weatherapp.fragments.states.ResultState
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.models.entities.SimpleWeatherForCity
import com.weatherapp.models.entities.WeatherOnDay
import com.weatherapp.providers.ResourceProvider

class CityWeatherFragment : Fragment() {

    private var viewModel: CityWeatherViewModel? = null
    private var binding: FragmentCityWeatherBinding? = null
    private var resourceProvider: ResourceProvider? = null
    private var daysAdapter: ThreeDaysAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true)
            .apply {
                scrimColor = Color.TRANSPARENT
                duration = 300
            }
        returnTransition = MaterialElevationScale(false).apply {
            duration = 300
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_city_weather, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resourceProvider = ResourceProvider(requireContext())
        binding = FragmentCityWeatherBinding.bind(view)
        val weatherForCity = fromJson(requireArguments().getString(ARG_CITY_WEATHER))
        val city = fromJson(requireArguments().getString(ARG_CITY)!!)
        val fromSearch = requireArguments().getBoolean(ARG_FROM_SEARCH)
        viewModel = if (weatherForCity != null) {
            CityWeatherViewModel(
                resourceProvider!!,
                fromSearch,
                city,
                weatherForCity
            )
        } else {
            CityWeatherViewModel(resourceProvider!!, fromSearch, city)
        }
        setObservers()
        setAdapters()
        setClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        viewModel?.onClear()
        viewModel = null
        resourceProvider = null
        daysAdapter = null
    }

    private fun setButtonAdd(status: Boolean) {
        if (status) {
            binding?.addButton?.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    viewModel?.addCity()
                    Navigation.findNavController(it).popBackStack()
                }
            }
        }
    }

    private fun setClickListeners() {

        binding?.openPreviousScreen?.setOnClickListener {
            Navigation.findNavController(it).apply {
                popBackStack()
            }
        }

        binding?.swipeRefreshLayout?.setOnRefreshListener {
            viewModel?.getWeatherForCity(
                fromJson(requireArguments().getString(ARG_CITY)!!).cityId
            )
            binding?.swipeRefreshLayout?.isRefreshing = false
        }
    }

    private fun setObservers() {
        viewModel?.resultLiveData?.observe(this.viewLifecycleOwner, this::handleResult)
        viewModel?.cityNameLiveData?.observe(this.viewLifecycleOwner, this::updateCityName)
        viewModel?.tempNowLiveData?.observe(this.viewLifecycleOwner, this::updateTempNow)
        viewModel?.maxTempLiveData?.observe(this.viewLifecycleOwner, this::updateTempMax)
        viewModel?.minTempLiveData?.observe(this.viewLifecycleOwner, this::updateTempMin)
        viewModel?.uvIndexLiveData?.observe(this.viewLifecycleOwner, this::updateUvIndex)
        viewModel?.uvIndexTextLiveData?.observe(this.viewLifecycleOwner, this::updateUvIndexText)
        viewModel?.weatherTextLiveData?.observe(this.viewLifecycleOwner, this::updateWeatherText)
        viewModel?.windSpeedLiveData?.observe(this.viewLifecycleOwner, this::updateWindSpeed)
        viewModel?.windDirectionLiveData?.observe(
            this.viewLifecycleOwner,
            this::updateWindDirection
        )
        viewModel?.sunsetLiveData?.observe(this.viewLifecycleOwner, this::updateSunset)
        viewModel?.humidityLiveData?.observe(this.viewLifecycleOwner, this::updateHumidity)
        viewModel?.visibilityLiveData?.observe(this.viewLifecycleOwner, this::updateVisibility)
        viewModel?.precipitationLiveData?.observe(
            this.viewLifecycleOwner,
            this::updatePrecipitation
        )
        viewModel?.days3ForecastLiveData?.observe(
            this.viewLifecycleOwner,
            this::updateWeatherDaysForecast
        )
        viewModel?.addButtonLiveData?.observe(this.viewLifecycleOwner, this::setButtonAdd)
    }

    private fun handleResult(result: ResultState) {
        when (result) {
            ResultState.ERROR -> {
                binding?.errorText?.visibility = View.VISIBLE
                hideAllViews()
            }
            ResultState.SUCCESS -> {
                // do nothing
            }
        }
    }

    private fun hideAllViews() {
        binding?.cityName?.visibility = View.GONE
        binding?.tempNow?.visibility = View.GONE
        binding?.textWeather?.visibility = View.GONE
        binding?.tempMin?.visibility = View.GONE
        binding?.tempMax?.visibility = View.GONE
        binding?.tv7DayForecast?.visibility = View.GONE
        binding?.rv7DayForecast?.visibility = View.GONE
        binding?.uvIndexLinearLayout?.visibility = View.GONE
        binding?.humidityLinearLayout?.visibility = View.GONE
        binding?.precipitationLinearLayout?.visibility = View.GONE
        binding?.sunsetLinearLayout?.visibility = View.GONE
        binding?.windLinearLayout?.visibility = View.GONE
        binding?.visibilityLinearLayout?.visibility = View.GONE
    }

    private fun setAdapters() {
        daysAdapter = ThreeDaysAdapter(resourceProvider!!)
        binding?.rv7DayForecast?.adapter = daysAdapter
    }

    private fun updateCityName(name: String) {
        binding?.cityName?.text = name
    }

    private fun updateTempNow(temp: String) {
        binding?.tempNow?.text = "$temp${getString(R.string.celsius)}"
    }

    private fun updateWeatherText(newText: String) {
        binding?.textWeather?.text = newText
    }

    private fun updateTempMax(temp: String) {
        binding?.tempMax?.text = "${getString(R.string.max)}: ${temp}${getString(R.string.celsius)}"
    }

    private fun updateTempMin(temp: String) {
        binding?.tempMin?.text = "${getString(R.string.min)}: $temp${getString(R.string.celsius)}"
    }

    private fun updateUvIndex(uvIndex: String) {
        binding?.uvIndex?.text = uvIndex
    }

    private fun updateUvIndexText(uvIndexText: String) {
        binding?.uvIndexText?.text = uvIndexText
    }

    private fun updateWindSpeed(speed: String) {
        binding?.windSpeed?.text = "$speed ${getString(R.string.meter_per_seconds)}"
    }

    private fun updateWindDirection(direction: String) {
        binding?.windDirection?.text = direction
    }

    private fun updateSunset(sunset: String) {
        binding?.sunsetTime?.text = sunset
    }

    private fun updateHumidity(humidity: String) {
        binding?.humidity?.text = "$humidity %"
    }

    private fun updateVisibility(visibility: String) {
        binding?.visibility?.text = "$visibility ${getString(R.string.kilometers)}"
    }

    private fun updatePrecipitation(precipitation: String) {
        binding?.precipitation?.text = "$precipitation ${getString(R.string.millimeters)}"
    }

    private fun updateWeatherDaysForecast(forecast: List<WeatherOnDay>) {
        daysAdapter?.submitList(forecast)
    }

    private fun fromJson(city: String?): SimpleWeatherForCity? {
        return Gson().fromJson(city, SimpleWeatherForCity::class.java)
    }

    private fun fromJson(city: String): DatabaseCity {
        return Gson().fromJson(city, DatabaseCity::class.java)
    }

    companion object {
        const val ARG_CITY_WEATHER = "city_weather"
        const val ARG_CITY = "city"
        const val ARG_FROM_SEARCH = "from_search"
    }


}