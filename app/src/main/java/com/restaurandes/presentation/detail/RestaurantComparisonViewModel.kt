package com.restaurandes.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurandes.data.analytics.AnalyticsService
import com.restaurandes.domain.model.Location
import com.restaurandes.domain.model.Restaurant
import com.restaurandes.domain.repository.LocationRepository
import com.restaurandes.domain.repository.RestaurantRepository
import com.restaurandes.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ComparableRestaurant(
    val restaurant: Restaurant,
    val distanceKm: Double
)

data class RestaurantComparisonUiState(
    val primaryRestaurant: ComparableRestaurant? = null,
    val secondaryRestaurant: ComparableRestaurant? = null,
    val userLocation: Location? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val availableRestaurants: List<Restaurant> = emptyList(),
    val showRestaurantPicker: Boolean = false,
    val suggestedRestaurant: Restaurant? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class RestaurantComparisonViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository,
    private val analyticsService: AnalyticsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestaurantComparisonUiState(isLoading = true))
    val uiState: StateFlow<RestaurantComparisonUiState> = _uiState.asStateFlow()

    private var cachedLocation: Location = Location(4.6017, -74.0659)
    private var cachedUserId: String? = null

    fun loadRestaurants(primaryRestaurantId: String, secondaryRestaurantId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val allRestaurantsDeferred = async { restaurantRepository.getRestaurants() }
            val primaryRestaurantDeferred = async { restaurantRepository.getRestaurantById(primaryRestaurantId) }
            val currentUserDeferred = async { userRepository.getCurrentUser() }
            val locationDeferred = async { locationRepository.getCurrentLocation() }
            val secondaryRestaurantDeferred = secondaryRestaurantId
                ?.takeIf { it.isNotBlank() }
                ?.let { id -> async { restaurantRepository.getRestaurantById(id) } }

            val allRestaurants = allRestaurantsDeferred.await().getOrDefault(emptyList())
            val primaryRestaurant = primaryRestaurantDeferred.await().getOrNull()

            if (primaryRestaurant == null) {
                _uiState.value = RestaurantComparisonUiState(
                    isLoading = false,
                    error = "No se pudo cargar el restaurante principal."
                )
                return@launch
            }

            val currentUser = currentUserDeferred.await().getOrNull()
            cachedUserId = currentUser?.id
            cachedLocation = locationDeferred.await().getOrNull()
                ?: Location(4.6017, -74.0659)

            analyticsService.logCompareOpened(
                primaryRestaurantId = primaryRestaurant.id,
                secondaryRestaurantId = secondaryRestaurantId,
                userId = cachedUserId
            )

            val others = allRestaurants.filter { it.id != primaryRestaurant.id }

            if (!secondaryRestaurantId.isNullOrBlank()) {
                val secondaryRestaurant = secondaryRestaurantDeferred?.await()?.getOrNull()
                if (secondaryRestaurant == null) {
                    _uiState.value = RestaurantComparisonUiState(
                        isLoading = false,
                        error = "No se pudo cargar el segundo restaurante."
                    )
                    return@launch
                }
                analyticsService.logCompareUsed(
                    primaryRestaurantId = primaryRestaurant.id,
                    secondaryRestaurantId = secondaryRestaurant.id,
                    selectionMode = "auto",
                    userId = cachedUserId
                )
                _uiState.value = RestaurantComparisonUiState(
                    primaryRestaurant = ComparableRestaurant(
                        restaurant = primaryRestaurant,
                        distanceKm = calculateDistanceKm(cachedLocation, primaryRestaurant)
                    ),
                    secondaryRestaurant = ComparableRestaurant(
                        restaurant = secondaryRestaurant,
                        distanceKm = calculateDistanceKm(cachedLocation, secondaryRestaurant)
                    ),
                    userLocation = cachedLocation,
                    isLoading = false,
                    availableRestaurants = others
                )
                return@launch
            }

            val suggested = others.maxByOrNull { candidate ->
                comparisonScore(
                    candidate = candidate,
                    primaryRestaurant = primaryRestaurant,
                    favoriteRestaurantIds = currentUser?.favoriteRestaurants.orEmpty(),
                    userLocation = cachedLocation
                )
            }

            _uiState.value = RestaurantComparisonUiState(
                primaryRestaurant = ComparableRestaurant(
                    restaurant = primaryRestaurant,
                    distanceKm = calculateDistanceKm(cachedLocation, primaryRestaurant)
                ),
                userLocation = cachedLocation,
                isLoading = false,
                showRestaurantPicker = true,
                availableRestaurants = others,
                suggestedRestaurant = suggested
            )
        }
    }

    fun showRestaurantPicker() {
        _uiState.value = _uiState.value.copy(showRestaurantPicker = true)
    }

    fun hideRestaurantPicker() {
        _uiState.value = _uiState.value.copy(showRestaurantPicker = false)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectSecondaryRestaurant(restaurant: Restaurant) {
        val primary = _uiState.value.primaryRestaurant ?: return
        _uiState.value = _uiState.value.copy(
            secondaryRestaurant = ComparableRestaurant(
                restaurant = restaurant,
                distanceKm = calculateDistanceKm(cachedLocation, restaurant)
            ),
            showRestaurantPicker = false
        )
        analyticsService.logCompareUsed(
            primaryRestaurantId = primary.restaurant.id,
            secondaryRestaurantId = restaurant.id,
            selectionMode = "manual",
            userId = cachedUserId
        )
    }

    fun onRestaurantActionAfterCompare(selectedRestaurantId: String, destination: String) {
        val primaryRestaurant = _uiState.value.primaryRestaurant?.restaurant ?: return
        val secondaryRestaurant = _uiState.value.secondaryRestaurant?.restaurant ?: return

        val comparedRestaurantId = if (selectedRestaurantId == primaryRestaurant.id) {
            secondaryRestaurant.id
        } else {
            primaryRestaurant.id
        }

        analyticsService.logRestaurantSelectedAfterCompare(
            selectedRestaurantId = selectedRestaurantId,
            comparedRestaurantId = comparedRestaurantId,
            userId = cachedUserId
        )
        analyticsService.logPostCompareNavigation(
            selectedRestaurantId = selectedRestaurantId,
            comparedRestaurantId = comparedRestaurantId,
            destination = destination,
            userId = cachedUserId
        )
    }

    private fun comparisonScore(
        candidate: Restaurant,
        primaryRestaurant: Restaurant,
        favoriteRestaurantIds: List<String>,
        userLocation: Location
    ): Double {
        var score = candidate.rating * 2
        if (candidate.category == primaryRestaurant.category) score += 3
        if (candidate.isCurrentlyOpen()) score += 2
        if (candidate.id in favoriteRestaurantIds) score += 2
        score -= calculateDistanceKm(userLocation, candidate) * 0.3
        score -= kotlin.math.abs(candidate.priceRange.length - primaryRestaurant.priceRange.length) * 0.5
        return score
    }

    private fun calculateDistanceKm(location: Location, restaurant: Restaurant): Double {
        return calculateDistanceKm(
            location.latitude,
            location.longitude,
            restaurant.latitude,
            restaurant.longitude
        )
    }

    private fun calculateDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
