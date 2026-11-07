package app.goldapp.data

data class NewsItem(
    val title: String,
    val source: String,
    val time: String,
    val link: String
)

data class YahooQuote(
    val symbol: String,
    val price: Double?,
    val changePercent: Double?,
    val closes: List<Float> = emptyList()
)

data class GoldState(
    val spotPrice: Double? = null,
    val spotUpdatedAt: String? = null,
    val futuresPrice: Double? = null,
    val futuresChangePercent: Double? = null,
    val chartCloses: List<Float> = emptyList(),
    val dxyPrice: Double? = null,
    val dxyChangePercent: Double? = null,
    val yield10y: Double? = null,
    val yieldChangePercent: Double? = null,
    val news: List<NewsItem> = emptyList(),
    val lastUpdatedTunis: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val fromCache: Boolean = false
)
