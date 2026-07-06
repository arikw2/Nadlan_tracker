package il.arik.nadlantracker.core.network

import kotlin.random.Random
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class RetryInterceptorTest {

    private val server = MockWebServer()
    private val sleeps = mutableListOf<Long>()

    /** random.nextLong(bound) pinned to 0 so backoff values are exact. */
    private val noJitter = object : Random() {
        override fun nextBits(bitCount: Int) = 0
        override fun nextLong(until: Long) = 0L
    }

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client() = OkHttpClient.Builder()
        .addInterceptor(
            RetryInterceptor(
                backoffMs = longArrayOf(2_000, 5_000, 12_000),
                sleepMs = { sleeps += it },
                random = noJitter,
            )
        )
        .build()

    private fun execute(): Int =
        client().newCall(Request.Builder().url(server.url("/x")).build())
            .execute().use { it.code }

    @Test
    fun `recovers from two 429s with backoff schedule`() {
        server.enqueue(MockResponse().setResponseCode(429))
        server.enqueue(MockResponse().setResponseCode(429))
        server.enqueue(MockResponse().setBody("ok"))

        assertEquals(200, execute())
        assertEquals(listOf(2_000L, 5_000L), sleeps)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `honors Retry-After instead of backoff`() {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "7"))
        server.enqueue(MockResponse().setBody("ok"))

        assertEquals(200, execute())
        assertEquals(listOf(7_000L), sleeps)
    }

    @Test
    fun `gives up after exhausting retries and returns last response`() {
        repeat(4) { server.enqueue(MockResponse().setResponseCode(500)) }

        assertEquals(500, execute())
        assertEquals(3, sleeps.size)
        assertEquals(4, server.requestCount)
    }

    @Test
    fun `403 twice surfaces RateLimitedException`() {
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(MockResponse().setResponseCode(403))

        assertThrows(RateLimitedException::class.java) { execute() }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `single 403 then success recovers`() {
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(MockResponse().setBody("ok"))

        assertEquals(200, execute())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `success passes straight through`() {
        server.enqueue(MockResponse().setBody("ok"))

        assertEquals(200, execute())
        assertEquals(0, sleeps.size)
    }
}
