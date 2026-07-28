package il.arik.nadlantracker.feature.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import il.arik.nadlantracker.R
import il.arik.nadlantracker.core.ui.Formatters
import il.arik.nadlantracker.core.ui.components.Sparkline
import il.arik.nadlantracker.core.ui.theme.Terracotta300
import il.arik.nadlantracker.feature.nav.BottomNavClearance

@Composable
fun HomeScreen(
    onRunFavorite: (queryJson: String, favoriteId: Long) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    // Enabling the first alert needs POST_NOTIFICATIONS on Android 13+.
    var pendingAlertId by remember { mutableStateOf<Long?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingAlertId?.let { id -> if (granted) viewModel.setAlertsEnabled(id, true) }
        pendingAlertId = null
    }

    fun toggleAlerts(area: AreaSummary) {
        when {
            area.alertsEnabled -> viewModel.setAlertsEnabled(area.id, false)
            Build.VERSION.SDK_INT >= 33 -> {
                pendingAlertId = area.id
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> viewModel.setAlertsEnabled(area.id, true)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = BottomNavClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineSmall)
                    val newestRun = state.areas.mapNotNull { it.lastRunAtEpochMs }.maxOrNull()
                    Text(
                        listOfNotNull(
                            stringResource(R.string.home_subtitle, state.areas.size),
                            newestRun?.let { stringResource(R.string.favorites_last_run, formatDay(it)) },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RoundIconButton(Icons.Filled.Tune, stringResource(R.string.home_settings), onSettings)
            }
        }

        item {
            when {
                state.indexLoading -> IndexHeroPlaceholder()
                state.index != null -> IndexHeroCard(
                    block = state.index!!,
                    rangeYears = state.heroRangeYears,
                    onRange = viewModel::setHeroRange,
                )
            }
        }

        if (state.areas.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(stringResource(R.string.home_tracked), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.home_tracked_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.areas, key = { it.id }) { area ->
                AreaCard(
                    area = area,
                    onClick = { if (!area.broken) onRunFavorite(area.queryJson, area.id) },
                    onToggleAlerts = { toggleAlerts(area) },
                    onDelete = { viewModel.delete(area.id) },
                )
            }
        } else if (!state.areasLoading) {
            item { EmptyAreas(onSearch) }
        }
    }
}

private fun formatDay(epochMs: Long): String = Formatters.date(
    java.time.Instant.ofEpochMilli(epochMs)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate(),
)

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .size(42.dp)
            .clickable(onClickLabel = contentDescription, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun IndexHeroCard(block: IndexBlock, rangeYears: Int, onRange: (Int) -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val onInk = MaterialTheme.colorScheme.inverseOnSurface
        Column(Modifier.padding(18.dp)) {
            Text(
                stringResource(R.string.home_index_card),
                style = MaterialTheme.typography.labelMedium,
                color = onInk.copy(alpha = 0.7f),
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    Formatters.number(block.latestValue),
                    style = MaterialTheme.typography.displaySmall,
                    color = onInk,
                )
                block.yoy?.let {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 6.dp),
                    ) {
                        Text(
                            stringResource(R.string.home_index_yoy, Formatters.percent(it)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Text(
                buildString {
                    append(Formatters.shortPeriod(block.period))
                    block.rentYoy?.let { append(" · ").append(stringResource(R.string.home_index_rent, Formatters.percent(it))) }
                },
                style = MaterialTheme.typography.bodySmall,
                color = onInk.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
            )
            val windowed = block.dwelling.points.takeLast(rangeYears * 12).map { it.value }
            Sparkline(
                values = windowed,
                color = Terracotta300,
                modifier = Modifier.fillMaxWidth().height(70.dp).padding(top = 12.dp),
                strokeWidthDp = 2.5f,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                HeroRangePill(stringResource(R.string.range_1y), rangeYears == 1, onInk) { onRange(1) }
                HeroRangePill(stringResource(R.string.range_3y), rangeYears == 3, onInk) { onRange(3) }
                HeroRangePill(stringResource(R.string.range_5y), rangeYears == 5, onInk) { onRange(5) }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeroRangePill(
    label: String,
    selected: Boolean,
    onInk: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else onInk.copy(alpha = 0.12f),
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
    ) {
        Box(Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else onInk,
            )
        }
    }
}

@Composable
private fun IndexHeroPlaceholder() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        modifier = Modifier.fillMaxWidth().height(180.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.inverseOnSurface)
        }
    }
}

@Composable
private fun AreaCard(
    area: AreaSummary,
    onClick: () -> Unit,
    onToggleAlerts: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(enabled = !area.broken, onClick = onClick),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        area.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val sub = when {
                        area.broken -> stringResource(R.string.favorites_broken)
                        area.dealsCount > 0 -> stringResource(
                            R.string.home_deals_count, area.city ?: "", area.dealsCount,
                        )
                        else -> stringResource(R.string.favorites_never_run)
                    }
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (area.broken) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!area.broken) {
                        IconButton(onClick = onToggleAlerts, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (area.alertsEnabled) Icons.Filled.Notifications
                                else Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.alerts_toggle),
                                tint = if (area.alertsEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = stringResource(R.string.favorites_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            if (area.medianPrice != null) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text(Formatters.price(area.medianPrice), style = MaterialTheme.typography.titleLarge)
                        area.pricePerSqm?.let {
                            Text(
                                stringResource(R.string.deal_price_per_sqm, Formatters.number(it)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val deltaColor = if ((area.deltaPercent ?: 0.0) >= 0)
                            MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        if (area.sparkline.size >= 2) {
                            Sparkline(
                                values = area.sparkline,
                                color = deltaColor,
                                modifier = Modifier.size(width = 90.dp, height = 32.dp),
                            )
                        }
                        area.deltaPercent?.let {
                            Text(
                                Formatters.percent(it),
                                style = MaterialTheme.typography.titleSmall,
                                color = deltaColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAreas(onSearch: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onSearch).padding(top = 6.dp),
            ) {
                Text(
                    stringResource(R.string.home_empty_cta),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
                )
            }
        }
    }
}
