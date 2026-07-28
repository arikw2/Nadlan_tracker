package il.arik.nadlantracker.feature.results

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.arik.nadlantracker.R
import il.arik.nadlantracker.core.ui.Formatters
import il.arik.nadlantracker.core.ui.components.StatTile
import il.arik.nadlantracker.data.mapper.Wkt
import il.arik.nadlantracker.domain.TrendCalculator
import il.arik.nadlantracker.domain.model.Deal

/** Official parcel page — shows construction year and building details. */
private fun govParcelUrl(gushHelka: String): String {
    val parcelId = gushHelka.split('/').take(2).joinToString("-")
    return "https://www.nadlan.gov.il/?view=kparcel_all&id=$parcelId&page=deals"
}

/**
 * Deal detail, shown in a ModalBottomSheet. Gives a single deal the context a
 * card can't: its ₪/m² against the surrounding median, the registry reference,
 * and the nearest comparable sales.
 */
@Composable
internal fun DealDetailContent(deal: Deal, allDeals: List<Deal>) {
    val uriHandler = LocalUriHandler.current

    // Median ₪/m² of the deal's own neighborhood when known, else the whole result set.
    val peers = remember(deal, allDeals) {
        val sameHood = allDeals.filter { it.neighborhood != null && it.neighborhood == deal.neighborhood }
        (if (sameHood.size >= 3) sameHood else allDeals).mapNotNull { it.pricePerSqm }
    }
    val peerMedian = remember(peers) { peers.takeIf { it.size >= 2 }?.let(TrendCalculator::median) }
    val nearest = remember(deal, allDeals) { nearestComparables(deal, allDeals) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(Formatters.price(deal.priceIls), style = MaterialTheme.typography.displaySmall)
            Text(
                stringResource(R.string.deal_detail_registered, Formatters.date(deal.date)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column {
            deal.address?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
            val place = listOfNotNull(deal.neighborhood, deal.city).joinToString(", ")
            if (place.isNotEmpty()) {
                Text(
                    place,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            deal.rooms?.let {
                StatTile(Formatters.number(it), stringResource(R.string.deal_detail_row_rooms), Modifier.weight(1f))
            }
            deal.areaSqm?.let {
                StatTile("${Formatters.number(it)} מ״ר", stringResource(R.string.deal_detail_row_area), Modifier.weight(1f))
            }
            deal.floor?.let {
                StatTile(it, stringResource(R.string.deal_detail_row_floor), Modifier.weight(1f))
            }
        }

        deal.pricePerSqm?.let { psm ->
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.deal_detail_psm_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        stringResource(R.string.deal_price_per_sqm, Formatters.number(psm)),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    peerMedian?.let { median ->
                        Text(
                            stringResource(R.string.deal_detail_neighborhood_median, Formatters.number(median)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (median > 0) {
                            val diff = (psm - median) / median * 100
                            val label = if (diff >= 0) R.string.deal_detail_vs_median_above
                            else R.string.deal_detail_vs_median_below
                            Text(
                                stringResource(label, Formatters.percent(kotlin.math.abs(diff))),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        val rows = listOfNotNull(
            deal.propertyType?.let { stringResource(R.string.deal_detail_row_type) to it },
            deal.gushHelka?.let { stringResource(R.string.deal_detail_row_gush) to it },
        )
        if (rows.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    rows.forEachIndexed { i, (key, value) ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                key,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(value, style = MaterialTheme.typography.titleSmall)
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

        if (nearest.isNotEmpty()) {
            Text(
                stringResource(R.string.deal_detail_near_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
            nearest.forEach { comparable -> ComparableRow(comparable) }
        }

        deal.gushHelka?.let { gushHelka ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clickable { runCatching { uriHandler.openUri(govParcelUrl(gushHelka)) } },
            ) {
                Text(
                    stringResource(R.string.deal_gov_link),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun ComparableRow(deal: Deal) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .width(7.dp)
                    .height(34.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    deal.address ?: deal.neighborhood ?: "",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    listOfNotNull(
                        deal.rooms?.let { "${Formatters.number(it)} חד׳" },
                        deal.areaSqm?.let { "${Formatters.number(it)} מ״ר" },
                        Formatters.date(deal.date),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                Formatters.compactPrice(deal.priceIls.toDouble()),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

/**
 * The three closest other deals: by parcel distance when both have coordinates,
 * otherwise by similarity of area within the same neighborhood.
 */
private fun nearestComparables(deal: Deal, all: List<Deal>, limit: Int = 3): List<Deal> {
    val others = all.filter { it !== deal }
    if (others.isEmpty()) return emptyList()
    if (deal.x != null && deal.y != null) {
        val located = others.filter { it.x != null && it.y != null }
        if (located.isNotEmpty()) {
            return located
                .sortedBy { Wkt.approximateMeters(it.x!!, it.y!!, deal.x, deal.y) }
                .take(limit)
        }
    }
    val area = deal.areaSqm
    val pool = others.filter { it.neighborhood == deal.neighborhood }.ifEmpty { others }
    return if (area != null) {
        pool.filter { it.areaSqm != null }.sortedBy { kotlin.math.abs(it.areaSqm!! - area) }.take(limit)
    } else {
        pool.sortedByDescending { it.date }.take(limit)
    }
}
