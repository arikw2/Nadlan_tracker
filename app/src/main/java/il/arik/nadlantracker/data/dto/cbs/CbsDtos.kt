package il.arik.nadlantracker.data.dto.cbs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Envelope of GET api.cbs.gov.il/index/data/price?id=40010&format=json.
 * The same series arrives in monthly and quarterly resolutions.
 */
@Serializable
data class CbsIndexResponse(
    val month: List<CbsSeriesDto> = emptyList(),
    val quarter: List<CbsSeriesDto> = emptyList(),
    val paging: CbsPagingDto? = null,
)

@Serializable
data class CbsSeriesDto(
    val code: Long? = null,
    val name: String? = null,
    val date: List<CbsPointDto> = emptyList(),
)

@Serializable
data class CbsPointDto(
    val year: Int? = null,
    val month: Int? = null,
    val monthDesc: String? = null,
    /** Month-over-month change, percent. */
    val percent: Double? = null,
    /** Year-over-year change, percent. */
    val percentYear: Double? = null,
    val currBase: CbsBaseDto? = null,
)

@Serializable
data class CbsBaseDto(
    val baseDesc: String? = null,
    val value: Double? = null,
)

@Serializable
data class CbsPagingDto(
    @SerialName("total_items") val totalItems: Int? = null,
    @SerialName("page_size") val pageSize: Int? = null,
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("last_page") val lastPage: Int? = null,
    @SerialName("next_url") val nextUrl: String? = null,
)
