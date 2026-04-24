package com.restaurandes.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurandes.data.analytics.AnalyticsService
import com.restaurandes.data.local.LocalUserPreferences
import com.restaurandes.domain.model.Review
import com.restaurandes.domain.model.User
import com.restaurandes.domain.repository.ReviewRepository
import com.restaurandes.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val reviewsCount: Int = 0,
    val favoritesCount: Int = 0,
    val reviews: List<Review> = emptyList()
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository,
    private val analyticsService: AnalyticsService,
    private val localUserPreferences: LocalUserPreferences
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

            userRepository.observeCurrentUser().collectLatest { user ->
                if (user == null) {
                    _uiState.value = ProfileUiState(isLoading = false)
                    return@collectLatest
                }

                val resolvedUserDeferred = async { userRepository.getCurrentUser().getOrNull() }
                val reviewsCountDeferred = async {
                    reviewRepository.getReviewsCountByUser(user.id).getOrDefault(0)
                }
                val reviewsDeferred = async {
                    reviewRepository.getReviewsByUser(user.id).getOrDefault(emptyList())
                }
                val resolvedUser = resolvedUserDeferred.await() ?: user

                _uiState.value = _uiState.value.copy(
                    user = resolvedUser,
                    isLoading = false,
                    reviewsCount = reviewsCountDeferred.await(),
                    favoritesCount = resolvedUser.favoriteRestaurants.size,
                    reviews = reviewsDeferred.await()
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val currentUser = _uiState.value.user
            val linkedBiometricAccount = localUserPreferences.getLinkedBiometricAccount()
            val shouldKeepSessionForBiometric = currentUser != null &&
                linkedBiometricAccount?.userId == currentUser.id

            if (!shouldKeepSessionForBiometric) {
                userRepository.signOut().fold(
                    onSuccess = { },
                    onFailure = { }
                )
            }
        }
    }
}
