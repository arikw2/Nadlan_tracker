package il.arik.nadlantracker.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import il.arik.nadlantracker.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onShowResults: (queryJson: String) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.pendingResultsQueryJson) {
        state.pendingResultsQueryJson?.let { queryJson ->
            viewModel.onResultsNavigated()
            onShowResults(queryJson)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.search_title), style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = state.searchText,
            onValueChange = viewModel::onSearchTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.isAutocompleteLoading) {
                    CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                }
            },
            singleLine = true,
        )

        if (state.candidates.isNotEmpty()) {
            ElevatedCard {
                LazyColumn(Modifier.height((56 * minOf(state.candidates.size, 5)).dp)) {
                    items(state.candidates) { candidate ->
                        ListItem(
                            headlineContent = { Text(candidate.displayText) },
                            leadingContent = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                            modifier = Modifier.clickable { viewModel.onCandidatePicked(candidate) },
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScopeChip(state, viewModel, SearchScope.STREET, R.string.search_scope_street)
            ScopeChip(state, viewModel, SearchScope.NEIGHBORHOOD, R.string.search_scope_neighborhood)
            ScopeChip(state, viewModel, SearchScope.RADIUS, R.string.search_scope_radius)
        }

        if (state.scope == SearchScope.RADIUS) {
            Column {
                Text(
                    stringResource(R.string.search_radius_label, state.radiusMeters),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = state.radiusMeters.toFloat(),
                    onValueChange = { viewModel.onRadiusChange((it / 50).toInt() * 50) },
                    valueRange = 100f..1000f,
                    steps = 17,
                )
            }
        }

        FilterSection(filters = state.filters, onFiltersChange = viewModel::onFiltersChange)

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = viewModel::onSearchClicked,
            enabled = state.selectedCandidate != null && !state.isResolving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isResolving) {
                CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.height(0.dp))
                Text("  " + stringResource(R.string.search_resolving))
            } else {
                Text(stringResource(R.string.search_button))
            }
        }

        when {
            state.resolveFailed -> Text(
                stringResource(R.string.search_resolve_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            state.selectedCandidate == null -> Text(
                stringResource(R.string.search_pick_location),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ScopeChip(
    state: SearchUiState,
    viewModel: SearchViewModel,
    scope: SearchScope,
    labelRes: Int,
) {
    FilterChip(
        selected = state.scope == scope,
        onClick = { viewModel.onScopeChange(scope) },
        label = { Text(stringResource(labelRes)) },
    )
}

@Composable
private fun FilterSection(
    filters: il.arik.nadlantracker.domain.model.DealFilters,
    onFiltersChange: (il.arik.nadlantracker.domain.model.DealFilters) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.filter_period), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PeriodChip(filters, onFiltersChange, null, R.string.filter_period_all)
            PeriodChip(filters, onFiltersChange, 1, R.string.filter_period_1y)
            PeriodChip(filters, onFiltersChange, 3, R.string.filter_period_3y)
            PeriodChip(filters, onFiltersChange, 5, R.string.filter_period_5y)
        }

        val roomsLabel = if (filters.roomsMin == null && filters.roomsMax == null) {
            stringResource(R.string.filter_rooms_any)
        } else {
            stringResource(
                R.string.filter_rooms,
                filters.roomsMin?.let(::formatRooms) ?: "1",
                filters.roomsMax?.let(::formatRooms) ?: "8+",
            )
        }
        Text(roomsLabel, style = MaterialTheme.typography.titleSmall)
        androidx.compose.material3.RangeSlider(
            value = (filters.roomsMin?.toFloat() ?: 1f)..(filters.roomsMax?.toFloat() ?: 8f),
            onValueChange = { range ->
                onFiltersChange(
                    filters.copy(
                        roomsMin = roundHalf(range.start).takeIf { it > 1.0 },
                        roomsMax = roundHalf(range.endInclusive).takeIf { it < 8.0 },
                    )
                )
            },
            valueRange = 1f..8f,
            steps = 13,
        )
    }
}

@Composable
private fun PeriodChip(
    filters: il.arik.nadlantracker.domain.model.DealFilters,
    onFiltersChange: (il.arik.nadlantracker.domain.model.DealFilters) -> Unit,
    years: Int?,
    labelRes: Int,
) {
    val selected = when (years) {
        null -> filters.fromYearMonth == null
        else -> filters.fromYearMonth == fromYearsAgo(years)
    }
    FilterChip(
        selected = selected,
        onClick = { onFiltersChange(filters.copy(fromYearMonth = years?.let(::fromYearsAgo))) },
        label = { Text(stringResource(labelRes)) },
    )
}

private fun fromYearsAgo(years: Int): String =
    java.time.YearMonth.now().minusYears(years.toLong()).toString()

private fun roundHalf(value: Float): Double = Math.round(value * 2) / 2.0

private fun formatRooms(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
