package com.restaurandes.domain.repository

import com.restaurandes.domain.model.Review

interface ReviewRepository {
    suspend fun getReviewsByRestaurant(restaurantId: String): Result<List<Review>>
    suspend fun addReview(review: Review): Result<Review>
    suspend fun updateReview(review: Review): Result<Review>
    suspend fun deleteReview(reviewId: String): Result<Unit>
    suspend fun getReviewsCountByUser(userId: String): Result<Int>
}
