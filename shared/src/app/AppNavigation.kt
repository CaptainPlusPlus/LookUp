package app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import day.domain.LocationRepository
import day.presentation.DaySkyScreenRoot
import org.koin.compose.koinInject
import welcome.presentation.WelcomeScreenRoot

sealed class RootRoute(val route: String) {
    object Welcome : RootRoute("welcome")
    object Main : RootRoute("main")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val locationRepository: LocationRepository = koinInject()
    
    // Flush database on startup for testing as requested
    LaunchedEffect(Unit) {
        locationRepository.clearLocation()
    }
    
    NavHost(
        navController = navController,
        startDestination = RootRoute.Welcome.route
    ) {
        composable(RootRoute.Welcome.route) {
            WelcomeScreenRoot(
                onLocationObtained = {
                    navController.navigate(RootRoute.Main.route) {
                        popUpTo(RootRoute.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        composable(RootRoute.Main.route) {
            DaySkyScreenRoot(
                onNavigateToWelcome = {
                    navController.navigate(RootRoute.Welcome.route) {
                        popUpTo(RootRoute.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
