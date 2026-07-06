package il.arik.nadlantracker.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A cached deal row. [dealKey] is a content hash — govmap emits duplicate
 * source records under different dealIds, so identity is derived from the
 * deal's substance. [queryHash] links the row to the search that fetched it.
 */
@Entity(
    tableName = "deals",
    primaryKeys = ["queryHash", "dealKey"],
    indices = [Index("queryHash")],
)
data class DealEntity(
    val queryHash: String,
    val dealKey: String,
    val dealDateEpochDay: Long,
    val priceIls: Long,
    val rooms: Double?,
    val areaSqm: Double?,
    val propertyType: String?,
    val address: String?,
    val city: String?,
    val neighborhood: String?,
    val floor: String?,
    val gushHelka: String?,
)

@Entity(tableName = "cache_meta")
data class CacheMetaEntity(
    @PrimaryKey val queryHash: String,
    val fetchedAtEpochMs: Long,
    val resultCount: Int,
)

@Entity(tableName = "favorite_searches")
data class FavoriteSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    /** Json-encoded [il.arik.nadlantracker.domain.model.SearchQuery]. */
    val queryJson: String,
    val createdAtEpochMs: Long,
    val lastRunAtEpochMs: Long?,
)

@Entity(tableName = "index_points", primaryKeys = ["seriesCode", "period"])
data class IndexPointEntity(
    val seriesCode: Long,
    /** "YYYY-MM" — sorts chronologically as text. */
    val period: String,
    val value: Double,
    val monthlyChangePercent: Double?,
    val yearlyChangePercent: Double?,
)
