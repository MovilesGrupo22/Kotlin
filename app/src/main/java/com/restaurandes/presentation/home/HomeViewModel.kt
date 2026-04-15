package com.restaurandes.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurandes.data.analytics.AnalyticsService
import com.restaurandes.domain.model.Restaurant
import com.restaurandes.domain.usecase.GetNearbyRestaurantsUseCase
import com.restaurandes.domain.usecase.GetRestaurantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
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
                    val selectedCategory = _uiState.value.selectedCategory
                    val filteredRestaurants = applyFilter(restaurants, selectedCategory)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            restaurants = filteredRestaurants,
                            allRestaurants = restaurants,
                            availableCategories = categories,
                            error = null,
                            contextCanvas = buildContextCanvas(restaurants, selectedCategory)
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
                            selectedCategory = "Nearby",
                            contextCanvas = buildContextCanvas(restaurants, "Nearby")
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
            val filtered = applyFilter(allRestaurants, category)

            _uiState.update {
                it.copy(
                    restaurants = filtered,
                    contextCanvas = buildContextCanvas(allRestaurants, category)
                )
            }
        }
    }
    
    fun onRestaurantClick(restaurantId: String, restaurantName: String) {
        analyticsService.logRestaurantView(restaurantId, restaurantName, null)
    }

    private fun applyFilter(restaurants: List<Restaurant>, category: String): List<Restaurant> {
        return when (category) {
            "All" -> restaurants
            "Open" -> restaurants.filter { it.isCurrentlyOpen() }
            "TopRated" -> restaurants.sortedByDescending { it.rating }
            "Nearby" -> restaurants
            else -> restaurants.filter { it.category == category }
        }
    }

    private fun buildContextCanvas(
        restaurants: List<Restaurant>,
        selectedCategory: String
    ): HomeContextCanvas? {
        if (restaurants.isEmpty()) return null

        val openCount = restaurants.count { it.isCurrentlyOpen() }
        val totalCount = restaurants.size
        val openFilterEnabled = selectedCategory == "Open"
        val actionLabel = if (openFilterEnabled) "Open filter: ON" else "Show open only"
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return when {
            openCount == 0 || openCount * 2 < totalCount -> HomeContextCanvas(
                message = "Most restaurants may be closed now.",
                actionLabel = actionLabel,
                tone = HomeContextTone.WARNING
            )

            currentHour in 5..10 -> HomeContextCanvas(
                message = "Good morning! Breakfast time.",
                actionLabel = actionLabel,
                tone = HomeContextTone.POSITIVE
            )

            openFilterEnabled -> HomeContextCanvas(
                message = "Showing restaurants that are open right now.",
                actionLabel = "Open filter: ON",
                tone = HomeContextTone.INFO
            )

            else -> HomeContextCanvas(
                message = "$openCount of $totalCount restaurants are open now.",
                actionLabel = actionLabel,
                tone = HomeContextTone.INFO
            )
        }
    }
}
