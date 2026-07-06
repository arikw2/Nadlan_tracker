package il.arik.nadlantracker.feature.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import il.arik.nadlantracker.core.network.NetworkModule
import il.arik.nadlantracker.core.ui.appContainer
import il.arik.nadlantracker.data.repository.DealsRepository
import il.arik.nadlantracker.data.repository.FavoritesRepository
import il.arik.nadlantracker.domain.TrendCalculator
import il.arik.nadlantracker.domain.model.Deal
import il.arik.nadlantracker.domain.model.SearchQuery
import il.arik.nadlantracker.domain.model.TrendSeries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

sealed interface ResultsUiState {
    data object Loading : ResultsUiState

    data class Data(
        val query: SearchQuery,
        val deals: List<Deal>,
        val isStale: Boolean,
        val medianPrice: TrendSeries,
        val pricePerSqm: TrendSeries,
        val dealCount: TrendSeries,
        val favoriteSaved: Boolean = false,
    ) : ResultsUiState

    data class Error(val query: SearchQuery?) : ResultsUiState
}

class ResultsViewModel(
    private val queryJson: String,
    private val initialForceRefresh: Boolean,
    private val favoriteId: Long?,
    private val dealsRepository: DealsRepository,
    private val favoritesRepository: FavoritesRepository,
    private val json: Json = NetworkModule.json,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultsUiState>(ResultsUiState.Loading)
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    private val query: SearchQuery? =
        runCatching { json.decodeFromString(SearchQuery.serializer(), queryJson) }.getOrNull()

    init {
        load(forceRefresh = initialForceRefresh)
    }

    fun refresh() = load(forceRefresh = true)

    private fun load(forceRefresh: Boolean) {
        val query = query ?: run {
            _uiState.value = ResultsUiState.Error(null)
            return
        }
        _uiState.value = ResultsUiState.Loading
        viewModelScope.launch {
            try {
                val result = dealsRepository.search(query, forceRefresh)
                favoriteId?.let { favoritesRepository.markRun(it) }
                _uiState.value = ResultsUiState.Data(
                    query = query,
                    deals = result.deals,
                    isStale = result.isStale,
                    medianPrice = TrendCalculator.medianPriceByMonth(result.deals),
                    pricePerSqm = TrendCalculator.medianPricePerSqmByMonth(result.deals),
                    dealCount = TrendCalculator.dealCountByMonth(result.deals),
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = ResultsUiState.Error(query)
            }
        }
    }

    fun saveFavorite(name: String) {
        val query = query ?: return
        viewModelScope.launch {
            favoritesRepository.save(name.ifBlank { query.displayName }, query)
            _uiState.update { state ->
                if (state is ResultsUiState.Data) state.copy(favoriteSaved = true) else state
            }
        }
    }

    companion object {
        fun factory(queryJson: String, forceRefresh: Boolean, favoriteId: Long?) = viewModelFactory {
            initializer {
                val container = appContainer()
                ResultsViewModel(
                    queryJson = queryJson,
                    initialForceRefresh = forceRefresh,
                    favoriteId = favoriteId,
                    dealsRepository = container.dealsRepository,
                    favoritesRepository = container.favoritesRepository,
                )
            }
        }
    }
}
