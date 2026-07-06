package il.arik.nadlantracker.feature.macro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import il.arik.nadlantracker.R
import il.arik.nadlantracker.core.ui.Formatters
import il.arik.nadlantracker.core.ui.charts.TrendLineChart
import il.arik.nadlantracker.domain.model.IndexPoint
import il.arik.nadlantracker.domain.model.TrendPoint

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
            Text(stringResource(R.string.macro_error))
            TextButton(onClick = { viewModel.load(forceRefresh = true) }) {
                Text(stringResource(R.string.results_retry))
            }
        }
        is MacroUiState.Data -> MacroContent(current)
    }
}

@Composable
private fun MacroContent(data: MacroUiState.Data) {
    var periodYears by remember { mutableIntStateOf(10) }

    val visiblePoints = remember(data.series, periodYears) {
        val points = data.series.points
        if (periodYears <= 0) points
        else points.takeLast(periodYears * 12)
    }
    val latest = data.series.points.lastOrNull()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.macro_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.macro_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (data.isStale) {
            Text(
                stringResource(R.string.results_stale_banner),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        latest?.let { StatRow(it) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PeriodChip(periodYears == 5, R.string.macro_period_5y) { periodYears = 5 }
            PeriodChip(periodYears == 10, R.string.macro_period_10y) { periodYears = 10 }
            PeriodChip(periodYears <= 0, R.string.macro_period_all) { periodYears = 0 }
        }

        if (visiblePoints.size >= 2) {
            TrendLineChart(
                visiblePoints.map { TrendPoint(it.period, it.value, 1) },
            )
        } else {
            Text(stringResource(R.string.trend_not_enough_data))
        }
    }
}

@Composable
private fun StatRow(latest: IndexPoint) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            label = stringResource(R.string.macro_latest_value),
            value = latest.value.toString(),
            subtitle = Formatters.shortPeriod(latest.period),
            modifier = Modifier.weight(1f),
        )
        latest.yearlyChangePercent?.let {
            StatCard(
                label = stringResource(R.string.macro_yoy),
                value = Formatters.percent(it),
                modifier = Modifier.weight(1f),
            )
        }
        latest.monthlyChangePercent?.let {
            StatCard(
                label = stringResource(R.string.macro_mom),
                value = Formatters.percent(it),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge)
            subtitle?.let {
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
private fun PeriodChip(selected: Boolean, labelRes: Int, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(stringResource(labelRes)) })
}
