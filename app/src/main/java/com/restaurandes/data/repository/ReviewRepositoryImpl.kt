package com.restaurandes.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.restaurandes.domain.model.Review
import com.restaurandes.domain.repository.ReviewRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewRepository {

    private companion object {
        const val COLLECTION_REVIEWS = "reviews"
        const val COLLECTION_RESTAURANTS = "restaurants"
    }

    private var lastKnownReviewsByRestaurant: Map<String, List<Review>> = emptyMap()
    private var lastKnownReviewsByUser: Map<String, List<Review>> = emptyMap()

    override suspend fun getReviewsByRestaurant(restaurantId: String): Result<List<Review>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_REVIEWS)
                .whereEqualTo("restaurantId", restaurantId)
                .get()
                .await()

            val reviews = snapshot.toReviews()
            lastKnownReviewsByRestaurant = lastKnownReviewsByRestaurant + (restaurantId to reviews)

            Result.success(reviews)
        } catch (e: Exception) {
            getReviewsByRestaurantFromCache(restaurantId, e)
        }
    }

    override suspend fun getReviewsByUser(userId: String): Result<List<Review>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_REVIEWS)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val reviews = snapshot.toReviews()
            lastKnownReviewsByUser = lastKnownReviewsByUser + (userId to reviews)

            Result.success(reviews)
        } catch (e: Exception) {
            getReviewsByUserFromCache(userId, e)
        }
    }

    override suspend fun addReview(review: Review): Result<Review> {
        return try {
            val document = firestore.collection(COLLECTION_REVIEWS).document()
            val reviewWithId = review.copy(id = document.id)

            document.set(
                mapOf(
                    "restaurantId" to review.restaurantId,
                    "userId" to review.userId,
                    "userName" to review.userName,
                    "rating" to review.rating,
                    "comment" to review.comment,
                    "timestamp" to review.timestamp,
                    "imageUrls" to review.imageUrls
                )
            ).await()

            updateRestaurantReviewStats(review.restaurantId)
            Result.success(reviewWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateReview(review: Review): Result<Review> {
        return try {
            firestore.collection(COLLECTION_REVIEWS)
                .document(review.id)
                .set(
                    mapOf(
                        "restaurantId" to review.restaurantId,
                        "userId" to review.userId,
                        "userName" to review.userName,
                        "rating" to review.rating,
                        "comment" to review.comment,
                        "timestamp" to review.timestamp,
                        "imageUrls" to review.imageUrls
                    )
                )
                .await()

            updateRestaurantReviewStats(review.restaurantId)
            Result.success(review)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteReview(reviewId: String): Result<Unit> {
        return try {
            val reviewDoc = firestore.collection(COLLECTION_REVIEWS)
                .document(reviewId)
                .get()
                .await()
            val restaurantId = reviewDoc.getString("restaurantId")

            firestore.collection(COLLECTION_REVIEWS)
                .document(reviewId)
                .delete()
                .await()

            if (!restaurantId.isNullOrBlank()) {
                updateRestaurantReviewStats(restaurantId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateRestaurantReviewStats(restaurantId: String) {
        val reviews = getReviewsByRestaurant(restaurantId).getOrNull().orEmpty()
        val reviewCount = reviews.size
        val averageRating = if (reviewCount == 0) 0.0 else reviews.map { it.rating }.average()

        firestore.collection(COLLECTION_RESTAURANTS)
            .document(restaurantId)
            .update(
                mapOf(
                    "reviewCount" to reviewCount,
                    "rating" to averageRating
                )
            )
            .await()
    }

    override suspend fun getReviewsCountByUser(userId: String): Result<Int> {
        return try {
            val snapshot = firestore.collection(COLLECTION_REVIEWS)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            Result.success(snapshot.size())
        } catch (e: Exception) {
            getReviewsByUserFromCache(userId, e).map { it.size }
        }
    }

    private suspend fun getReviewsByRestaurantFromCache(
        restaurantId: String,
        originalError: Exception
    ): Result<List<Review>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_REVIEWS)
                .whereEqualTo("restaurantId", restaurantId)
                .get(Source.CACHE)
                .await()

            val reviews = snapshot.toReviews()
            lastKnownReviewsByRestaurant = lastKnownReviewsByRestaurant + (restaurantId to reviews)
            Result.success(reviews)
        } catch (cacheError: Exception) {
            lastKnownReviewsByRestaurant[restaurantId]?.let { Result.success(it) }
                ?: Result.failure(Exception("Reviews not available offline", originalError))
        }
    }

    private suspend fun getReviewsByUserFromCache(
        userId: String,
        originalError: Exception
    ): Result<List<Review>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_REVIEWS)
                .whereEqualTo("userId", userId)
                .get(Source.CACHE)
                .await()

            val reviews = snapshot.toReviews()
            lastKnownReviewsByUser = lastKnownReviewsByUser + (userId to reviews)
            Result.success(reviews)
        } catch (cacheError: Exception) {
            lastKnownReviewsByUser[userId]?.let { Result.success(it) }
                ?: Result.failure(Exception("User reviews not available offline", originalError))
        }
    }

    private fun QuerySnapshot.toReviews(): List<Review> {
        return documents.mapNotNull { doc ->
            try {
                Review(
                    id = doc.id,
                    restaurantId = doc.getString("restaurantId") ?: return@mapNotNull null,
                    userId = doc.getString("userId") ?: "",
                    userName = doc.getString("userName") ?: "Usuario",
                    rating = doc.getDouble("rating") ?: 0.0,
                    comment = doc.getString("comment") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    imageUrls = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>().orEmpty()
                )
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.timestamp }
    }
}
