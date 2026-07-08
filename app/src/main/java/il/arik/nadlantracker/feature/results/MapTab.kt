package il.arik.nadlantracker.feature.results

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.CompositionLocalProvider
import il.arik.nadlantracker.R
import il.arik.nadlantracker.data.mapper.Wkt
import il.arik.nadlantracker.domain.model.Deal
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

private data class MapDot(val geoPoint: GeoPoint, val color: Int)

/** Quantile-based ₪/m² color scale, cheap green → expensive red. */
private val SCALE_COLORS = intArrayOf(
    0xFF2E7D32.toInt(), // green
    0xFF9BC53D.toInt(),
    0xFFF2C14E.toInt(), // yellow
    0xFFEF8354.toInt(),
    0xFFC5283D.toInt(), // red
)
private val NO_AREA_COLOR = 0xFF9E9E9E.toInt()

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
        MapDot(GeoPoint(lat, lon), colorFor(deal))
    }
}

/** Single overlay drawing every deal as a translucent colored dot. */
private class DealsOverlay(private val dots: List<MapDot>) : Overlay() {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.WHITE
        alpha = 180
    }
    private val screenPoint = Point()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val radius = (mapView.zoomLevelDouble * 0.9f).toFloat().coerceIn(7f, 16f)
        dots.forEach { dot ->
            mapView.projection.toPixels(dot.geoPoint, screenPoint)
            fill.color = dot.color
            fill.alpha = 170
            canvas.drawCircle(screenPoint.x.toFloat(), screenPoint.y.toFloat(), radius, fill)
            canvas.drawCircle(screenPoint.x.toFloat(), screenPoint.y.toFloat(), radius, stroke)
        }
    }
}

@Composable
internal fun MapTab(data: ResultsUiState.Data) {
    val dots = remember(data.deals) { buildDots(data.deals) }
    if (dots.isEmpty()) {
        CenteredMessage(stringResource(R.string.map_no_coordinates))
        return
    }

    Column(Modifier.fillMaxSize()) {
        // osmdroid renders tiles LTR; keep the map itself out of the RTL flip.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(Modifier.weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        MapView(context).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            minZoomLevel = 7.0
                        }
                    },
                    update = { mapView ->
                        mapView.overlays.removeAll { it is DealsOverlay }
                        mapView.overlays.add(DealsOverlay(dots))
                        val box = BoundingBox.fromGeoPoints(dots.map { it.geoPoint })
                        mapView.post {
                            mapView.zoomToBoundingBox(box.increaseByScale(1.3f), false)
                            mapView.invalidate()
                        }
                    },
                )
            }
        }
        Legend()
    }
}

@Composable
private fun Legend() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.map_legend_cheap), style = MaterialTheme.typography.bodySmall)
        SCALE_COLORS.forEach { color ->
            Box(
                Modifier
                    .size(14.dp)
                    .background(androidx.compose.ui.graphics.Color(color), CircleShape),
            )
        }
        Text(stringResource(R.string.map_legend_expensive), style = MaterialTheme.typography.bodySmall)
    }
}
