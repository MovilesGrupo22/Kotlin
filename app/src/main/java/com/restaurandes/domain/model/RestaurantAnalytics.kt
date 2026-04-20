package com.restaurandes.domain.model

data class RestaurantAnalytics(
    val restaurantId: String,
    val restaurantName: String,
    val viewCount: Long = 0,
    val favoriteCount: Long = 0
) {
    val interactionScore: Long get() = viewCount + favoriteCount * 2
}
