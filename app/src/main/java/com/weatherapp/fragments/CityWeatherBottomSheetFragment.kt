package com.weatherapp.fragments

import android.app.Activity
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.weatherapp.R
import com.weatherapp.database.CityWeatherRepository
import com.weatherapp.databinding.CityWeatherBottomSheetLayoutBinding
import com.weatherapp.fragments.adapters.ThreeDaysAdapter
import com.weatherapp.fragments.states.BackgroundState
import com.weatherapp.fragments.states.LoadingState
import com.weatherapp.fragments.states.ResultState
import com.weatherapp.fragments.utils.getDrawable
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.models.entities.WeatherOnDay
import com.weatherapp.providers.ResourceProvider
import com.weatherapp.receivers.NetworkChangeListener
import com.weatherapp.receivers.NetworkStateReceiver

class CityWeatherBottomSheetFragment : BottomSheetDialogFragment(), NetworkChangeListener {
    private var _binding: CityWeatherBottomSheetLayoutBinding? = null
    private val binding: CityWeatherBottomSheetLayoutBinding get() = _binding!!

    private lateinit var resourceProvider: ResourceProvider
    private lateinit var repository: CityWeatherRepository

    private lateinit var viewModel: CityWeatherBottomSheetViewModel

    private lateinit var daysAdapter: ThreeDaysAdapter

    private lateinit var city: DatabaseCity
    private val args: CityWeatherBottomSheetFragmentArgs by navArgs()

    private var networkStateReceiver: NetworkStateReceiver? = null

    override fun getTheme() = R.style.AppBottomSheetDialogTheme


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CityWeatherBottomSheetLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        resourceProvider = ResourceProvider(requireContext())
        setAdapters()
        repository = CityWeatherRepository(requireContext())
        viewModel = CityWeatherBottomSheetViewModel(resourceProvider, repository)
        setListeners()
        setObservers()
        city = DatabaseCity(args.cityName.replace("_", " "), args.cityId, args.timezone, null)
        viewModel.getWeatherForCity(city)
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            val bottomSheet =
                it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
            val behavior = BottomSheetBehavior.from(bottomSheet)
            val layoutParams = bottomSheet.layoutParams
            if (layoutParams != null) {
                layoutParams.height = (getWindowHeight() * 0.9).toInt()
            }
            bottomSheet.layoutParams = layoutParams
            behavior.state = BottomSheetBehavior.STATE_EXPANDED

            behavior.peekHeight = getWindowHeight() / 2
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModel.onClear()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (networkStateReceiver != null)
            activity?.unregisterReceiver(networkStateReceiver)
        networkStateReceiver = null
    }


    private fun setListeners() {
        binding.addButton.setOnClickListener {
            viewModel.addCityToDatabase(city)
            dismiss()
        }

        binding.openPreviousScreen.setOnClickListener {
            dismiss()
        }
    }

    private fun setObservers() {
        viewModel.resultState.observe(this.viewLifecycleOwner, this::handleResult)
        viewModel.cityNameLiveData.observe(this.viewLifecycleOwner, this::updateCityName)
        viewModel.tempNowLiveData.observe(this.viewLifecycleOwner, this::updateTempNow)
        viewModel.maxTempLiveData.observe(this.viewLifecycleOwner, this::updateTempMax)
        viewModel.minTempLiveData.observe(this.viewLifecycleOwner, this::updateTempMin)
        viewModel.uvIndexLiveData.observe(this.viewLifecycleOwner, this::updateUvIndex)
        viewModel.uvIndexTextLiveData.observe(this.viewLifecycleOwner, this::updateUvIndexText)
        viewModel.weatherTextLiveData.observe(this.viewLifecycleOwner, this::updateWeatherText)
        viewModel.windSpeedLiveData.observe(this.viewLifecycleOwner, this::updateWindSpeed)
        viewModel.windDirectionLiveData.observe(
            this.viewLifecycleOwner,
            this::updateWindDirection
        )
        viewModel.sunsetLiveData.observe(this.viewLifecycleOwner, this::updateSunset)
        viewModel.humidityLiveData.observe(this.viewLifecycleOwner, this::updateHumidity)
        viewModel.visibilityLiveData.observe(this.viewLifecycleOwner, this::updateVisibility)
        viewModel.precipitationLiveData.observe(
            this.viewLifecycleOwner,
            this::updatePrecipitation
        )
        viewModel.days3ForecastLiveData.observe(
            this.viewLifecycleOwner,
            this::updateWeatherDaysForecast
        )
        viewModel.backgroundState.observe(this.viewLifecycleOwner, this::updateColorBackground)
        viewModel.loadingState.observe(this.viewLifecycleOwner, this::setLoadingState)
        viewModel.addButtonState.observe(this.viewLifecycleOwner, this::handleAddButtonState)
    }

    private fun handleResult(result: ResultState) {
        when (result) {
            ResultState.ERROR -> {
                binding.errorText.visibility = View.VISIBLE
                setNetworkReceiver()
                hideAllViews()
            }
            ResultState.SUCCESS -> {
                binding.errorText.visibility = View.GONE
                showAllViews()
            }
        }
    }

    private fun handleAddButtonState(state: Boolean) {
        binding.addButton.visibility = when (state) {
            true -> View.VISIBLE
            false -> View.GONE
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
        daysAdapter = ThreeDaysAdapter(resourceProvider)
        binding.rv7DayForecast.adapter = daysAdapter
    }

    private fun updateCityName(name: String) {
        binding.cityName.text = name
    }

    private fun updateColorBackground(state: BackgroundState) {
        binding.root.background = when (state) {
            BackgroundState.NIGHT ->
                getDrawable("night_bottom_gradient", resourceProvider)
            BackgroundState.MORNING ->
                getDrawable("morning_bottom_gradient", resourceProvider)
            BackgroundState.NOON ->
                getDrawable("noon_bottom_gradient", resourceProvider)
            BackgroundState.EVENING ->
                getDrawable("evening_bottom_gradient", resourceProvider)
            BackgroundState.LIGHT_RAIN ->
                getDrawable("light_rain_bottom_gradient", resourceProvider)
            BackgroundState.HEAVY_RAIN ->
                getDrawable("heavy_rain_bottom_gradient", resourceProvider)
        }
    }

    private fun setLoadingState(state: LoadingState) {
        when (state) {
            LoadingState.LOADING -> {
                binding.progressBar.visibility = View.VISIBLE
            }
            LoadingState.READY -> {
                binding.progressBar.visibility = View.GONE
            }
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
        daysAdapter.submitList(forecast)
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
        viewModel.getWeatherForCity(city)
    }

    private fun getWindowHeight(): Int {
        // Calculate window height for fullscreen use
        val displayMetrics = DisplayMetrics()
        (context as Activity?)!!.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }
}