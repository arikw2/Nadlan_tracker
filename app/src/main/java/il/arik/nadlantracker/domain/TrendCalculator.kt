package il.arik.nadlantracker.domain

import il.arik.nadlantracker.domain.model.Deal
import il.arik.nadlantracker.domain.model.DealFilters
import il.arik.nadlantracker.domain.model.TrendPoint
import il.arik.nadlantracker.domain.model.TrendSeries
import java.time.YearMonth

/**
 * Pure aggregation over deals — powers every chart in the app.
 * Prices use the median (nadlan data is outlier-heavy); price-per-sqm uses
 * the median over deals that have a positive area.
 */
object TrendCalculator {

    fun applyFilters(deals: List<Deal>, filters: DealFilters): List<Deal> {
        if (filters.isEmpty) return deals
        val from = filters.fromYearMonth?.let(YearMonth::parse)
        val to = filters.toYearMonth?.let(YearMonth::parse)
        return deals.filter { deal ->
            val period = YearMonth.from(deal.date)
            (from == null || period >= from) &&
                (to == null || period <= to) &&
                (filters.roomsMin == null || (deal.rooms ?: 0.0) >= filters.roomsMin) &&
                (filters.roomsMax == null || (deal.rooms ?: Double.MAX_VALUE) <= filters.roomsMax) &&
                (filters.propertyTypes.isEmpty() || deal.propertyType in filters.propertyTypes)
        }
    }

    fun medianPriceByMonth(deals: List<Deal>, label: String = "מחיר חציוני"): TrendSeries =
        aggregateByMonth(deals, label) { monthDeals ->
            median(monthDeals.map { it.priceIls.toDouble() })
        }

    fun medianPricePerSqmByMonth(deals: List<Deal>, label: String = "מחיר למ\"ר"): TrendSeries =
        aggregateByMonth(deals.filter { it.pricePerSqm != null }, label) { monthDeals ->
            median(monthDeals.mapNotNull { it.pricePerSqm })
        }

    fun dealCountByMonth(deals: List<Deal>, label: String = "מספר עסקאות"): TrendSeries =
        aggregateByMonth(deals, label) { it.size.toDouble() }

    private fun aggregateByMonth(
        deals: List<Deal>,
        label: String,
        aggregate: (List<Deal>) -> Double,
    ): TrendSeries {
        val points = deals
            .groupBy { YearMonth.from(it.date) }
            .map { (period, monthDeals) ->
                TrendPoint(period, aggregate(monthDeals), monthDeals.size)
            }
            .sortedBy { it.period }
        return TrendSeries(label, points)
    }

    fun median(values: List<Double>): Double {
        require(values.isNotEmpty()) { "median of empty list" }
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }
}
