package il.arik.nadlantracker.feature.macro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import il.arik.nadlantracker.R
import il.arik.nadlantracker.core.ui.Formatters
import il.arik.nadlantracker.core.ui.components.Sparkline
import il.arik.nadlantracker.core.ui.components.StatTile
import il.arik.nadlantracker.domain.model.IndexSeries
import il.arik.nadlantracker.feature.nav.BottomNavClearance

@Composable
fun MacroScreen(viewModel: MacroViewModel = viewModel(factory = MacroViewModel.Factory)) {
    val state by viewModel.uiState.collectAsState()

    when (val current = state) {
        is MacroUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is MacroUiState.Error -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.macro_error), style = MaterialTheme.typography.bodyLarge)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 14.dp)
                    .clickable { viewModel.load(forceRefresh = true) },
            ) {
                Text(
                    stringResource(R.string.results_retry),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        }
        is MacroUiState.Data -> MacroContent(current)
    }
}

@Composable
private fun MacroContent(data: MacroUiState.Data) {
    var periodYears by remember { mutableIntStateOf(10) }
    // Resolved here: stringResource is @Composable and can't be called in item {}.
    val note = divergenceNote(data)
    val buyColor = MaterialTheme.colorScheme.primary
    val rentColor = MaterialTheme.colorScheme.secondary

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = BottomNavClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(stringResource(R.string.macro_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.macro_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (data.isStale) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.results_stale_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                RangePill(stringResource(R.string.macro_period_5y), periodYears == 5) { periodYears = 5 }
                RangePill(stringResource(R.string.macro_period_10y), periodYears == 10) { periodYears = 10 }
                RangePill(stringResource(R.string.macro_period_all), periodYears <= 0) { periodYears = 0 }
            }
        }

        item {
            IndexSection(
                title = stringResource(R.string.macro_prices_section),
                series = data.dwellingSeries,
                periodYears = periodYears,
                lineColor = buyColor,
            )
        }

        data.rentSeries?.let { rent ->
            item {
                IndexSection(
                    title = stringResource(R.string.macro_rent_section),
                    series = rent,
                    periodYears = periodYears,
                    lineColor = rentColor,
                )
            }
        }

        // Buy-vs-rent divergence is the point of this screen — state it in words.
        note?.let { text ->
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(15.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun divergenceNote(data: MacroUiState.Data): String? {
    val buy = data.dwellingSeries.points.lastOrNull()?.yearlyChangePercent ?: return null
    val rent = data.rentSeries?.points?.lastOrNull()?.yearlyChangePercent ?: return null
    return if (buy >= rent) {
        stringResource(R.string.macro_note_buy_faster, Formatters.percent(buy), Formatters.percent(rent))
    } else {
        stringResource(R.string.macro_note_rent_faster, Formatters.percent(rent), Formatters.percent(buy))
    }
}

@Composable
private fun IndexSection(
    title: String,
    series: IndexSeries,
    periodYears: Int,
    lineColor: Color,
) {
    val visiblePoints = remember(series, periodYears) {
        if (periodYears <= 0) series.points else series.points.takeLast(periodYears * 12)
    }
    val latest = series.points.lastOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)

        if (latest != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    value = Formatters.number(latest.value),
                    caption = stringResource(R.string.macro_latest_value),
                    subtitle = Formatters.shortPeriod(latest.period),
                    modifier = Modifier.weight(1f),
                )
                latest.yearlyChangePercent?.let {
                    StatTile(
                        value = Formatters.percent(it),
                        caption = stringResource(R.string.macro_yoy),
                        modifier = Modifier.weight(1f),
                    )
                }
                latest.monthlyChangePercent?.let {
                    StatTile(
                        value = Formatters.percent(it),
                        caption = stringResource(R.string.macro_mom),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (visiblePoints.size >= 2) {
                Sparkline(
                    values = visiblePoints.map { it.value },
                    color = lineColor,
                    modifier = Modifier.fillMaxWidth().height(100.dp).padding(14.dp),
                    strokeWidthDp = 2.4f,
                )
            } else {
                Text(
                    stringResource(R.string.trend_not_enough_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun RowScope.RangePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
    ) {
        Box(Modifier.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
