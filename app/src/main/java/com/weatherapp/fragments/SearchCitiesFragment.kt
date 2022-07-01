package com.weatherapp.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.jakewharton.rxbinding3.widget.textChanges
import com.weatherapp.R
import com.weatherapp.databinding.FragmentSearchCitiesBinding
import com.weatherapp.fragments.adapters.DividerItemDecorator
import com.weatherapp.fragments.adapters.SearchCityAdapter
import com.weatherapp.fragments.states.*
import com.weatherapp.models.entities.DatabaseCity
import com.weatherapp.navigation.CityWeatherListener
import com.weatherapp.providers.ResourceProvider

class SearchCitiesFragment : Fragment(), CityWeatherListener {

    private var viewModel: SearchCitiesViewModel? = null
    private var resourceProvider: ResourceProvider? = null
    private lateinit var searchCityAdapter: SearchCityAdapter

    private var _binding: FragmentSearchCitiesBinding? = null
    private val binding get() = _binding!!

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
            .map { text -> text.toString() }
            .distinctUntilChanged()
            .subscribe(viewModel?.queryInput!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModel?.onClear()
        viewModel = null
    }

    private fun setAdapters() {
        val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(requireContext(), R.drawable.divider)!!)
        binding.rvCities.addItemDecoration(dividerItemDecoration)
        searchCityAdapter = SearchCityAdapter()
        binding.rvCities.adapter = searchCityAdapter
    }

    private fun setObservers() {
        viewModel?.resultOutput?.observe(this.viewLifecycleOwner, this::handleSearchResult)
    }

    private fun handleSearchResult(result: SearchResult) {
        when(result) {
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

    private fun setOnClickListeners() {
        binding.backArrow.setOnClickListener {
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

    override fun openCityWeather(city: DatabaseCity, originView: View) {

    }

    companion object {
        fun newInstance() = SearchCitiesFragment()
    }

}