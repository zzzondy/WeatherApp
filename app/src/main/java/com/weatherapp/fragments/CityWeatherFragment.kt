package com.weatherapp.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.google.gson.Gson
import com.weatherapp.R
import com.weatherapp.databinding.FragmentCityWeatherBinding
import com.weatherapp.fragments.adapters.ThreeDaysAdapter
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.models.entities.WeatherOnDay
import com.weatherapp.providers.ResourceProvider

class CityWeatherFragment : Fragment() {

    private var viewModel: CityWeatherViewModel? = null
    private var binding: FragmentCityWeatherBinding? = null
    private var resourceProvider: ResourceProvider? = null
    private var daysAdapter: ThreeDaysAdapter? = null

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
        val city = arguments?.getString(ARG_CITY, null)
        viewModel = if (city != null) {
            CityWeatherViewModel(fromJson(city).cityName, resourceProvider!!)
        } else {
            CityWeatherViewModel("Россошь", resourceProvider!!)
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

    private fun setClickListeners() {
        binding?.openListOfCitiesButton?.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_cityWeatherFragment_to_listOfCitiesFragment)
        }

        binding?.swipeRefreshLayout?.setOnRefreshListener {
            viewModel?.getWeatherForCity("Россошь")
            binding?.swipeRefreshLayout?.isRefreshing = false
        }
    }

    private fun setObservers() {
        viewModel?.cityNameLiveData?.observe(this.viewLifecycleOwner, this::updateCityName)
        viewModel?.tempNowLiveData?.observe(this.viewLifecycleOwner, this::updateTempNow)
        viewModel?.maxTempLiveData?.observe(this.viewLifecycleOwner, this::updateTempMax)
        viewModel?.minTempLiveData?.observe(this.viewLifecycleOwner, this::updateTempMin)
        viewModel?.uvIndexLiveData?.observe(this.viewLifecycleOwner, this::updateUvIndex)
        viewModel?.uvIndexTextLiveData?.observe(this.viewLifecycleOwner, this::updateUvIndexText)
        viewModel?.weatherTextLiveData?.observe(this.viewLifecycleOwner, this::updateWeatherText)
        viewModel?.windSpeedLiveData?.observe(this.viewLifecycleOwner, this::updateWindSpeed)
        viewModel?.windDirectionLiveData?.observe(this.viewLifecycleOwner, this::updateWindDirection)
        viewModel?.sunsetLiveData?.observe(this.viewLifecycleOwner, this::updateSunset)
        viewModel?.humidityLiveData?.observe(this.viewLifecycleOwner, this::updateHumidity)
        viewModel?.visibilityLiveData?.observe(this.viewLifecycleOwner, this::updateVisibility)
        viewModel?.precipitationLiveData?.observe(this.viewLifecycleOwner, this::updatePrecipitation)
        viewModel?.days3ForecastLiveData?.observe(this.viewLifecycleOwner, this::updateWeatherDaysForecast)
    }

    private fun setAdapters() {
        daysAdapter = ThreeDaysAdapter(resourceProvider!!)
        binding?.rv10DayForecast?.adapter = daysAdapter
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

    private fun fromJson(city: String): DatabaseCity {
        return Gson().fromJson(city, DatabaseCity::class.java)
    }

    companion object {
        fun newInstance() = CityWeatherFragment()
        const val ARG_CITY = "city"
    }


}