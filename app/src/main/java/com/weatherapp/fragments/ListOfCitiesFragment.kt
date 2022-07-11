package com.weatherapp.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialSharedAxis
import com.google.gson.Gson
import com.weatherapp.R
import com.weatherapp.database.CityWeatherRepository
import com.weatherapp.databinding.FragmentListOfCitiesBinding
import com.weatherapp.fragments.adapters.CityWeatherAdapter
import com.weatherapp.fragments.adapters.CityWeatherItemAnimator
import com.weatherapp.fragments.states.BackgroundState
import com.weatherapp.fragments.states.ListState
import com.weatherapp.fragments.utils.getDrawable
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.models.entities.SimpleWeatherForCity
import com.weatherapp.navigation.CityWeatherListener
import com.weatherapp.providers.FusedLocationProvider
import com.weatherapp.providers.GetUserLocationResult
import com.weatherapp.providers.LocationProvider
import com.weatherapp.providers.ResourceProvider
import com.weatherapp.receivers.NetworkChangeListener
import com.weatherapp.receivers.NetworkStateReceiver


class ListOfCitiesFragment : Fragment(), CityWeatherListener, NetworkChangeListener {

    private var viewModel: ListOfCitiesViewModel? = null
    private var resourceProvider: ResourceProvider? = null
    private var cityWeatherRepository: CityWeatherRepository? = null
    private var cityWeatherAdapter: CityWeatherAdapter? = null
    private var locationProvider: LocationProvider? = null

    private var _binding: FragmentListOfCitiesBinding? = null
    private val binding: FragmentListOfCitiesBinding get() = _binding!!

    private var networkStateReceiver: NetworkStateReceiver? = null

