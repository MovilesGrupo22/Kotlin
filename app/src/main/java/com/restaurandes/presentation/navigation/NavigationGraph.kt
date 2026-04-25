package com.restaurandes.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.restaurandes.presentation.analytics.AnalyticsScreen
import com.restaurandes.presentation.auth.LoginScreen
import com.restaurandes.presentation.auth.RegisterScreen
import com.restaurandes.presentation.detail.RestaurantComparisonScreen
import com.restaurandes.presentation.detail.RestaurantDetailScreen
import com.restaurandes.presentation.detail.RestaurantReviewsScreen
import com.restaurandes.presentation.favorites.FavoritesScreen
import com.restaurandes.presentation.home.HomeScreen
import com.restaurandes.presentation.map.MapScreen
import com.restaurandes.presentation.profile.ProfileScreen
import com.restaurandes.presentation.search.SearchScreen

typealias BiometricQuickAccessHandler = ((() -> Unit) -> Unit)

@Composable
fun NavigationGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
    showBiometricQuickAccess: Boolean = false,
    onBiometricQuickAccess: BiometricQuickAccessHandler? = null
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                showBiometricQuickAccess = false,
                onBiometricQuickAccess = null
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetail = { restaurantId ->
                    navController.navigate(Screen.RestaurantDetail.createRoute(restaurantId))
                },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(
            route = Screen.RestaurantDetail.route,
            arguments = listOf(navArgument("restaurantId") { type = NavType.StringType })
        ) { backStackEntry ->
            val restaurantId = backStackEntry.arguments?.getString("restaurantId") ?: return@composable
            RestaurantDetailScreen(
                restaurantId = restaurantId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReviews = {
                    navController.navigate(Screen.RestaurantReviews.createRoute(restaurantId))
                },
                onNavigateToCompare = {
                    navController.navigate(Screen.RestaurantComparison.createRoute(restaurantId))
                }
            )
        }

        composable(
            route = Screen.RestaurantReviews.route,
            arguments = listOf(navArgument("restaurantId") { type = NavType.StringType })
        ) { backStackEntry ->
            val restaurantId = backStackEntry.arguments?.getString("restaurantId") ?: return@composable
            RestaurantReviewsScreen(
                restaurantId = restaurantId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RestaurantComparison.route,
            arguments = listOf(
                navArgument("primaryRestaurantId") { type = NavType.StringType },
                navArgument("secondaryRestaurantId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val primaryRestaurantId = backStackEntry.arguments?.getString("primaryRestaurantId")
                ?: return@composable
            val secondaryRestaurantId = backStackEntry.arguments?.getString("secondaryRestaurantId")
            RestaurantComparisonScreen(
                primaryRestaurantId = primaryRestaurantId,
                secondaryRestaurantId = secondaryRestaurantId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { restaurantId ->
                    navController.navigate(Screen.RestaurantDetail.createRoute(restaurantId))
                }
            )
        }

        composable(Screen.Map.route) {
            MapScreen(
                onNavigateToDetail = { restaurantId ->
                    navController.navigate(Screen.RestaurantDetail.createRoute(restaurantId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateToDetail = { restaurantId ->
                    navController.navigate(Screen.RestaurantDetail.createRoute(restaurantId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateToDetail = { restaurantId ->
                    navController.navigate(Screen.RestaurantDetail.createRoute(restaurantId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
