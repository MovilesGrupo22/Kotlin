package com.restaurandes.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restaurandes.data.analytics.AnalyticsService
import com.restaurandes.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val analyticsService: AnalyticsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        // Limpiar espacios en blanco
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = password.trim()
        
        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Email y contraseña son requeridos")
            return
        }

        // Validar formato de email
        if (!isValidEmail(cleanEmail)) {
            _uiState.value = _uiState.value.copy(error = "El email debe tener formato válido (ejemplo: usuario@correo.com)")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = userRepository.signIn(cleanEmail, cleanPassword)
                result.fold(
                    onSuccess = { user ->
                        // Analytics handled by repository
                        _uiState.value = LoginUiState(isSuccess = true)
                    },
                    onFailure = { error ->
                        val errorMessage = when {
                            error.message?.contains("password is invalid", ignoreCase = true) == true -> 
                                "Contraseña incorrecta"
                            error.message?.contains("no user record", ignoreCase = true) == true -> 
                                "No existe una cuenta con este email"
                            error.message?.contains("badly formatted", ignoreCase = true) == true -> 
                                "Email inválido"
                            error.message?.contains("network", ignoreCase = true) == true -> 
                                "Error de conexión. Verifica tu internet."
                            else -> error.message ?: "Error al iniciar sesión"
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error inesperado"
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = userRepository.signInWithGoogle(idToken)
                result.fold(
                    onSuccess = { _uiState.value = LoginUiState(isSuccess = true) },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Error al iniciar sesión con Google"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error inesperado"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailPattern.matches(email)
    }
}
