package com.restaurandes.domain.repository

import com.restaurandes.domain.model.RestaurantAnalytics

interface RestaurantAnalyticsRepository {
    suspend fun trackView(restaurantId: String, restaurantName: String)
    suspend fun trackFavorite(restaurantId: String, restaurantName: String)
    suspend fun getTopViewedRestaurants(limit: Int = 10): Result<List<RestaurantAnalytics>>
    suspend fun getTopInteractedRestaurants(limit: Int = 10): Result<List<RestaurantAnalytics>>
}
