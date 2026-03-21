package com.restaurandes.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.restaurandes.domain.model.Restaurant
import com.restaurandes.domain.repository.RestaurantRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.math.*

class RestaurantRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : RestaurantRepository {

    private companion object {
        const val COLLECTION_RESTAURANTS = "restaurants"
    }

    override suspend fun getRestaurants(): Result<List<Restaurant>> {
        return try {
            android.util.Log.d("RestaurantRepo", "Fetching restaurants from Firestore...")
            val snapshot = firestore.collection(COLLECTION_RESTAURANTS)
                .get()
                .await()
            
            android.util.Log.d("RestaurantRepo", "Snapshot size: ${snapshot.documents.size}")
            
            val restaurants = snapshot.documents.mapNotNull { doc ->
                try {
                    android.util.Log.d("RestaurantRepo", "Parsing doc: ${doc.id}")
                    android.util.Log.d("RestaurantRepo", "All fields: ${doc.data}")
                    val imageUrl = doc.getString("imageURL") ?: ""
                    android.util.Log.d("RestaurantRepo", "Image URL: '$imageUrl'")
                    Restaurant(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: "",
                        priceRange = doc.getString("priceRange") ?: "$$",
                        rating = doc.getDouble("rating") ?: 0.0,
                        imageUrl = imageUrl,
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        address = doc.getString("address") ?: "",
                        phone = doc.getString("phone") ?: "",
                        openingHours = doc.getString("openingHours") ?: "",
                        isOpen = doc.getBoolean("isOpen") ?: false,
                        lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis(),
                        tags = (doc.get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0
                    )
                } catch (e: Exception) {
                    android.util.Log.e("RestaurantRepo", "Error parsing doc: ${doc.id}", e)
                    null
                }
            }
            
            android.util.Log.d("RestaurantRepo", "Successfully loaded ${restaurants.size} restaurants")
            Result.success(restaurants)
        } catch (e: Exception) {
            android.util.Log.e("RestaurantRepo", "Error fetching restaurants", e)
            Result.failure(e)
        }
    }

    override suspend fun getRestaurantById(id: String): Result<Restaurant> {
        return try {
            val doc = firestore.collection(COLLECTION_RESTAURANTS)
                .document(id)
                .get()
                .await()
            
            if (doc.exists()) {
                val restaurant = Restaurant(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    category = doc.getString("category") ?: "",
                    priceRange = doc.getString("priceRange") ?: "$$",
                    rating = doc.getDouble("rating") ?: 0.0,
                    imageUrl = doc.getString("imageURL") ?: "",
                    latitude = doc.getDouble("latitude") ?: 0.0,
                    longitude = doc.getDouble("longitude") ?: 0.0,
                    address = doc.getString("address") ?: "",
                    phone = doc.getString("phone") ?: "",
                    openingHours = doc.getString("openingHours") ?: "",
                    isOpen = doc.getBoolean("isOpen") ?: false,
                    lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis(),
                    tags = (doc.get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0
                )
                Result.success(restaurant)
            } else {
                Result.failure(Exception("Restaurant not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchRestaurants(query: String): Result<List<Restaurant>> {
        return try {
            val allRestaurants = getRestaurants().getOrNull() ?: emptyList()
            
            val filtered = allRestaurants.filter { restaurant ->
                restaurant.name.contains(query, ignoreCase = true) ||
                restaurant.description.contains(query, ignoreCase = true) ||
                restaurant.category.contains(query, ignoreCase = true) ||
                restaurant.tags.any { it.contains(query, ignoreCase = true) }
            }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRestaurantsByCategory(category: String): Result<List<Restaurant>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_RESTAURANTS)
                .whereEqualTo("category", category)
                .get()
                .await()
            
            val restaurants = snapshot.documents.mapNotNull { doc ->
                try {
                    Restaurant(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: "",
                        priceRange = doc.getString("priceRange") ?: "$$",
                        rating = doc.getDouble("rating") ?: 0.0,
                        imageUrl = doc.getString("imageURL") ?: "",
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        address = doc.getString("address") ?: "",
                        phone = doc.getString("phone") ?: "",
                        openingHours = doc.getString("openingHours") ?: "",
                        isOpen = doc.getBoolean("isOpen") ?: false,
                        lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis(),
                        tags = (doc.get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(restaurants)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNearbyRestaurants(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Result<List<Restaurant>> {
        return try {
            val allRestaurants = getRestaurants().getOrNull() ?: emptyList()
            
            val nearby = allRestaurants.filter { restaurant ->
                val distance = calculateDistance(
                    latitude, longitude,
                    restaurant.latitude, restaurant.longitude
                )
                distance <= radiusKm
            }
            
            Result.success(nearby.sortedBy { restaurant ->
                calculateDistance(
                    latitude, longitude,
                    restaurant.latitude, restaurant.longitude
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeRestaurants(): Flow<List<Restaurant>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_RESTAURANTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val restaurants = snapshot.documents.mapNotNull { doc ->
                        try {
                            Restaurant(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                description = doc.getString("description") ?: "",
                                category = doc.getString("category") ?: "",
                                priceRange = doc.getString("priceRange") ?: "$$",
                                rating = doc.getDouble("rating") ?: 0.0,
                                imageUrl = doc.getString("imageURL") ?: "",
                                latitude = doc.getDouble("latitude") ?: 0.0,
                                longitude = doc.getDouble("longitude") ?: 0.0,
                                address = doc.getString("address") ?: "",
                                phone = doc.getString("phone") ?: "",
                                openingHours = doc.getString("openingHours") ?: "",
                                isOpen = doc.getBoolean("isOpen") ?: false,
                                lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis(),
                                tags = (doc.get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(restaurants)
                }
            }
        
        awaitClose { listener.remove() }
    }

    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
