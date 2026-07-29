package il.arik.nadlantracker.feature.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import il.arik.nadlantracker.R
import il.arik.nadlantracker.core.ui.AppPrefs
import il.arik.nadlantracker.feature.compare.CompareScreen
import il.arik.nadlantracker.feature.home.HomeScreen
import il.arik.nadlantracker.feature.macro.MacroScreen
import il.arik.nadlantracker.feature.onboarding.OnboardingScreen
import il.arik.nadlantracker.feature.results.ResultsScreen
import il.arik.nadlantracker.feature.search.SearchScreen
import il.arik.nadlantracker.feature.settings.SettingsScreen
import kotlin.reflect.KClass

private data class TopDest(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    val labelRes: Int,
)

private val topDestinations = listOf(
    TopDest(NavRoutes.Home, NavRoutes.Home::class, Icons.Filled.Bookmark, R.string.nav_home),
    TopDest(NavRoutes.Search, NavRoutes.Search::class, Icons.Filled.Search, R.string.nav_search),
    TopDest(NavRoutes.Macro, NavRoutes.Macro::class, Icons.Filled.ShowChart, R.string.nav_macro),
    TopDest(NavRoutes.Compare, NavRoutes.Compare::class, Icons.AutoMirrored.Filled.CompareArrows, R.string.nav_compare),
)

/** Bottom padding top-level screens add so scrolling content clears the floating pill. */
val BottomNavClearance = 96.dp

@Composable
fun NadlanNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showNav = topDestinations.any { currentDestination?.hasRoute(it.routeClass) == true }

    val context = androidx.compose.ui.platform.LocalContext.current
    val start: Any = remember(context) {
        if (AppPrefs.isOnboarded(context)) NavRoutes.Home else NavRoutes.Onboarding
    }

    // The Surface paints the themed background across the whole window, including
    // behind the system bars — without it the platform windowBackground showed
    // through and LocalContentColor fell back to its non-themed default.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            Modifier
                .fillMaxSize()
                // Keep content and the floating pill clear of the status bar, the
                // system navigation bar and any display cutout. IME is deliberately
                // excluded so the keyboard may cover the pill rather than lift it.
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)),
        ) {
            NavHost(
                navController = navController,
                startDestination = start,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable<NavRoutes.Onboarding> {
                    OnboardingScreen(
                        onSeedResolved = { queryJson ->
                            navController.navigate(NavRoutes.Home) {
                                popUpTo<NavRoutes.Onboarding> { inclusive = true }
                            }
                            navController.navigate(NavRoutes.Results(queryJson, forceRefresh = true))
                        },
                        onSearch = {
                            navController.navigate(NavRoutes.Home) {
                                popUpTo<NavRoutes.Onboarding> { inclusive = true }
                            }
                            navController.navigateTop(NavRoutes.Search)
                        },
                    )
                }
                composable<NavRoutes.Home> {
                    HomeScreen(
                        onRunFavorite = { queryJson, favoriteId ->
                            navController.navigate(NavRoutes.Results(queryJson, forceRefresh = true, favoriteId = favoriteId))
                        },
                        onSearch = { navController.navigateTop(NavRoutes.Search) },
                        onSettings = { navController.navigate(NavRoutes.Settings) },
                    )
                }
                composable<NavRoutes.Search> {
                    SearchScreen(
                        onShowResults = { queryJson -> navController.navigate(NavRoutes.Results(queryJson)) },
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
                composable<NavRoutes.Compare> { CompareScreen() }
                composable<NavRoutes.Macro> { MacroScreen() }
                composable<NavRoutes.Settings> {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
            }

            if (showNav) {
                FloatingPillNav(
                    destinations = topDestinations,
                    currentDestination = currentDestination,
                    onSelect = { navController.navigateTop(it) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                )
            }
        }
    }
}

/**
 * Switch bottom-nav tabs, keeping Home as the single root.
 *
 * Pops to Home explicitly rather than to `graph.startDestinationId`: on a first
 * run the graph starts at Onboarding, which onboarding itself pops inclusively,
 * so popping to the start destination would find nothing on the back stack and
 * silently do nothing — tabs would stack up and system-back would walk through
 * every tab visited instead of returning Home.
 */
private fun NavHostController.navigateTop(route: Any) {
    navigate(route) {
        popUpTo<NavRoutes.Home> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun FloatingPillNav(
    destinations: List<TopDest>,
    currentDestination: NavDestination?,
    onSelect: (Any) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(64.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 12.dp,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { dest ->
                val selected = currentDestination?.hasRoute(dest.routeClass) == true
                PillNavItem(dest, selected) { onSelect(dest.route) }
            }
        }
    }
}

@Composable
private fun RowScope.PillNavItem(dest: TopDest, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
        label = "navBg",
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.inverseOnSurface,
        label = "navFg",
    )
    val labelWidth by animateDpAsState(if (selected) 70.dp else 0.dp, label = "navLabel")

    Surface(
        color = bg,
        shape = CircleShape,
        modifier = Modifier.weight(1f).height(48.dp).clickable(onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(dest.icon, contentDescription = stringResource(dest.labelRes), tint = fg)
            Text(
                text = if (labelWidth > 0.dp) "  " + stringResource(dest.labelRes) else "",
                color = fg,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.widthIn(max = labelWidth),
            )
        }
    }
}
