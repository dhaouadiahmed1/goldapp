package app.goldapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.goldapp.data.ChartRange
import app.goldapp.data.GoldRepository
import app.goldapp.data.GoldState
import app.goldapp.data.MarketAsset
import app.goldapp.data.MarketDetail
import app.goldapp.data.MarketQuote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GoldViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GoldRepository(application.applicationContext)

    private val _state = MutableStateFlow(repository.cachedOrEmpty().copy(isLoading = true))
    val state: StateFlow<GoldState> = _state.asStateFlow()

    // Markets tab (v0.6): live quotes for all assets + the open asset's detail.
    private val _marketQuotes = MutableStateFlow<List<MarketQuote>>(emptyList())
    val marketQuotes: StateFlow<List<MarketQuote>> = _marketQuotes.asStateFlow()

    private val _marketDetail = MutableStateFlow<MarketDetail?>(null)
    val marketDetail: StateFlow<MarketDetail?> = _marketDetail.asStateFlow()

    init {
        refresh()
        loadMarkets()
    }

    fun loadMarkets() {
        viewModelScope.launch {
            _marketQuotes.value = repository.fetchMarketQuotes()
        }
    }

    fun loadAssetDetail(asset: MarketAsset, range: ChartRange) {
        _marketDetail.value = MarketDetail(asset = asset, range = range, loading = true)
        viewModelScope.launch {
            _marketDetail.value = repository.fetchMarketDetail(asset, range)
        }
    }

    fun clearAssetDetail() {
        _marketDetail.value = null
    }

    fun refresh() {
        val current = _state.value
        _state.value = current.copy(isRefreshing = true, isLoading = current.spotPrice == null)
        viewModelScope.launch {
            val fresh = repository.refresh()
            _state.value = fresh.copy(isLoading = false, isRefreshing = false)
        }
    }

    /** Simple words for the trend, from the futures daily change. No prediction. */
    fun trendWords(): String {
        val change = _state.value.futuresChangePercent
        return when {
            change == null -> "Trend: not available yet — tap Refresh."
            change > 0.5 -> "Up today — gold is higher than yesterday."
            change < -0.5 -> "Down today — gold is lower than yesterday."
            else -> "Sideways today — gold is almost flat vs yesterday."
        }
    }

    fun whyItMoved(): String {
        val s = _state.value
        val parts = mutableListOf<String>()
        s.dxyChangePercent?.let {
            parts.add(if (it > 0) "The dollar (DXY) is up, which usually pushes gold down."
            else if (it < 0) "The dollar (DXY) is down, which usually helps gold."
            else "The dollar (DXY) is almost flat.")
        }
        s.yieldChangePercent?.let {
            parts.add(if (it > 0) "US 10-year yields are up, which usually pushes gold down."
            else if (it < 0) "US 10-year yields are down, which usually helps gold."
            else "US 10-year yields are almost flat.")
        }
        if (parts.isEmpty()) return "Why it moved: not available yet — tap Refresh to load the dollar and yields."
        return "Why it moved: " + parts.joinToString(" ")
    }
}
