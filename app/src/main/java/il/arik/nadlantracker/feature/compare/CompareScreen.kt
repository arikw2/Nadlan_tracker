package il.arik.nadlantracker.feature.compare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import il.arik.nadlantracker.R
import il.arik.nadlantracker.core.ui.charts.TrendMultiLineChart

@Composable
fun CompareScreen(viewModel: CompareViewModel = viewModel(factory = CompareViewModel.Factory)) {
    val state by viewModel.uiState.collectAsState()

    // Read theme colors outside the LazyListScope lambdas — those aren't @Composable.
    val colorA = MaterialTheme.colorScheme.primary
    val colorB = MaterialTheme.colorScheme.secondary

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = il.arik.nadlantracker.feature.nav.BottomNavClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(stringRes(R.string.compare_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringRes(R.string.compare_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.areas.size < 2) {
            item {
                Text(
                    stringRes(R.string.compare_need_two),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            return@LazyColumn
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val nameA = state.areas.firstOrNull { it.id == state.selectedA }?.displayName
                val nameB = state.areas.firstOrNull { it.id == state.selectedB }?.displayName
                SlotCard(stringRes(R.string.compare_slot_a), nameA, colorA, Modifier.weight(1f))
                SlotCard(stringRes(R.string.compare_slot_b), nameB, colorB, Modifier.weight(1f))
            }
        }

        item {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.areas.forEach { area ->
                    val slot = when (area.id) {
                        state.selectedA -> colorA
                        state.selectedB -> colorB
                        else -> null
                    }
                    AreaPickChip(area.displayName, slot) { viewModel.toggle(area.id) }
                }
            }
        }

        if (state.loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        state.result?.let { result ->
            if (result.isStale) {
                item {
                    Text(
                        stringRes(R.string.results_stale_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            item {
                CardBox {
                    Text(stringRes(R.string.compare_chart_title), style = MaterialTheme.typography.titleMedium)
                    if (result.priceA.points.size >= 2 || result.priceB.points.size >= 2) {
                        TrendMultiLineChart(listOf(result.priceA.points, result.priceB.points))
                        Text(
                            stringRes(R.string.compare_normalized_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        )
                    } else {
                        Text(
                            stringRes(R.string.trend_not_enough_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { ComparisonTable(result.rows, colorA, colorB) }
            result.psmGap?.let { gap ->
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringRes(
                                R.string.compare_insight_psm,
                                gap.dearerLabel,
                                "%.0f".format(gap.percent),
                                gap.cheaperLabel,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun stringRes(id: Int) = androidx.compose.ui.res.stringResource(id)

@Composable
private fun stringRes(id: Int, vararg args: Any) = androidx.compose.ui.res.stringResource(id, *args)

@Composable
private fun SlotCard(slotLabel: String, name: String?, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(2.dp, if (name != null) color else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Text(slotLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                name ?: "—",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun AreaPickChip(name: String, slotColor: Color?, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (slotColor != null) slotColor else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, slotColor ?: MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            name,
            style = MaterialTheme.typography.labelLarge,
            color = if (slotColor != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun ComparisonTable(rows: List<CompareRow>, colorA: Color, colorB: Color) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            rows.forEachIndexed { i, row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(row.a, style = MaterialTheme.typography.titleSmall, color = colorA, modifier = Modifier.weight(1f))
                    Text(
                        stringRes(row.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    Text(row.b, style = MaterialTheme.typography.titleSmall, color = colorB, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }
                if (i < rows.size - 1) {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 15.dp)
                            .height(1.dp).background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
        }
    }
}

@Composable
private fun CardBox(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
