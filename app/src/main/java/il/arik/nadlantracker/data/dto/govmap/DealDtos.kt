package il.arik.nadlantracker.data.dto.govmap

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/**
 * One building/parcel returned by GET /api/real-estate/deals/{x},{y}/{radius}.
 * The endpoint returns a bare JSON array of these.
 */
@Serializable
data class RadiusBuildingDto(
    val dealscount: JsonPrimitive? = null,
    val settlementNameHeb: String? = null,
    val streetNameHeb: String? = null,
    val houseNum: String? = null,
    @SerialName("polygon_id") val polygonId: String? = null,
    val objectid: Long? = null,
)

/** Envelope of street-deals / neighborhood-deals. totalCount arrives as a string. */
@Serializable
data class DealsResponse(
    val totalCount: JsonPrimitive? = null,
    val data: List<DealDto> = emptyList(),
)

/**
 * A single transaction. All fields nullable — the upstream schema is not
 * officially documented and rows with partial data are common (e.g. duplicate
 * source records with null propertyTypeDescription).
 */
@Serializable
data class DealDto(
    val objectid: Long? = null,
    val dealId: Long? = null,
    val dealAmount: JsonPrimitive? = null,
    val dealDate: String? = null,
    val assetArea: Double? = null,
    val assetRoomNum: Double? = null,
    val floorNo: String? = null,
    val propertyTypeDescription: String? = null,
    val dealNatureDescription: String? = null,
    val settlementId: Long? = null,
    val settlementNameHeb: String? = null,
    val settlementNameEng: String? = null,
    val streetCode: String? = null,
    val streetNameHeb: String? = null,
    val streetNameEng: String? = null,
    val houseNum: String? = null,
    val neighborhood: String? = null,
    val gushNum: Long? = null,
    val parcelNum: Long? = null,
    val subParcelNum: Long? = null,
    val polygonId: String? = null,
    val sourceorder: Int? = null,
)
