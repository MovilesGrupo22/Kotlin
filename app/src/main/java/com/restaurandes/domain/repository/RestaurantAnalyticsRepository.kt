package com.restaurandes.domain.repository

import com.restaurandes.domain.model.CategoryTimeSlotStat
import com.restaurandes.domain.model.RestaurantAnalytics

interface RestaurantAnalyticsRepository {
    suspend fun trackView(restaurantId: String, restaurantName: String)
    suspend fun trackFavorite(restaurantId: String, restaurantName: String)
    suspend fun trackCategoryExplored(category: String, timeSlot: String)
    suspend fun getTopViewedRestaurants(limit: Int = 10): Result<List<RestaurantAnalytics>>
    suspend fun getTopInteractedRestaurants(limit: Int = 10): Result<List<RestaurantAnalytics>>
    suspend fun getCategoryTimeSlotStats(): Result<List<CategoryTimeSlotStat>>
}
