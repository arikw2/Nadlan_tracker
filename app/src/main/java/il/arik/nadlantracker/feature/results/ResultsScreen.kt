package il.arik.nadlantracker.feature.results

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import il.arik.nadlantracker.R
import il.arik.nadlantracker.core.ui.Formatters
import il.arik.nadlantracker.core.ui.components.StatTile
import il.arik.nadlantracker.domain.TrendCalculator
import il.arik.nadlantracker.domain.model.Deal

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

    Column(Modifier.fillMaxSize()) {
        ResultsHeader(
            state = state,
            onBack = onBack,
            onRefresh = viewModel::refresh,
            onSaveFavorite = { showSaveDialog = true },
        )
        when (val current = state) {
            is ResultsUiState.Loading -> LoadingBox()
            is ResultsUiState.Error -> WafEdgeState(onRetry = viewModel::refresh)
            is ResultsUiState.Data -> ResultsContent(current, onWidenPeriod = viewModel::widenPeriod)
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
private fun ResultsHeader(
    state: ResultsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSaveFavorite: () -> Unit,
) {
    val data = state as? ResultsUiState.Data
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RoundButton(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = stringResource(R.string.action_back),
            onClick = onBack,
        )
        Column(Modifier.weight(1f)) {
            Text(
                data?.query?.displayName ?: "",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            data?.let {
                Text(
                    resultsSubtitle(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        RoundButton(
            icon = if (data?.favoriteSaved == true) Icons.Filled.Star else Icons.Outlined.StarOutline,
            contentDescription = stringResource(
                if (data?.favoriteSaved == true) R.string.results_favorite_saved
                else R.string.results_save_favorite,
            ),
            onClick = { if (data?.favoriteSaved != true) onSaveFavorite() },
            filled = true,
            enabled = data != null,
        )
        RoundButton(
            Icons.Filled.Refresh,
            contentDescription = stringResource(R.string.results_refresh),
            onClick = onRefresh,
        )
    }
}

@Composable
private fun resultsSubtitle(data: ResultsUiState.Data): String {
    val city = data.deals.mapNotNull { it.city }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    val countLabel = stringResource(R.string.results_summary_count)
    return listOfNotNull(city, "${data.deals.size} $countLabel").joinToString(" · ")
}

@Composable
private fun RoundButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    filled: Boolean = false,
    enabled: Boolean = true,
) {
    Surface(
        shape = CircleShape,
        color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (filled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .size(40.dp)
            .clickable(enabled = enabled, onClickLabel = contentDescription, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultsContent(data: ResultsUiState.Data, onWidenPeriod: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var sheetDeal by remember { mutableStateOf<Deal?>(null) }

    Column(Modifier.fillMaxSize()) {
        if (data.isStale) StaleBanner()

        SegmentedTabs(
            selected = selectedTab,
            labels = listOf(
                stringResource(R.string.results_tab_deals),
                stringResource(R.string.results_tab_trends),
                stringResource(R.string.results_tab_map),
            ),
            onSelect = { selectedTab = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )

        when (selectedTab) {
            0 -> DealListTab(data, onDealClick = { sheetDeal = it })
            1 -> TrendsTab(data, onWidenPeriod = onWidenPeriod)
            2 -> MapTab(data)
        }
    }

    sheetDeal?.let { deal ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { sheetDeal = null }, sheetState = sheetState) {
            DealDetailContent(deal = deal, allDeals = data.deals)
        }
    }
}

@Composable
private fun StaleBanner() {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                stringResource(R.string.results_stale_banner),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun SegmentedTabs(
    selected: Int,
    labels: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            labels.forEachIndexed { i, label ->
                val isSel = i == selected
                Surface(
                    shape = CircleShape,
                    color = if (isSel) MaterialTheme.colorScheme.inverseSurface
                    else androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier.weight(1f).clickable { onSelect(i) },
                ) {
                    Box(Modifier.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                        Text(
                            label,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isSel) MaterialTheme.colorScheme.inverseOnSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DealListTab(data: ResultsUiState.Data, onDealClick: (Deal) -> Unit) {
    if (data.deals.isEmpty()) {
        CenteredMessage(stringResource(R.string.results_empty))
        return
    }
    val medianPrice = remember(data.deals) {
        data.deals.map { it.priceIls.toDouble() }.takeIf { it.isNotEmpty() }?.let(TrendCalculator::median)
    }
    val medianPsm = remember(data.deals) {
        data.deals.mapNotNull { it.pricePerSqm }.takeIf { it.isNotEmpty() }?.let(TrendCalculator::median)
    }
    val sorted = remember(data.deals) { data.deals.sortedByDescending { it.date } }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    value = medianPrice?.let { Formatters.compactPrice(it) } ?: "—",
                    caption = stringResource(R.string.results_summary_median),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = medianPsm?.let { Formatters.compactPrice(it) } ?: "—",
                    caption = stringResource(R.string.results_summary_psm),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = data.deals.size.toString(),
                    caption = stringResource(R.string.results_summary_count),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Text(
                stringResource(R.string.results_deal_sort, data.deals.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        items(sorted) { deal -> DealCard(deal, onClick = { onDealClick(deal) }) }
    }
}

@Composable
private fun DealCard(deal: Deal, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Formatters.price(deal.priceIls), style = MaterialTheme.typography.titleLarge)
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                deal.rooms?.let { Chip(stringResource(R.string.deal_rooms, Formatters.number(it))) }
                deal.areaSqm?.let { Chip(stringResource(R.string.deal_area, Formatters.number(it))) }
                deal.floor?.let { Chip(stringResource(R.string.deal_floor, it)) }
                deal.pricePerSqm?.let {
                    Chip(stringResource(R.string.deal_price_per_sqm, Formatters.compactPrice(it)))
                }
            }
            val secondary = listOfNotNull(
                deal.propertyType,
                deal.neighborhood,
                deal.gushHelka?.let { stringResource(R.string.deal_gush_helka, it) },
            ).joinToString(" · ")
            if (secondary.isNotEmpty()) {
                Text(
                    secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun Chip(text: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** govmap WAF / rate-limit block: name the cause and offer a retry. */
@Composable
private fun WafEdgeState(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(78.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
        Text(
            stringResource(R.string.edge_waf_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            stringResource(R.string.edge_waf_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 18.dp).clickable(onClick = onRetry),
        ) {
            Text(
                stringResource(R.string.edge_waf_retry),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 13.dp),
            )
        }
    }
}

@Composable
internal fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
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
            TextButton(onClick = { onSave(name) }) { Text(stringResource(R.string.favorite_dialog_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.favorite_dialog_cancel)) }
        },
    )
}
