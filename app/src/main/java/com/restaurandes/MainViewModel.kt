package com.restaurandes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.restaurandes.data.local.LocalUserPreferences
import com.restaurandes.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val startDestination: String? = null,
    val isInitializing: Boolean = true,
    val isBiometricLocked: Boolean = false,
    val shouldRequestBiometric: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val localUserPreferences: LocalUserPreferences
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var hasProcessedInitialAuthState = false
    private var unlockedUserId: String? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        viewModelScope.launch {
            val currentUserId = firebaseAuth.currentUser?.uid
            val shouldGateWithBiometric = !hasProcessedInitialAuthState

            updateSessionState(
                userId = currentUserId,
                shouldGateWithBiometric = shouldGateWithBiometric
            )

            hasProcessedInitialAuthState = true
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    fun onBiometricAuthenticated() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            unlockedUserId = currentUserId
        }

        _uiState.update {
            it.copy(
                isBiometricLocked = false,
                shouldRequestBiometric = false
            )
        }
    }

    private suspend fun updateSessionState(
        userId: String?,
        shouldGateWithBiometric: Boolean
    ) {
        if (userId == null) {
            unlockedUserId = null
            _uiState.value = MainUiState(
                startDestination = Screen.Login.route,
                isInitializing = false
            )
            return
        }

        val requiresBiometric = shouldGateWithBiometric &&
            unlockedUserId != userId &&
            localUserPreferences.isBiometricEnabled(userId)

        if (!requiresBiometric) {
            unlockedUserId = userId
        }

        _uiState.value = MainUiState(
            startDestination = Screen.Home.route,
            isInitializing = false,
            isBiometricLocked = requiresBiometric,
            shouldRequestBiometric = requiresBiometric
        )
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }
}
