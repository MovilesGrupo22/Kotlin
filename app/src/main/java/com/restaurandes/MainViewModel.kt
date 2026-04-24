package com.restaurandes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.restaurandes.data.local.LinkedBiometricAccount
import com.restaurandes.data.local.LocalUserPreferences
import com.restaurandes.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val startDestination: String? = null,
    val isInitializing: Boolean = true,
    val linkedBiometricAccount: LinkedBiometricAccount? = null,
    val shouldShowBiometricQuickAccess: Boolean = false,
    val canUnlockLinkedAccount: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val localUserPreferences: LocalUserPreferences
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        viewModelScope.launch {
            updateSessionState(firebaseAuth.currentUser?.uid)
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    private suspend fun updateSessionState(currentUserId: String?) {
        val linkedAccount = localUserPreferences.getLinkedBiometricAccount()
        val canUnlockLinkedAccount = currentUserId != null &&
            linkedAccount?.userId == currentUserId
        val shouldShowBiometricQuickAccess = linkedAccount != null

        _uiState.value = MainUiState(
            startDestination = when {
                canUnlockLinkedAccount -> Screen.Login.route
                currentUserId != null -> Screen.Home.route
                else -> Screen.Login.route
            },
            isInitializing = false,
            linkedBiometricAccount = linkedAccount,
            shouldShowBiometricQuickAccess = shouldShowBiometricQuickAccess,
            canUnlockLinkedAccount = canUnlockLinkedAccount
        )
    }

    fun clearLinkedBiometricAccount() {
        viewModelScope.launch {
            localUserPreferences.clearLinkedBiometricAccount()
            updateSessionState(auth.currentUser?.uid)
        }
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }
}
