package com.restaurandes.presentation.map

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val fineLocationPermission = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Centro por defecto: Uniandes
    val uniandes = LatLng(4.6019, -74.0661)
    val cameraPositionState = rememberCameraPositionState()

    var locationMessage by remember { mutableStateOf<String?>(null) }
    var isLocatingUser by remember { mutableStateOf(true) }
    var hasAdjustedCamera by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!fineLocationPermission.status.isGranted) {
            fineLocationPermission.launchPermissionRequest()
        }
    }

    // Obtiene ubicación del usuario, pero NO centra automáticamente ahí
    LaunchedEffect(fineLocationPermission.status.isGranted) {
        if (!fineLocationPermission.status.isGranted) {
            isLocatingUser = false
            locationMessage = "Mostrando restaurantes cerca de Uniandes."
            return@LaunchedEffect
        }

        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val location: Location? =
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()

            if (location == null) {
                locationMessage = "No pude obtener tu ubicación. Mostrando restaurantes cerca de Uniandes."
            }
        } catch (_: Exception) {
            locationMessage = "Error obteniendo ubicación. Mostrando restaurantes cerca de Uniandes."
        } finally {
            isLocatingUser = false
        }
    }

    // Ajusta la cámara a los restaurantes; si no hay, centra en Uniandes
    LaunchedEffect(uiState.restaurants, isLocatingUser) {
        if (hasAdjustedCamera || isLocatingUser) return@LaunchedEffect

        val validRestaurants = uiState.restaurants.filter {
            it.latitude != 0.0 && it.longitude != 0.0
        }

        if (validRestaurants.isNotEmpty()) {
            if (validRestaurants.size == 1) {
                val restaurant = validRestaurants.first()
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(
                        LatLng(restaurant.latitude, restaurant.longitude),
                        16f
                    )
                )
            } else {
                val boundsBuilder = LatLngBounds.Builder()

                validRestaurants.forEach { restaurant ->
                    boundsBuilder.include(LatLng(restaurant.latitude, restaurant.longitude))
                }

                val bounds = boundsBuilder.build()

                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngBounds(bounds, 140)
                )
            }
        } else {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(uniandes, 15f)
            )
        }

        hasAdjustedCamera = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map View") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = fineLocationPermission.status.isGranted
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = fineLocationPermission.status.isGranted,
                    zoomControlsEnabled = true,
                    compassEnabled = true,
                    mapToolbarEnabled = true
                )
            ) {
                uiState.restaurants.forEach { restaurant ->
                    if (restaurant.latitude != 0.0 && restaurant.longitude != 0.0) {
                        Marker(
                            state = MarkerState(
                                position = LatLng(restaurant.latitude, restaurant.longitude)
                            ),
                            title = restaurant.name,
                            snippet = "${restaurant.category} • ${restaurant.rating}",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                getMarkerColor(restaurant.category)
                            ),
                            onClick = {
                                onNavigateToDetail(restaurant.id)
                                false
                            }
                        )
                    }
                }

            }

            PriceFilterRow(
                selectedFilter = uiState.selectedPriceFilter,
                onFilterSelected = viewModel::setSelectedPriceFilter,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            when {
                uiState.isLoading || isLocatingUser -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null -> {
                    InfoBanner(
                        text = uiState.error ?: "",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    )
                }

                locationMessage != null -> {
                    InfoBanner(
                        text = locationMessage ?: "",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    )
                }
            }

            if (uiState.restaurants.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .widthIn(max = 420.dp),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${uiState.restaurants.size} restaurantes en el mapa",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Toca un marcador para abrir el detalle del restaurante.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private fun getMarkerColor(category: String): Float {
    return when (category.trim().lowercase()) {
        "americana" -> BitmapDescriptorFactory.HUE_RED
        "casero" -> BitmapDescriptorFactory.HUE_ORANGE
        "colombiana" -> BitmapDescriptorFactory.HUE_YELLOW
        "cafetería", "cafe", "café" -> BitmapDescriptorFactory.HUE_AZURE
        "postres" -> BitmapDescriptorFactory.HUE_VIOLET
        "italiana" -> BitmapDescriptorFactory.HUE_GREEN
        "mexicana" -> BitmapDescriptorFactory.HUE_ROSE
        "asiática", "asiatica", "japonesa" -> BitmapDescriptorFactory.HUE_CYAN
        else -> BitmapDescriptorFactory.HUE_MAGENTA
    }
}

@Composable
private fun PriceFilterRow(
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val prices = listOf("$", "$$", "$$$")

    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(24.dp)
            )
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = "Precio:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        prices.forEach { price ->
            FilterChip(
                selected = selectedFilter == price,
                onClick = { onFilterSelected(price) },
                label = { Text(price) }
            )
        }
    }
}

@Composable
private fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}