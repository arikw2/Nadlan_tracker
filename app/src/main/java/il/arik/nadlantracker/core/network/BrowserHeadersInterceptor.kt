package il.arik.nadlantracker.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Presents the app as the govmap web frontend. The govmap WAF is markedly
 * more permissive toward requests that carry the site's own browser headers.
 */
class BrowserHeadersInterceptor(
    private val hostSuffix: String = "govmap.gov.il",
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.url.host.endsWith(hostSuffix)) {
            return chain.proceed(request)
        }
        val decorated = request.newBuilder()
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://www.govmap.gov.il/")
            .header("Origin", "https://www.govmap.gov.il")
            .header("Accept", "application/json")
            .build()
        return chain.proceed(decorated)
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/126.0 Mobile Safari/537.36"
    }
}
