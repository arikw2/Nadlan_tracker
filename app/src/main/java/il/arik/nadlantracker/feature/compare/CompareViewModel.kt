package il.arik.nadlantracker.feature.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import il.arik.nadlantracker.R
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

data class ComparableArea(val id: Long, val displayName: String, val query: SearchQuery)

/** [labelRes] keeps the row caption in strings.xml rather than in the ViewModel. */
data class CompareRow(val labelRes: Int, val a: String, val b: String)

/** The ₪/m² gap, for the UI to phrase. Null when either side lacks area data. */
data class PsmGap(val dearerLabel: String, val cheaperLabel: String, val percent: Double)

data class CompareResult(
    val labelA: String,
    val labelB: String,
    val priceA: TrendSeries,
    val priceB: TrendSeries,
    val rows: List<CompareRow>,
    val psmGap: PsmGap?,
    val isStale: Boolean,
)

data class CompareUiState(
    val areas: List<ComparableArea> = emptyList(),
    val selectedA: Long? = null,
    val selectedB: Long? = null,
    val loading: Boolean = false,
    val result: CompareResult? = null,
)

class CompareViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val dealsRepository: DealsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.observeAll().collect { favorites ->
                val areas = favorites.mapNotNull { f ->
                    f.query?.let { ComparableArea(f.id, f.displayName, it) }
                }
                _uiState.update { it.copy(areas = areas) }
            }
        }
    }

    /** Toggle an area into the first free slot (A then B); tapping a selected one clears it. */
    fun toggle(id: Long) {
        _uiState.update { s ->
            when (id) {
                s.selectedA -> s.copy(selectedA = null, result = null)
                s.selectedB -> s.copy(selectedB = null, result = null)
                else -> when {
                    s.selectedA == null -> s.copy(selectedA = id, result = null)
                    s.selectedB == null -> s.copy(selectedB = id, result = null)
                    else -> s // both full — ignore
                }
            }
        }
        maybeCompute()
    }

    private fun maybeCompute() {
        val s = _uiState.value
        val a = s.areas.firstOrNull { it.id == s.selectedA } ?: return
        val b = s.areas.firstOrNull { it.id == s.selectedB } ?: return
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                val resA = dealsRepository.search(a.query)
                val resB = dealsRepository.search(b.query)
                _uiState.update {
                    it.copy(
                        loading = false,
                        result = buildResult(a.displayName, b.displayName, resA.deals, resB.deals, resA.isStale || resB.isStale),
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    private fun buildResult(
        labelA: String,
        labelB: String,
        dealsA: List<Deal>,
        dealsB: List<Deal>,
        stale: Boolean,
    ): CompareResult {
        fun medianPrice(d: List<Deal>) = d.map { it.priceIls.toDouble() }.takeIf { it.isNotEmpty() }?.let(TrendCalculator::median)
        fun medianPsm(d: List<Deal>) = d.mapNotNull { it.pricePerSqm }.takeIf { it.isNotEmpty() }?.let(TrendCalculator::median)
        fun medianArea(d: List<Deal>) = d.mapNotNull { it.areaSqm }.takeIf { it.isNotEmpty() }?.let(TrendCalculator::median)

        val psmA = medianPsm(dealsA)
        val psmB = medianPsm(dealsB)
        val gap = if (psmA != null && psmB != null && psmA > 0 && psmB > 0) {
            val (dear, cheap) = if (psmA >= psmB) psmA to psmB else psmB to psmA
            PsmGap(
                dearerLabel = if (psmA >= psmB) labelA else labelB,
                cheaperLabel = if (psmA >= psmB) labelB else labelA,
                percent = (dear - cheap) / cheap * 100,
            )
        } else null

        val fmt = il.arik.nadlantracker.core.ui.Formatters
        fun money(v: Double?) = v?.let { fmt.price(it.toLong()) } ?: "—"
        fun num(v: Double?) = v?.let { fmt.number(it) } ?: "—"

        return CompareResult(
            labelA = labelA,
            labelB = labelB,
            priceA = TrendCalculator.medianPriceByMonth(dealsA),
            priceB = TrendCalculator.medianPriceByMonth(dealsB),
            rows = listOf(
                CompareRow(R.string.compare_row_median, money(medianPrice(dealsA)), money(medianPrice(dealsB))),
                CompareRow(R.string.compare_row_psm, num(psmA), num(psmB)),
                CompareRow(R.string.compare_row_area, num(medianArea(dealsA)), num(medianArea(dealsB))),
                CompareRow(R.string.compare_row_deals, dealsA.size.toString(), dealsB.size.toString()),
            ),
            psmGap = gap,
            isStale = stale,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                CompareViewModel(c.favoritesRepository, c.dealsRepository)
            }
        }
    }
}
