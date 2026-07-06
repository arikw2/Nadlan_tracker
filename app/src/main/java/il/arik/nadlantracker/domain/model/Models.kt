package il.arik.nadlantracker.domain.model

import java.time.LocalDate
import java.time.YearMonth

/** A candidate returned by address autocomplete, in EPSG:3857 coordinates. */
data class LocationCandidate(
    val displayText: String,
    val type: String,
    val x: Double,
    val y: Double,
)

/** A normalized real-estate transaction. */
data class Deal(
    val date: LocalDate,
    val priceIls: Long,
    val rooms: Double?,
    val areaSqm: Double?,
    val propertyType: String?,
    val address: String?,
    val city: String?,
    val neighborhood: String?,
    val floor: String?,
    /** Land-registry reference: "גוש/חלקה[/תת-חלקה]", e.g. "6902/274/7". */
    val gushHelka: String? = null,
) {
    val pricePerSqm: Double?
        get() = areaSqm?.takeIf { it > 0 }?.let { priceIls / it }
}

data class TrendPoint(
    val period: YearMonth,
    val value: Double,
    val count: Int,
)

data class TrendSeries(
    val label: String,
    val points: List<TrendPoint>,
)

data class IndexPoint(
    val period: YearMonth,
    /** Index value on the CBS base. */
    val value: Double,
    /** Month-over-month change, percent. */
    val monthlyChangePercent: Double?,
    /** Year-over-year change, percent. */
    val yearlyChangePercent: Double?,
)

data class IndexSeries(
    val code: Long,
    val name: String,
    val points: List<IndexPoint>,
)
