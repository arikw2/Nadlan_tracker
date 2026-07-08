package il.arik.nadlantracker.feature.nav

import kotlinx.serialization.Serializable

object NavRoutes {

    @Serializable
    object Search

    /** [queryJson] is a Json-encoded [il.arik.nadlantracker.domain.model.SearchQuery]. */
    @Serializable
    data class Results(val queryJson: String, val forceRefresh: Boolean = false, val favoriteId: Long = -1)

    @Serializable
    object Favorites

    /** Two Json-encoded SearchQuery values to overlay on shared charts. */
    @Serializable
    data class Compare(val queryJsonA: String, val queryJsonB: String)

    @Serializable
    object Macro

    @Serializable
    object Settings
}
