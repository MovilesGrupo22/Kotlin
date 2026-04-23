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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
            val verdict = computeSmartVerdict(primary, secondary)
            SmartVerdictCard(
                primaryName = p.name,
                secondaryName = s.name,
                verdict = verdict
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

data class SmartVerdict(
    val primaryScore: Int,
    val secondaryScore: Int,
    val primaryWins: Boolean,
    val description: String,
    val winnerName: String,
    val reasons: List<Pair<String, String>>
)

private fun priceScore(priceRange: String): Double {
    return when (priceRange.count { it == '$' }) {
        1 -> 25.0
        2 -> 17.0
        3 -> 8.0
        else -> 12.0
    }
}

private fun computeSmartVerdict(
    primary: ComparableRestaurant,
    secondary: ComparableRestaurant
): SmartVerdict {
    val p = primary.restaurant
    val s = secondary.restaurant

    fun score(r: com.restaurandes.domain.model.Restaurant, distKm: Double): Int {
        val rating = (r.rating / 5.0) * 40
        val price = priceScore(r.priceRange)
        val reviews = minOf(15.0, r.reviewCount * 1.5)
        val open = if (r.isCurrentlyOpen()) 10.0 else 0.0
        val distance = maxOf(0.0, 10.0 - distKm * 2.0)
        return (rating + price + reviews + open + distance).toInt().coerceIn(0, 100)
    }

    val pScore = score(p, primary.distanceKm)
    val sScore = score(s, secondary.distanceKm)
    val primaryWins = pScore >= sScore
    val winner = if (primaryWins) p else s
    val winnerScore = if (primaryWins) pScore else sScore
    val loserScore = if (primaryWins) sScore else pScore
    val diff = winnerScore - loserScore

    val reasons = mutableListOf<Pair<String, String>>()

    if (kotlin.math.abs(p.rating - s.rating) > 0.09) {
        val betterRated = if (p.rating > s.rating) p else s
        if (betterRated.name == winner.name) {
            reasons += "⭐" to "Better rated  ${String.format("%.1f", betterRated.rating)} vs ${String.format("%.1f", if (betterRated == p) s.rating else p.rating)}"
        }
    }

    if (p.priceRange != s.priceRange) {
        val cheaperWins = priceScore(p.priceRange) > priceScore(s.priceRange)
        val betterValue = if (cheaperWins) p else s
        if (betterValue.name == winner.name) {
            reasons += "💰" to "Better value  (${betterValue.priceRange} vs ${if (betterValue == p) s.priceRange else p.priceRange})"
        }
    }

    if (kotlin.math.abs(primary.distanceKm - secondary.distanceKm) > 0.1) {
        val closer = if (primary.distanceKm < secondary.distanceKm) primary else secondary
        if (closer.restaurant.name == winner.name) {
            reasons += "📍" to "Closer  (${String.format("%.1f", closer.distanceKm)} km vs ${String.format("%.1f", if (closer == primary) secondary.distanceKm else primary.distanceKm)} km)"
        }
    }

    if (p.reviewCount != s.reviewCount) {
        val moreReviewed = if (p.reviewCount > s.reviewCount) p else s
        if (moreReviewed.name == winner.name) {
            reasons += "📝" to "More reviews  (${moreReviewed.reviewCount} vs ${if (moreReviewed == p) s.reviewCount else p.reviewCount})"
        }
    }

    if (p.isCurrentlyOpen() != s.isCurrentlyOpen() && winner.isCurrentlyOpen()) {
        reasons += "🕐" to "Currently open"
    }

    val topReason = reasons.firstOrNull()?.second?.substringBefore("  ") ?: "overall score"
    val description = when {
        diff <= 4 -> "${winner.name} edges ahead by a thin margin. Both restaurants are solid choices, but ${winner.name} has a slight advantage in $topReason."
        diff <= 14 -> "${winner.name} has a clear advantage. It stands out mainly for $topReason."
        else -> "${winner.name} is the clear winner here, particularly due to $topReason."
    }

    return SmartVerdict(
        primaryScore = pScore,
        secondaryScore = sScore,
        primaryWins = primaryWins,
        description = description,
        winnerName = winner.name,
        reasons = reasons
    )
}

@Composable
private fun SmartVerdictCard(
    primaryName: String,
    secondaryName: String,
    verdict: SmartVerdict
) {
    val primary = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "✦", color = primary, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Smart Verdict",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primary
                )
            }

            ScoreBar(
                name = primaryName,
                score = verdict.primaryScore,
                isWinner = verdict.primaryWins,
                barColor = if (verdict.primaryWins) primary else MaterialTheme.colorScheme.outline
            )

            ScoreBar(
                name = secondaryName,
                score = verdict.secondaryScore,
                isWinner = !verdict.primaryWins,
                barColor = if (!verdict.primaryWins) primary else MaterialTheme.colorScheme.outline
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    text = verdict.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (verdict.reasons.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = verdict.winnerName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = primary
                        )
                        verdict.reasons.forEach { (emoji, text) ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = emoji, style = MaterialTheme.typography.bodySmall)
                                Text(text = text, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBar(
    name: String,
    score: Int,
    isWinner: Boolean,
    barColor: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (isWinner) {
                Text(
                    text = "🏆 $score/100",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "$score/100",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(score / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
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

