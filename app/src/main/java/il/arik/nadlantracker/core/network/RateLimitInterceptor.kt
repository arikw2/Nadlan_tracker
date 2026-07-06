package il.arik.nadlantracker.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Per-host minimum spacing between requests (token-bucket of size one).
 * All app traffic funnels through the shared OkHttp client, so bursts from
 * autocomplete + deal fetches cannot hammer the rate-limited govmap API.
 *
 * OkHttp interceptors are blocking by design; the wait happens on OkHttp's
 * dispatcher thread, never the main thread. Clock and sleep are injectable
 * for deterministic tests.
 */
class RateLimitInterceptor(
    private val minIntervalMsPerHost: Map<String, Long> = DEFAULT_INTERVALS,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val sleepMs: (Long) -> Unit = Thread::sleep,
) : Interceptor {

    private val lastRequestAt = HashMap<String, Long>()
    private val lock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val host = chain.request().url.host
        val interval = minIntervalMsPerHost.entries.firstOrNull { host.endsWith(it.key) }?.value
        if (interval != null) {
            val waitMs = synchronized(lock) {
                val now = nowMs()
                val last = lastRequestAt[host]
                val wait = if (last == null) 0L else (last + interval - now).coerceAtLeast(0)
                lastRequestAt[host] = now + wait
                wait
            }
            if (waitMs > 0) sleepMs(waitMs)
        }
        return chain.proceed(chain.request())
    }

    companion object {
        val DEFAULT_INTERVALS = mapOf(
            "govmap.gov.il" to 1500L,
            "cbs.gov.il" to 500L,
        )
    }
}
