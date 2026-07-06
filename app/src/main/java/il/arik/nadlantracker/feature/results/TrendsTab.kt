package il.arik.nadlantracker.feature.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import il.arik.nadlantracker.R
import il.arik.nadlantracker.core.ui.charts.TrendColumnChart
import il.arik.nadlantracker.core.ui.charts.TrendLineChart
import il.arik.nadlantracker.domain.model.TrendSeries

@Composable
internal fun TrendsTab(data: ResultsUiState.Data) {
    val hasEnoughData = data.medianPrice.points.size >= 2
    if (!hasEnoughData) {
        CenteredMessage(stringResource(R.string.trend_not_enough_data))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            ChartSection(stringResource(R.string.trend_median_price), data.medianPrice) {
                TrendLineChart(data.medianPrice.points)
            }
        }
        if (data.pricePerSqm.points.size >= 2) {
            item {
                ChartSection(stringResource(R.string.trend_price_per_sqm), data.pricePerSqm) {
                    TrendLineChart(data.pricePerSqm.points)
                }
            }
        }
        item {
            ChartSection(stringResource(R.string.trend_deal_count), data.dealCount) {
                TrendColumnChart(data.dealCount.points)
            }
        }
    }
}

@Composable
private fun ChartSection(title: String, series: TrendSeries, chart: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        chart()
    }
}
