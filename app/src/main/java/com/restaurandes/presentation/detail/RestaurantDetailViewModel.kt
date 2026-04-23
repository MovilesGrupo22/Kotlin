package com.restaurandes.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurandes.data.analytics.AnalyticsService
import com.restaurandes.domain.model.Restaurant
import com.restaurandes.domain.model.getCurrentTimeSlot
import com.restaurandes.domain.repository.RestaurantAnalyticsRepository
import com.restaurandes.domain.repository.RestaurantRepository
import com.restaurandes.domain.repository.ReviewRepository
import com.restaurandes.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
    private val reviewRepository: ReviewRepository,
    private val analyticsService: AnalyticsService,
    private val restaurantAnalyticsRepository: RestaurantAnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestaurantDetailUiState())
    val uiState: StateFlow<RestaurantDetailUiState> = _uiState.asStateFlow()

    fun loadRestaurant(restaurantId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // These sources are independent, so we resolve them in parallel.
                val restaurantDeferred = async { restaurantRepository.getRestaurantById(restaurantId) }
                val currentUserDeferred = async { userRepository.getCurrentUser() }
                val reviewsDeferred = async { reviewRepository.getReviewsByRestaurant(restaurantId) }

                val result = restaurantDeferred.await()
                val currentUser = currentUserDeferred.await().getOrNull()
                val reviews = reviewsDeferred.await().getOrDefault(emptyList())

                result.fold(
                    onSuccess = { restaurant ->
                        val isFavorite = currentUser?.favoriteRestaurants?.contains(restaurantId) == true
                        val restaurantWithFreshReviews = mergeRestaurantWithReviews(
                            restaurant = restaurant,
                            reviews = reviews
                        )

                        _uiState.value = RestaurantDetailUiState(
                            restaurant = restaurantWithFreshReviews,
                            isFavorite = isFavorite,
                            isLoading = false
                        )

                        val userId = currentUser?.id
                        analyticsService.logRestaurantView(
                            restaurantId,
                            restaurantWithFreshReviews.name,
                            userId
                        )

                        launch {
                            restaurantAnalyticsRepository.trackView(
                                restaurantId,
                                restaurantWithFreshReviews.name
                            )
                        }

                        launch {
                            restaurantAnalyticsRepository.trackCategoryExplored(
                                restaurantWithFreshReviews.category,
                                getCurrentTimeSlot()
                            )
                        }
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

    private fun mergeRestaurantWithReviews(
        restaurant: Restaurant,
        reviews: List<com.restaurandes.domain.model.Review>
    ): Restaurant {
        if (reviews.isEmpty()) return restaurant

        return restaurant.copy(
            rating = reviews.map { it.rating }.average(),
            reviewCount = reviews.size
        )
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
