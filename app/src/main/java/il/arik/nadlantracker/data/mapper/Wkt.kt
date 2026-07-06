package il.arik.nadlantracker.data.mapper

/** Minimal WKT coordinate extraction — just enough for govmap shapes. */
object Wkt {

    private val FIRST_PAIR = Regex("""(-?\d+(?:\.\d+)?)\s+(-?\d+(?:\.\d+)?)""")

    /** First coordinate pair of any WKT ("POINT(x y)", "MULTIPOLYGON(((x y,...") */
    fun firstCoordinate(wkt: String?): Pair<Double, Double>? {
        if (wkt.isNullOrBlank()) return null
        val match = FIRST_PAIR.find(wkt) ?: return null
        val x = match.groupValues[1].toDoubleOrNull() ?: return null
        val y = match.groupValues[2].toDoubleOrNull() ?: return null
        return x to y
    }

    /**
     * Approximate ground distance in meters between two EPSG:3857 points.
     * Web-Mercator distances are inflated by 1/cos(lat); at Israel's latitude
     * (~31.5°N) the correction factor is ~0.852.
     */
    fun approximateMeters(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x1 - x2
        val dy = y1 - y2
        return Math.sqrt(dx * dx + dy * dy) * ISRAEL_MERCATOR_SCALE
    }

    private const val ISRAEL_MERCATOR_SCALE = 0.852
}
