package il.arik.nadlantracker.feature.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import il.arik.nadlantracker.R
import il.arik.nadlantracker.core.ui.charts.TrendMultiLineChart
import il.arik.nadlantracker.core.ui.charts.trendSeriesColors
import il.arik.nadlantracker.domain.model.TrendSeries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    queryJsonA: String,
    queryJsonB: String,
    onBack: () -> Unit,
    viewModel: CompareViewModel = viewModel(
        factory = CompareViewModel.factory(queryJsonA, queryJsonB),
    ),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compare_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            is CompareUiState.Loading -> Box(
                Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is CompareUiState.Error -> Column(
                Modifier.padding(padding).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.results_error))
                TextButton(onClick = viewModel::load) {
                    Text(stringResource(R.string.results_retry))
                }
            }

            is CompareUiState.Data -> Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (current.isStale) {
                    Text(
                        stringResource(R.string.results_stale_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }

                ComparisonLegend(current.labelA, current.labelB)

                ComparisonSection(
                    title = stringResource(R.string.trend_median_price),
                    a = current.priceA,
                    b = current.priceB,
                )
                ComparisonSection(
                    title = stringResource(R.string.trend_price_per_sqm),
                    a = current.sqmA,
                    b = current.sqmB,
                )
            }
        }
    }
}

@Composable
private fun ComparisonLegend(labelA: String, labelB: String) {
    val colors = trendSeriesColors()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(labelA to colors.getOrElse(0) { MaterialTheme.colorScheme.primary },
               labelB to colors.getOrElse(1) { MaterialTheme.colorScheme.tertiary })
            .forEach { (label, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(12.dp).background(color, CircleShape))
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
    }
}

@Composable
private fun ComparisonSection(title: String, a: TrendSeries, b: TrendSeries) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (a.points.size >= 2 || b.points.size >= 2) {
            TrendMultiLineChart(listOf(a.points, b.points))
        } else {
            Text(stringResource(R.string.trend_not_enough_data))
        }
    }
}
