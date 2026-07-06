package il.arik.nadlantracker.core.network

import java.io.IOException
import kotlin.random.Random
import okhttp3.Interceptor
import okhttp3.Response

/** Surfaced to the UI as "the government source is busy, showing saved data". */
class RateLimitedException(message: String) : IOException(message)

/**
 * Retries transient failures (HTTP 429/5xx and I/O errors) with exponential
 * backoff + jitter, honoring Retry-After when present. HTTP 403 — typically
 * the govmap WAF, not a transient state — is retried once, then surfaced as
 * [RateLimitedException] so the UI can fall back to cached data.
 */
class RetryInterceptor(
    private val backoffMs: LongArray = longArrayOf(2_000, 5_000, 12_000),
    private val sleepMs: (Long) -> Unit = Thread::sleep,
    private val random: Random = Random.Default,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var forbiddenCount = 0
        var lastException: IOException? = null
        var retryAfterMs: Long? = null

        for (attempt in 0..backoffMs.size) {
            if (attempt > 0) {
                val base = backoffMs[attempt - 1]
                sleepMs(retryAfterMs ?: (base + random.nextLong(base / 4 + 1)))
                retryAfterMs = null
            }

            val response = try {
                chain.proceed(chain.request())
            } catch (e: IOException) {
                lastException = e
                continue
            }

            when {
                response.code == 403 -> {
                    forbiddenCount++
                    response.close()
                    if (forbiddenCount > 1) {
                        throw RateLimitedException("govmap returned 403 twice — likely WAF block")
                    }
                }
                response.code == 429 || response.code in 500..599 -> {
                    if (attempt == backoffMs.size) return response
                    retryAfterMs = response.header("Retry-After")
                        ?.toLongOrNull()?.times(1000)?.takeIf { it > 0 }
                    response.close()
                }
                else -> return response
            }
        }
        throw lastException ?: RateLimitedException("request kept failing after retries")
    }
}
