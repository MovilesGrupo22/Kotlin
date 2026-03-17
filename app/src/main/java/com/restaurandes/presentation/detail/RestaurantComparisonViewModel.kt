package com.restaurandes.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurandes.domain.model.Location
import com.restaurandes.domain.model.Restaurant
import com.restaurandes.domain.repository.LocationRepository
import com.restaurandes.domain.repository.RestaurantRepository
import com.restaurandes.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val error: String? = null
)

@HiltViewModel
class RestaurantComparisonViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestaurantComparisonUiState(isLoading = true))
    val uiState: StateFlow<RestaurantComparisonUiState> = _uiState.asStateFlow()

    fun loadRestaurants(primaryRestaurantId: String, secondaryRestaurantId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val allRestaurants = restaurantRepository.getRestaurants().getOrNull().orEmpty()
            val primaryRestaurant = restaurantRepository.getRestaurantById(primaryRestaurantId).getOrNull()

            if (primaryRestaurant == null) {
                _uiState.value = RestaurantComparisonUiState(
                    isLoading = false,
                    error = "No se pudo cargar el restaurante principal."
                )
                return@launch
            }

            val currentUser = userRepository.getCurrentUser().getOrNull()
            val location = locationRepository.getCurrentLocation().getOrNull()
                ?: Location(4.6017, -74.0659)

            val secondaryRestaurant = when {
                !secondaryRestaurantId.isNullOrBlank() ->
                    restaurantRepository.getRestaurantById(secondaryRestaurantId).getOrNull()

                else -> chooseComparisonCandidate(
                    primaryRestaurant = primaryRestaurant,
                    restaurants = allRestaurants,
                    favoriteRestaurantIds = currentUser?.favoriteRestaurants.orEmpty(),
                    userLocation = location
                )
            }

            if (secondaryRestaurant == null) {
                _uiState.value = RestaurantComparisonUiState(
                    isLoading = false,
                    error = "No encontramos un segundo restaurante real para comparar."
                )
                return@launch
            }

            _uiState.value = RestaurantComparisonUiState(
                primaryRestaurant = ComparableRestaurant(
                    restaurant = primaryRestaurant,
                    distanceKm = calculateDistanceKm(location, primaryRestaurant)
                ),
                secondaryRestaurant = ComparableRestaurant(
                    restaurant = secondaryRestaurant,
                    distanceKm = calculateDistanceKm(location, secondaryRestaurant)
                ),
                userLocation = location,
                isLoading = false
            )
        }
    }

    private fun chooseComparisonCandidate(
        primaryRestaurant: Restaurant,
        restaurants: List<Restaurant>,
        favoriteRestaurantIds: List<String>,
        userLocation: Location
    ): Restaurant? {
        return restaurants
            .asSequence()
            .filter { it.id != primaryRestaurant.id }
            .sortedByDescending { candidate ->
                comparisonScore(
                    candidate = candidate,
                    primaryRestaurant = primaryRestaurant,
                    favoriteRestaurantIds = favoriteRestaurantIds,
                    userLocation = userLocation
                )
            }
            .firstOrNull()
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
