package com.weatherapp.fragments

import android.content.IntentFilter
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.models.entities.SimpleWeatherForCity
import com.weatherapp.navigation.CityWeatherListener
import com.weatherapp.providers.ResourceProvider
import com.weatherapp.receivers.NetworkChangeListener
import com.weatherapp.receivers.NetworkStateReceiver
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator


class ListOfCitiesFragment : Fragment(), CityWeatherListener, NetworkChangeListener {

    private var viewModel: ListOfCitiesViewModel? = null
    private var resourceProvider: ResourceProvider? = null
    private var cityWeatherRepository: CityWeatherRepository? = null
    private var cityWeatherAdapter: CityWeatherAdapter? = null

    private var _binding: FragmentListOfCitiesBinding? = null
    private val binding: FragmentListOfCitiesBinding get() = _binding!!

    private var networkStateReceiver: NetworkStateReceiver? = null

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
        resourceProvider = ResourceProvider(requireContext())
        cityWeatherRepository = CityWeatherRepository(requireContext())
        viewModel = ListOfCitiesViewModel(cityWeatherRepository!!)
        setRecyclerViewSwipeListener()
        setObservers()
        setAdapters()
        setRefreshListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveCityPositions()
        _binding = null
        resourceProvider = null
        cityWeatherRepository?.onClear()
        cityWeatherRepository = null
        cityWeatherAdapter?.onClear()
        cityWeatherAdapter = null
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.unregisterReceiver(networkStateReceiver)
        networkStateReceiver = null
        viewModel?.onClear()
        viewModel = null
    }

    private fun setRefreshListener() {
        binding.openSearchCities.setOnClickListener {
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
                duration = 300
            }
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
                duration = 300
            }
            Navigation.findNavController(it)
                .navigate(R.id.action_listOfCitiesFragment_to_searchCitiesFragment)
        }

    }

    private fun setRecyclerViewSwipeListener() {
        val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object :
            ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT
            ) {
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
                RecyclerViewSwipeDecorator.Builder(
                    requireContext(),
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
                    .addBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.deleteRed
                        )
                    )
                    .setIconHorizontalMargin(8)
                    .addSwipeLeftActionIcon(R.drawable.delete)
                    .addSwipeLeftLabel(getString(R.string.delete))
                    .setSwipeLeftLabelColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.white
                        )
                    ).create().decorate()
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvCities)

    }

    private fun setObservers() {
        viewModel?.listOfCitiesLiveData?.observe(this.viewLifecycleOwner, this::updateList)
    }

    private fun setAdapters() {
        cityWeatherAdapter = CityWeatherAdapter(resourceProvider!!, this, this)
        binding.rvCities.itemAnimator = CityWeatherItemAnimator()
        binding.rvCities.adapter = cityWeatherAdapter
    }

    private fun updateList(newCities: List<DatabaseCity>) {
        cityWeatherAdapter?.onItemsUpdated(newCities)
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
            R.id.cityWeatherFragment,
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

}