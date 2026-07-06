package il.arik.nadlantracker.core.network

import il.arik.nadlantracker.data.dto.cbs.CbsIndexResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * CBS (Central Bureau of Statistics) price-index API.
 * Dwelling price index is code 40010. Pages cap at 100 rows.
 */
interface CbsApi {

    @GET("index/data/price")
    suspend fun indexData(
        @Query("id") code: Long,
        @Query("last") last: Int? = null,
        @Query("startPeriod") startPeriod: String? = null, // "MM-YYYY"
        @Query("endPeriod") endPeriod: String? = null,     // "MM-YYYY"
        @Query("page") page: Int? = null,
        @Query("format") format: String = "json",
        @Query("lang") lang: String = "he",
    ): CbsIndexResponse

    companion object {
        const val DWELLING_PRICE_INDEX_CODE = 40010L
    }
}
