package il.arik.nadlantracker.feature.results

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import il.arik.nadlantracker.R
import il.arik.nadlantracker.data.mapper.Wkt
import il.arik.nadlantracker.domain.model.Deal
import kotlin.math.hypot
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

private data class MapDot(val geoPoint: GeoPoint, val color: Int, val deal: Deal)

/** Quantile-based ₪/m² color scale, cheap green → expensive red. */
private val SCALE_COLORS = intArrayOf(
    0xFF2E7D32.toInt(), // green
    0xFF9BC53D.toInt(),
    0xFFF2C14E.toInt(), // yellow
    0xFFEF8354.toInt(),
    0xFFC5283D.toInt(), // red
)
private val NO_AREA_COLOR = 0xFF9E9E9E.toInt()

/** Drawn dot radius. Shared with hit-testing so taps line up with what's visible. */
private fun dotRadius(mapView: MapView): Float =
    (mapView.zoomLevelDouble * 0.9f).toFloat().coerceIn(7f, 16f)

private fun buildDots(deals: List<Deal>): List<MapDot> {
    val located = deals.filter { it.x != null && it.y != null }
    val sqmPrices = located.mapNotNull { it.pricePerSqm }.sorted()
    fun colorFor(deal: Deal): Int {
        val ppsqm = deal.pricePerSqm ?: return NO_AREA_COLOR
        if (sqmPrices.size < SCALE_COLORS.size) return SCALE_COLORS[SCALE_COLORS.size / 2]
        val rank = sqmPrices.binarySearch(ppsqm).let { if (it < 0) -it - 1 else it }
        val bucket = (rank * SCALE_COLORS.size / sqmPrices.size).coerceAtMost(SCALE_COLORS.size - 1)
        return SCALE_COLORS[bucket]
    }
    return located.map { deal ->
        val (lat, lon) = Wkt.toLatLon(deal.x!!, deal.y!!)
        MapDot(GeoPoint(lat, lon), colorFor(deal), deal)
    }
}

/**
 * Single overlay drawing every deal as a translucent colored dot, and turning a
 * tap into a deal selection. Hit-testing picks the nearest dot within a
 * finger-sized slop of the drawn radius, so overlapping parcels resolve to the
 * one actually aimed at rather than the first in the list.
 */
private class DealsOverlay(
    val dots: List<MapDot>,
    private val onTap: (Deal) -> Unit,
) : Overlay() {

    /** Highlighted deal. Set from composition; changing it only needs an invalidate. */
    var selected: Deal? = null

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.WHITE
        alpha = 180
    }
    // Two rings, dark under light, so the selection reads over any tile.
    private val ringOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.BLACK
        alpha = 90
    }
    private val ringInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        color = Color.WHITE
    }
    private val screenPoint = Point()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val radius = dotRadius(mapView)
        var selectedAt: Pair<Float, Float>? = null
        dots.forEach { dot ->
            mapView.projection.toPixels(dot.geoPoint, screenPoint)
            val cx = screenPoint.x.toFloat()
            val cy = screenPoint.y.toFloat()
            fill.color = dot.color
            fill.alpha = 170
            canvas.drawCircle(cx, cy, radius, fill)
            canvas.drawCircle(cx, cy, radius, stroke)
            if (dot.deal == selected) selectedAt = cx to cy
        }
        // Drawn last so the ring is never buried under a neighbouring dot.
        selectedAt?.let { (cx, cy) ->
            canvas.drawCircle(cx, cy, radius + 6f, ringOuter)
            canvas.drawCircle(cx, cy, radius + 6f, ringInner)
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val threshold = dotRadius(mapView) + 14f
        var best: MapDot? = null
        var bestDistance = Float.MAX_VALUE
        dots.forEach { dot ->
            mapView.projection.toPixels(dot.geoPoint, screenPoint)
            val distance = hypot(screenPoint.x - e.x, screenPoint.y - e.y)
            if (distance <= threshold && distance < bestDistance) {
                bestDistance = distance
                best = dot
            }
        }
        val hit = best ?: return false // let the map handle taps on empty space
        onTap(hit.deal)
        return true
    }
}

@Composable
internal fun MapTab(
    data: ResultsUiState.Data,
    selectedDeal: Deal?,
    onDealClick: (Deal) -> Unit,
) {
    val dots = remember(data.deals) { buildDots(data.deals) }
    if (dots.isEmpty()) {
        CenteredMessage(stringResource(R.string.map_no_coordinates))
        return
    }

    // Keeps the overlay's tap callback current without rebuilding the overlay —
    // rebuilding it would re-run the framing effect and throw away the user's pan.
    val currentOnClick by rememberUpdatedState(onDealClick)
    var mapRef by remember { mutableStateOf<MapView?>(null) }
    val overlay = remember(dots) { DealsOverlay(dots) { currentOnClick(it) } }

    // The map fills the tab and rounds into the layout; the legend floats on top
    // as a pill so it stops eating a strip of map.
    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(22.dp)),
    ) {
        // osmdroid renders tiles LTR; keep the map itself out of the RTL flip.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        minZoomLevel = 7.0
                        mapRef = this
                    }
                },
            )
        }
        FloatingLegend(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp),
        )
    }

    // Attach and frame only when the deal set (or the map) actually changes, so
    // selecting a deal no longer resets the viewport.
    LaunchedEffect(mapRef, overlay) {
        val mapView = mapRef ?: return@LaunchedEffect
        mapView.overlays.removeAll { it is DealsOverlay }
        mapView.overlays.add(overlay)
        mapView.post {
            if (overlay.dots.size == 1) {
                // A single point makes a degenerate bounding box; center instead.
                mapView.controller.setZoom(17.0)
                mapView.controller.setCenter(overlay.dots.first().geoPoint)
            } else {
                val box = BoundingBox.fromGeoPoints(overlay.dots.map { it.geoPoint })
                mapView.zoomToBoundingBox(box.increaseByScale(1.3f), false)
            }
            mapView.invalidate()
        }
    }

    LaunchedEffect(mapRef, overlay, selectedDeal) {
        overlay.selected = selectedDeal
        mapRef?.invalidate()
    }
}

@Composable
private fun FloatingLegend(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 4.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.map_legend_cheap),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SCALE_COLORS.forEach { color ->
                Box(
                    Modifier
                        .size(13.dp)
                        .background(androidx.compose.ui.graphics.Color(color), CircleShape),
                )
            }
            Text(
                stringResource(R.string.map_legend_expensive),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
