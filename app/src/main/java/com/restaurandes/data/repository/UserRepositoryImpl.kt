package com.restaurandes.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.restaurandes.data.analytics.AnalyticsService
import com.restaurandes.domain.model.User
import com.restaurandes.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val analyticsService: AnalyticsService
) : UserRepository {

    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val firestore: FirebaseFirestore by lazy { Firebase.firestore }
    
    private val _currentUser = MutableStateFlow<User?>(null)
    private var lastKnownUser: User? = null
    
    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                val existingUser = _currentUser.value?.takeIf { it.id == firebaseUser.uid }
                    ?: lastKnownUser?.takeIf { it.id == firebaseUser.uid }

                _currentUser.value = User(
                    id = firebaseUser.uid,
                    email = existingUser?.email ?: firebaseUser.email ?: "",
                    name = existingUser?.name
                        ?: firebaseUser.displayName
                        ?: firebaseUser.email?.substringBefore("@")
                        ?: "User",
                    photoUrl = existingUser?.photoUrl ?: firebaseUser.photoUrl?.toString(),
                    favoriteRestaurants = existingUser?.favoriteRestaurants ?: emptyList(),
                    dietaryPreferences = existingUser?.dietaryPreferences ?: emptyList(),
                    createdAt = existingUser?.createdAt ?: System.currentTimeMillis()
                )
            } else {
                _currentUser.value = null
            }
        }
    }

    override suspend fun getCurrentUser(): Result<User?> {
        return try {
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                val doc = try {
                    firestore.collection("users")
                        .document(firebaseUser.uid)
                        .get()
                        .await()
                } catch (_: Exception) {
                    firestore.collection("users")
                        .document(firebaseUser.uid)
                        .get(Source.CACHE)
                        .await()
                }
                
                val user = if (doc.exists()) {
                    User(
                        id = firebaseUser.uid,
                        email = doc.getString("email") ?: firebaseUser.email ?: "",
                        name = doc.getString("name") ?: firebaseUser.displayName ?: "",
                        photoUrl = doc.getString("photoUrl") ?: firebaseUser.photoUrl?.toString(),
                        favoriteRestaurants = (doc.get("favoriteRestaurants") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList(),
                        dietaryPreferences = (doc.get("dietaryPreferences") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList(),
                        createdAt = doc.getLong("createdAt")
                            ?: lastKnownUser?.takeIf { it.id == firebaseUser.uid }?.createdAt
                            ?: System.currentTimeMillis()
                    )
                } else {
                    val newUser = User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User",
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        createdAt = lastKnownUser?.takeIf { it.id == firebaseUser.uid }?.createdAt
                            ?: System.currentTimeMillis()
                    )
                    saveUserToFirestore(newUser)
                    newUser
                }
                _currentUser.value = user
                lastKnownUser = user
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            lastKnownUser?.let { Result.success(it) } ?: Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Authentication failed"))
            
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: email,
                name = firebaseUser.displayName ?: email.substringBefore("@"),
                photoUrl = firebaseUser.photoUrl?.toString(),
                createdAt = lastKnownUser?.takeIf { it.id == firebaseUser.uid }?.createdAt
                    ?: System.currentTimeMillis()
            )
            
            getCurrentUser()
            analyticsService.logSignIn("email", firebaseUser.uid)
            analyticsService.logUserSession(firebaseUser.uid)
            
            Result.success(_currentUser.value ?: user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Google authentication failed"))

            getCurrentUser()
            analyticsService.logSignIn("google", firebaseUser.uid)
            analyticsService.logUserSession(firebaseUser.uid)

            Result.success(_currentUser.value ?: User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User",
                photoUrl = firebaseUser.photoUrl?.toString(),
                createdAt = lastKnownUser?.takeIf { it.id == firebaseUser.uid }?.createdAt
                    ?: System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Registration failed"))
            
            val user = User(
                id = firebaseUser.uid,
                email = email,
                name = name,
                photoUrl = firebaseUser.photoUrl?.toString(),
                createdAt = System.currentTimeMillis()
            )
            
            saveUserToFirestore(user)
            _currentUser.value = user
            lastKnownUser = user
            analyticsService.logSignUp("email", firebaseUser.uid)
            analyticsService.logUserSession(firebaseUser.uid)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            auth.signOut()
            _currentUser.value = null
            userId?.let {
                analyticsService.logUserSessionEnd(it, 0)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUser(user: User): Result<User> {
        return try {
            saveUserToFirestore(user)
            _currentUser.value = user
            lastKnownUser = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addFavoriteRestaurant(restaurantId: String): Result<Unit> {
        return try {
            val currentUser = _currentUser.value ?: return Result.failure(Exception("User not logged in"))
            val updatedFavorites = currentUser.favoriteRestaurants.toMutableList().apply {
                if (!contains(restaurantId)) add(restaurantId)
            }
            val updatedUser = currentUser.copy(favoriteRestaurants = updatedFavorites)
            
            firestore.collection("users")
                .document(currentUser.id)
                .update("favoriteRestaurants", updatedFavorites)
                .await()

            _currentUser.value = updatedUser
            lastKnownUser = updatedUser
            analyticsService.logRestaurantFavorited(restaurantId, "", currentUser.id)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFavoriteRestaurant(restaurantId: String): Result<Unit> {
        return try {
            val currentUser = _currentUser.value ?: return Result.failure(Exception("User not logged in"))
            val updatedFavorites = currentUser.favoriteRestaurants.toMutableList().apply {
                remove(restaurantId)
            }
            val updatedUser = currentUser.copy(favoriteRestaurants = updatedFavorites)
            
            firestore.collection("users")
                .document(currentUser.id)
                .update("favoriteRestaurants", updatedFavorites)
                .await()

            _currentUser.value = updatedUser
            lastKnownUser = updatedUser
            analyticsService.logRestaurantUnfavorited(restaurantId, currentUser.id)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeCurrentUser(): Flow<User?> {
        return _currentUser.asStateFlow()
    }
    
    private suspend fun saveUserToFirestore(user: User) {
        firestore.collection("users")
            .document(user.id)
            .set(
                mapOf(
                    "email" to user.email,
                    "name" to user.name,
                    "photoUrl" to user.photoUrl,
                    "favoriteRestaurants" to user.favoriteRestaurants,
                    "dietaryPreferences" to user.dietaryPreferences,
                    "createdAt" to user.createdAt
                )
            )
            .await()
    }
}
