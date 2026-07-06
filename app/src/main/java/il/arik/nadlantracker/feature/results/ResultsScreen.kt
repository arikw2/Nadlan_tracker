package il.arik.nadlantracker.feature.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import il.arik.nadlantracker.domain.model.Deal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    queryJson: String,
    forceRefresh: Boolean,
    favoriteId: Long?,
    onBack: () -> Unit,
    viewModel: ResultsViewModel = viewModel(
        factory = ResultsViewModel.factory(queryJson, forceRefresh, favoriteId),
    ),
) {
    val state by viewModel.uiState.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        (state as? ResultsUiState.Data)?.query?.displayName ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    val data = state as? ResultsUiState.Data
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.results_refresh))
                    }
                    IconButton(
                        onClick = { if (data?.favoriteSaved != true) showSaveDialog = true },
                        enabled = data != null,
                    ) {
                        Icon(
                            if (data?.favoriteSaved == true) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = stringResource(R.string.results_save_favorite),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                is ResultsUiState.Loading -> LoadingBox()
                is ResultsUiState.Error -> ErrorBox(onRetry = viewModel::refresh)
                is ResultsUiState.Data -> ResultsContent(current)
            }
        }
    }

    if (showSaveDialog) {
        val defaultName = (state as? ResultsUiState.Data)?.query?.displayName ?: ""
        SaveFavoriteDialog(
            defaultName = defaultName,
            onSave = { name ->
                viewModel.saveFavorite(name)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
        )
    }
}

@Composable
private fun ResultsContent(data: ResultsUiState.Data) {
    var selectedTab by remember { mutableIntStateOf(0) }

    if (data.isStale) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(R.string.results_stale_banner),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    TabRow(selectedTabIndex = selectedTab) {
        Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text(stringResource(R.string.results_tab_deals)) },
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text(stringResource(R.string.results_tab_trends)) },
        )
    }

    when (selectedTab) {
        0 -> DealListTab(data)
        1 -> TrendsTab(data)
    }
}

@Composable
private fun DealListTab(data: ResultsUiState.Data) {
    if (data.deals.isEmpty()) {
        CenteredMessage(stringResource(R.string.results_empty))
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                stringResource(R.string.results_deal_count, data.deals.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(data.deals) { deal -> DealCard(deal) }
    }
}

@Composable
private fun DealCard(deal: Deal) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Formatters.price(deal.priceIls), style = MaterialTheme.typography.titleMedium)
                Text(
                    Formatters.date(deal.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                listOfNotNull(deal.address, deal.city).joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                deal.rooms?.let {
                    DetailChip(stringResource(R.string.deal_rooms, Formatters.number(it)))
                }
                deal.areaSqm?.let {
                    DetailChip(stringResource(R.string.deal_area, Formatters.number(it)))
                }
                deal.floor?.let { DetailChip(stringResource(R.string.deal_floor, it)) }
                deal.pricePerSqm?.let {
                    DetailChip(stringResource(R.string.deal_price_per_sqm, Formatters.compactPrice(it)))
                }
            }
            deal.propertyType?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun LoadingBox() {
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator() }
}

@Composable
private fun ErrorBox(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.results_error), style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.results_retry)) }
    }
}

@Composable
internal fun CenteredMessage(text: String) {
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SaveFavoriteDialog(
    defaultName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorite_dialog_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.favorite_dialog_hint)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) {
                Text(stringResource(R.string.favorite_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.favorite_dialog_cancel))
            }
        },
    )
}
