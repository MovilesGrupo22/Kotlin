package com.restaurandes.presentation.home

import com.restaurandes.domain.model.Restaurant

enum class HomeContextTone {
    POSITIVE,
    INFO,
    WARNING
}

data class HomeContextCanvas(
    val message: String,
    val actionLabel: String? = null,
    val tone: HomeContextTone = HomeContextTone.INFO
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val restaurants: List<Restaurant> = emptyList(),
    val allRestaurants: List<Restaurant> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val error: String? = null,
    val userLocation: Pair<Double, Double>? = null,
    val selectedCategory: String = "All",
    val contextCanvas: HomeContextCanvas? = null
)
