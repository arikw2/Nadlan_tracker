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

    @GET("api/real-estate/street-deals/{polygonId}")
    suspend fun streetDeals(
        @Path("polygonId") polygonId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): DealsResponse

    @GET("api/real-estate/neighborhood-deals/{polygonId}")
    suspend fun neighborhoodDeals(
        @Path("polygonId") polygonId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): DealsResponse
}
