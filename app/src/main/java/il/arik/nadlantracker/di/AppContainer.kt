package il.arik.nadlantracker.di

import android.content.Context
import androidx.room.Room
import il.arik.nadlantracker.core.database.NadlanDatabase
import il.arik.nadlantracker.core.network.NetworkModule
import il.arik.nadlantracker.data.repository.CbsRepository
import il.arik.nadlantracker.data.repository.DealsRepository
import il.arik.nadlantracker.data.repository.FavoritesRepository
import il.arik.nadlantracker.data.repository.GeoRepository

/**
 * Manual dependency container. Kept deliberately simple (no Hilt) — a single
 * module app with a handful of screens.
 */
class AppContainer(appContext: Context) {

    private val database: NadlanDatabase = Room
        .databaseBuilder(appContext, NadlanDatabase::class.java, "nadlan.db")
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    private val okHttpClient = NetworkModule.okHttpClient()
    private val govMapApi = NetworkModule.govMapApi(okHttpClient)
    private val cbsApi = NetworkModule.cbsApi(okHttpClient)

    val geoRepository = GeoRepository(govMapApi)

    val dealsRepository = DealsRepository(
        api = govMapApi,
        dealDao = database.dealDao(),
        cacheMetaDao = database.cacheMetaDao(),
        json = NetworkModule.json,
    )

    val favoritesRepository = FavoritesRepository(
        dao = database.favoriteDao(),
        json = NetworkModule.json,
    )

    val cbsRepository = CbsRepository(
        api = cbsApi,
        indexDao = database.indexDao(),
        cacheMetaDao = database.cacheMetaDao(),
    )

    suspend fun clearAllCaches() {
        dealsRepository.clearCache()
        cbsRepository.clearCache()
    }
}
