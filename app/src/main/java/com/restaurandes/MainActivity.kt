package com.restaurandes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.restaurandes.presentation.navigation.NavigationGraph
import com.restaurandes.presentation.navigation.Screen
import com.restaurandes.security.BiometricAuthManager
import com.restaurandes.ui.theme.RestaurandesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var biometricAuthManager: BiometricAuthManager

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

                    var startDestination by remember {
                        mutableStateOf(
                            if (FirebaseAuth.getInstance().currentUser != null) {
                                Screen.Home.route
                            } else {
                                Screen.Login.route
                            }
                        )
                    }

                    var pendingBiometricUnlock by remember {
                        mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
                    }

                    LaunchedEffect(pendingBiometricUnlock) {
                        if (pendingBiometricUnlock) {
                            if (biometricAuthManager.isBiometricAvailable()) {
                                biometricAuthManager.authenticate(
                                    title = "Desbloqueo biométrico",
                                    subtitle = "Accede a Restaurandes",
                                    description = "Usa tu huella, rostro o PIN del dispositivo",
                                    onSuccess = {
                                        pendingBiometricUnlock = false
                                    },
                                    onError = { error ->
                                        Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                        FirebaseAuth.getInstance().signOut()
                                        startDestination = Screen.Login.route
                                        pendingBiometricUnlock = false
                                    }
                                )
                            } else {
                                pendingBiometricUnlock = false
                            }
                        }
                    }

                    NavigationGraph(
                        navController = navController,
                        startDestination = startDestination,
                        onBiometricLoginRequired = { onSuccess ->
                            if (biometricAuthManager.isBiometricAvailable()) {
                                biometricAuthManager.authenticate(
                                    title = "Verificación biométrica",
                                    subtitle = "Confirma tu identidad",
                                    description = "Usa tu huella, rostro o PIN del dispositivo",
                                    onSuccess = onSuccess,
                                    onError = { error ->
                                        Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                onSuccess()
                            }
                        }
                    )
                }
            }
        }
    }
}