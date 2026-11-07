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
    val closes: List<Float> = emptyList(),
    val prevClose: Double? = null
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

/** Markets tab (v0.6, LiveMarkets). */
enum class MarketCategory(val label: String) { METALS("Metals"), CRYPTO("Crypto"), FOREX("Forex") }

enum class ChartRange(val label: String, val yahooRange: String, val interval: String) {
    DAY("1D", "1d", "15m"),
    WEEK("1W", "5d", "1d"),
    MONTH("1M", "1mo", "1d")
}

data class MarketAsset(
    val id: String,
    val name: String,
    val symbol: String,
    val yahooSymbol: String,
    val category: MarketCategory,
    val newsQuery: String,
    val moves: String,
    val decimals: Int,
    val dollarPrefix: Boolean = true,
    /** gold-api spot symbol (XAU / XAG) when the live price should come from spot, not futures. */
    val spotSymbol: String? = null
)

val MARKET_ASSETS = listOf(
    MarketAsset("gold", "Gold", "XAU/USD", "GC=F", MarketCategory.METALS, "gold price",
        "Dollar up or US yields up usually pushes gold down. Inflation, Fed cuts and fear usually help it.",
        decimals = 2, spotSymbol = "XAU"),
    MarketAsset("silver", "Silver", "XAG/USD", "SI=F", MarketCategory.METALS, "silver price",
        "Silver follows gold, but moves more: it is also an industrial metal, so factory and solar demand matter.",
        decimals = 2, spotSymbol = "XAG"),
    MarketAsset("btc", "Bitcoin", "BTC/USD", "BTC-USD", MarketCategory.CRYPTO, "Bitcoin price",
        "Bitcoin moves on risk appetite, ETF flows, Fed rate expectations and crypto-specific news. It trades 24/7.",
        decimals = 2),
    MarketAsset("eth", "Ethereum", "ETH/USD", "ETH-USD", MarketCategory.CRYPTO, "Ethereum price",
        "Ethereum follows Bitcoin, plus its own upgrades, staking and ETF flows. It trades 24/7.",
        decimals = 2),
    MarketAsset("sol", "Solana", "SOL/USD", "SOL-USD", MarketCategory.CRYPTO, "Solana price",
        "Solana is more volatile than Bitcoin or Ethereum and follows crypto market mood closely. It trades 24/7.",
        decimals = 2),
    MarketAsset("eurusd", "Euro / Dollar", "EUR/USD", "EURUSD=X", MarketCategory.FOREX, "EUR USD",
        "EUR/USD moves on the gap between US and European interest rates, and on ECB vs Fed speeches.",
        decimals = 4, dollarPrefix = false),
    MarketAsset("gbpusd", "Pound / Dollar", "GBP/USD", "GBPUSD=X", MarketCategory.FOREX, "GBP USD",
        "GBP/USD moves on UK vs US interest rates, UK jobs and inflation data, and Bank of England decisions.",
        decimals = 4, dollarPrefix = false),
    MarketAsset("usdjpy", "Dollar / Yen", "USD/JPY", "USDJPY=X", MarketCategory.FOREX, "USD JPY",
        "USD/JPY moves on the gap between US and Japanese rates. A strong dollar usually pushes it up.",
        decimals = 2, dollarPrefix = false)
)

data class MarketQuote(
    val asset: MarketAsset,
    val price: Double? = null,
    val changePercent: Double? = null,
    val failed: Boolean = false
)

data class MarketDetail(
    val asset: MarketAsset,
    val price: Double? = null,
    val changePercent: Double? = null,
    val closes: List<Float> = emptyList(),
    val news: List<NewsItem> = emptyList(),
    val range: ChartRange = ChartRange.DAY,
    val loading: Boolean = false,
    val error: String? = null
)
