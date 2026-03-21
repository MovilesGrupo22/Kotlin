package com.restaurandes.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurandes.data.analytics.AnalyticsService
import com.restaurandes.domain.model.Restaurant
import com.restaurandes.domain.usecase.GetNearbyRestaurantsUseCase
import com.restaurandes.domain.usecase.GetRestaurantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRestaurantsUseCase: GetRestaurantsUseCase,
    private val getNearbyRestaurantsUseCase: GetNearbyRestaurantsUseCase,
    private val analyticsService: AnalyticsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRestaurants()
        analyticsService.logSectionView(AnalyticsService.AppSection.HOME, null)
    }

    fun loadRestaurants() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            getRestaurantsUseCase().fold(
                onSuccess = { restaurants ->
                    val categories = restaurants.map { it.category }.distinct().sorted()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            restaurants = restaurants,
                            allRestaurants = restaurants,
                            availableCategories = categories,
                            error = null
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Unknown error"
                        )
                    }
                }
            )
        }
    }

    fun loadNearbyRestaurants() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getNearbyRestaurantsUseCase(radiusKm = 5.0).fold(
                onSuccess = { restaurants ->
                    val categories = restaurants.map { it.category }.distinct().sorted()
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            restaurants = restaurants,
                            allRestaurants = restaurants,
                            availableCategories = categories,
                            error = null,
                            selectedCategory = "Nearby"
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Location unavailable"
                        )
                    }
                }
            )
        }
    }

    fun filterByCategory(category: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedCategory = category) }
            analyticsService.logFilterUsed(category, null)
            analyticsService.logSectionInteraction(
                AnalyticsService.AppSection.HOME,
                "filter_$category",
                null
            )
            
            val allRestaurants = _uiState.value.allRestaurants
            
            val filtered = when (category) {
                "All" -> allRestaurants
                "Open" -> allRestaurants.filter { it.isCurrentlyOpen() }
                "TopRated" -> allRestaurants.sortedByDescending { it.rating }
                else -> allRestaurants.filter { it.category == category }
            }
            
            _uiState.update { it.copy(restaurants = filtered) }
        }
    }
    
    fun onRestaurantClick(restaurantId: String, restaurantName: String) {
        analyticsService.logRestaurantView(restaurantId, restaurantName, null)
    }
}
