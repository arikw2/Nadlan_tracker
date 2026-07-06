package il.arik.nadlantracker.domain

import il.arik.nadlantracker.core.network.NetworkModule
import il.arik.nadlantracker.domain.model.DealFilters
import il.arik.nadlantracker.domain.model.SearchQuery
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchQuerySerializationTest {

    private val json = NetworkModule.json

    private fun roundTrip(query: SearchQuery): SearchQuery =
        json.decodeFromString(SearchQuery.serializer(), json.encodeToString(SearchQuery.serializer(), query))

    @Test
    fun `all subtypes round trip`() {
        val filters = DealFilters(fromYearMonth = "2023-01", roomsMin = 2.5, propertyTypes = setOf("דירה"))
        val queries = listOf(
            SearchQuery.Street("6902-274", "רנ\"ק, תל אביב", filters),
            SearchQuery.Neighborhood("6902-274", "הצפון הישן", filters),
            SearchQuery.Radius(3871101.9, 3774606.0, 250, "סביב דיזנגוף", filters),
        )
        queries.forEach { assertEquals(it, roundTrip(it)) }
    }

    /**
     * Frozen v1 wire format — favorites persist this JSON. If this test
     * breaks, stored favorites broke too: add migration, don't just fix
     * the assertion.
     */
    @Test
    fun `frozen v1 json still decodes`() {
        val v1 = """
            {"type":"street","polygonId":"6902-274","displayName":"רנ\"ק, תל אביב",
             "filters":{"fromYearMonth":"2023-01","roomsMin":2.5,"propertyTypes":["דירה"]}}
        """.trimIndent()

        val decoded = json.decodeFromString(SearchQuery.serializer(), v1)

        assertEquals(
            SearchQuery.Street(
                polygonId = "6902-274",
                displayName = "רנ\"ק, תל אביב",
                filters = DealFilters(fromYearMonth = "2023-01", roomsMin = 2.5, propertyTypes = setOf("דירה")),
            ),
            decoded,
        )
    }
}
