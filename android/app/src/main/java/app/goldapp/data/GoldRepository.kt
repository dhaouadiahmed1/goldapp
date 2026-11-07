package app.goldapp.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Xml
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.TimeUnit

/**
 * Verified free, no-key data sources (checked 7 Nov 2026 with curl):
 * - Spot gold: https://api.gold-api.com/price/XAU  -> HTTP 200, live spot price
 * - Gold futures, DXY, 10Y yield + chart history:
 *   https://query1.finance.yahoo.com/v8/finance/chart/GC=F?range=1mo&interval=1d
 *   https://query1.finance.yahoo.com/v8/finance/chart/DX-Y.NYB?range=5d&interval=1d
 *   https://query2.finance.yahoo.com/v8/finance/chart/%5ETNX?range=5d&interval=1d
 *   All returned HTTP 200 with real prices on 7 Nov 2026.
 * - News: Google News RSS
 *   https://news.google.com/rss/search?q=gold%20price&hl=en-US&gl=US&ceid=US:en
 *   HTTP 200, real items with title / link / pubDate / source.
 *
 * NOT used (verified broken on 7 Nov 2026):
 * - Kitco RSS (kitco-gold.xml) -> 404
 * - https://api.gold-api.com/history/XAU -> 401, needs an API key
 * - Investing.com RSS news_301.rss -> cryptocurrency news, not gold
 *
 * No API key, no login, no account. Only INTERNET permission.
 * Nothing here is fake: if a source fails, the caller shows an honest
 * error and keeps the last cached price.
 */
class GoldRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("goldapp_cache", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val tunis = ZoneId.of("Africa/Tunis")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm, dd MMM yyyy", Locale.ENGLISH)

    fun nowTunis(): String =
        Instant.now().atZone(tunis).format(timeFmt)

    private fun get(url: String): String? = try {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 GoldApp/0.1")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) null else resp.body?.string()
        }
    } catch (_: Exception) {
        null
    }

    private fun fetchSpotPrice(): Double? {
        val body = get("https://api.gold-api.com/price/XAU") ?: return null
        return try {
            val json = JSONObject(body)
            val price = json.optDouble("price", Double.NaN)
            if (price.isNaN() || price <= 0) null else price
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchYahoo(symbol: String, range: String, host: String = "query1"): YahooQuote? {
        val encoded = URLEncoder.encode(symbol, "UTF-8").replace("+", "%20")
        val url = "https://$host.finance.yahoo.com/v8/finance/chart/$encoded?range=$range&interval=1d"
        val body = get(url) ?: return null
        return try {
            val result = JSONObject(body)
                .getJSONObject("chart")
                .getJSONArray("result")
                .getJSONObject(0)
            val meta = result.getJSONObject("meta")
            val price = if (meta.has("regularMarketPrice") && !meta.isNull("regularMarketPrice"))
                meta.getDouble("regularMarketPrice") else null
            val change = if (meta.has("regularMarketChangePercent") && !meta.isNull("regularMarketChangePercent"))
                meta.getDouble("regularMarketChangePercent") else null
            val closes = mutableListOf<Float>()
            try {
                val quote = result.getJSONObject("indicators")
                    .getJSONArray("quote").getJSONObject(0)
                val arr = quote.getJSONArray("close")
                for (i in 0 until arr.length()) {
                    if (!arr.isNull(i)) closes.add(arr.getDouble(i).toFloat())
                }
            } catch (_: Exception) {
                // closes are optional; price/change are the important part
            }
            YahooQuote(symbol, price, change, closes)
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchNews(): List<NewsItem> {
        val url = "https://news.google.com/rss/search?q=gold%20price&hl=en-US&gl=US&ceid=US:en"
        val body = get(url) ?: return emptyList()
        val items = mutableListOf<NewsItem>()
        try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(body.byteInputStream(Charsets.UTF_8), null)
            var event = parser.eventType
            var inItem = false
            var title = ""
            var link = ""
            var pubDate = ""
            var source = ""
            while (event != XmlPullParser.END_DOCUMENT && items.size < 20) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "item" -> { inItem = true; title = ""; link = ""; pubDate = ""; source = "" }
                            "title" -> if (inItem) title = parser.nextText()
                            "link" -> if (inItem) link = parser.nextText()
                            "pubDate" -> if (inItem) pubDate = parser.nextText()
                            "source" -> if (inItem) source = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item" && inItem) {
                            inItem = false
                            if (title.isNotBlank() && link.isNotBlank()) {
                                items.add(NewsItem(title.trim(), source.ifBlank { "Google News" }, pubDate, link.trim()))
                            }
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return items
    }

    private fun saveCache(state: GoldState) {
        prefs.edit()
            .putString("spot_price", state.spotPrice?.toString())
            .putString("spot_updated", state.spotUpdatedAt)
            .putString("futures_price", state.futuresPrice?.toString())
            .putString("futures_change", state.futuresChangePercent?.toString())
            .putString("dxy_price", state.dxyPrice?.toString())
            .putString("dxy_change", state.dxyChangePercent?.toString())
            .putString("yield_10y", state.yield10y?.toString())
            .putString("yield_change", state.yieldChangePercent?.toString())
            .putString("chart_closes", state.chartCloses.joinToString(","))
            .putString("last_updated", state.lastUpdatedTunis)
            .apply()
    }

    private fun loadCache(): GoldState {
        val spot = prefs.getString("spot_price", null)?.toDoubleOrNull()
        if (spot == null) return GoldState()
        return GoldState(
            spotPrice = spot,
            spotUpdatedAt = prefs.getString("spot_updated", null),
            futuresPrice = prefs.getString("futures_price", null)?.toDoubleOrNull(),
            futuresChangePercent = prefs.getString("futures_change", null)?.toDoubleOrNull(),
            chartCloses = prefs.getString("chart_closes", "")
                ?.split(",")?.mapNotNull { it.toFloatOrNull() } ?: emptyList(),
            dxyPrice = prefs.getString("dxy_price", null)?.toDoubleOrNull(),
            dxyChangePercent = prefs.getString("dxy_change", null)?.toDoubleOrNull(),
            yield10y = prefs.getString("yield_10y", null)?.toDoubleOrNull(),
            yieldChangePercent = prefs.getString("yield_change", null)?.toDoubleOrNull(),
            lastUpdatedTunis = prefs.getString("last_updated", null),
            fromCache = true
        )
    }

    suspend fun refresh(): GoldState = withContext(Dispatchers.IO) {
        val cached = loadCache()
        val spot = fetchSpotPrice()
        val goldFutures = fetchYahoo("GC=F", "1mo")
        val dxy = fetchYahoo("DX-Y.NYB", "5d")
        val tnx = fetchYahoo("^TNX", "5d", host = "query2")
        val news = fetchNews()

        // If everything failed, keep the cache and say so honestly.
        if (spot == null && goldFutures == null && dxy == null && tnx == null && news.isEmpty()) {
            return@withContext cached.copy(
                error = "Couldn't load live data. Check your connection and tap Refresh.",
                fromCache = cached.spotPrice != null
            )
        }

        val state = GoldState(
            spotPrice = spot ?: cached.spotPrice,
            futuresPrice = goldFutures?.price ?: cached.futuresPrice,
            futuresChangePercent = goldFutures?.changePercent ?: cached.futuresChangePercent,
            chartCloses = if (goldFutures != null && goldFutures.closes.isNotEmpty())
                goldFutures.closes else cached.chartCloses,
            dxyPrice = dxy?.price ?: cached.dxyPrice,
            dxyChangePercent = dxy?.changePercent ?: cached.dxyChangePercent,
            yield10y = tnx?.price ?: cached.yield10y,
            yieldChangePercent = tnx?.changePercent ?: cached.yieldChangePercent,
            news = news.ifEmpty { cached.news },
            lastUpdatedTunis = nowTunis(),
            fromCache = spot == null && cached.spotPrice != null,
            error = if (spot == null) "Live spot price couldn't load — showing last saved price." else null
        )
        saveCache(state)
        state
    }

    fun cachedOrEmpty(): GoldState = loadCache()
}
