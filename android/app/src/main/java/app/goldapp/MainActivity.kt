package app.goldapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.goldapp.data.GoldState
import app.goldapp.data.NewsItem
import app.goldapp.ui.GoldViewModel
import app.goldapp.ui.theme.GoldAppTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GoldViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoldAppTheme {
                GoldApp(viewModel)
            }
        }
    }
}

private enum class Tab(val label: String) { HOME("Home"), NEWS("News"), CALENDAR("Calendar"), CHECK("Daily Check") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldApp(viewModel: GoldViewModel) {
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableStateOf(Tab.HOME) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GoldApp  •  XAU/USD") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.HOME,
                    onClick = { tab = Tab.HOME },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = tab == Tab.NEWS,
                    onClick = { tab = Tab.NEWS },
                    icon = { Icon(Icons.Filled.Article, contentDescription = null) },
                    label = { Text("News") }
                )
                NavigationBarItem(
                    selected = tab == Tab.CALENDAR,
                    onClick = { tab = Tab.CALENDAR },
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    label = { Text("Calendar") }
                )
                NavigationBarItem(
                    selected = tab == Tab.CHECK,
                    onClick = { tab = Tab.CHECK },
                    icon = { Icon(Icons.Filled.Assessment, contentDescription = null) },
                    label = { Text("Daily Check") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            when (tab) {
                Tab.HOME -> HomeScreen(state, onRefresh = { viewModel.refresh() })
                Tab.NEWS -> NewsScreen(state)
                Tab.CALENDAR -> CalendarScreen()
                Tab.CHECK -> DailyCheckScreen(state, viewModel)
            }
        }
    }
}

private fun fmtPrice(v: Double?): String =
    if (v == null) "—" else "$" + String.format("%,.2f", v)

private fun fmtPct(v: Double?): String =
    if (v == null) "—" else String.format("%+.2f%%", v)

@Composable
fun HomeScreen(state: GoldState, onRefresh: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Gold  •  XAU/USD (spot, per ounce)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (state.isLoading && state.spotPrice == null) {
                        Text("Loading live price…")
                    } else {
                        Text(
                            fmtPrice(state.spotPrice),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Futures today: ${fmtPrice(state.futuresPrice)}  (${fmtPct(state.futuresChangePercent)})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (state.fromCache) "Last saved price — updated ${state.lastUpdatedTunis ?: "—"} (Tunis time). Tap Refresh for live data."
                            else "Updated ${state.lastUpdatedTunis ?: "—"} (Tunis time)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    state.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Color(0xFFFFB74D), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onRefresh, enabled = !state.isRefreshing) {
                        Text(if (state.isRefreshing) "Refreshing…" else "Refresh price")
                    }
                    Text(
                        "Spot price: gold-api.com. Futures + chart: Yahoo Finance (GC=F). Prices are indicative spot / futures, not a buy price from a shop.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Gold price — last 30 days", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (state.chartCloses.size >= 2) {
                        PriceChart(state.chartCloses)
                    } else {
                        Text("Chart not loaded yet — tap Refresh.")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Dollar & Rates — what moves gold", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("US Dollar Index (DXY): ${state.dxyPrice?.let { String.format("%.2f", it) } ?: "—"}  (${fmtPct(state.dxyChangePercent)})")
                    Text(
                        "Dollar up → gold usually down. Dollar down → gold usually up.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("US 10-year yield: ${state.yield10y?.let { String.format("%.2f%%", it) } ?: "—"}  (${fmtPct(state.yieldChangePercent)})")
                    Text(
                        "Yields up → gold usually down. Yields down → gold usually up.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Source: Yahoo Finance (DX-Y.NYB, ^TNX).", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tradingview.com/symbols/XAUUSD/")))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Open full chart on TradingView") }
        }
    }
}

@Composable
fun PriceChart(closes: List<Float>) {
    val gold = Color(0xFFD4AF37)
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        val min = closes.minOrNull() ?: 0f
        val max = closes.maxOrNull() ?: 1f
        val range = (max - min).takeIf { it > 0 } ?: 1f
        val stepX = size.width / (closes.size - 1).coerceAtLeast(1)
        val path = Path()
        closes.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - min) / range) * size.height * 0.9f - size.height * 0.05f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = gold, style = Stroke(width = 4f, cap = StrokeCap.Round))
        val last = closes.last()
        val lastY = size.height - ((last - min) / range) * size.height * 0.9f - size.height * 0.05f
        drawCircle(color = gold, radius = 10f, center = Offset(size.width, lastY))
    }
}

