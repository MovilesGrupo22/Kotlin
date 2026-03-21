package com.restaurandes.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurandes.data.analytics.AnalyticsService
import com.restaurandes.domain.model.Restaurant
import com.restaurandes.domain.repository.RestaurantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val restaurants: List<Restaurant> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    private val analyticsService: AnalyticsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var allRestaurants: List<Restaurant> = emptyList()

    init {
        loadAllRestaurants()
        analyticsService.logSectionView(AnalyticsService.AppSection.SEARCH, null)
    }

    private fun loadAllRestaurants() {
        viewModelScope.launch {
            try {
                val result = restaurantRepository.getRestaurants()
                result.fold(
                    onSuccess = { restaurants ->
                        allRestaurants = restaurants
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message ?: "Error loading restaurants"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Unexpected error"
                )
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(restaurants = emptyList())
            return
        }
        
        searchRestaurants(query)
    }

    private fun searchRestaurants(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            
            try {
                val filteredRestaurants = allRestaurants.filter { restaurant ->
                    restaurant.name.contains(query, ignoreCase = true) ||
                    restaurant.description.contains(query, ignoreCase = true) ||
                    restaurant.category.contains(query, ignoreCase = true) ||
                    restaurant.tags.any { it.contains(query, ignoreCase = true) }
                }
                
                _uiState.value = _uiState.value.copy(
                    restaurants = filteredRestaurants,
                    isSearching = false
                )
                
                analyticsService.logSearch(query, filteredRestaurants.size, null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = e.message ?: "Search error"
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.value = SearchUiState()
    }
}
