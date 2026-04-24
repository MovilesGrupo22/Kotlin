package com.restaurandes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
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

                    LaunchedEffect(uiState.shouldRequestBiometric) {
                        if (uiState.shouldRequestBiometric) {
                            if (biometricAuthManager.isBiometricAvailable()) {
                                biometricAuthManager.authenticate(
                                    title = "Desbloqueo biometrico",
                                    subtitle = "Accede a Restaurandes",
                                    description = "Usa tu huella, rostro o PIN del dispositivo",
                                    onSuccess = {
                                        mainViewModel.onBiometricAuthenticated()
                                    },
                                    onError = { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            error,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        FirebaseAuth.getInstance().signOut()
                                    }
                                )
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "La proteccion biometrica no esta disponible en este dispositivo.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                FirebaseAuth.getInstance().signOut()
                            }
                        }
                    }

                    when {
                        uiState.isInitializing -> LoadingGate()
                        uiState.isBiometricLocked -> BiometricGate()
                        else -> NavigationGraph(
                            navController = navController,
                            startDestination = uiState.startDestination ?: Screen.Login.route
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

@Composable
private fun BiometricGate() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Verificando acceso biometrico...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
