package il.arik.nadlantracker.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import il.arik.nadlantracker.core.database.NadlanDatabase
import il.arik.nadlantracker.core.database.entity.DealEntity
import il.arik.nadlantracker.core.database.entity.FavoriteSearchEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DealDaoTest {

    private lateinit var db: NadlanDatabase

    private fun entity(queryHash: String, key: String, epochDay: Long = 19_000) = DealEntity(
        queryHash = queryHash,
        dealKey = key,
        dealDateEpochDay = epochDay,
        priceIls = 1_000_000,
        rooms = 3.0,
        areaSqm = 80.0,
        propertyType = "דירה",
        address = "בדיקה 1",
        city = "תל אביב-יפו",
        neighborhood = null,
        floor = null,
        gushHelka = null,
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            NadlanDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `replaceForQuery swaps only that query's rows`() = runTest {
        val dao = db.dealDao()
        dao.insertAll(listOf(entity("q1", "a"), entity("q1", "b"), entity("q2", "c")))

        dao.replaceForQuery("q1", listOf(entity("q1", "d")))

        assertEquals(listOf("d"), dao.getByQueryHash("q1").map { it.dealKey })
        assertEquals(listOf("c"), dao.getByQueryHash("q2").map { it.dealKey })
    }

    @Test
    fun `deals are ordered newest first`() = runTest {
        val dao = db.dealDao()
        dao.insertAll(listOf(entity("q", "old", 100), entity("q", "new", 200)))

        assertEquals(listOf("new", "old"), dao.getByQueryHash("q").map { it.dealKey })
    }

    @Test
    fun `favorites flow reflects inserts and deletes`() = runTest {
        val dao = db.favoriteDao()
        val id = dao.insert(
            FavoriteSearchEntity(
                displayName = "רנ\"ק",
                queryJson = "{}",
                createdAtEpochMs = 1,
                lastRunAtEpochMs = null,
            )
        )
        assertEquals(1, dao.observeAll().first().size)

        dao.markRun(id, 42)
        assertEquals(42L, dao.observeAll().first().single().lastRunAtEpochMs)

        dao.delete(id)
        assertEquals(0, dao.observeAll().first().size)
    }
}
