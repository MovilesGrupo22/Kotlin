package com.restaurandes.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurandes.domain.model.Restaurant
import com.restaurandes.domain.model.Review
import com.restaurandes.domain.repository.ReviewRepository
import com.restaurandes.domain.repository.RestaurantRepository
import com.restaurandes.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestaurantReviewsUiState(
    val restaurant: Restaurant? = null,
    val reviews: List<Review> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val currentUserName: String? = null,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class RestaurantReviewsViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestaurantReviewsUiState(isLoading = true))
    val uiState: StateFlow<RestaurantReviewsUiState> = _uiState.asStateFlow()

    private var activeRestaurantId: String? = null

    fun loadRestaurant(restaurantId: String) {
        activeRestaurantId = restaurantId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)

            val restaurantResult = restaurantRepository.getRestaurantById(restaurantId)
            val reviewsResult = reviewRepository.getReviewsByRestaurant(restaurantId)
            val userResult = userRepository.getCurrentUser()

            val restaurant = restaurantResult.getOrNull()
            if (restaurant == null) {
                _uiState.value = RestaurantReviewsUiState(
                    isLoading = false,
                    error = restaurantResult.exceptionOrNull()?.message ?: "No se pudo cargar el restaurante."
                )
                return@launch
            }

            _uiState.value = RestaurantReviewsUiState(
                restaurant = restaurant,
                reviews = reviewsResult.getOrNull().orEmpty(),
                isLoading = false,
                currentUserName = userResult.getOrNull()?.name,
                error = reviewsResult.exceptionOrNull()?.message
            )
        }
    }

    fun submitReview(rating: Double, comment: String) {
        val restaurantId = activeRestaurantId ?: return

        viewModelScope.launch {
            val user = userRepository.getCurrentUser().getOrNull()
            if (user == null) {
                _uiState.value = _uiState.value.copy(error = "Debes iniciar sesión para escribir una reseña.")
                return@launch
            }

            if (comment.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "Escribe un comentario antes de enviar la reseña.")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null, message = null)

            val result = reviewRepository.addReview(
                Review(
                    id = "",
                    restaurantId = restaurantId,
                    userId = user.id,
                    userName = user.name,
                    rating = rating,
                    comment = comment.trim(),
                    timestamp = System.currentTimeMillis()
                )
            )

            if (result.isSuccess) {
                loadRestaurant(restaurantId)
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    message = "Tu reseña fue guardada."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = result.exceptionOrNull()?.message ?: "No se pudo guardar la reseña."
                )
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }
}
