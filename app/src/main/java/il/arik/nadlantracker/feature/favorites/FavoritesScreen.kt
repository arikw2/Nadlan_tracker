package il.arik.nadlantracker.feature.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import il.arik.nadlantracker.R
import il.arik.nadlantracker.core.ui.Formatters
import il.arik.nadlantracker.feature.results.CenteredMessage
import java.time.Instant
import java.time.ZoneId

@Composable
fun FavoritesScreen(
    onRunFavorite: (queryJson: String, favoriteId: Long) -> Unit,
    viewModel: FavoritesViewModel = viewModel(factory = FavoritesViewModel.Factory),
) {
    val favorites by viewModel.favorites.collectAsState()

    val current = favorites ?: return
    if (current.isEmpty()) {
        CenteredMessage(stringResource(R.string.favorites_empty))
        return
    }

    Column(Modifier.fillMaxSize().padding(top = 16.dp)) {
        Text(
            stringResource(R.string.favorites_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(current, key = { it.id }) { favorite ->
                Card(Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(favorite.displayName) },
                        supportingContent = {
                            val query = favorite.query
                            if (query == null) {
                                Text(
                                    stringResource(R.string.favorites_broken),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            } else {
                                Text(lastRunLabel(favorite.lastRunAtEpochMs))
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.delete(favorite.id) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.favorites_delete),
                                )
                            }
                        },
                        modifier = Modifier.clickable(enabled = favorite.query != null) {
                            favorite.query?.let { query ->
                                onRunFavorite(viewModel.encodeQuery(query), favorite.id)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun lastRunLabel(lastRunAtEpochMs: Long?): String {
    if (lastRunAtEpochMs == null) return stringResource(R.string.favorites_never_run)
    val date = Instant.ofEpochMilli(lastRunAtEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
    return stringResource(R.string.favorites_last_run, Formatters.date(date))
}
