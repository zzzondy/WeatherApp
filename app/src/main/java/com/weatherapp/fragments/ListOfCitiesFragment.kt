package com.weatherapp.fragments

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.weatherapp.R
import com.weatherapp.database.CityWeatherRepository
import com.weatherapp.databinding.FragmentListOfCitiesBinding
import com.weatherapp.fragments.adapters.CityWeatherAdapter
import com.weatherapp.fragments.adapters.CityWeatherItemAnimator
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.navigation.CityWeatherListener
import com.weatherapp.providers.ResourceProvider


class ListOfCitiesFragment : Fragment(), CityWeatherListener {

    private var viewModel: ListOfCitiesViewModel? = null
    private var binding: FragmentListOfCitiesBinding? = null
    private var resourceProvider: ResourceProvider? = null
    private var cityWeatherRepository: CityWeatherRepository? = null
    private var cityWeatherAdapter: CityWeatherAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_list_of_cities, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resourceProvider = ResourceProvider(requireContext())
        binding = FragmentListOfCitiesBinding.bind(view)
        cityWeatherRepository = CityWeatherRepository(requireContext())
        viewModel = ListOfCitiesViewModel(cityWeatherRepository!!)
        setRecyclerViewSwipeListener()
        setObservers()
        setAdapters()
        setRefreshListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        viewModel?.onClear()
        viewModel = null
        resourceProvider = null
        cityWeatherRepository?.onClear()
        cityWeatherRepository = null
        cityWeatherAdapter?.onClear()
        cityWeatherAdapter = null
    }

    private fun setRefreshListener() {
        binding?.root?.setOnRefreshListener {
            viewModel?.getCities()
            binding?.root?.isRefreshing = false
        }
    }

    private fun setRecyclerViewSwipeListener() {
        val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object :
            ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT
            ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                val position = viewHolder.adapterPosition
                viewModel?.deleteCity(cityWeatherAdapter?.getCityWithId(position)!!)
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(binding?.rvCities)

    }

    private fun setObservers() {
        viewModel?.listOfCitiesLiveData?.observe(this.viewLifecycleOwner, this::updateList)
    }

    private fun setAdapters() {
        cityWeatherAdapter = CityWeatherAdapter(resourceProvider!!, this)
        binding?.rvCities?.itemAnimator = CityWeatherItemAnimator()
        binding?.rvCities?.adapter = cityWeatherAdapter
    }

    private fun updateList(newCities: List<DatabaseCity>) {
        cityWeatherAdapter?.submitList(newCities)
    }

    override fun openCityWeather(city: DatabaseCity) {
        Navigation.findNavController(requireView()).navigate(
            R.id.cityWeatherFragment,
            bundleOf(CityWeatherFragment.ARG_CITY to toJson(city))
        )
    }

    private fun toJson(city: DatabaseCity): String? {
        return Gson().toJson(city)
    }

    companion object {
        fun newInstance() = ListOfCitiesFragment()
    }

}