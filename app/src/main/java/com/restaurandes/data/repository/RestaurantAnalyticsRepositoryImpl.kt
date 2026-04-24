package com.restaurandes.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.restaurandes.domain.model.CategoryPeakStat
import com.restaurandes.domain.model.CategoryTimeSlotStat
import com.restaurandes.domain.model.RestaurantAnalytics
import com.restaurandes.domain.repository.RestaurantAnalyticsRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class RestaurantAnalyticsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : RestaurantAnalyticsRepository {

    companion object {
        private const val COLLECTION = "restaurant_analytics"
        private const val COLLECTION_CATEGORY_STATS = "category_time_stats"
        private const val COLLECTION_PEAK_STATS = "category_peak_stats"
    }

    override suspend fun trackView(restaurantId: String, restaurantName: String) {
        try {
            firestore.collection(COLLECTION).document(restaurantId)
                .set(
                    mapOf(
                        "restaurantId" to restaurantId,
                        "restaurantName" to restaurantName,
                        "viewCount" to FieldValue.increment(1)
                    ),
                    SetOptions.merge()
                ).await()
        } catch (_: Exception) {}
    }

    override suspend fun trackFavorite(restaurantId: String, restaurantName: String) {
        try {
            firestore.collection(COLLECTION).document(restaurantId)
                .set(
                    mapOf(
                        "restaurantId" to restaurantId,
                        "restaurantName" to restaurantName,
                        "favoriteCount" to FieldValue.increment(1)
                    ),
                    SetOptions.merge()
                ).await()
        } catch (_: Exception) {}
    }

    override suspend fun trackCategoryExplored(category: String, timeSlot: String) {
        if (category.isBlank() || timeSlot.isBlank()) return
        try {
            val docId = "${category.lowercase().replace(" ", "_")}_$timeSlot"
            firestore.collection(COLLECTION_CATEGORY_STATS).document(docId)
                .set(
                    mapOf(
                        "category" to category,
                        "timeSlot" to timeSlot,
                        "count" to FieldValue.increment(1)
                    ),
                    SetOptions.merge()
                ).await()
        } catch (_: Exception) {}
    }

    override suspend fun getCategoryTimeSlotStats(): Result<List<CategoryTimeSlotStat>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_CATEGORY_STATS).get().await()
            val result = snapshot.documents.mapNotNull { doc ->
                try {
                    CategoryTimeSlotStat(
                        category = doc.getString("category") ?: return@mapNotNull null,
                        timeSlot = doc.getString("timeSlot") ?: return@mapNotNull null,
                        count = doc.getLong("count") ?: 0
                    )
                } catch (_: Exception) { null }
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun trackCategoryPeak(category: String, timeSlot: String, dayOfWeek: String) {
        if (category.isBlank() || timeSlot.isBlank() || dayOfWeek.isBlank()) return
        try {
            val slug = category.lowercase().replace(" ", "_")
            val docId = "${slug}_${timeSlot}_${dayOfWeek}"
            firestore.collection(COLLECTION_PEAK_STATS).document(docId)
                .set(
                    mapOf(
                        "category" to category,
                        "timeSlot" to timeSlot,
                        "dayOfWeek" to dayOfWeek,
                        "count" to FieldValue.increment(1)
                    ),
                    SetOptions.merge()
                ).await()
        } catch (_: Exception) {}
    }

    override suspend fun getCategoryPeakStats(): Result<List<CategoryPeakStat>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_PEAK_STATS).get().await()
            val result = snapshot.documents.mapNotNull { doc ->
                try {
                    CategoryPeakStat(
                        category = doc.getString("category") ?: return@mapNotNull null,
                        timeSlot = doc.getString("timeSlot") ?: return@mapNotNull null,
                        dayOfWeek = doc.getString("dayOfWeek") ?: return@mapNotNull null,
                        count = doc.getLong("count") ?: 0
                    )
                } catch (_: Exception) { null }
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTopViewedRestaurants(limit: Int): Result<List<RestaurantAnalytics>> {
        return try {
            val snapshot = firestore.collection(COLLECTION)
                .orderBy("viewCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val result = snapshot.documents.mapNotNull { doc ->
                try {
                    RestaurantAnalytics(
                        restaurantId = doc.getString("restaurantId") ?: doc.id,
                        restaurantName = doc.getString("restaurantName") ?: "",
                        viewCount = doc.getLong("viewCount") ?: 0,
                        favoriteCount = doc.getLong("favoriteCount") ?: 0
                    )
                } catch (_: Exception) { null }
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTopInteractedRestaurants(limit: Int): Result<List<RestaurantAnalytics>> {
        return try {
            val snapshot = firestore.collection(COLLECTION).get().await()

            val result = snapshot.documents.mapNotNull { doc ->
                try {
                    RestaurantAnalytics(
                        restaurantId = doc.getString("restaurantId") ?: doc.id,
                        restaurantName = doc.getString("restaurantName") ?: "",
                        viewCount = doc.getLong("viewCount") ?: 0,
                        favoriteCount = doc.getLong("favoriteCount") ?: 0
                    )
                } catch (_: Exception) { null }
            }
                .sortedByDescending { it.interactionScore }
                .take(limit)

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
