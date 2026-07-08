package il.arik.nadlantracker.data

import il.arik.nadlantracker.Fixtures
import il.arik.nadlantracker.core.network.NetworkModule
import il.arik.nadlantracker.data.dto.govmap.DealsResponse
import il.arik.nadlantracker.data.mapper.DealMapper
import il.arik.nadlantracker.data.mapper.Wkt
import java.time.LocalDate
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DealMapperTest {

    private val json = NetworkModule.json

    @Test
    fun `maps street deals fixture to normalized domain deals`() {
        val response = json.decodeFromString<DealsResponse>(Fixtures.read("street_deals.json"))

        val mapped = response.data.mapNotNull(DealMapper::fromDto)

        assertTrue(mapped.isNotEmpty())
        val first = mapped.first()
        assertEquals(LocalDate.of(2025, 7, 22), first.date)
        assertEquals(9_600_000L, first.priceIls)
        assertEquals("רנ\"ק 12", first.address) // house number "12.0" normalized
        assertEquals("תל אביב-יפו", first.city)
        assertEquals("6902/274/7", first.gushHelka)
        assertNotNull(mapped.first().x) // parcel coordinate parsed from shape
    }

    @Test
    fun `near-duplicate source records collapse, richer row wins`() {
        val response = json.decodeFromString<DealsResponse>(Fixtures.read("street_deals.json"))
        val mapped = response.data.mapNotNull(DealMapper::fromDto)

        val deduped = DealMapper.dedupeNearDuplicates(mapped)

        // The fixture registers three transactions twice — dates one day
        // apart, amounts within a fraction of a percent, different dealIds.
        assertEquals(mapped.size - 3, deduped.size)
        // The surviving twin is the one that carries a property type.
        assertTrue(deduped.filter { it.address == "רנ\"ק 12" }.count { it.propertyType == null } == 0)
    }

    @Test
    fun `rows without date or price are dropped`() {
        val noDate = json.decodeFromString<DealsResponse>(
            """{"data":[{"dealAmount":100},{"dealDate":"2024-01-01T00:00:00Z"},
                        {"dealDate":"2024-01-01T00:00:00Z","dealAmount":0}]}"""
        )
        assertTrue(noDate.data.mapNotNull(DealMapper::fromDto).isEmpty())
    }

    @Test
    fun `formatted price strings parse`() {
        val response = json.decodeFromString<DealsResponse>(
            """{"data":[{"dealDate":"2024-01-01T00:00:00Z","dealAmount":"1,234,567 ₪"}]}"""
        )
        assertEquals(1_234_567L, response.data.mapNotNull(DealMapper::fromDto).single().priceIls)
    }

    @Test
    fun `wkt point and polygon coordinates parse`() {
        assertEquals(
            3871101.97 to 3774606.05,
            Wkt.firstCoordinate("POINT(3871101.97 3774606.05)"),
        )
        assertNotNull(Wkt.firstCoordinate("MULTIPOLYGON(((3871028.2 3774670.4,3870997.4 3774678.8)))"))
        assertNull(Wkt.firstCoordinate(null))
        assertNull(Wkt.firstCoordinate("garbage"))
    }

    @Test
    fun `autocomplete candidate parses coordinates from shape`() {
        val candidate = DealMapper.candidateFromDto(
            il.arik.nadlantracker.data.dto.govmap.AutocompleteResultDto(
                text = "דיזנגוף תל אביב",
                type = "street",
                shape = "POINT(3871101.97 3774606.05)",
            )
        )
        assertEquals(3871101.97, candidate!!.x, 0.0)
    }

    @Test
    fun `mercator distance approximation is sane`() {
        // ~1000 mercator units at Israel latitude ≈ 852 ground meters
        val distance = Wkt.approximateMeters(0.0, 0.0, 1000.0, 0.0)
        assertEquals(852.0, distance, 0.5)
    }

    @Test
    fun `entity round trip preserves the deal`() {
        val deal = json.decodeFromString<DealsResponse>(Fixtures.read("street_deals.json"))
            .data.mapNotNull(DealMapper::fromDto).first()

        val roundTripped = DealMapper.fromEntity(DealMapper.toEntity("hash", deal))

        assertEquals(deal, roundTripped)
    }

    @Test
    fun `mercator to wgs84 lands on tel aviv`() {
        val (lat, lon) = Wkt.toLatLon(3871101.97, 3774606.05)
        assertEquals(34.77, lon, 0.05)
        assertEquals(32.06, lat, 0.05)
    }
}
