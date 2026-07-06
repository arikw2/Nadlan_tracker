package il.arik.nadlantracker.data.dto.govmap

import kotlinx.serialization.Serializable

@Serializable
data class AutocompleteRequest(
    val searchText: String,
    val language: String = "he",
    val isAccurate: Boolean = false,
    val maxResults: Int = 10,
)

@Serializable
data class AutocompleteResponse(
    val resultsCount: Int? = null,
    val results: List<AutocompleteResultDto> = emptyList(),
)

/**
 * A single autocomplete candidate. `shape` is a WKT point in EPSG:3857,
 * e.g. "POINT(3871101.97 3774606.05)". `type` is one of:
 * street / address / poi / institutes / settlement.
 */
@Serializable
data class AutocompleteResultDto(
    val id: String? = null,
    val text: String? = null,
    val type: String? = null,
    val score: Double? = null,
    val shape: String? = null,
    val originalText: String? = null,
)
