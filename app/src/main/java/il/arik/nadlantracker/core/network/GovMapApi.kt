package il.arik.nadlantracker.core.network

import il.arik.nadlantracker.data.dto.govmap.AutocompleteRequest
import il.arik.nadlantracker.data.dto.govmap.AutocompleteResponse
import il.arik.nadlantracker.data.dto.govmap.DealsResponse
import il.arik.nadlantracker.data.dto.govmap.RadiusBuildingDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Public govmap.gov.il API backing nadlan.gov.il transaction data.
 * Contract discovered by probing (see scripts/probe_api.sh). Server-side
 * date filters are ignored by these endpoints — filtering happens client-side.
 */
interface GovMapApi {

    @POST("api/search-service/autocomplete")
    suspend fun autocomplete(@Body body: AutocompleteRequest): AutocompleteResponse

    /** [point] is "x,y" in EPSG:3857, e.g. "3871101.97,3774606.05". */
    @GET("api/real-estate/deals/{point}/{radius}")
    suspend fun dealsByRadius(
        @Path("point") point: String,
        @Path("radius") radius: Int,
    ): List<RadiusBuildingDto>

    /**
     * Server-side filters (probed from the govmap frontend, all optional):
     * [startDate]/[endDate] as "YYYY-MM-DD", [roomNums] as a comma list
     * ("3,3.5,4"), [propertyType] as the Hebrew description (e.g. "דירה").
     * The frontend also sends dealType, but the server 500s on it.
     */
    @GET("api/real-estate/street-deals/{polygonId}")
    suspend fun streetDeals(
        @Path("polygonId") polygonId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("roomNums") roomNums: String? = null,
        @Query("propertyType") propertyType: String? = null,
    ): DealsResponse

    @GET("api/real-estate/neighborhood-deals/{polygonId}")
    suspend fun neighborhoodDeals(
        @Path("polygonId") polygonId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("roomNums") roomNums: String? = null,
        @Query("propertyType") propertyType: String? = null,
    ): DealsResponse

    /** Deals across the whole settlement (city/town) containing the polygon. */
    @GET("api/real-estate/settlement-deals/{polygonId}")
    suspend fun settlementDeals(
        @Path("polygonId") polygonId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("roomNums") roomNums: String? = null,
        @Query("propertyType") propertyType: String? = null,
    ): DealsResponse
}
