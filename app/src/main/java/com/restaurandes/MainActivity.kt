package com.restaurandes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.restaurandes.presentation.navigation.NavigationGraph
import com.restaurandes.presentation.navigation.Screen
import com.restaurandes.security.BiometricAuthManager
import com.restaurandes.ui.theme.RestaurandesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private lateinit var biometricAuthManager: BiometricAuthManager
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        biometricAuthManager = BiometricAuthManager(this)

        enableEdgeToEdge()
        setContent {
            RestaurandesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val uiState by mainViewModel.uiState.collectAsState()

                    if (uiState.isInitializing) {
                        LoadingGate()
                    } else {
                        NavigationGraph(
                            navController = navController,
                            startDestination = uiState.startDestination ?: Screen.Login.route,
                            showBiometricQuickAccess = uiState.shouldShowBiometricQuickAccess,
                            onBiometricQuickAccess = { onSuccess ->
                                if (uiState.linkedBiometricAccount == null) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Activa el acceso biometrico desde el perfil para vincular una cuenta.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (!uiState.canUnlockLinkedAccount) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Haz login manual con la cuenta vinculada una vez para reactivar el acceso biometrico.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (biometricAuthManager.isBiometricAvailable()) {
                                    biometricAuthManager.authenticate(
                                        title = "Ingreso biometrico",
                                        subtitle = "Accede a Restaurandes",
                                        description = "Usa tu huella, rostro o PIN del dispositivo",
                                        onSuccess = onSuccess,
                                        onError = { error ->
                                            Toast.makeText(
                                                this@MainActivity,
                                                error,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "La proteccion biometrica no esta disponible en este dispositivo.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingGate() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
