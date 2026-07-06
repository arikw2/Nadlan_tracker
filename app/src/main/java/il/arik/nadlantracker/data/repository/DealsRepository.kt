package il.arik.nadlantracker.data.repository

import il.arik.nadlantracker.core.database.dao.CacheMetaDao
import il.arik.nadlantracker.core.database.dao.DealDao
import il.arik.nadlantracker.core.database.entity.CacheMetaEntity
import il.arik.nadlantracker.core.network.GovMapApi
import il.arik.nadlantracker.data.dto.govmap.DealsResponse
import il.arik.nadlantracker.data.mapper.DealMapper
import il.arik.nadlantracker.data.mapper.Wkt
import il.arik.nadlantracker.domain.TrendCalculator
import il.arik.nadlantracker.domain.model.Deal
import il.arik.nadlantracker.domain.model.DealFilters
import il.arik.nadlantracker.domain.model.SearchQuery
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class DealsRepository(
    private val api: GovMapApi,
    private val dealDao: DealDao,
    private val cacheMetaDao: CacheMetaDao,
    private val json: Json,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val cacheTtlMs: Long = DEFAULT_TTL_MS,
) {

    data class DealsResult(
        val deals: List<Deal>,
        val isStale: Boolean,
        val fetchedAtEpochMs: Long,
    )

    private val inFlight = ConcurrentHashMap<String, Mutex>()

    /**
     * Cache-first search. The cache key covers only the fetch scope —
     * [DealFilters] apply locally afterwards, so changing filters never
     * refetches. On network failure an existing cache is served stale.
     */
    suspend fun search(query: SearchQuery, forceRefresh: Boolean = false): DealsResult {
        val hash = scopeHash(query)
        return inFlight.getOrPut(hash) { Mutex() }.withLock {
            val meta = cacheMetaDao.get(hash)
            val fresh = meta != null && nowMs() - meta.fetchedAtEpochMs < cacheTtlMs

            if (meta != null && fresh && !forceRefresh) {
                return@withLock loadCached(hash, query.filters, isStale = false, meta.fetchedAtEpochMs)
            }

            try {
                val deals = fetchAll(query)
                val fetchedAt = nowMs()
                dealDao.replaceForQuery(hash, deals.map { DealMapper.toEntity(hash, it) })
                cacheMetaDao.upsert(CacheMetaEntity(hash, fetchedAt, deals.size))
                loadCached(hash, query.filters, isStale = false, fetchedAt)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (meta == null) throw e
                loadCached(hash, query.filters, isStale = true, meta.fetchedAtEpochMs)
            }
        }
    }

    suspend fun clearCache() {
        dealDao.clear()
        cacheMetaDao.clear()
    }

    private suspend fun loadCached(
        hash: String,
        filters: DealFilters,
        isStale: Boolean,
        fetchedAt: Long,
    ): DealsResult {
        val all = dealDao.getByQueryHash(hash).map(DealMapper::fromEntity)
        return DealsResult(TrendCalculator.applyFilters(all, filters), isStale, fetchedAt)
    }

    private suspend fun fetchAll(query: SearchQuery): List<Deal> {
        val mapped = when (query) {
            is SearchQuery.Street -> pageAll { limit, offset ->
                api.streetDeals(query.polygonId, limit, offset)
            }
            is SearchQuery.Neighborhood -> pageAll { limit, offset ->
                api.neighborhoodDeals(query.polygonId, limit, offset)
            }
            is SearchQuery.Radius -> fetchRadius(query)
        }
        val exact = mapped
            .map { it.deal }
            .distinctBy(DealMapper::dealKey)
        return DealMapper.dedupeNearDuplicates(exact)
    }

    /**
     * The radius endpoint lists buildings, not deals. Fetch the street deals
     * of the few distinct streets inside the ring, then keep only parcels
     * whose coordinate actually falls within the requested radius.
     */
    private suspend fun fetchRadius(query: SearchQuery.Radius): List<DealMapper.MappedDeal> {
        val buildings = api.dealsByRadius("${query.x},${query.y}", query.radiusMeters)
        val streetPolygons = buildings
            .filter { it.polygonId != null }
            .distinctBy { it.streetNameHeb ?: it.polygonId }
            .take(MAX_RADIUS_STREETS)
            .map { it.polygonId!! }

        return streetPolygons
            .flatMap { polygonId -> pageAll { limit, offset -> api.streetDeals(polygonId, limit, offset) } }
            .filter { mapped ->
                mapped.x != null && mapped.y != null &&
                    Wkt.approximateMeters(mapped.x, mapped.y, query.x, query.y) <= query.radiusMeters
            }
    }

    private suspend fun pageAll(
        fetchPage: suspend (limit: Int, offset: Int) -> DealsResponse,
    ): List<DealMapper.MappedDeal> {
        val result = mutableListOf<DealMapper.MappedDeal>()
        var offset = 0
        while (offset < MAX_ROWS) {
            val page = fetchPage(PAGE_SIZE, offset)
            result += page.data.mapNotNull(DealMapper::fromDto)
            if (page.data.size < PAGE_SIZE) break
            offset += PAGE_SIZE
        }
        return result
    }

    /** Stable cache key for the fetch scope: the query with filters blanked. */
    fun scopeHash(query: SearchQuery): String {
        val scopeOnly: SearchQuery = when (query) {
            is SearchQuery.Street -> query.copy(filters = DealFilters(), displayName = "")
            is SearchQuery.Neighborhood -> query.copy(filters = DealFilters(), displayName = "")
            is SearchQuery.Radius -> query.copy(filters = DealFilters(), displayName = "")
        }
        val encoded = json.encodeToString(SearchQuery.serializer(), scopeOnly)
        return MessageDigest.getInstance("SHA-256")
            .digest(encoded.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val DEFAULT_TTL_MS: Long = 24 * 60 * 60 * 1000
        private const val PAGE_SIZE = 200
        private const val MAX_ROWS = 1000
        private const val MAX_RADIUS_STREETS = 5
    }
}
