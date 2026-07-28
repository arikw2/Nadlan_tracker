package il.arik.nadlantracker.feature.nav

import kotlinx.serialization.Serializable

object NavRoutes {

    /** Start destination — the tracked-areas home (מעקב). */
    @Serializable
    object Home

    @Serializable
    object Search

    /** [queryJson] is a Json-encoded [il.arik.nadlantracker.domain.model.SearchQuery]. */
    @Serializable
    data class Results(val queryJson: String, val forceRefresh: Boolean = false, val favoriteId: Long = -1)

    /** Compare is its own tab now — the two areas are picked inside the screen. */
    @Serializable
    object Compare

    @Serializable
    object Macro

    @Serializable
    object Settings

    /** First-run intro; shown once before any area is tracked. */
    @Serializable
    object Onboarding
}
