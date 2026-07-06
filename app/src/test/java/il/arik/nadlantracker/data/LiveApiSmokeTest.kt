package il.arik.nadlantracker.data

import il.arik.nadlantracker.core.network.CbsApi
import il.arik.nadlantracker.core.network.NetworkModule
import il.arik.nadlantracker.data.dto.govmap.AutocompleteRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Live smoke test against the real government APIs — exercises the actual
 * Retrofit wiring (converter, Hebrew POST bodies, comma-in-path encoding,
 * interceptor chain) that hermetic tests can't cover.
 *
 * Skipped unless RUN_LIVE_API_TESTS=1 so CI stays hermetic and rate-limit
 * friendly. Run locally with:
 *   RUN_LIVE_API_TESTS=1 ./gradlew testDebugUnitTest --tests '*LiveApiSmokeTest*'
 */
class LiveApiSmokeTest {

    private fun assumeEnabled() =
        assumeTrue("live API tests disabled", System.getenv("RUN_LIVE_API_TESTS") == "1")

    @Test
    fun `govmap autocomplete, radius, and street deals round trip`() = runBlocking<Unit> {
        assumeEnabled()
        val api = NetworkModule.govMapApi(NetworkModule.okHttpClient())

        val candidates = api.autocomplete(AutocompleteRequest("דיזנגוף תל אביב"))
        assertTrue(candidates.results.isNotEmpty())

        val buildings = api.dealsByRadius("3871101.97,3774606.05", 100)
        val polygonId = buildings.firstNotNullOf { it.polygonId }

        val deals = api.streetDeals(polygonId, limit = 10, offset = 0)
        assertTrue(deals.data.isNotEmpty())
    }

    @Test
    fun `cbs dwelling index responds`() = runBlocking<Unit> {
        assumeEnabled()
        val api = NetworkModule.cbsApi(NetworkModule.okHttpClient())

        val response = api.indexData(CbsApi.DWELLING_PRICE_INDEX_CODE, last = 12)
        assertTrue(response.month.first().date.size == 12)
    }
}
