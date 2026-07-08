package il.arik.nadlantracker.data.repository

import il.arik.nadlantracker.core.database.dao.CacheMetaDao
import il.arik.nadlantracker.core.database.dao.IndexDao
import il.arik.nadlantracker.core.database.entity.CacheMetaEntity
import il.arik.nadlantracker.core.network.CbsApi
import il.arik.nadlantracker.data.mapper.IndexMapper
import il.arik.nadlantracker.domain.model.IndexSeries
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CbsRepository(
    private val api: CbsApi,
    private val indexDao: IndexDao,
    private val cacheMetaDao: CacheMetaDao,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val cacheTtlMs: Long = DealsRepository.DEFAULT_TTL_MS,
) {

    data class IndexResult(val series: IndexSeries, val isStale: Boolean)

    private val lock = Mutex()

    suspend fun dwellingIndex(forceRefresh: Boolean = false): IndexResult =
        indexSeries(CbsApi.DWELLING_PRICE_INDEX_CODE, "מחירי דירות", forceRefresh)

    suspend fun rentIndex(forceRefresh: Boolean = false): IndexResult =
        indexSeries(CbsApi.RENT_INDEX_CODE, "שכר דירה", forceRefresh)

    suspend fun indexSeries(
        code: Long,
        fallbackName: String,
        forceRefresh: Boolean = false,
    ): IndexResult = lock.withLock {
        val cacheKey = "cbs:$code"
        val meta = cacheMetaDao.get(cacheKey)
        val fresh = meta != null && nowMs() - meta.fetchedAtEpochMs < cacheTtlMs

        if (meta != null && (fresh && !forceRefresh)) {
            return@withLock IndexResult(loadCached(code, fallbackName), isStale = false)
        }

        try {
            var page = 1
            var name = fallbackName
            while (page <= MAX_PAGES) {
                val response = api.indexData(code, page = page)
                val series = response.month.firstOrNull { it.code == code }
                    ?: response.month.firstOrNull()
                series?.name?.let { name = it }
                val points = series?.date.orEmpty()
                    .mapNotNull { IndexMapper.toEntity(code, it) }
                indexDao.upsertAll(points)
                val lastPage = response.paging?.lastPage ?: 1
                if (page >= lastPage || points.isEmpty()) break
                page++
            }
            cacheMetaDao.upsert(CacheMetaEntity(cacheKey, nowMs(), 0))
            IndexResult(loadCached(code, name), isStale = false)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            if (meta == null) throw e
            IndexResult(loadCached(code, fallbackName), isStale = true)
        }
    }

    private suspend fun loadCached(code: Long, seriesName: String): IndexSeries {
        val points = indexDao.getSeries(code).map(IndexMapper::fromEntity)
        return IndexSeries(code, seriesName, points)
    }

    suspend fun clearCache() {
        indexDao.clear()
    }

    companion object {
        private const val MAX_PAGES = 6
    }
}
