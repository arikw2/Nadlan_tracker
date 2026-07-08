package il.arik.nadlantracker.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import il.arik.nadlantracker.core.network.NetworkModule
import il.arik.nadlantracker.core.ui.appContainer
import il.arik.nadlantracker.data.repository.FavoritesRepository
import il.arik.nadlantracker.domain.model.SearchQuery
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class FavoritesViewModel(
    private val repository: FavoritesRepository,
    private val json: Json = NetworkModule.json,
) : ViewModel() {

    val favorites: StateFlow<List<FavoritesRepository.Favorite>?> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun encodeQuery(query: SearchQuery): String =
        json.encodeToString(SearchQuery.serializer(), query)

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun setAlertsEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setAlertsEnabled(id, enabled) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { FavoritesViewModel(appContainer().favoritesRepository) }
        }
    }
}
