package il.arik.nadlantracker.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import il.arik.nadlantracker.core.network.NetworkModule
import il.arik.nadlantracker.core.ui.appContainer
import il.arik.nadlantracker.data.repository.GeoRepository
import il.arik.nadlantracker.domain.model.DealFilters
import il.arik.nadlantracker.domain.model.LocationCandidate
import il.arik.nadlantracker.domain.model.SearchQuery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class SearchScope { STREET, NEIGHBORHOOD, RADIUS }

data class SearchUiState(
    val searchText: String = "",
    val candidates: List<LocationCandidate> = emptyList(),
    val selectedCandidate: LocationCandidate? = null,
    val scope: SearchScope = SearchScope.STREET,
    val radiusMeters: Int = 300,
    val filters: DealFilters = DealFilters(),
    val isAutocompleteLoading: Boolean = false,
    val isResolving: Boolean = false,
    val resolveFailed: Boolean = false,
    /** One-shot: Json of the resolved SearchQuery, consumed by navigation. */
    val pendingResultsQueryJson: String? = null,
)

class SearchViewModel(
    private val geoRepository: GeoRepository,
    private val json: Json = NetworkModule.json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val searchTextFlow = MutableStateFlow("")
    private var resolveJob: Job? = null

    init {
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            searchTextFlow.debounce(300).collect { text ->
                if (text.trim().length < 2) {
                    _uiState.update { it.copy(candidates = emptyList(), isAutocompleteLoading = false) }
                    return@collect
                }
                _uiState.update { it.copy(isAutocompleteLoading = true) }
                val candidates = runCatching { geoRepository.autocomplete(text) }
                    .getOrDefault(emptyList())
                _uiState.update { it.copy(candidates = candidates, isAutocompleteLoading = false) }
            }
        }
    }

    fun onSearchTextChange(text: String) {
        _uiState.update { it.copy(searchText = text, selectedCandidate = null, resolveFailed = false) }
        searchTextFlow.value = text
    }

    fun onCandidatePicked(candidate: LocationCandidate) {
        _uiState.update {
            it.copy(
                searchText = candidate.displayText,
                selectedCandidate = candidate,
                candidates = emptyList(),
                resolveFailed = false,
            )
        }
    }

    fun onScopeChange(scope: SearchScope) = _uiState.update { it.copy(scope = scope) }

    fun onRadiusChange(meters: Int) = _uiState.update { it.copy(radiusMeters = meters) }

    fun onFiltersChange(filters: DealFilters) = _uiState.update { it.copy(filters = filters) }

    /** Builds the SearchQuery (resolving a polygon when needed) off the UI thread. */
    fun onSearchClicked() {
        val state = _uiState.value
        val candidate = state.selectedCandidate ?: return
        resolveJob?.cancel()
        resolveJob = viewModelScope.launch {
            _uiState.update { it.copy(isResolving = true, resolveFailed = false) }
            val query: SearchQuery? = when (state.scope) {
                SearchScope.RADIUS -> SearchQuery.Radius(
                    x = candidate.x,
                    y = candidate.y,
                    radiusMeters = state.radiusMeters,
                    displayName = candidate.displayText,
                    filters = state.filters,
                )
                SearchScope.STREET, SearchScope.NEIGHBORHOOD -> {
                    val polygonId = runCatching {
                        geoRepository.resolvePolygonId(candidate.x, candidate.y, candidate.displayText)
                    }.getOrNull()
                    when {
                        polygonId == null -> null
                        state.scope == SearchScope.STREET -> SearchQuery.Street(
                            polygonId, candidate.displayText, state.filters,
                        )
                        else -> SearchQuery.Neighborhood(
                            polygonId, candidate.displayText, state.filters,
                        )
                    }
                }
            }
            _uiState.update {
                it.copy(
                    isResolving = false,
                    resolveFailed = query == null,
                    pendingResultsQueryJson = query?.let { q ->
                        json.encodeToString(SearchQuery.serializer(), q)
                    },
                )
            }
        }
    }

    fun onResultsNavigated() = _uiState.update { it.copy(pendingResultsQueryJson = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer { SearchViewModel(appContainer().geoRepository) }
        }
    }
}
