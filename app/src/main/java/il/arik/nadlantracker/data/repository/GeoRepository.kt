package il.arik.nadlantracker.data.repository

import il.arik.nadlantracker.core.network.GovMapApi
import il.arik.nadlantracker.data.dto.govmap.AutocompleteRequest
import il.arik.nadlantracker.data.mapper.DealMapper
import il.arik.nadlantracker.domain.model.LocationCandidate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Address search + polygon resolution. */
class GeoRepository(private val api: GovMapApi) {

    private val cacheLock = Mutex()
    private val autocompleteCache =
        object : LinkedHashMap<String, List<LocationCandidate>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<LocationCandidate>>) =
                size > 50
        }

    suspend fun autocomplete(text: String): List<LocationCandidate> {
        val key = text.trim()
        if (key.length < 2) return emptyList()
        cacheLock.withLock { autocompleteCache[key] }?.let { return it }

        val candidates = api.autocomplete(AutocompleteRequest(searchText = key))
            .results
            .mapNotNull(DealMapper::candidateFromDto)
        cacheLock.withLock { autocompleteCache[key] = candidates }
        return candidates
    }

    /**
     * Finds the parcel polygon to anchor street/neighborhood deal queries,
     * widening the search ring until buildings appear. When [preferText] is
     * given (the picked autocomplete entry), buildings on that street win.
     */
    suspend fun resolvePolygonId(x: Double, y: Double, preferText: String? = null): String? {
        for (radius in intArrayOf(50, 150, 400)) {
            val buildings = api.dealsByRadius("$x,$y", radius).filter { it.polygonId != null }
            if (buildings.isEmpty()) continue
            if (preferText != null) {
                buildings.firstOrNull { building ->
                    val street = building.streetNameHeb?.trim()
                    street != null && street.isNotEmpty() && preferText.contains(street)
                }?.let { return it.polygonId }
            }
            return buildings.first().polygonId
        }
        return null
    }
}
