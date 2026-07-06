package il.arik.nadlantracker.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RateLimitInterceptorTest {

    private val server = MockWebServer()
    private var fakeNow = 0L
    private val sleeps = mutableListOf<Long>()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(intervalMs: Long) = OkHttpClient.Builder()
        .addInterceptor(
            RateLimitInterceptor(
                minIntervalMsPerHost = mapOf(server.hostName to intervalMs),
                nowMs = { fakeNow },
                sleepMs = { sleeps += it; fakeNow += it },
            )
        )
        .build()

    private fun get(client: OkHttpClient) {
        client.newCall(Request.Builder().url(server.url("/x")).build()).execute().close()
    }

    @Test
    fun `second rapid request waits out the interval`() {
        server.enqueue(MockResponse())
        server.enqueue(MockResponse())
        val client = client(1500)

        get(client)
        get(client)

        assertEquals(listOf(1500L), sleeps)
    }

    @Test
    fun `request after the interval passes does not sleep`() {
        server.enqueue(MockResponse())
        server.enqueue(MockResponse())
        val client = client(1500)

        get(client)
        fakeNow += 5_000
        get(client)

        assertTrue(sleeps.isEmpty())
    }

    @Test
    fun `hosts without a configured interval are not throttled`() {
        server.enqueue(MockResponse())
        server.enqueue(MockResponse())
        val client = OkHttpClient.Builder()
            .addInterceptor(
                RateLimitInterceptor(
                    minIntervalMsPerHost = mapOf("govmap.gov.il" to 1500L),
                    nowMs = { fakeNow },
                    sleepMs = { sleeps += it },
                )
            )
            .build()

        get(client)
        get(client)

        assertTrue(sleeps.isEmpty())
    }
}
