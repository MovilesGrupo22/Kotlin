package com.restaurandes.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.restaurandesDataStore by preferencesDataStore(
    name = "restaurandes_user_preferences"
)

data class LinkedBiometricAccount(
    val userId: String,
    val name: String,
    val email: String
)

@Singleton
class LocalUserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        val LAST_HOME_FILTER = stringPreferencesKey("last_home_filter")
        val BIOMETRIC_LINKED_USER_ID = stringPreferencesKey("biometric_linked_user_id")
        val BIOMETRIC_LINKED_USER_NAME = stringPreferencesKey("biometric_linked_user_name")
        val BIOMETRIC_LINKED_USER_EMAIL = stringPreferencesKey("biometric_linked_user_email")
    }

    val lastHomeFilter: Flow<String> = context.restaurandesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[LAST_HOME_FILTER] ?: "All"
        }

    val linkedBiometricAccount: Flow<LinkedBiometricAccount?> = context.restaurandesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val userId = preferences[BIOMETRIC_LINKED_USER_ID]
            val name = preferences[BIOMETRIC_LINKED_USER_NAME]
            val email = preferences[BIOMETRIC_LINKED_USER_EMAIL]

            if (userId.isNullOrBlank() || email.isNullOrBlank()) {
                null
            } else {
                LinkedBiometricAccount(
                    userId = userId,
                    name = name ?: email.substringBefore("@"),
                    email = email
                )
            }
        }

    suspend fun getLastHomeFilter(): String {
        return lastHomeFilter.first()
    }

    suspend fun saveLastHomeFilter(filter: String) {
        context.restaurandesDataStore.edit { preferences ->
            preferences[LAST_HOME_FILTER] = filter
        }
    }

    suspend fun getLinkedBiometricAccount(): LinkedBiometricAccount? {
        return linkedBiometricAccount.first()
    }

    suspend fun saveLinkedBiometricAccount(
        userId: String,
        name: String,
        email: String
    ) {
        context.restaurandesDataStore.edit { preferences ->
            preferences[BIOMETRIC_LINKED_USER_ID] = userId
            preferences[BIOMETRIC_LINKED_USER_NAME] = name
            preferences[BIOMETRIC_LINKED_USER_EMAIL] = email
        }
    }

    suspend fun clearLinkedBiometricAccount() {
        context.restaurandesDataStore.edit { preferences ->
            preferences.remove(BIOMETRIC_LINKED_USER_ID)
            preferences.remove(BIOMETRIC_LINKED_USER_NAME)
            preferences.remove(BIOMETRIC_LINKED_USER_EMAIL)
        }
    }
}
