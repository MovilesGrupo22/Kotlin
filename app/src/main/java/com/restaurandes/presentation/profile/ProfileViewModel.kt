package com.restaurandes.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurandes.data.analytics.AnalyticsService
import com.restaurandes.domain.model.User
import com.restaurandes.domain.repository.ReviewRepository
import com.restaurandes.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val reviewsCount: Int = 0,
    val favoritesCount: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository,
    private val analyticsService: AnalyticsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
        analyticsService.logSectionView(AnalyticsService.AppSection.PROFILE, null)
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            userRepository.observeCurrentUser().collect { user ->
                val reviewsCount = user?.let {
                    reviewRepository.getReviewsCountByUser(it.id).getOrDefault(0)
                } ?: 0

                val favoritesCount = user?.favoriteRestaurants?.size ?: 0

                _uiState.value = _uiState.value.copy(
                    user = user,
                    isLoading = false,
                    reviewsCount = reviewsCount,
                    favoritesCount = favoritesCount
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.signOut().fold(
                onSuccess = { },
                onFailure = { }
            )
        }
    }
}