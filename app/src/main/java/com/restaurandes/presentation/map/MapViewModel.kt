package com.restaurandes.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurandes.domain.model.Restaurant
import com.restaurandes.domain.usecase.GetRestaurantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val isLoading: Boolean = false,
    val restaurants: List<Restaurant> = emptyList(),
    val selectedPriceFilter: String? = null,
    val error: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val getRestaurantsUseCase: GetRestaurantsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState(isLoading = true))
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var allRestaurants: List<Restaurant> = emptyList()

    init {
        loadRestaurants()
    }

    fun loadRestaurants() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            getRestaurantsUseCase().fold(
                onSuccess = { restaurants ->
                    allRestaurants = restaurants
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        restaurants = applyFilter(restaurants, _uiState.value.selectedPriceFilter),
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        restaurants = emptyList(),
                        error = exception.message ?: "No se pudieron cargar los restaurantes"
                    )
                }
            )
        }
    }

    fun setSelectedPriceFilter(price: String?) {
        val newFilter = if (price == _uiState.value.selectedPriceFilter) null else price
        _uiState.value = _uiState.value.copy(
            selectedPriceFilter = newFilter,
            restaurants = applyFilter(allRestaurants, newFilter)
        )
    }

    private fun applyFilter(restaurants: List<Restaurant>, filter: String?): List<Restaurant> {
        if (filter == null) return restaurants
        return restaurants.filter { it.priceRange.trim() == filter }
    }
}
