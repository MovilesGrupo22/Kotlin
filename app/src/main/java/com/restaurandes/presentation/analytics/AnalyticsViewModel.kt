package com.restaurandes.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurandes.domain.model.CategoryTimeSlotStat
import com.restaurandes.domain.model.RestaurantAnalytics
import com.restaurandes.domain.repository.RestaurantAnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val topViewed: List<RestaurantAnalytics> = emptyList(),
    val topInteracted: List<RestaurantAnalytics> = emptyList(),
    val categoryTimeSlotStats: List<CategoryTimeSlotStat> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: RestaurantAnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState(isLoading = true))
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val viewedDeferred = async { analyticsRepository.getTopViewedRestaurants(limit = 8) }
            val interactedDeferred = async { analyticsRepository.getTopInteractedRestaurants(limit = 8) }
            val categoryStatsDeferred = async { analyticsRepository.getCategoryTimeSlotStats() }

            val viewedResult = viewedDeferred.await()
            val interactedResult = interactedDeferred.await()
            val categoryStatsResult = categoryStatsDeferred.await()

            val error = viewedResult.exceptionOrNull()
                ?: interactedResult.exceptionOrNull()
                ?: categoryStatsResult.exceptionOrNull()

            _uiState.value = AnalyticsUiState(
                isLoading = false,
                topViewed = viewedResult.getOrNull().orEmpty(),
                topInteracted = interactedResult.getOrNull().orEmpty(),
                categoryTimeSlotStats = categoryStatsResult.getOrNull().orEmpty(),
                error = error?.message
            )
        }
    }
}
