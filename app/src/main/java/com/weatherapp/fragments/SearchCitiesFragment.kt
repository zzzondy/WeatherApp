package com.weatherapp.fragments

import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.MaterialSharedAxis
import com.jakewharton.rxbinding3.widget.textChanges
import com.weatherapp.R
import com.weatherapp.databinding.FragmentSearchCitiesBinding
import com.weatherapp.fragments.adapters.DividerItemDecorator
import com.weatherapp.fragments.adapters.SearchCityAdapter
import com.weatherapp.fragments.states.*
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.models.entities.SimpleWeatherForCity
import com.weatherapp.navigation.CityWeatherListener
import com.weatherapp.providers.ResourceProvider
import com.weatherapp.receivers.NetworkChangeListener
import com.weatherapp.receivers.NetworkStateReceiver

class SearchCitiesFragment : Fragment(), CityWeatherListener, NetworkChangeListener {

    private var viewModel: SearchCitiesViewModel? = null
    private var resourceProvider: ResourceProvider? = null
    private lateinit var searchCityAdapter: SearchCityAdapter

    private var _binding: FragmentSearchCitiesBinding? = null
    private val binding get() = _binding!!

    private var networkStateReceiver: NetworkStateReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
            duration = 350
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
            duration = 350
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchCitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resourceProvider = ResourceProvider(requireContext())
        viewModel = SearchCitiesViewModel(resourceProvider!!)
        setAdapters()
        setClickOnKeyListener()
        setOnClickListeners()
        setObservers()
        binding.queryLine.textChanges()
            .map { text -> text.toString().replace(" ", "") }
            .distinctUntilChanged()
            .subscribe(viewModel?.queryInput!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModel?.onClear()
        viewModel = null
    }

    override fun onPause() {
        super.onPause()
        if (networkStateReceiver != null)
            activity?.unregisterReceiver(networkStateReceiver)
        networkStateReceiver = null
    }

    private fun setAdapters() {
        val dividerItemDecoration =
            DividerItemDecorator(ContextCompat.getDrawable(requireContext(), R.drawable.divider)!!)
        binding.rvCities.addItemDecoration(dividerItemDecoration)
        searchCityAdapter = SearchCityAdapter(this)
        binding.rvCities.adapter = searchCityAdapter
        binding.rvCities.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideKeyboard()
                }
            }
        })
    }

    private fun setObservers() {
        viewModel?.resultOutput?.observe(this.viewLifecycleOwner, this::handleSearchResult)
        viewModel?.searchStateOutput?.observe(this.viewLifecycleOwner, this::handleSearchState)
    }

    private fun handleSearchResult(result: SearchResult) {
        when (result) {
            is ValidResult -> {
                binding.cityPlaceholder.visibility = View.GONE
                binding.rvCities.visibility = View.VISIBLE
                searchCityAdapter.submitList(result.result)
            }
            is ErrorResult -> {
                searchCityAdapter.submitList(emptyList())
                binding.cityPlaceholder.visibility = View.VISIBLE
                binding.rvCities.visibility = View.GONE
                binding.cityPlaceholder.setText(R.string.errorText)
                setNetworkReceiver()
            }
            is EmptyResult -> {
                searchCityAdapter.submitList(emptyList())
                binding.rvCities.visibility = View.GONE
                binding.cityPlaceholder.visibility = View.VISIBLE
                binding.cityPlaceholder.setText(R.string.empty_result)
            }
            is EmptyQuery -> {
                searchCityAdapter.submitList(emptyList())
                binding.cityPlaceholder.visibility = View.VISIBLE
                binding.rvCities.visibility = View.GONE
                binding.cityPlaceholder.setText(R.string.city_placeholder)
            }
            is TerminalError -> {
                searchCityAdapter.submitList(emptyList())
                binding.rvCities.visibility = View.GONE
                binding.cityPlaceholder.visibility = View.VISIBLE
                binding.cityPlaceholder.setText(R.string.error_unknown_on_download)
            }
        }
    }

    private fun handleSearchState(state: LoadingState) {
        when (state) {
            LoadingState.LOADING -> binding.searchProgress.visibility = View.VISIBLE
            LoadingState.READY -> binding.searchProgress.visibility = View.GONE
        }
    }

    private fun setOnClickListeners() {
        binding.backArrow.setOnClickListener {
            hideKeyboard()
            Navigation.findNavController(it).popBackStack()
        }

        binding.searchButton.setOnClickListener {
            hideKeyboard()
        }
    }

    private fun setClickOnKeyListener() {
        val inputMethodManager =
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        binding.queryLine.requestFocus()
        inputMethodManager.showSoftInput(binding.queryLine, InputMethodManager.SHOW_IMPLICIT)
        binding.queryLine.setOnKeyListener { view, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN) {
                binding.searchButton.performClick()
                return@setOnKeyListener true
            }
            false
        }
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun openCityWeather(
        city: DatabaseCity,
        originView: View,
        cityWeather: SimpleWeatherForCity?
    ) {
        val action =
            SearchCitiesFragmentDirections.actionSearchCitiesFragmentToCityWeatherBottomSheetFragment(
                city.cityName, city.cityId, city.timezone
            )
        Navigation.findNavController(originView)
            .navigate(action)
    }

    override fun onNetworkChanged(status: Boolean) {
        binding.queryLine
            .textChanges()
            .map { text -> text.toString().replace(" ", "") }
            .distinctUntilChanged()
            .subscribe(viewModel?.queryInput!!)
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
}