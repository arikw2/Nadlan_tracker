package il.arik.nadlantracker.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Client-side deal filters. The govmap endpoints ignore date-range params,
 * so all filtering happens locally over the fetched rows.
 */
@Serializable
data class DealFilters(
    val fromYearMonth: String? = null, // "2024-01"
    val toYearMonth: String? = null,
    val roomsMin: Double? = null,
    val roomsMax: Double? = null,
    val propertyTypes: Set<String> = emptySet(),
) {
    val isEmpty: Boolean
        get() = fromYearMonth == null && toYearMonth == null &&
            roomsMin == null && roomsMax == null && propertyTypes.isEmpty()
}

/**
 * A runnable, persistable search. Favorites store this exact JSON (the
 * class discriminator doubles as the schema version anchor), and the deal
 * cache is keyed by a stable hash of it.
 */
@Serializable
sealed interface SearchQuery {
    val displayName: String
    val filters: DealFilters

    @Serializable
    @SerialName("street")
    data class Street(
        val polygonId: String,
        override val displayName: String,
        override val filters: DealFilters = DealFilters(),
    ) : SearchQuery

    @Serializable
    @SerialName("neighborhood")
    data class Neighborhood(
        val polygonId: String,
        override val displayName: String,
        override val filters: DealFilters = DealFilters(),
    ) : SearchQuery

    @Serializable
    @SerialName("radius")
    data class Radius(
        val x: Double,
        val y: Double,
        val radiusMeters: Int,
        override val displayName: String,
        override val filters: DealFilters = DealFilters(),
    ) : SearchQuery
}
