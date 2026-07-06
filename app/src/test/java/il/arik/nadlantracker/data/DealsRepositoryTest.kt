package il.arik.nadlantracker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import il.arik.nadlantracker.Fixtures
import il.arik.nadlantracker.core.database.NadlanDatabase
import il.arik.nadlantracker.core.network.GovMapApi
import il.arik.nadlantracker.core.network.NetworkModule
import il.arik.nadlantracker.data.dto.govmap.AutocompleteRequest
import il.arik.nadlantracker.data.dto.govmap.AutocompleteResponse
import il.arik.nadlantracker.data.dto.govmap.DealsResponse
import il.arik.nadlantracker.data.dto.govmap.RadiusBuildingDto
import il.arik.nadlantracker.data.repository.DealsRepository
import il.arik.nadlantracker.domain.model.DealFilters
import il.arik.nadlantracker.domain.model.SearchQuery
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Configurable fake — no mocking library needed. */
private class FakeGovMapApi : GovMapApi {
    var streetDealsCalls = 0
    var failNetwork = false
    var streetResponse: DealsResponse = DealsResponse()

    override suspend fun autocomplete(body: AutocompleteRequest): AutocompleteResponse =
        AutocompleteResponse()

    override suspend fun dealsByRadius(point: String, radius: Int): List<RadiusBuildingDto> =
        emptyList()

    override suspend fun streetDeals(polygonId: String, limit: Int, offset: Int): DealsResponse {
        streetDealsCalls++
        if (failNetwork) throw IOException("network down")
        // Single-page behaviour: only the first page has data.
        return if (offset == 0) streetResponse else DealsResponse()
    }

    override suspend fun neighborhoodDeals(polygonId: String, limit: Int, offset: Int): DealsResponse =
        streetDeals(polygonId, limit, offset)
}

@RunWith(RobolectricTestRunner::class)
class DealsRepositoryTest {

    private lateinit var db: NadlanDatabase
    private lateinit var api: FakeGovMapApi
    private lateinit var repository: DealsRepository
    private var fakeNow = 1_000_000L

    private val query = SearchQuery.Street("6902-274", "רנ\"ק, תל אביב")

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            NadlanDatabase::class.java,
        ).allowMainThreadQueries().build()
        api = FakeGovMapApi()
        api.streetResponse = NetworkModule.json.decodeFromString(Fixtures.read("street_deals.json"))
        repository = DealsRepository(
            api = api,
            dealDao = db.dealDao(),
            cacheMetaDao = db.cacheMetaDao(),
            json = NetworkModule.json,
            nowMs = { fakeNow },
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `first search fetches and caches, second is served from cache`() = runTest {
        val first = repository.search(query)
        val second = repository.search(query)

        assertEquals(1, api.streetDealsCalls)
        assertTrue(first.deals.isNotEmpty())
        assertEquals(first.deals, second.deals)
        assertEquals(false, second.isStale)
    }

    @Test
    fun `expired cache refetches`() = runTest {
        repository.search(query)
        fakeNow += DealsRepository.DEFAULT_TTL_MS + 1

        repository.search(query)

        assertEquals(2, api.streetDealsCalls)
    }

    @Test
    fun `forceRefresh bypasses fresh cache`() = runTest {
        repository.search(query)
        repository.search(query, forceRefresh = true)

        assertEquals(2, api.streetDealsCalls)
    }

    @Test
    fun `network failure serves stale cache with flag`() = runTest {
        val fresh = repository.search(query)
        fakeNow += DealsRepository.DEFAULT_TTL_MS + 1
        api.failNetwork = true

        val stale = repository.search(query)

        assertTrue(stale.isStale)
        assertEquals(fresh.deals, stale.deals)
    }

    @Test
    fun `network failure with no cache propagates`() = runTest {
        api.failNetwork = true

        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking { repository.search(query) }
        }
    }

    @Test
    fun `filters apply locally without refetching`() = runTest {
        val unfiltered = repository.search(query)
        val filtered = repository.search(
            query.copy(filters = DealFilters(roomsMin = 4.0)),
        )

        assertEquals(1, api.streetDealsCalls) // same scope hash → cache hit
        assertTrue(filtered.deals.size < unfiltered.deals.size)
        assertTrue(filtered.deals.all { (it.rooms ?: 0.0) >= 4.0 })
    }

    @Test
    fun `scope hash ignores filters and display name but not scope`() {
        val base = repository.scopeHash(query)

        assertEquals(base, repository.scopeHash(query.copy(filters = DealFilters(roomsMin = 3.0))))
        assertEquals(base, repository.scopeHash(query.copy(displayName = "אחר")))
        assertTrue(base != repository.scopeHash(query.copy(polygonId = "1-1")))
        assertTrue(base != repository.scopeHash(SearchQuery.Neighborhood("6902-274", "x")))
    }

    @Test
    fun `duplicate source rows are deduped in cache`() = runTest {
        val result = repository.search(query)

        val rawRows = api.streetResponse.data.size
        assertTrue(result.deals.size < rawRows)
    }
}
