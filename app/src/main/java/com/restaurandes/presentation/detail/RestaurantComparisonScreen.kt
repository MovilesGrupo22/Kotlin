package com.restaurandes.presentation.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantComparisonScreen(
    primaryRestaurantId: String,
    secondaryRestaurantId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: RestaurantComparisonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(primaryRestaurantId, secondaryRestaurantId) {
        viewModel.loadRestaurants(primaryRestaurantId, secondaryRestaurantId)
    }

    if (uiState.showRestaurantPicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideRestaurantPicker() },
            sheetState = sheetState
        ) {
            Text(
                text = "Elige un restaurante para comparar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(16.dp)
            )

            val suggested = uiState.suggestedRestaurant
            if (suggested != null) {
                Text(
                    text = "Sugerido para ti",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { viewModel.selectSecondaryRestaurant(suggested) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = suggested.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${suggested.category} · ${suggested.priceRange} · ${String.format("%.1f", suggested.rating)}★",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
            }

            LazyColumn {
                items(uiState.availableRestaurants.filter { it.id != suggested?.id }) { restaurant ->
                    ListItem(
                        headlineContent = { Text(restaurant.name) },
                        supportingContent = {
                            Text("${restaurant.category} • ${restaurant.priceRange} • ${String.format("%.1f", restaurant.rating)}★")
                        },
                        modifier = Modifier.clickable { viewModel.selectSecondaryRestaurant(restaurant) }
                    )
                    HorizontalDivider()
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comparar", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.primaryRestaurant != null) {
                        IconButton(onClick = { viewModel.showRestaurantPicker() }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Cambiar")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            uiState.primaryRestaurant != null && uiState.secondaryRestaurant != null -> {
                ComparisonContent(
                    primaryRestaurant = uiState.primaryRestaurant!!,
                    secondaryRestaurant = uiState.secondaryRestaurant!!,
                    paddingValues = paddingValues,
                    onNavigateToDetail = onNavigateToDetail
                )
            }

            uiState.primaryRestaurant != null && uiState.secondaryRestaurant == null && !uiState.showRestaurantPicker -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Selecciona un restaurante para comparar",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = { viewModel.showRestaurantPicker() }) {
                            Text("Elegir restaurante")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonContent(
    primaryRestaurant: ComparableRestaurant,
    secondaryRestaurant: ComparableRestaurant,
    paddingValues: PaddingValues,
    onNavigateToDetail: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        Text(
            text = "Comparando 2 restaurantes",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ComparisonCard(
                comparableRestaurant = primaryRestaurant,
                otherRestaurant = secondaryRestaurant,
                modifier = Modifier.weight(1f),
                onNavigateToDetail = onNavigateToDetail
            )
            ComparisonCard(
                comparableRestaurant = secondaryRestaurant,
                otherRestaurant = primaryRestaurant,
                modifier = Modifier.weight(1f),
                onNavigateToDetail = onNavigateToDetail
            )
        }
    }
}

@Composable
private fun ComparisonCard(
    comparableRestaurant: ComparableRestaurant,
    otherRestaurant: ComparableRestaurant,
    modifier: Modifier = Modifier,
    onNavigateToDetail: (String) -> Unit
) {
    val restaurant = comparableRestaurant.restaurant
    val winsRating = restaurant.rating >= otherRestaurant.restaurant.rating
    val winsPrice = restaurant.priceRange.length <= otherRestaurant.restaurant.priceRange.length
    val winsDistance = comparableRestaurant.distanceKm <= otherRestaurant.distanceKm

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = restaurant.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = restaurant.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            ComparisonMetricRow(
                icon = {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                },
                value = String.format("%.1f", restaurant.rating),
                highlight = winsRating
            )
            ComparisonMetricRow(
                icon = {
                    Text(text = restaurant.priceRange, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                },
                value = "Precio",
                highlight = winsPrice
            )
            ComparisonMetricRow(
                icon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                },
                value = String.format("%.1f km", comparableRestaurant.distanceKm),
                highlight = winsDistance
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onNavigateToDetail(restaurant.id) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Detalles", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ComparisonMetricRow(
    icon: @Composable () -> Unit,
    value: String,
    highlight: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
            icon()
        }
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (highlight) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
