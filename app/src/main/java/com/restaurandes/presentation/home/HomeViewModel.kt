package com.restaurandes.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurandes.data.analytics.AnalyticsService
import com.restaurandes.data.local.LocalUserPreferences
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
    private val analyticsService: AnalyticsService,
    private val localUserPreferences: LocalUserPreferences
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
                    val persistedCategory = localUserPreferences.getLastHomeFilter()
                    val currentCategory = _uiState.value.selectedCategory
                    val restoredCategory = getRestoredCategory(
                        currentCategory = currentCategory,
                        persistedCategory = persistedCategory,
                        availableCategories = categories
                    )
                    val selectedCategory = getInitialContextCategory(restoredCategory)
                    val filteredRestaurants = applyFilter(restaurants, selectedCategory)
                    val trendingCampusInsight = buildTrendingCampusInsight(restaurants)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            restaurants = filteredRestaurants,
                            allRestaurants = restaurants,
                            availableCategories = categories,
                            error = null,
                            selectedCategory = selectedCategory,
                            contextCanvas = buildContextCanvas(restaurants, selectedCategory),
                            trendingCampusInsight = trendingCampusInsight
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
                    val trendingCampusInsight = buildTrendingCampusInsight(restaurants)
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            restaurants = restaurants,
                            allRestaurants = restaurants,
                            availableCategories = categories,
                            error = null,
                            selectedCategory = "Nearby",
                            contextCanvas = buildContextCanvas(restaurants, "Nearby"),
                            trendingCampusInsight = trendingCampusInsight
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
            localUserPreferences.saveLastHomeFilter(category)
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

    private fun getRestoredCategory(
        currentCategory: String,
        persistedCategory: String,
        availableCategories: List<String>
    ): String {
        val validFilters = setOf("All", "Open", "TopRated", "Nearby") + availableCategories
        val preferredCategory = if (currentCategory != "All") currentCategory else persistedCategory
        return if (preferredCategory in validFilters) preferredCategory else "All"
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
        val timeMessage = getTimeContextMessage(currentHour)

        return when {
            currentHour in 6..20 -> HomeContextCanvas(
                message = timeMessage,
                actionLabel = actionLabel,
                tone = if (openCount > 0) HomeContextTone.POSITIVE else HomeContextTone.WARNING
            )

            openFilterEnabled -> HomeContextCanvas(
                message = timeMessage,
                actionLabel = "Open filter: ON",
                tone = HomeContextTone.WARNING
            )

            else -> HomeContextCanvas(
                message = timeMessage,
                actionLabel = actionLabel,
                tone = if (openCount * 2 < totalCount) HomeContextTone.WARNING else HomeContextTone.INFO
            )
        }
    }

    private fun getInitialContextCategory(currentCategory: String): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (currentCategory == "All" && hour in 6..20) {
            "Open"
        } else {
            currentCategory
        }
    }

    private fun getTimeContextMessage(hour: Int): String {
        return when {
            hour in 6..10 -> "🌅 Good morning! Breakfast time."
            hour in 11..13 -> "☀️ Lunch time! Check what's open."
            hour in 14..17 -> "🕑 Afternoon snack time."
            hour in 18..20 -> "🌆 Dinner time! Find a restaurant."
            else -> "🌙 Most restaurants may be closed now."
        }
    }

    private fun buildTrendingCampusInsight(restaurants: List<Restaurant>): TrendingCampusInsight? {
        if (restaurants.isEmpty()) return null

        val trendingRestaurant = restaurants.maxWithOrNull(
            compareBy<Restaurant> { if (it.isCurrentlyOpen()) 1 else 0 }
                .thenBy { it.rating }
                .thenBy { it.reviewCount }
                .thenBy { it.tags.size }
        ) ?: return null

        val reason = when {
            trendingRestaurant.isCurrentlyOpen() && trendingRestaurant.reviewCount > 0 ->
                "Open now with strong campus feedback"
            trendingRestaurant.isCurrentlyOpen() ->
                "Open now and highly rated"
            trendingRestaurant.reviewCount > 0 ->
                "Popular pick based on reviews"
            else ->
                "Recommended from current restaurant data"
        }

        return TrendingCampusInsight(
            restaurant = trendingRestaurant,
            reason = reason
        )
    }
}
