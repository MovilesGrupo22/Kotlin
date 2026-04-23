package com.restaurandes.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
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
        const val CACHE_TTL_MS = 5 * 60 * 1000L
    }

    private var cachedRestaurants: List<Restaurant> = emptyList()
    private var cacheTimestamp: Long = 0L

    private fun isCacheValid() =
        cachedRestaurants.isNotEmpty() &&
        System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS
    private var lastKnownRestaurants: List<Restaurant> = emptyList()

    override suspend fun getRestaurants(): Result<List<Restaurant>> {
        if (isCacheValid()) {
            android.util.Log.d("RestaurantRepo", "Returning cached restaurants (${cachedRestaurants.size})")
            return Result.success(cachedRestaurants)
        }

        return try {
            android.util.Log.d("RestaurantRepo", "Fetching restaurants from Firestore...")
            val snapshot = firestore.collection(COLLECTION_RESTAURANTS)
                .get()
                .await()
            
            android.util.Log.d("RestaurantRepo", "Snapshot size: ${snapshot.documents.size}")
            val restaurants = snapshot.toRestaurants()
            lastKnownRestaurants = restaurants
            
            android.util.Log.d("RestaurantRepo", "Successfully loaded ${restaurants.size} restaurants")
            cachedRestaurants = restaurants
            cacheTimestamp = System.currentTimeMillis()
            Result.success(restaurants)
        } catch (e: Exception) {
            android.util.Log.w("RestaurantRepo", "Network fetch failed, trying offline cache", e)
            getRestaurantsFromCache(e)
        }
    }

    override suspend fun getRestaurantById(id: String): Result<Restaurant> {
        return try {
            val doc = firestore.collection(COLLECTION_RESTAURANTS)
                .document(id)
                .get()
                .await()
            
            if (doc.exists()) {
                val restaurant = doc.toRestaurant()
                lastKnownRestaurants = upsertRestaurant(lastKnownRestaurants, restaurant)
                Result.success(restaurant)
            } else {
                Result.failure(Exception("Restaurant not found"))
            }
        } catch (e: Exception) {
            android.util.Log.w("RestaurantRepo", "Detail network fetch failed, trying offline cache", e)
            getRestaurantByIdFromCache(id, e)
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
            val restaurants = getRestaurants().getOrThrow()
                .filter { it.category.equals(category, ignoreCase = true) }
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
                            doc.toRestaurant()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    lastKnownRestaurants = restaurants
                    trySend(restaurants)
                }
            }
        
        awaitClose { listener.remove() }
    }

    private suspend fun getRestaurantsFromCache(originalError: Exception): Result<List<Restaurant>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_RESTAURANTS)
                .get(Source.CACHE)
                .await()
            val restaurants = snapshot.toRestaurants()

            if (restaurants.isNotEmpty()) {
                lastKnownRestaurants = restaurants
                android.util.Log.d("RestaurantRepo", "Loaded ${restaurants.size} restaurants from Firestore cache")
                Result.success(restaurants)
            } else if (lastKnownRestaurants.isNotEmpty()) {
                android.util.Log.d("RestaurantRepo", "Loaded ${lastKnownRestaurants.size} restaurants from memory cache")
                Result.success(lastKnownRestaurants)
            } else {
                Result.failure(Exception("No cached restaurants available", originalError))
            }
        } catch (cacheError: Exception) {
            if (lastKnownRestaurants.isNotEmpty()) {
                Result.success(lastKnownRestaurants)
            } else {
                Result.failure(Exception("No cached restaurants available", cacheError))
            }
        }
    }

    private suspend fun getRestaurantByIdFromCache(
        id: String,
        originalError: Exception
    ): Result<Restaurant> {
        return try {
            val doc = firestore.collection(COLLECTION_RESTAURANTS)
                .document(id)
                .get(Source.CACHE)
                .await()

            if (doc.exists()) {
                val restaurant = doc.toRestaurant()
                lastKnownRestaurants = upsertRestaurant(lastKnownRestaurants, restaurant)
                Result.success(restaurant)
            } else {
                lastKnownRestaurants.firstOrNull { it.id == id }?.let { Result.success(it) }
                    ?: Result.failure(Exception("Restaurant not available offline", originalError))
            }
        } catch (cacheError: Exception) {
            lastKnownRestaurants.firstOrNull { it.id == id }?.let { Result.success(it) }
                ?: Result.failure(Exception("Restaurant not available offline", cacheError))
        }
    }

    private fun QuerySnapshot.toRestaurants(): List<Restaurant> {
        return documents.mapNotNull { doc ->
            try {
                android.util.Log.d("RestaurantRepo", "Parsing doc: ${doc.id}")
                doc.toRestaurant()
            } catch (e: Exception) {
                android.util.Log.e("RestaurantRepo", "Error parsing doc: ${doc.id}", e)
                null
            }
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toRestaurant(): Restaurant {
        val imageUrl = getString("imageURL") ?: ""
        android.util.Log.d("RestaurantRepo", "Image URL: '$imageUrl'")
        return Restaurant(
            id = id,
            name = getString("name") ?: "",
            description = getString("description") ?: "",
            category = getString("category") ?: "",
            priceRange = getString("priceRange") ?: "$$",
            rating = getDouble("rating") ?: 0.0,
            imageUrl = imageUrl,
            latitude = getDouble("latitude") ?: 0.0,
            longitude = getDouble("longitude") ?: 0.0,
            address = getString("address") ?: "",
            phone = getString("phone") ?: "",
            openingHours = getString("openingHours") ?: "",
            isOpen = getBoolean("isOpen") ?: false,
            lastUpdated = getLong("lastUpdated") ?: System.currentTimeMillis(),
            tags = (get("tags") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            reviewCount = getLong("reviewCount")?.toInt() ?: 0
        )
    }

    private fun upsertRestaurant(
        restaurants: List<Restaurant>,
        restaurant: Restaurant
    ): List<Restaurant> {
        return if (restaurants.any { it.id == restaurant.id }) {
            restaurants.map { if (it.id == restaurant.id) restaurant else it }
        } else {
            restaurants + restaurant
        }
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
