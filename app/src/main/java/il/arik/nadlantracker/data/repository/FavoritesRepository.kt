package il.arik.nadlantracker.data.repository

import il.arik.nadlantracker.core.database.dao.FavoriteDao
import il.arik.nadlantracker.core.database.entity.FavoriteSearchEntity
import il.arik.nadlantracker.domain.model.SearchQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class FavoritesRepository(
    private val dao: FavoriteDao,
    private val json: Json,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {

    /** [query] is null when a stored favorite no longer decodes (schema drift). */
    data class Favorite(
        val id: Long,
        val displayName: String,
        val query: SearchQuery?,
        val createdAtEpochMs: Long,
        val lastRunAtEpochMs: Long?,
        val alertsEnabled: Boolean = false,
        val lastSeenCount: Int? = null,
    )

    fun observeAll(): Flow<List<Favorite>> = dao.observeAll().map { entities ->
        entities.map(::toFavorite)
    }

    suspend fun alertEnabledFavorites(): List<Favorite> =
        dao.getAlertEnabled().map(::toFavorite)

    private fun toFavorite(entity: il.arik.nadlantracker.core.database.entity.FavoriteSearchEntity) =
        Favorite(
            id = entity.id,
            displayName = entity.displayName,
            query = runCatching {
                json.decodeFromString(SearchQuery.serializer(), entity.queryJson)
            }.getOrNull(),
            createdAtEpochMs = entity.createdAtEpochMs,
            lastRunAtEpochMs = entity.lastRunAtEpochMs,
            alertsEnabled = entity.alertsEnabled,
            lastSeenCount = entity.lastSeenCount,
        )

    suspend fun setAlertsEnabled(id: Long, enabled: Boolean) = dao.setAlertsEnabled(id, enabled)

    suspend fun updateLastSeenCount(id: Long, count: Int) = dao.updateLastSeenCount(id, count)

    suspend fun save(displayName: String, query: SearchQuery): Long = dao.insert(
        FavoriteSearchEntity(
            displayName = displayName,
            queryJson = json.encodeToString(SearchQuery.serializer(), query),
            createdAtEpochMs = nowMs(),
            lastRunAtEpochMs = null,
        )
    )

    suspend fun markRun(id: Long) = dao.markRun(id, nowMs())

    suspend fun delete(id: Long) = dao.delete(id)
}
