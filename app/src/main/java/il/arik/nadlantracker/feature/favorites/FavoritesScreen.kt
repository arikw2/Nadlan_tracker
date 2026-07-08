package il.arik.nadlantracker.feature.favorites

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import il.arik.nadlantracker.R
import il.arik.nadlantracker.core.ui.Formatters
import il.arik.nadlantracker.data.repository.FavoritesRepository
import il.arik.nadlantracker.feature.results.CenteredMessage
import java.time.Instant
import java.time.ZoneId

@Composable
fun FavoritesScreen(
    onRunFavorite: (queryJson: String, favoriteId: Long) -> Unit,
    onCompare: (queryJsonA: String, queryJsonB: String) -> Unit,
    viewModel: FavoritesViewModel = viewModel(factory = FavoritesViewModel.Factory),
) {
    val favorites by viewModel.favorites.collectAsState()
    var compareMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    // Enabling the first alert needs POST_NOTIFICATIONS on Android 13+.
    var pendingAlertId by remember { mutableStateOf<Long?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingAlertId?.let { id -> if (granted) viewModel.setAlertsEnabled(id, true) }
        pendingAlertId = null
    }

    val current = favorites ?: return
    if (current.isEmpty()) {
        CenteredMessage(stringResource(R.string.favorites_empty))
        return
    }

    Column(Modifier.fillMaxSize().padding(top = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.favorites_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            FilterChip(
                selected = compareMode,
                onClick = {
                    compareMode = !compareMode
                    selectedIds = emptySet()
                },
                label = { Text(stringResource(R.string.compare_mode)) },
            )
        }

        if (compareMode) {
            val hint = when (selectedIds.size) {
                0, 1 -> stringResource(R.string.compare_pick_two)
                else -> null
            }
            hint?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (selectedIds.size == 2) {
                val picked = current.filter { it.id in selectedIds && it.query != null }
                if (picked.size == 2) {
                    Button(
                        onClick = {
                            onCompare(
                                viewModel.encodeQuery(picked[0].query!!),
                                viewModel.encodeQuery(picked[1].query!!),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Text(stringResource(R.string.compare_button))
                    }
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(current, key = { it.id }) { favorite ->
                FavoriteCard(
                    favorite = favorite,
                    compareMode = compareMode,
                    selected = favorite.id in selectedIds,
                    onToggleSelect = { checked ->
                        selectedIds = when {
                            checked && selectedIds.size < 2 -> selectedIds + favorite.id
                            !checked -> selectedIds - favorite.id
                            else -> selectedIds
                        }
                    },
                    onClick = {
                        favorite.query?.let { query ->
                            onRunFavorite(viewModel.encodeQuery(query), favorite.id)
                        }
                    },
                    onToggleAlerts = {
                        if (favorite.alertsEnabled) {
                            viewModel.setAlertsEnabled(favorite.id, false)
                        } else if (Build.VERSION.SDK_INT >= 33) {
                            pendingAlertId = favorite.id
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setAlertsEnabled(favorite.id, true)
                        }
                    },
                    onDelete = { viewModel.delete(favorite.id) },
                )
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    favorite: FavoritesRepository.Favorite,
    compareMode: Boolean,
    selected: Boolean,
    onToggleSelect: (Boolean) -> Unit,
    onClick: () -> Unit,
    onToggleAlerts: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(favorite.displayName) },
            supportingContent = {
                if (favorite.query == null) {
                    Text(
                        stringResource(R.string.favorites_broken),
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(lastRunLabel(favorite.lastRunAtEpochMs))
                }
            },
            leadingContent = if (compareMode && favorite.query != null) {
                { Checkbox(checked = selected, onCheckedChange = onToggleSelect) }
            } else {
                null
            },
            trailingContent = {
                Row {
                    if (favorite.query != null) {
                        IconButton(onClick = onToggleAlerts) {
                            Icon(
                                if (favorite.alertsEnabled) Icons.Filled.Notifications
                                else Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.alerts_toggle),
                                tint = if (favorite.alertsEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.favorites_delete),
                        )
                    }
                }
            },
            modifier = Modifier.clickable(enabled = favorite.query != null) {
                if (compareMode) onToggleSelect(!selected) else onClick()
            },
        )
    }
}

@Composable
private fun lastRunLabel(lastRunAtEpochMs: Long?): String {
    if (lastRunAtEpochMs == null) return stringResource(R.string.favorites_never_run)
    val date = Instant.ofEpochMilli(lastRunAtEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
    return stringResource(R.string.favorites_last_run, Formatters.date(date))
}
