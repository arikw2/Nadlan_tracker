package il.arik.nadlantracker.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import il.arik.nadlantracker.R
import il.arik.nadlantracker.core.ui.components.FieldLabel
import il.arik.nadlantracker.domain.model.DealFilters
import il.arik.nadlantracker.feature.nav.BottomNavClearance

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
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 18.dp)
            .padding(bottom = BottomNavClearance),
    ) {
        Text(stringResource(R.string.search_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.search_source_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SearchField(
            value = state.searchText,
            onValueChange = viewModel::onSearchTextChange,
            loading = state.isAutocompleteLoading,
            modifier = Modifier.padding(top = 16.dp),
        )

        if (state.candidates.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Column {
                    state.candidates.take(5).forEach { candidate ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onCandidatePicked(candidate) }
                                .padding(horizontal = 15.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp),
                            )
                            Text(
                                candidate.displayText,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (candidate.type.isNotBlank()) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text(
                                        candidate.type,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FieldLabel(stringResource(R.string.search_scope_label), Modifier.padding(top = 22.dp, bottom = 9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ScopePill(state, viewModel, SearchScope.STREET, R.string.search_scope_street)
            ScopePill(state, viewModel, SearchScope.NEIGHBORHOOD, R.string.search_scope_neighborhood)
            ScopePill(state, viewModel, SearchScope.SETTLEMENT, R.string.search_scope_settlement)
            ScopePill(state, viewModel, SearchScope.RADIUS, R.string.search_scope_radius)
        }

        if (state.scope == SearchScope.RADIUS) {
            PanelBox(Modifier.padding(top = 14.dp)) {
                Text(
                    stringResource(R.string.search_radius_label, state.radiusMeters),
                    style = MaterialTheme.typography.titleSmall,
                )
                Slider(
                    value = state.radiusMeters.toFloat(),
                    onValueChange = { viewModel.onRadiusChange((it / 50).toInt() * 50) },
                    valueRange = 100f..1000f,
                    steps = 17,
                )
            }
        }

        FieldLabel(stringResource(R.string.filter_period), Modifier.padding(top = 22.dp, bottom = 9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            PeriodPill(state.filters, viewModel::onFiltersChange, null, R.string.filter_period_all)
            PeriodPill(state.filters, viewModel::onFiltersChange, 1, R.string.filter_period_1y)
            PeriodPill(state.filters, viewModel::onFiltersChange, 3, R.string.filter_period_3y)
            PeriodPill(state.filters, viewModel::onFiltersChange, 5, R.string.filter_period_5y)
        }

        FieldLabel(stringResource(R.string.filter_rooms_label), Modifier.padding(top = 22.dp, bottom = 9.dp))
        PanelBox {
            val filters = state.filters
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
            RangeSlider(
                value = (filters.roomsMin?.toFloat() ?: 1f)..(filters.roomsMax?.toFloat() ?: 8f),
                onValueChange = { range ->
                    viewModel.onFiltersChange(
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

        val enabled = state.selectedCandidate != null && !state.isResolving
        Surface(
            shape = CircleShape,
            color = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = if (enabled) 6.dp else 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .clickable(enabled = enabled) { viewModel.onSearchClicked() },
        ) {
            Row(
                Modifier.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.isResolving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
                Text(
                    if (state.isResolving) "  " + stringResource(R.string.search_resolving)
                    else stringResource(R.string.search_button),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when {
            state.resolveFailed -> Text(
                stringResource(R.string.search_resolve_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
            state.selectedCandidate == null -> Text(
                stringResource(R.string.search_pick_location),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 17.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.merge(
                        MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (value.isEmpty()) {
                    Text(
                        stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (loading) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun RowScope.ScopePill(
    state: SearchUiState,
    viewModel: SearchViewModel,
    scope: SearchScope,
    labelRes: Int,
) {
    Pill(
        label = stringResource(labelRes),
        selected = state.scope == scope,
        modifier = Modifier.weight(1f),
        onClick = { viewModel.onScopeChange(scope) },
    )
}

@Composable
private fun RowScope.PeriodPill(
    filters: DealFilters,
    onFiltersChange: (DealFilters) -> Unit,
    years: Int?,
    labelRes: Int,
) {
    val selected = when (years) {
        null -> filters.fromYearMonth == null
        else -> filters.fromYearMonth == fromYearsAgo(years)
    }
    Pill(
        label = stringResource(labelRes),
        selected = selected,
        modifier = Modifier.weight(1f),
        onClick = { onFiltersChange(filters.copy(fromYearMonth = years?.let(::fromYearsAgo))) },
    )
}

@Composable
private fun Pill(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Box(Modifier.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PanelBox(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), content = content)
    }
}

private fun fromYearsAgo(years: Int): String =
    java.time.YearMonth.now().minusYears(years.toLong()).toString()

private fun roundHalf(value: Float): Double = Math.round(value * 2) / 2.0

private fun formatRooms(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
