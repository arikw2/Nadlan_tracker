package il.arik.nadlantracker.feature.results

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.arik.nadlantracker.R
import il.arik.nadlantracker.core.ui.Formatters
import il.arik.nadlantracker.core.ui.charts.TrendColumnChart
import il.arik.nadlantracker.core.ui.charts.TrendLineChart
import il.arik.nadlantracker.domain.model.TrendSeries

/** Minimum monthly points before a trend line is worth drawing. */
private const val MIN_TREND_POINTS = 3

@Composable
internal fun TrendsTab(data: ResultsUiState.Data, onWidenPeriod: () -> Unit) {
    if (data.medianPrice.points.size < MIN_TREND_POINTS) {
        ThinDataEdgeState(dealCount = data.deals.size, onWidenPeriod = onWidenPeriod)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ChartCard(stringResource(R.string.trend_median_price), data.medianPrice, priceRange = true) {
                TrendLineChart(data.medianPrice.points)
            }
        }
        if (data.pricePerSqm.points.size >= 2) {
            item {
                ChartCard(stringResource(R.string.trend_price_per_sqm), data.pricePerSqm, priceRange = true) {
                    TrendLineChart(data.pricePerSqm.points)
                }
            }
        }
        item {
            ChartCard(stringResource(R.string.trend_deal_count), data.dealCount, priceRange = false) {
                TrendColumnChart(data.dealCount.points)
            }
        }
    }
}

/**
 * Each chart prints its own value range in the header instead of relying on a
 * dense axis — the design's "range printed, not a full axis" call.
 */
@Composable
private fun ChartCard(
    title: String,
    series: TrendSeries,
    priceRange: Boolean,
    chart: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                val values = series.points.map { it.value }
                if (values.isNotEmpty()) {
                    val lo = values.min()
                    val hi = values.max()
                    val fmt: (Double) -> String =
                        if (priceRange) Formatters::compactPrice else { v -> Formatters.number(v) }
                    Text(
                        "${fmt(lo)} – ${fmt(hi)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(Modifier.padding(top = 12.dp)) { chart() }
        }
    }
}

/** Too few months to draw a trend: say how thin it is and offer the fix. */
@Composable
private fun ThinDataEdgeState(dealCount: Int, onWidenPeriod: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.edge_thin_title, dealCount),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.edge_thin_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.padding(top = 16.dp).clickable(onClick = onWidenPeriod),
        ) {
            Text(
                stringResource(R.string.edge_thin_widen_period),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
            )
        }
    }
}
