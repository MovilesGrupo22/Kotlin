package com.restaurandes.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.restaurandes.domain.model.Restaurant

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

    LaunchedEffect(primaryRestaurantId, secondaryRestaurantId) {
        viewModel.loadRestaurants(primaryRestaurantId, secondaryRestaurantId)
    }

    val showSummary = uiState.primaryRestaurant != null &&
            uiState.secondaryRestaurant != null &&
            !uiState.showRestaurantPicker

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare restaurants", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (showSummary) {
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

            showSummary -> {
                ComparisonSummaryContent(
                    primary = uiState.primaryRestaurant!!,
                    secondary = uiState.secondaryRestaurant!!,
                    paddingValues = paddingValues
                )
            }

            uiState.primaryRestaurant != null -> {
                val searchQuery = uiState.searchQuery
                val filtered = remember(uiState.availableRestaurants, searchQuery) {
                    if (searchQuery.isBlank()) {
                        uiState.availableRestaurants
                    } else {
                        val q = searchQuery.lowercase()
                        uiState.availableRestaurants.filter { r ->
                            r.name.lowercase().contains(q) ||
                                    r.category.lowercase().contains(q) ||
                                    r.tags.any { it.lowercase().contains(q) }
                        }
                    }
                }
                SelectionContent(
                    primary = uiState.primaryRestaurant!!,
                    selected = uiState.secondaryRestaurant,
                    availableRestaurants = filtered,
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onSelectRestaurant = viewModel::selectSecondaryRestaurant,
                    paddingValues = paddingValues
                )
            }
        }
    }
}

@Composable
private fun SelectionContent(
    primary: ComparableRestaurant,
    selected: ComparableRestaurant?,
    availableRestaurants: List<Restaurant>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectRestaurant: (Restaurant) -> Unit,
    paddingValues: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Choose another restaurant to compare with ${primary.restaurant.name}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RestaurantPickerCard(
                    label = "Current",
                    restaurant = primary.restaurant,
                    modifier = Modifier.weight(1f)
                )
                RestaurantPickerCard(
                    label = "Selected",
                    restaurant = selected?.restaurant,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name, category or tag") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
        }

        item {
            Text(
                text = "Available restaurants",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(availableRestaurants) { restaurant ->
            val isSelected = selected?.restaurant?.id == restaurant.id
            RestaurantListItem(
                restaurant = restaurant,
                isSelected = isSelected,
                onClick = { onSelectRestaurant(restaurant) }
            )
            HorizontalDivider()
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun RestaurantPickerCard(
    label: String,
    restaurant: Restaurant?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (restaurant != null) {
                Column {
                    AsyncImage(
                        model = restaurant.imageUrl,
                        contentDescription = restaurant.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentScale = ContentScale.Crop,
                        error = ColorPainter(Color.Gray),
                        placeholder = ColorPainter(Color.LightGray)
                    )
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = restaurant.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${restaurant.category} • ${restaurant.priceRange}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = if (restaurant.isCurrentlyOpen())
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = if (restaurant.isCurrentlyOpen()) "Open" else "Closed",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (restaurant.isCurrentlyOpen())
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Not selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RestaurantListItem(
    restaurant: Restaurant,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = restaurant.imageUrl,
            contentDescription = restaurant.name,
            modifier = Modifier
                .size(60.dp)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop,
            error = ColorPainter(Color.Gray),
            placeholder = ColorPainter(Color.LightGray)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = restaurant.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${restaurant.category} • ${restaurant.priceRange} • ★ ${String.format("%.1f", restaurant.rating)} • ${restaurant.reviewCount} reviews",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ComparisonSummaryContent(
    primary: ComparableRestaurant,
    secondary: ComparableRestaurant,
    paddingValues: PaddingValues
) {
    val p = primary.restaurant
    val s = secondary.restaurant

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Comparison summary",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            ComparisonRow(
                label = "Category",
                primaryValue = p.category,
                secondaryValue = s.category
            )
        }

        item {
            ComparisonRow(
                label = "Price range",
                primaryValue = p.priceRange,
                secondaryValue = s.priceRange
            )
        }

        item {
            ComparisonRow(
                label = "Rating",
                primaryValue = "${String.format("%.1f", p.rating)} ★",
                secondaryValue = "${String.format("%.1f", s.rating)} ★",
                primaryWins = p.rating >= s.rating,
                secondaryWins = s.rating > p.rating
            )
        }

        item {
            ComparisonRow(
                label = "Reviews",
                primaryValue = p.reviewCount.toString(),
                secondaryValue = s.reviewCount.toString()
            )
        }

        item {
            ComparisonRow(
                label = "Open now",
                primaryValue = if (p.isCurrentlyOpen()) "Yes" else "No",
                secondaryValue = if (s.isCurrentlyOpen()) "Yes" else "No"
            )
        }

        item {
            ComparisonRow(
                label = "Opening hours",
                primaryValue = p.openingHours,
                secondaryValue = s.openingHours
            )
        }

        item {
            ComparisonRow(
                label = "Address",
                primaryValue = p.address,
                secondaryValue = s.address
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    primaryValue: String,
    secondaryValue: String,
    primaryWins: Boolean = false,
    secondaryWins: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ComparisonValueChip(
                    value = primaryValue,
                    highlight = primaryWins,
                    modifier = Modifier.weight(1f)
                )
                ComparisonValueChip(
                    value = secondaryValue,
                    highlight = secondaryWins,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ComparisonValueChip(
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

