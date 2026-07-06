package il.arik.nadlantracker.data

import il.arik.nadlantracker.Fixtures
import il.arik.nadlantracker.core.network.NetworkModule
import il.arik.nadlantracker.data.dto.govmap.AutocompleteResponse
import il.arik.nadlantracker.data.dto.govmap.DealsResponse
import il.arik.nadlantracker.data.dto.govmap.RadiusBuildingDto
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests against JSON recorded from the live govmap API
 * (scripts/probe_api.sh). If the upstream schema drifts, these fail first.
 */
class GovMapParsingTest {

    private val json = NetworkModule.json

    @Test
    fun `autocomplete fixture parses with coordinates`() {
        val response = json.decodeFromString<AutocompleteResponse>(Fixtures.read("autocomplete.json"))

        assertTrue(response.results.isNotEmpty())
        val street = response.results.first()
        assertEquals("street", street.type)
        assertTrue(street.shape!!.startsWith("POINT("))
        assertTrue(street.text!!.contains("דיזנגוף"))
    }

    @Test
    fun `radius deals fixture parses as building list with polygon ids`() {
        val buildings = json.decodeFromString<List<RadiusBuildingDto>>(Fixtures.read("radius_deals.json"))

        assertTrue(buildings.isNotEmpty())
        assertTrue(buildings.any { it.polygonId != null })
        assertTrue(buildings.any { it.dealscount?.jsonPrimitive?.content?.toIntOrNull() != null })
    }

    @Test
    fun `street deals fixture parses full deal rows`() {
        val response = json.decodeFromString<DealsResponse>(Fixtures.read("street_deals.json"))

        assertEquals("98", response.totalCount?.jsonPrimitive?.content)
        assertTrue(response.data.isNotEmpty())
        val deal = response.data.first()
        assertEquals("תל אביב-יפו", deal.settlementNameHeb)
        assertEquals(9_600_000L, deal.dealAmount?.jsonPrimitive?.content?.toDouble()?.toLong())
        assertEquals("2025-07-22T00:00:00Z", deal.dealDate)
        assertEquals(4.0, deal.assetRoomNum!!, 0.0)
        assertEquals(118.0, deal.assetArea!!, 0.0)
    }

    @Test
    fun `neighborhood deals fixture parses`() {
        val response = json.decodeFromString<DealsResponse>(Fixtures.read("neighborhood_deals.json"))

        assertTrue(response.data.isNotEmpty())
        assertTrue(response.data.all { it.dealDate != null })
    }

    @Test
    fun `rows with nulls and unknown keys do not fail parsing`() {
        val mutated = """
            {
              "totalCount": 2,
              "someNewEnvelopeKey": {"nested": true},
              "data": [
                {"dealId": 1, "dealAmount": "1234567", "dealDate": "2024-01-01T00:00:00Z",
                 "brandNewField": [1,2,3], "propertyTypeDescription": null},
                {"objectid": 7}
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString<DealsResponse>(mutated)

        assertEquals(2, response.data.size)
        assertEquals("1234567", response.data[0].dealAmount?.jsonPrimitive?.content)
        assertEquals(7L, response.data[1].objectid)
    }
}
