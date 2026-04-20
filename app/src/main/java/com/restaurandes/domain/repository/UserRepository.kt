package com.restaurandes.domain.repository

import com.restaurandes.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getCurrentUser(): Result<User?>
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signUp(email: String, password: String, name: String): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun updateUser(user: User): Result<User>
    suspend fun addFavoriteRestaurant(restaurantId: String): Result<Unit>
    suspend fun removeFavoriteRestaurant(restaurantId: String): Result<Unit>
    fun observeCurrentUser(): Flow<User?>
}
