package il.arik.nadlantracker.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import il.arik.nadlantracker.core.database.dao.CacheMetaDao
import il.arik.nadlantracker.core.database.dao.DealDao
import il.arik.nadlantracker.core.database.dao.FavoriteDao
import il.arik.nadlantracker.core.database.dao.IndexDao
import il.arik.nadlantracker.core.database.entity.CacheMetaEntity
import il.arik.nadlantracker.core.database.entity.DealEntity
import il.arik.nadlantracker.core.database.entity.FavoriteSearchEntity
import il.arik.nadlantracker.core.database.entity.IndexPointEntity

@Database(
    entities = [
        DealEntity::class,
        CacheMetaEntity::class,
        FavoriteSearchEntity::class,
        IndexPointEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NadlanDatabase : RoomDatabase() {
    abstract fun dealDao(): DealDao
    abstract fun cacheMetaDao(): CacheMetaDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun indexDao(): IndexDao
}
