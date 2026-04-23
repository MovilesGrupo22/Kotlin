package com.restaurandes.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurandes.data.analytics.AnalyticsService
import com.restaurandes.domain.model.Restaurant
import com.restaurandes.domain.model.getCurrentTimeSlot
import com.restaurandes.domain.repository.RestaurantAnalyticsRepository
import com.restaurandes.domain.repository.RestaurantRepository
import com.restaurandes.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestaurantDetailUiState(
    val restaurant: Restaurant? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RestaurantDetailViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    private val userRepository: UserRepository,
    private val analyticsService: AnalyticsService,
    private val restaurantAnalyticsRepository: RestaurantAnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestaurantDetailUiState())
    val uiState: StateFlow<RestaurantDetailUiState> = _uiState.asStateFlow()

    fun loadRestaurant(restaurantId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = restaurantRepository.getRestaurantById(restaurantId)
                result.fold(
                    onSuccess = { restaurant ->
                        val currentUser = userRepository.getCurrentUser().getOrNull()
                        val isFavorite = currentUser?.favoriteRestaurants?.contains(restaurantId) == true

                        _uiState.value = RestaurantDetailUiState(
                            restaurant = restaurant,
                            isFavorite = isFavorite,
                            isLoading = false
                        )

                        val userId = currentUser?.id
                        analyticsService.logRestaurantView(restaurantId, restaurant.name, userId)
                        restaurantAnalyticsRepository.trackView(restaurantId, restaurant.name)
                        restaurantAnalyticsRepository.trackCategoryExplored(
                            restaurant.category,
                            getCurrentTimeSlot()
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Error loading restaurant"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unexpected error"
                )
            }
        }
    }

    fun toggleFavorite() {
        val restaurant = _uiState.value.restaurant ?: return
        val isFavorite = _uiState.value.isFavorite

        viewModelScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser().getOrNull()
                currentUser?.id ?: return@launch

                if (isFavorite) {
                    userRepository.removeFavoriteRestaurant(restaurant.id)
                } else {
                    userRepository.addFavoriteRestaurant(restaurant.id)
                    restaurantAnalyticsRepository.trackFavorite(restaurant.id, restaurant.name)
                }
                _uiState.value = _uiState.value.copy(isFavorite = !isFavorite)
            } catch (_: Exception) {
            }
        }
    }
}
