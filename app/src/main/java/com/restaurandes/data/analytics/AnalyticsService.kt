package com.restaurandes.data.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analytics Service for tracking Business Questions
 * Sprint 2 - Business Intelligence Implementation
 *
 * Tracks:
 * - BQ1 (Type 1): Weekly Active Users
 * - BQ2 (Type 2): Section Interactions (Home/Map/Search/Favorites)
 * - BQ3 (Type 3): View-to-Favorite Conversion Rate
 */
@Singleton
class AnalyticsService @Inject constructor() {

    private val analytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }

    // BQ1: User Sessions (Weekly Active Users)
    fun logUserSession(userId: String) {
        val bundle = Bundle().apply {
            putString("user_id", userId)
            putLong("timestamp", System.currentTimeMillis())
        }
        analytics.logEvent("user_session_start", bundle)
    }

    fun logUserSessionEnd(userId: String, durationSeconds: Long) {
        val bundle = Bundle().apply {
            putString("user_id", userId)
            putLong("session_duration", durationSeconds)
            putLong("timestamp", System.currentTimeMillis())
        }
        analytics.logEvent("user_session_end", bundle)
    }

    // BQ2: Section Interactions
    enum class AppSection {
        HOME, MAP, SEARCH, FAVORITES, PROFILE, DETAIL
    }

    fun logSectionView(section: AppSection, userId: String?) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, section.name.lowercase())
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "${section.name}Screen")
            userId?.let { putString("user_id", it) }
            putLong("timestamp", System.currentTimeMillis())
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logSectionInteraction(section: AppSection, action: String, userId: String?) {
        val bundle = Bundle().apply {
            putString("section", section.name.lowercase())
            putString("action", action)
            userId?.let { putString("user_id", it) }
            putLong("timestamp", System.currentTimeMillis())
        }
        analytics.logEvent("section_interaction", bundle)
    }

    // BQ3: View-to-Favorite Conversion
    fun logRestaurantView(restaurantId: String, restaurantName: String, userId: String?) {
        val bundle = Bundle().apply {
            putString("restaurant_id", restaurantId)
            putString("restaurant_name", restaurantName)
            userId?.let { putString("user_id", it) }
            putLong("timestamp", System.currentTimeMillis())
        }
        analytics.logEvent("restaurant_view", bundle)
    }

    fun logRestaurantFavorited(restaurantId: String, restaurantName: String, userId: String) {
        val bundle = Bundle().apply {
            putString("restaurant_id", restaurantId)
            putString("restaurant_name", restaurantName)
            putString("user_id", userId)
            putLong("timestamp", System.currentTimeMillis())
        }
        analytics.logEvent("restaurant_favorited", bundle)
    }

    fun logRestaurantUnfavorited(restaurantId: String, userId: String) {
        val bundle = Bundle().apply {
            putString("restaurant_id", restaurantId)
            putString("user_id", userId)
            putLong("timestamp", System.currentTimeMillis())
        }
        analytics.logEvent("restaurant_unfavorited", bundle)
    }

    // Authentication Events
    fun logSignIn(method: String, userId: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
            putString("user_id", userId)
        }
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
    }

    fun logSignUp(method: String, userId: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
            putString("user_id", userId)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
    }

    // Search Events
    fun logSearch(query: String, resultsCount: Int, userId: String?) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SEARCH_TERM, query)
            putInt("results_count", resultsCount)
            userId?.let { putString("user_id", it) }
        }
        analytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
    }

    // BQ3: Compare Feature Usage
    fun logCompareUsed(
        primaryRestaurantId: String,
        secondaryRestaurantId: String,
        selectionMode: String,
        userId: String?
    ) {
        val bundle = Bundle().apply {
            putString("primary_restaurant_id", primaryRestaurantId)
            putString("secondary_restaurant_id", secondaryRestaurantId)
            putString("selection_mode", selectionMode) // "auto" or "manual"
            userId?.let { putString("user_id", it) }
            putLong("timestamp", System.currentTimeMillis())
        }
        analytics.logEvent("compare_used", bundle)
    }

    // Filter Events
    fun logFilterUsed(filterType: String, userId: String?) {
        val bundle = Bundle().apply {
            putString("filter_type", filterType)
            userId?.let { putString("user_id", it) }
        }
        analytics.logEvent("filter_applied", bundle)
    }

    // User Properties (for segmentation)
    fun setUserProperties(userId: String, preferences: Map<String, String>) {
        analytics.setUserId(userId)
        preferences.forEach { (key, value) ->
            analytics.setUserProperty(key, value)
        }
    }
}
