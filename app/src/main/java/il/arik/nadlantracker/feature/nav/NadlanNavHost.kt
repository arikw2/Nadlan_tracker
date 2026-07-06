package il.arik.nadlantracker.feature.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import il.arik.nadlantracker.R
import il.arik.nadlantracker.feature.favorites.FavoritesScreen
import il.arik.nadlantracker.feature.macro.MacroScreen
import il.arik.nadlantracker.feature.results.ResultsScreen
import il.arik.nadlantracker.feature.search.SearchScreen
import il.arik.nadlantracker.feature.settings.SettingsScreen
import kotlin.reflect.KClass

private data class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    val labelRes: Int,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(NavRoutes.Search, NavRoutes.Search::class, Icons.Filled.Search, R.string.nav_search),
    TopLevelDestination(NavRoutes.Favorites, NavRoutes.Favorites::class, Icons.Filled.Star, R.string.nav_favorites),
    TopLevelDestination(NavRoutes.Macro, NavRoutes.Macro::class, Icons.Filled.BarChart, R.string.nav_macro),
    TopLevelDestination(NavRoutes.Settings, NavRoutes.Settings::class, Icons.Filled.Settings, R.string.nav_settings),
)

@Composable
fun NadlanNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    val selected = currentDestination
                        ?.hasRoute(destination.routeClass) == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Search,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<NavRoutes.Search> {
                SearchScreen(
                    onShowResults = { queryJson ->
                        navController.navigate(NavRoutes.Results(queryJson))
                    },
                )
            }
            composable<NavRoutes.Results> { entry ->
                val route = entry.toRoute<NavRoutes.Results>()
                ResultsScreen(
                    queryJson = route.queryJson,
                    forceRefresh = route.forceRefresh,
                    favoriteId = route.favoriteId.takeIf { it >= 0 },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<NavRoutes.Favorites> {
                FavoritesScreen(
                    onRunFavorite = { queryJson, favoriteId ->
                        navController.navigate(
                            NavRoutes.Results(queryJson, forceRefresh = true, favoriteId = favoriteId)
                        )
                    },
                )
            }
            composable<NavRoutes.Macro> { MacroScreen() }
            composable<NavRoutes.Settings> { SettingsScreen() }
        }
    }
}
