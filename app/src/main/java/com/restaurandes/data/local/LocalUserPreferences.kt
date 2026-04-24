package com.restaurandes.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
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

@Singleton
class LocalUserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        val LAST_HOME_FILTER = stringPreferencesKey("last_home_filter")
    }

    private fun biometricEnabledKey(userId: String) =
        booleanPreferencesKey("biometric_enabled_$userId")

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

    suspend fun getLastHomeFilter(): String {
        return lastHomeFilter.first()
    }

    suspend fun saveLastHomeFilter(filter: String) {
        context.restaurandesDataStore.edit { preferences ->
            preferences[LAST_HOME_FILTER] = filter
        }
    }

    suspend fun isBiometricEnabled(userId: String): Boolean {
        return context.restaurandesDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[biometricEnabledKey(userId)] ?: false
            }
            .first()
    }

    suspend fun saveBiometricEnabled(userId: String, enabled: Boolean) {
        context.restaurandesDataStore.edit { preferences ->
            preferences[biometricEnabledKey(userId)] = enabled
        }
    }
}
