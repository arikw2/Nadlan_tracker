package il.arik.nadlantracker.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import il.arik.nadlantracker.core.database.entity.CacheMetaEntity
import il.arik.nadlantracker.core.database.entity.DealEntity
import il.arik.nadlantracker.core.database.entity.FavoriteSearchEntity
import il.arik.nadlantracker.core.database.entity.IndexPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DealDao {

    @Query("SELECT * FROM deals WHERE queryHash = :queryHash ORDER BY dealDateEpochDay DESC")
    suspend fun getByQueryHash(queryHash: String): List<DealEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(deals: List<DealEntity>)

    @Query("DELETE FROM deals WHERE queryHash = :queryHash")
    suspend fun deleteByQueryHash(queryHash: String)

    @Transaction
    suspend fun replaceForQuery(queryHash: String, deals: List<DealEntity>) {
        deleteByQueryHash(queryHash)
        insertAll(deals)
    }

    @Query("DELETE FROM deals")
    suspend fun clear()
}

@Dao
interface CacheMetaDao {

    @Query("SELECT * FROM cache_meta WHERE queryHash = :queryHash")
    suspend fun get(queryHash: String): CacheMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: CacheMetaEntity)

    @Query("DELETE FROM cache_meta")
    suspend fun clear()
}

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorite_searches ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<FavoriteSearchEntity>>

    @Insert
    suspend fun insert(favorite: FavoriteSearchEntity): Long

    @Query("UPDATE favorite_searches SET lastRunAtEpochMs = :runAtEpochMs WHERE id = :id")
    suspend fun markRun(id: Long, runAtEpochMs: Long)

    @Query("UPDATE favorite_searches SET alertsEnabled = :enabled WHERE id = :id")
    suspend fun setAlertsEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE favorite_searches SET lastSeenCount = :count WHERE id = :id")
    suspend fun updateLastSeenCount(id: Long, count: Int)

    @Query("SELECT * FROM favorite_searches WHERE alertsEnabled = 1")
    suspend fun getAlertEnabled(): List<FavoriteSearchEntity>

    @Query("DELETE FROM favorite_searches WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface IndexDao {

    @Query("SELECT * FROM index_points WHERE seriesCode = :seriesCode ORDER BY period ASC")
    suspend fun getSeries(seriesCode: Long): List<IndexPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(points: List<IndexPointEntity>)

    @Query("DELETE FROM index_points")
    suspend fun clear()
}
