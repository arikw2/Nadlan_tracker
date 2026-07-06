package il.arik.nadlantracker.data

import il.arik.nadlantracker.Fixtures
import il.arik.nadlantracker.core.network.NetworkModule
import il.arik.nadlantracker.data.dto.cbs.CbsIndexResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CbsParsingTest {

    private val json = NetworkModule.json

    @Test
    fun `cbs dwelling index fixture parses monthly series`() {
        val response = json.decodeFromString<CbsIndexResponse>(Fixtures.read("cbs_price_last24.json"))

        val series = response.month.single { it.code == 40010L }
        assertEquals("מחירי דירות", series.name)
        assertEquals(24, series.date.size)

        val latest = series.date.first()
        assertEquals(2026, latest.year)
        assertEquals(3, latest.month)
        assertNotNull(latest.currBase?.value)
        assertNotNull(latest.percentYear)
        assertTrue(response.paging?.totalItems!! > 0)
    }

    @Test
    fun `unknown keys and missing fields do not fail parsing`() {
        val mutated = """
            {"month":[{"code":40010,"name":"מחירי דירות","futureKey":1,
                       "date":[{"year":2026,"month":3},{"percent":0.5}]}]}
        """.trimIndent()

        val response = json.decodeFromString<CbsIndexResponse>(mutated)

        assertEquals(2, response.month.single().date.size)
    }
}
