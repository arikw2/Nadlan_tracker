package il.arik.nadlantracker.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import il.arik.nadlantracker.core.network.NetworkModule
import il.arik.nadlantracker.core.ui.appContainer
import il.arik.nadlantracker.data.repository.GeoRepository
import il.arik.nadlantracker.domain.model.SearchQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** A one-tap seed area: resolved to a settlement search when picked. */
data class SeedArea(val name: String, val city: String)

data class OnboardingUiState(
    val resolvingName: String? = null,
    val failedName: String? = null,
    /** One-shot: Json of the resolved SearchQuery, consumed by navigation. */
    val pendingQueryJson: String? = null,
)

class OnboardingViewModel(
    private val geoRepository: GeoRepository,
    private val json: Json = NetworkModule.json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val seeds = listOf(
        SeedArea("תל אביב–יפו", "מחוז תל אביב"),
        SeedArea("ירושלים", "מחוז ירושלים"),
        SeedArea("חיפה", "מחוז חיפה"),
        SeedArea("ראשון לציון", "מחוז המרכז"),
        SeedArea("באר שבע", "מחוז הדרום"),
        SeedArea("רמת גן", "מחוז תל אביב"),
    )

    fun pick(seed: SeedArea) {
        if (_uiState.value.resolvingName != null) return
        _uiState.update { it.copy(resolvingName = seed.name, failedName = null) }
        viewModelScope.launch {
            val query = runCatching {
                val candidate = geoRepository.autocomplete(seed.name).firstOrNull() ?: return@runCatching null
                val polygonId = geoRepository.resolvePolygonId(candidate.x, candidate.y, null) ?: return@runCatching null
                SearchQuery.Settlement(polygonId, seed.name)
            }.getOrNull()
            if (query == null) {
                _uiState.update { it.copy(resolvingName = null, failedName = seed.name) }
            } else {
                _uiState.update {
                    it.copy(
                        resolvingName = null,
                        pendingQueryJson = json.encodeToString(SearchQuery.serializer(), query),
                    )
                }
            }
        }
    }

    fun onNavigated() = _uiState.update { it.copy(pendingQueryJson = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer { OnboardingViewModel(appContainer().geoRepository) }
        }
    }
}
