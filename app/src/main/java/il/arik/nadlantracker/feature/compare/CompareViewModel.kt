package il.arik.nadlantracker.feature.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import il.arik.nadlantracker.core.network.NetworkModule
import il.arik.nadlantracker.core.ui.appContainer
import il.arik.nadlantracker.data.repository.DealsRepository
import il.arik.nadlantracker.domain.TrendCalculator
import il.arik.nadlantracker.domain.model.SearchQuery
import il.arik.nadlantracker.domain.model.TrendSeries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

sealed interface CompareUiState {
    data object Loading : CompareUiState

    data class Data(
        val labelA: String,
        val labelB: String,
        val priceA: TrendSeries,
        val priceB: TrendSeries,
        val sqmA: TrendSeries,
        val sqmB: TrendSeries,
        val isStale: Boolean,
    ) : CompareUiState

    data object Error : CompareUiState
}

class CompareViewModel(
    private val queryJsonA: String,
    private val queryJsonB: String,
    private val dealsRepository: DealsRepository,
    private val json: Json = NetworkModule.json,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CompareUiState>(CompareUiState.Loading)
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = CompareUiState.Loading
        viewModelScope.launch {
            try {
                val queryA = json.decodeFromString(SearchQuery.serializer(), queryJsonA)
                val queryB = json.decodeFromString(SearchQuery.serializer(), queryJsonB)
                // Sequential on purpose — govmap traffic is globally throttled anyway.
                val resultA = dealsRepository.search(queryA)
                val resultB = dealsRepository.search(queryB)
                _uiState.value = CompareUiState.Data(
                    labelA = queryA.displayName,
                    labelB = queryB.displayName,
                    priceA = TrendCalculator.medianPriceByMonth(resultA.deals),
                    priceB = TrendCalculator.medianPriceByMonth(resultB.deals),
                    sqmA = TrendCalculator.medianPricePerSqmByMonth(resultA.deals),
                    sqmB = TrendCalculator.medianPricePerSqmByMonth(resultB.deals),
                    isStale = resultA.isStale || resultB.isStale,
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = CompareUiState.Error
            }
        }
    }

    companion object {
        fun factory(queryJsonA: String, queryJsonB: String) = viewModelFactory {
            initializer {
                CompareViewModel(queryJsonA, queryJsonB, appContainer().dealsRepository)
            }
        }
    }
}
