package il.arik.nadlantracker.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    /**
     * Tolerant by design: the government APIs are undocumented and their
     * schemas drift. Unknown keys are ignored, quoted numbers coerce, and
     * missing fields fall back to DTO defaults.
     */
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(BrowserHeadersInterceptor())
        .addInterceptor(RateLimitInterceptor())
        .addInterceptor(RetryInterceptor())
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun govMapApi(client: OkHttpClient, baseUrl: String = "https://www.govmap.gov.il/"): GovMapApi =
        retrofit(client, baseUrl).create(GovMapApi::class.java)

    fun cbsApi(client: OkHttpClient, baseUrl: String = "https://api.cbs.gov.il/"): CbsApi =
        retrofit(client, baseUrl).create(CbsApi::class.java)

    private fun retrofit(client: OkHttpClient, baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