    private var isRationaleShown = false
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted)
                onLocationPermissionGranted()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListOfCitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        view.doOnPreDraw {
            startPostponedEnterTransition()
        }
        restorePreferencesData()
        resourceProvider = ResourceProvider(requireContext())
        cityWeatherRepository = CityWeatherRepository(requireContext())
        locationProvider = FusedLocationProvider(requireContext())
        viewModel =
            ListOfCitiesViewModel(resourceProvider!!, cityWeatherRepository!!, locationProvider!!, this)
        setRecyclerViewSwipeListener()
        setObservers()
        setAdapters()
        setClickListeners()
    }

    override fun onStart() {
        super.onStart()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onLocationPermissionGranted()
        } else {
            binding.constraintLayoutCurrentLocation.setOnClickListener {
                onOpenLocationWeather()
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        requestPermissionLauncher.unregister()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        resourceProvider = null
        cityWeatherRepository?.onClear()
        cityWeatherRepository = null
        cityWeatherAdapter?.onClear()
        cityWeatherAdapter = null
        locationProvider = null
    }

    override fun onStop() {
        super.onStop()
        saveCityPositions()
        savePreferencesData()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (networkStateReceiver != null)
            activity?.unregisterReceiver(networkStateReceiver)
        networkStateReceiver = null
        viewModel?.onClear()
        viewModel = null
    }

    private fun onOpenLocationWeather() {
        activity?.let {
            when {
                ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED -> onLocationPermissionGranted()
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> showLocationPermissionExplanationDialog()
                isRationaleShown -> showLocationPermissionDeniedDialog()
                else -> requestLocationPermission()

            }
        }
    }

    private fun setClickListeners() {
        binding.openSearchCities.setOnClickListener {
            exitTransition = MaterialElevationScale(true).apply {
                duration = 300
            }
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
                duration = 300
            }
            Navigation.findNavController(it)
                .navigate(R.id.action_listOfCitiesFragment_to_searchCitiesFragment)
        }

        binding.openLocationWeather.setOnClickListener {
            onOpenLocationWeather()
        }

    }

    private fun setRecyclerViewSwipeListener() {
        val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object :
            ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT
            ) {
            private val icon = getDrawable("delete", resourceProvider!!)
            private val background =
                ColorDrawable(ContextCompat.getColor(requireContext(), R.color.deleteRed))

            override fun isLongPressDragEnabled(): Boolean {
                return true
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                cityWeatherAdapter?.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
                vibrate(requireView())
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                val position = viewHolder.adapterPosition
                viewModel?.deleteCity(cityWeatherAdapter?.getCityWithId(position)!!)
                vibrate(requireView())
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
                val itemView = viewHolder.itemView
                val backgroundCornerOffset = 30
                val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                val iconBottom = iconTop + icon.intrinsicHeight
                if (dX < 0) {
                    val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    background.setBounds(
                        itemView.right + dX.toInt() - backgroundCornerOffset,
                        itemView.top, itemView.right, itemView.bottom
                    )
                } else {
                    background.setBounds(0, 0, 0, 0)
                    icon.setBounds(0, 0, 0, 0)
                }
                background.draw(c)
                icon.draw(c)
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvCities)

    }

    private fun setObservers() {
        viewModel?.listOfCitiesLiveData?.observe(this.viewLifecycleOwner, this::updateList)
        viewModel?.resultState?.observe(this.viewLifecycleOwner, this::handleResult)
        viewModel?.locationLiveData?.observe(this.viewLifecycleOwner, this::handleLocationResult)
    }

    private fun setAdapters() {
        cityWeatherAdapter = CityWeatherAdapter(resourceProvider!!, this, this)
        binding.rvCities.itemAnimator = CityWeatherItemAnimator()
        binding.rvCities.adapter = cityWeatherAdapter
    }

    private fun updateList(newCities: List<DatabaseCity>) {
        cityWeatherAdapter?.onItemsUpdated(newCities)
    }

    private fun handleResult(state: ListState) {
        when (state) {
            ListState.EMPTY -> {
                binding.rvCities.visibility = View.GONE
                binding.nothingToShow.visibility = View.VISIBLE
            }
            ListState.NOT_EMPTY -> {
                binding.rvCities.visibility = View.VISIBLE
                binding.nothingToShow.visibility = View.GONE
            }
        }
    }

    private fun handleLocationResult(locationResult: GetUserLocationResult) {
        when (locationResult) {
            is GetUserLocationResult.Success -> {
                setCityName(locationResult.weatherCity.cityName)
                setTempNow(locationResult.weatherCity.tempNow)
                setMaxTemp(locationResult.weatherCity.tempMax)
                setMinTemp(locationResult.weatherCity.tempMin)
                setWeatherText(locationResult.weatherCity.textWeather)
                viewModel?.backgroundLiveData?.observe(
                    this.viewLifecycleOwner,
                    this::setBackgroundTime
                )
                binding.constraintLayoutCurrentLocation.transitionName = getString(R.string.location_city)
                binding.constraintLayoutCurrentLocation.setOnClickListener {
                    openCityWeather(
                        DatabaseCity(
                            locationResult.weatherCity.cityName,
                            locationResult.weatherCity.cityId,
                            "Europe/Moscow",
                            null
                        ), it, locationResult.weatherCity
                    )
                }
            }
            else -> Log.println(Log.ASSERT, "error", locationResult.toString())
        }
    }

    private fun setBackgroundTime(state: BackgroundState) {
        binding.constraintLayoutCurrentLocation.background =
            when (state) {
                BackgroundState.MORNING -> getDrawable("morning_gradient", resourceProvider!!)
                BackgroundState.NOON -> getDrawable("noon_gradient", resourceProvider!!)
                BackgroundState.EVENING -> getDrawable("evening_gradient", resourceProvider!!)
                BackgroundState.NIGHT -> getDrawable("night_gradient", resourceProvider!!)
                BackgroundState.LIGHT_RAIN -> getDrawable("light_rain_gradient", resourceProvider!!)
                BackgroundState.HEAVY_RAIN -> getDrawable("heavy_rain_gradient", resourceProvider!!)
            }
    }

    private fun setCityName(cityName: String) {
        binding.nameCity.text = cityName
    }

    private fun setWeatherText(text: String) {
        binding.weatherTextCity.text = text
    }

    private fun setTempNow(temp: String) {
        binding.weatherTemp.text = "$temp${getString(R.string.celsius)}"
    }

    private fun setMaxTemp(temp: String) {
        binding.maxTemp.text = "${getString(R.string.max)}: $temp"
    }

    private fun setMinTemp(temp: String) {
        binding.minTemp.text = "${getString(R.string.min)}: $temp"
    }

    private fun showAllViews() {
        binding.nameCity.visibility = View.VISIBLE
        binding.weatherTextCity.visibility = View.VISIBLE
        binding.weatherTemp.visibility = View.VISIBLE
        binding.minTemp.visibility = View.VISIBLE
        binding.maxTemp.visibility = View.VISIBLE
        binding.noAccess.visibility = View.GONE
    }

    override fun openCityWeather(
        city: DatabaseCity,
        originView: View,
        cityWeather: SimpleWeatherForCity?
    ) {
        exitTransition = MaterialElevationScale(true).apply {
            duration = 300
        }
        reenterTransition = MaterialElevationScale(false).apply {
            duration = 300
        }
        val extras = FragmentNavigatorExtras(originView to "cityWeatherFragment")
        Navigation.findNavController(requireView()).navigate(
            R.id.action_listOfCitiesFragment_to_cityWeatherFragment,
            bundleOf(
                CityWeatherFragment.ARG_CITY_WEATHER to toJson(cityWeather),
                CityWeatherFragment.ARG_FROM_SEARCH to false,
                CityWeatherFragment.ARG_CITY to toJson(city)
            ),
            null, extras
        )
    }

    override fun onNetworkChanged(status: Boolean) {
        when (status) {
            true -> {
                updateList(emptyList())
                Thread.sleep(100)
                viewModel?.getCities()
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel?.requestLocation()
                }
                activity?.unregisterReceiver(networkStateReceiver)
                networkStateReceiver = null
            }
            else -> {}
        }
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

    private fun requestLocationPermission() {
        context?.let {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private fun onLocationPermissionGranted() {
        showAllViews()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel?.requestLocation()
        }
    }

    private fun showLocationPermissionExplanationDialog() {
        context?.let {
            AlertDialog.Builder(it)
                .setMessage(R.string.permission_dialog_explanation_text)
                .setPositiveButton(R.string.dialog_positive_button) { dialog, _ ->
                    isRationaleShown = true
                    requestLocationPermission()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_negative_button) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun showLocationPermissionDeniedDialog() {
        context?.let {
            AlertDialog.Builder(it)
                .setMessage(R.string.permission_dialog_denied_text)
                .setPositiveButton(R.string.dialog_positive_button) { dialog, _ ->
                    startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + it.packageName)
                        )
                    )
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_negative_button) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun toJson(city: SimpleWeatherForCity?): String? {
        return Gson().toJson(city)
    }

    private fun toJson(city: DatabaseCity): String {
        return Gson().toJson(city)
    }

    private fun vibrate(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    private fun saveCityPositions() {
        cityWeatherAdapter?.items?.forEachIndexed { index, city ->
            viewModel?.updatePosition(city, index)
        }
    }

    private fun savePreferencesData() {
        requireContext().getSharedPreferences(PERMISSION_SHARED_PREF, Context.MODE_PRIVATE)
            .edit()
            .apply {
                putBoolean(KEY_LOCATION_PERMISSION_RATIONALE_SHOWN, isRationaleShown)
                apply()
            }
    }

    private fun restorePreferencesData() {
        isRationaleShown = requireContext().getSharedPreferences(PERMISSION_SHARED_PREF, Context.MODE_PRIVATE)?.getBoolean(
            KEY_LOCATION_PERMISSION_RATIONALE_SHOWN, false
        ) ?: false
    }

    companion object {
        private const val PERMISSION_SHARED_PREF = "PERMISSION_SHARED_PREF"
        private const val KEY_LOCATION_PERMISSION_RATIONALE_SHOWN =
            "KEY_LOCATION_PERMISSION_RATIONALE_SHOWN"
    }

}