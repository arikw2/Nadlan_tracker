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

    @Serializable
    object Macro

    @Serializable
    object Settings
}
