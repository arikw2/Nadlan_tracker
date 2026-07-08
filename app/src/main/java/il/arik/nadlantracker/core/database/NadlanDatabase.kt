package il.arik.nadlantracker.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = true,
)
abstract class NadlanDatabase : RoomDatabase() {
    abstract fun dealDao(): DealDao
    abstract fun cacheMetaDao(): CacheMetaDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun indexDao(): IndexDao

    companion object {
        /** v2 adds the land-registry reference to cached deals. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE deals ADD COLUMN gushHelka TEXT")
            }
        }

        /** v3 adds the parcel centroid for the map view. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE deals ADD COLUMN x REAL")
                db.execSQL("ALTER TABLE deals ADD COLUMN y REAL")
            }
        }
    }
}
