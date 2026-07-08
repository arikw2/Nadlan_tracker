package il.arik.nadlantracker.feature.macro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import il.arik.nadlantracker.core.ui.appContainer
import il.arik.nadlantracker.data.repository.CbsRepository
import il.arik.nadlantracker.domain.model.IndexSeries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MacroUiState {
    data object Loading : MacroUiState

    data class Data(
        val dwellingSeries: IndexSeries,
        /** Null when the rent series failed to load — prices still render. */
        val rentSeries: IndexSeries?,
        val isStale: Boolean,
    ) : MacroUiState

    data object Error : MacroUiState
}

class MacroViewModel(private val repository: CbsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<MacroUiState>(MacroUiState.Loading)
    val uiState: StateFlow<MacroUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load(forceRefresh: Boolean = false) {
        _uiState.value = MacroUiState.Loading
        viewModelScope.launch {
            try {
                val dwelling = repository.dwellingIndex(forceRefresh)
                val rent = runCatching { repository.rentIndex(forceRefresh) }.getOrNull()
                _uiState.value = MacroUiState.Data(
                    dwellingSeries = dwelling.series,
                    rentSeries = rent?.series,
                    isStale = dwelling.isStale,
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = MacroUiState.Error
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { MacroViewModel(appContainer().cbsRepository) }
        }
    }
}
