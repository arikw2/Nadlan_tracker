package il.arik.nadlantracker.domain

import il.arik.nadlantracker.domain.model.Deal
import il.arik.nadlantracker.domain.model.DealFilters
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class TrendCalculatorTest {

    private fun deal(
        date: String,
        price: Long,
        rooms: Double? = 3.0,
        area: Double? = 80.0,
        type: String? = "דירה",
    ) = Deal(
        date = LocalDate.parse(date),
        priceIls = price,
        rooms = rooms,
        areaSqm = area,
        propertyType = type,
        address = "בדיקה 1",
        city = "תל אביב-יפו",
        neighborhood = null,
        floor = null,
    )

    @Test
    fun `median is exact for odd and even counts`() {
        assertEquals(2.0, TrendCalculator.median(listOf(3.0, 1.0, 2.0)), 0.0)
        assertEquals(2.5, TrendCalculator.median(listOf(4.0, 1.0, 2.0, 3.0)), 0.0)
    }

    @Test
    fun `median price buckets by month and resists outliers`() {
        val deals = listOf(
            deal("2024-01-05", 1_000_000),
            deal("2024-01-20", 2_000_000),
            deal("2024-01-25", 90_000_000), // outlier
            deal("2024-03-10", 3_000_000),
        )

        val series = TrendCalculator.medianPriceByMonth(deals)

        assertEquals(2, series.points.size)
        val january = series.points.first()
        assertEquals(YearMonth.of(2024, 1), january.period)
        assertEquals(2_000_000.0, january.value, 0.0)
        assertEquals(3, january.count)
        assertEquals(YearMonth.of(2024, 3), series.points[1].period)
    }

    @Test
    fun `price per sqm ignores deals without area`() {
        val deals = listOf(
            deal("2024-01-05", 1_600_000, area = 80.0),  // 20k/sqm
            deal("2024-01-06", 2_400_000, area = 60.0),  // 40k/sqm
            deal("2024-01-07", 9_999_999, area = null),
        )

        val series = TrendCalculator.medianPricePerSqmByMonth(deals)

        assertEquals(1, series.points.size)
        assertEquals(30_000.0, series.points.first().value, 0.001)
        assertEquals(2, series.points.first().count)
    }

    @Test
    fun `deal counts by month`() {
        val deals = listOf(
            deal("2024-01-05", 1), deal("2024-01-06", 2), deal("2024-02-01", 3),
        )

        val series = TrendCalculator.dealCountByMonth(deals)

        assertEquals(listOf(2.0, 1.0), series.points.map { it.value })
    }

    @Test
    fun `filters constrain period rooms and type`() {
        val deals = listOf(
            deal("2023-12-31", 1, rooms = 3.0),
            deal("2024-01-15", 2, rooms = 2.0),
            deal("2024-02-15", 3, rooms = 4.0),
            deal("2024-03-15", 4, rooms = 4.0, type = "מגרש"),
            deal("2024-06-01", 5, rooms = 4.0),
        )

        val filtered = TrendCalculator.applyFilters(
            deals,
            DealFilters(
                fromYearMonth = "2024-01",
                toYearMonth = "2024-05",
                roomsMin = 3.0,
                propertyTypes = setOf("דירה"),
            ),
        )

        assertEquals(listOf(3L), filtered.map { it.priceIls })
    }

    @Test
    fun `empty filters return the list unchanged`() {
        val deals = listOf(deal("2024-01-05", 1, rooms = null, type = null))
        assertEquals(deals, TrendCalculator.applyFilters(deals, DealFilters()))
    }
}