@Composable
fun NewsScreen(state: GoldState) {
    val context = LocalContext.current
    if (state.news.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Gold news", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("No news loaded yet. Check your connection and tap Refresh (top right).")
            OutlinedButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://news.google.com/search?q=gold%20price&hl=en-US&gl=US&ceid=US:en")))
            }) { Text("Open gold news in browser") }
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Gold news", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { Text("Source: Google News, search \"gold price\". Tap a headline to read it.", style = MaterialTheme.typography.bodySmall) }
        items(state.news) { item: NewsItem ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.link)))
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("${item.source}  •  ${item.time}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("What moves gold most", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "v0.1 does not fetch the calendar live. The events below are the ones to watch, in simple words. Tap the button at the bottom for the live calendar in your browser.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⭐ Next verified event", fontWeight = FontWeight.Bold)
                    Text("US CPI (inflation) — Tue 10 Nov 2026, 14:30 Tunis time.")
                    Text(
                        "Checked on 7 Nov 2026. CPI is the inflation report — higher than expected usually pushes gold down, lower than expected usually helps gold.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CPI — inflation", fontWeight = FontWeight.Bold)
                    Text("Prices in the US. High inflation can mean interest rates stay high, which usually hurts gold.")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("NFP — US jobs", fontWeight = FontWeight.Bold)
                    Text("Jobs added in the US, first Friday of the month, 14:30 Tunis time. Strong jobs → dollar up → gold usually down.")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Fed decision — interest rate", fontWeight = FontWeight.Bold)
                    Text("The US central bank sets the rate. Rate cut → gold usually up. Rate hike → gold usually down.")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Fed speeches", fontWeight = FontWeight.Bold)
                    Text("What Fed officials say about future rates can move gold in minutes. Avoid trading right before them.")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.forexfactory.com/calendar")))
                }, modifier = Modifier.weight(1f)) { Text("Forex Factory") }
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.investing.com/economic-calendar/")))
                }, modifier = Modifier.weight(1f)) { Text("Investing.com") }
            }
        }
    }
}

@Composable
fun DailyCheckScreen(state: GoldState, viewModel: GoldViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Daily Check", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Same short format as the WhatsApp check. Updated ${state.lastUpdatedTunis ?: "—"} (Tunis time).", style = MaterialTheme.typography.bodySmall)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Price", fontWeight = FontWeight.Bold)
                    Text("Gold (XAU/USD): ${fmtPrice(state.spotPrice)} per ounce")
                    Text("Futures today: ${fmtPrice(state.futuresPrice)} (${fmtPct(state.futuresChangePercent)})")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Trend", fontWeight = FontWeight.Bold)
                    Text(viewModel.trendWords())
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Why it moved", fontWeight = FontWeight.Bold)
                    Text(viewModel.whyItMoved())
                    if (state.news.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Top headline: ${state.news.first().title}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("What to watch today", fontWeight = FontWeight.Bold)
                    Text("US CPI (inflation) — Tue 10 Nov 2026, 14:30 Tunis time. Check the Calendar tab for the live calendar before trading.")
                }
            }
        }
        item {
            Text(
                "Educational only — not financial advice. No buy / sell signal. No app can predict where gold will go. Practice on demo first and never risk money you cannot afford to lose.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
