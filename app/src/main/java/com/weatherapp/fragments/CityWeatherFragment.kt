package com.weatherapp.fragments

import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialElevationScale
import com.google.gson.Gson
import com.weatherapp.R
import com.weatherapp.databinding.FragmentCityWeatherBinding
import com.weatherapp.fragments.adapters.ThreeDaysAdapter
import com.weatherapp.fragments.states.BackgroundState
import com.weatherapp.fragments.states.ResultState
import com.weatherapp.fragments.utils.getDrawable
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.models.entities.SimpleWeatherForCity
import com.weatherapp.models.entities.WeatherOnDay
import com.weatherapp.providers.ResourceProvider
import com.weatherapp.receivers.NetworkChangeListener
import com.weatherapp.receivers.NetworkStateReceiver

class CityWeatherFragment : Fragment(), NetworkChangeListener {

    private var viewModel: CityWeatherViewModel? = null
    private var resourceProvider: ResourceProvider? = null
    private var daysAdapter: ThreeDaysAdapter? = null
    private var networkStateReceiver: NetworkStateReceiver? = null

    private var _binding: FragmentCityWeatherBinding? = null
    private val binding: FragmentCityWeatherBinding get() = _binding!!

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
    ): View {
        _binding = FragmentCityWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resourceProvider = ResourceProvider(requireContext())
        val weatherForCity = fromJson(requireArguments().getString(ARG_CITY_WEATHER))
        val city = fromJson(requireArguments().getString(ARG_CITY)!!)
        val fromSearch = requireArguments().getBoolean(ARG_FROM_SEARCH)
        viewModel = if (weatherForCity != null) {
            CityWeatherViewModel(
                resourceProvider!!,
                fromSearch,
                this,
                city,
                weatherForCity
            )
        } else {
            CityWeatherViewModel(resourceProvider!!, fromSearch, this, city)
        }
        setObservers()
        setAdapters()
        setClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModel?.onClear()
        viewModel = null
        resourceProvider = null
        daysAdapter = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (networkStateReceiver != null)
            activity?.unregisterReceiver(networkStateReceiver)
        networkStateReceiver = null
    }

    private fun setButtonAdd(status: Boolean) {
        if (status) {
            binding.addButton.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    viewModel?.addCity()
                    Navigation.findNavController(it).popBackStack()
                }
            }
        }
    }

    private fun setClickListeners() {

        binding.openPreviousScreen.setOnClickListener {
            Navigation.findNavController(it).apply {
                popBackStack()
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel?.getWeatherForCity(
                fromJson(requireArguments().getString(ARG_CITY)!!).cityId
            )
            binding.swipeRefreshLayout.isRefreshing = false
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
        viewModel?.backgroundLiveData?.observe(this.viewLifecycleOwner, this::updateColorBackground)
    }

    private fun handleResult(result: ResultState) {
        when (result) {
            ResultState.ERROR -> {
                binding.errorText.visibility = View.VISIBLE
                hideAllViews()
            }
            ResultState.SUCCESS -> {
                binding.errorText.visibility = View.GONE
                showAllViews()
            }
        }
    }

    private fun hideAllViews() {
        binding.tempNow.visibility = View.GONE
        binding.textWeather.visibility = View.GONE
        binding.tempMin.visibility = View.GONE
        binding.tempMax.visibility = View.GONE
        binding.tv7DayForecast.visibility = View.GONE
        binding.rv7DayForecast.visibility = View.GONE
        binding.uvIndexLinearLayout.visibility = View.GONE
        binding.humidityLinearLayout.visibility = View.GONE
        binding.precipitationLinearLayout.visibility = View.GONE
        binding.sunsetLinearLayout.visibility = View.GONE
        binding.windLinearLayout.visibility = View.GONE
        binding.visibilityLinearLayout.visibility = View.GONE
    }

    private fun showAllViews() {
        binding.cityName.visibility = View.VISIBLE
        binding.tempNow.visibility = View.VISIBLE
        binding.textWeather.visibility = View.VISIBLE
        binding.tempMin.visibility = View.VISIBLE
        binding.tempMax.visibility = View.VISIBLE
        binding.tv7DayForecast.visibility = View.VISIBLE
        binding.rv7DayForecast.visibility = View.VISIBLE
        binding.uvIndexLinearLayout.visibility = View.VISIBLE
        binding.humidityLinearLayout.visibility = View.VISIBLE
        binding.precipitationLinearLayout.visibility = View.VISIBLE
        binding.sunsetLinearLayout.visibility = View.VISIBLE
        binding.windLinearLayout.visibility = View.VISIBLE
        binding.visibilityLinearLayout.visibility = View.VISIBLE
    }

    private fun setAdapters() {
        daysAdapter = ThreeDaysAdapter(resourceProvider!!)
        binding.rv7DayForecast.adapter = daysAdapter
    }

    private fun updateCityName(name: String) {
        binding.cityName.text = name
    }

    private fun updateColorBackground(state: BackgroundState) {
        binding.root.background = when (state) {
            BackgroundState.NIGHT ->
                getDrawable("night_big_gradient", resourceProvider!!)
            BackgroundState.MORNING ->
                getDrawable("morning_big_gradient", resourceProvider!!)
            BackgroundState.NOON ->
                getDrawable("noon_big_gradient", resourceProvider!!)
            BackgroundState.EVENING ->
                getDrawable("evening_big_gradient", resourceProvider!!)
            BackgroundState.LIGHT_RAIN ->
                getDrawable("light_rain_big_gradient", resourceProvider!!)
            BackgroundState.HEAVY_RAIN ->
                getDrawable("heavy_rain_big_gradient", resourceProvider!!)
        }
    }

    private fun updateTempNow(temp: String) {
        binding.tempNow.text = "$temp${getString(R.string.celsius)}"
    }

    private fun updateWeatherText(newText: String) {
        binding.textWeather.text = newText
    }

    private fun updateTempMax(temp: String) {
        binding.tempMax.text = "${getString(R.string.max)}: ${temp}${getString(R.string.celsius)}"
    }

    private fun updateTempMin(temp: String) {
        binding.tempMin.text = "${getString(R.string.min)}: $temp${getString(R.string.celsius)}"
    }

    private fun updateUvIndex(uvIndex: String) {
        binding.uvIndex.text = uvIndex
    }

    private fun updateUvIndexText(uvIndexText: String) {
        binding.uvIndexText.text = uvIndexText
    }

    private fun updateWindSpeed(speed: String) {
        binding.windSpeed.text = "$speed ${getString(R.string.meter_per_seconds)}"
    }

    private fun updateWindDirection(direction: String) {
        binding.windDirection.text = direction
    }

    private fun updateSunset(sunset: String) {
        binding.sunsetTime.text = sunset
    }

    private fun updateHumidity(humidity: String) {
        binding.humidity.text = "$humidity %"
    }

    private fun updateVisibility(visibility: String) {
        binding.visibility.text = "$visibility ${getString(R.string.kilometers)}"
    }

    private fun updatePrecipitation(precipitation: String) {
        binding.precipitation.text = "$precipitation ${getString(R.string.millimeters)}"
    }

    private fun updateWeatherDaysForecast(forecast: List<WeatherOnDay>) {
        daysAdapter?.submitList(forecast)
    }

    override fun setNetworkReceiver() {
        if (networkStateReceiver == null) {
            networkStateReceiver = NetworkStateReceiver.getInstance(this)
            activity?.registerReceiver(
                networkStateReceiver,
                IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
            )
        }
    }

    override fun onNetworkChanged(status: Boolean) {
        viewModel?.getWeatherForCity()
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