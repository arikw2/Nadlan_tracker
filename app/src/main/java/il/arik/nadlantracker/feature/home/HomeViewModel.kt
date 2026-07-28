package il.arik.nadlantracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import il.arik.nadlantracker.core.network.NetworkModule
import il.arik.nadlantracker.core.ui.appContainer
import il.arik.nadlantracker.data.repository.CbsRepository
import il.arik.nadlantracker.data.repository.DealsRepository
import il.arik.nadlantracker.data.repository.FavoritesRepository
import il.arik.nadlantracker.domain.TrendCalculator
import il.arik.nadlantracker.domain.model.IndexSeries
import il.arik.nadlantracker.domain.model.SearchQuery
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class IndexBlock(
    val dwelling: IndexSeries,
    val latestValue: Double,
    val yoy: Double?,
    val rentYoy: Double?,
    val period: YearMonth,
    val isStale: Boolean,
)

data class AreaSummary(
    val id: Long,
    val queryJson: String,
    val displayName: String,
    val city: String?,
    val dealsCount: Int,
    val medianPrice: Long?,
    val pricePerSqm: Double?,
    val deltaPercent: Double?,
    val sparkline: List<Double>,
    val alertsEnabled: Boolean,
    val broken: Boolean,
    val lastRunAtEpochMs: Long?,
)

data class HomeUiState(
    val indexLoading: Boolean = true,
    val index: IndexBlock? = null,
    val areasLoading: Boolean = true,
    val areas: List<AreaSummary> = emptyList(),
    val heroRangeYears: Int = 5,
)

class HomeViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val dealsRepository: DealsRepository,
    private val cbsRepository: CbsRepository,
    private val json: Json = NetworkModule.json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadIndex()
        observeAreas()
    }

    fun setHeroRange(years: Int) = _uiState.update { it.copy(heroRangeYears = years) }

    fun setAlertsEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { favoritesRepository.setAlertsEnabled(id, enabled) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { favoritesRepository.delete(id) }
    }

    private fun loadIndex() {
        viewModelScope.launch {
            try {
                val dwelling = cbsRepository.dwellingIndex()
                val rent = runCatching { cbsRepository.rentIndex() }.getOrNull()
                val latest = dwelling.series.points.lastOrNull()
                _uiState.update {
                    it.copy(
                        indexLoading = false,
                        index = latest?.let { p ->
                            IndexBlock(
                                dwelling = dwelling.series,
                                latestValue = p.value,
                                yoy = p.yearlyChangePercent,
                                rentYoy = rent?.series?.points?.lastOrNull()?.yearlyChangePercent,
                                period = p.period,
                                isStale = dwelling.isStale,
                            )
                        },
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { it.copy(indexLoading = false) }
            }
        }
    }

    private fun observeAreas() {
        viewModelScope.launch {
            favoritesRepository.observeAll().collect { favorites ->
                // Show the names immediately, then enrich each with cached deal stats.
                _uiState.update { state ->
                    state.copy(
                        areasLoading = false,
                        areas = favorites.map { fav ->
                            state.areas.firstOrNull { it.id == fav.id }
                                ?.copy(
                                    displayName = fav.displayName,
                                    alertsEnabled = fav.alertsEnabled,
                                    lastRunAtEpochMs = fav.lastRunAtEpochMs,
                                )
                                ?: AreaSummary(
                                    id = fav.id,
                                    queryJson = fav.query?.let { encode(it) } ?: "",
                                    displayName = fav.displayName,
                                    city = null,
                                    dealsCount = 0,
                                    medianPrice = null,
                                    pricePerSqm = null,
                                    deltaPercent = null,
                                    sparkline = emptyList(),
                                    alertsEnabled = fav.alertsEnabled,
                                    broken = fav.query == null,
                                    lastRunAtEpochMs = fav.lastRunAtEpochMs,
                                )
                        },
                    )
                }
                favorites.forEach { fav -> fav.query?.let { enrich(fav.id, it) } }
            }
        }
    }

    private fun enrich(id: Long, query: SearchQuery) {
        viewModelScope.launch {
            val deals = runCatching { dealsRepository.search(query).deals }.getOrNull() ?: return@launch
            if (deals.isEmpty()) return@launch
            val prices = deals.map { it.priceIls.toDouble() }
            val psm = deals.mapNotNull { it.pricePerSqm }
            val monthly = TrendCalculator.medianPriceByMonth(deals).points
            val delta = if (monthly.size >= 2) {
                val first = monthly.first().value
                val last = monthly.last().value
                if (first > 0) (last - first) / first * 100 else null
            } else null
            val city = deals.mapNotNull { it.city }
                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            _uiState.update { state ->
                state.copy(
                    areas = state.areas.map { a ->
                        if (a.id != id) a else a.copy(
                            city = city,
                            dealsCount = deals.size,
                            medianPrice = TrendCalculator.median(prices).toLong(),
                            pricePerSqm = psm.takeIf { it.isNotEmpty() }?.let { TrendCalculator.median(it) },
                            deltaPercent = delta,
                            sparkline = monthly.takeLast(12).map { it.value },
                        )
                    },
                )
            }
        }
    }

    private fun encode(query: SearchQuery): String =
        json.encodeToString(SearchQuery.serializer(), query)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                HomeViewModel(c.favoritesRepository, c.dealsRepository, c.cbsRepository)
            }
        }
    }
}
