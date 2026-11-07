package app.goldapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.goldapp.data.GoldState
import app.goldapp.data.NewsItem
import app.goldapp.ui.GoldViewModel
import app.goldapp.ui.theme.GoldAccent
import app.goldapp.ui.theme.GoldAppTheme
import app.goldapp.ui.theme.GoldGreen
import app.goldapp.ui.theme.GoldPrimary
import app.goldapp.ui.theme.GoldRed

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

/** Reference level shown on the Target card. Not a prediction, not advice. */
private const val REFERENCE_TARGET = 4500.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldApp(viewModel: GoldViewModel) {
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableStateOf(Tab.HOME) }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher),
                            contentDescription = "GoldApp logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "GoldApp",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "XAU/USD  •  Spot",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldPrimary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
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
                Tab.HOME -> HomeScreen(state, onRefresh = { viewModel.refresh() }, onNavigate = { tab = it })
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

/** Rounded white card used across the app (light finance template). */
@Composable
private fun ModernCard(
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = border
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

/** Small coloured change chip: green when the value is up, red when down. */
@Composable
private fun ChangeChip(changePercent: Double?) {
    val isUp = (changePercent ?: 0.0) >= 0.0
    val chipColor = if (changePercent == null) MaterialTheme.colorScheme.surfaceVariant else if (isUp) GoldGreen else GoldRed
    Surface(
        color = chipColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = if (changePercent == null) "—" else fmtPct(changePercent),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/** DXY / 10-year yield mini-card, used on the News tab. */
@Composable
private fun MarketMiniCard(
    title: String,
    value: String,
    changePercent: Double?,
    explanation: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = GoldPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            ChangeChip(changePercent)
            Spacer(Modifier.height(8.dp))
            Text(
                explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HomeScreen(state: GoldState, onRefresh: () -> Unit, onNavigate: (Tab) -> Unit) {
    val context = LocalContext.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Gold", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(
                "You are on Top",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "of your Gold",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            ModernCard {
                Text(
                    "Gold price  •  XAU/USD",
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                if (state.isLoading && state.spotPrice == null) {
                    Text("Loading live price…")
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            fmtPrice(state.spotPrice),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        ChangeChip(state.futuresChangePercent)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Futures today: ${fmtPrice(state.futuresPrice)}  (${fmtPct(state.futuresChangePercent)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (state.fromCache) "Last saved price — updated ${state.lastUpdatedTunis ?: "—"} (Tunis time). Tap Refresh for live data."
                        else "Updated ${state.lastUpdatedTunis ?: "—"} (Tunis time)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = GoldRed, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onRefresh,
                    enabled = !state.isRefreshing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = Color(0xFF111111)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (state.isRefreshing) "Refreshing…" else "Refresh price",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Spot price: gold-api.com. Futures + chart: Yahoo Finance (GC=F). Prices are indicative spot / futures, not a buy price from a shop.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            ModernCard {
                Text("Gold price — last 30 days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (state.chartCloses.size >= 2) {
                    PriceChart(state.chartCloses)
                } else {
                    Text("Chart not loaded yet — tap Refresh.")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Spot", style = MaterialTheme.typography.labelLarge, color = GoldPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(fmtPrice(state.spotPrice), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Futures", style = MaterialTheme.typography.labelLarge, color = GoldPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(fmtPrice(state.futuresPrice), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Text(
                "Your Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f).clickable { onNavigate(Tab.NEWS) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Gold news", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Latest headlines, tap to read.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f).clickable { onNavigate(Tab.CHECK) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Daily Check", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Price • Trend • What to watch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tradingview.com/symbols/XAUUSD/")))
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Open full chart on TradingView") }
        }
    }
}

@Composable
fun PriceChart(closes: List<Float>) {
    val gold = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val min = closes.minOrNull() ?: 0f
        val max = closes.maxOrNull() ?: 1f
        val range = (max - min).takeIf { it > 0 } ?: 1f
        val stepX = size.width / (closes.size - 1).coerceAtLeast(1)
        fun yOf(v: Float): Float =
            size.height - ((v - min) / range) * size.height * 0.85f - size.height * 0.075f
        // Faint gridlines
        val gridColor = Color.Gray.copy(alpha = 0.18f)
        for (i in 1..3) {
            val gy = size.height * i / 4f
            drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
        }
        val linePath = Path()
        closes.forEachIndexed { i, v ->
            val x = i * stepX
            val y = yOf(v)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        // Soft gold fill under the line
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gold.copy(alpha = 0.35f), gold.copy(alpha = 0.0f))
            ),
            style = Fill
        )
        drawPath(linePath, color = gold, style = Stroke(width = 4f, cap = StrokeCap.Round))
        val last = closes.last()
        val lastX = (closes.size - 1) * stepX
        val lastY = yOf(last)
        drawCircle(color = Color.White, radius = 12f, center = Offset(lastX, lastY))
        drawCircle(color = gold, radius = 8f, center = Offset(lastX, lastY))
    }
}

@Composable
fun NewsScreen(state: GoldState) {
    val context = LocalContext.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Markets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "What moves gold most, right now.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MarketMiniCard(
                    title = "DXY",
                    value = state.dxyPrice?.let { String.format("%.2f", it) } ?: "—",
                    changePercent = state.dxyChangePercent,
                    explanation = "Dollar up → gold usually down. Dollar down → gold usually up.",
                    modifier = Modifier.weight(1f)
                )
                MarketMiniCard(
                    title = "10-YEAR YIELD",
                    value = state.yield10y?.let { String.format("%.2f%%", it) } ?: "—",
                    changePercent = state.yieldChangePercent,
                    explanation = "Yields up → gold usually down. Yields down → gold usually up.",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Text(
                "Source: Yahoo Finance (DX-Y.NYB, ^TNX).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GoldAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daily Check", style = MaterialTheme.typography.labelLarge, color = Color(0xFF111111), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        fmtPrice(state.spotPrice),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111111)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Why it moved, and what to watch today — US CPI, Tue 10 Nov 2026, 14:30 Tunis time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF111111)
                    )
                }
            }
        }
        item {
            ModernCard {
                Text("US CPI (inflation) — Tue 10 Nov 2026, 14:30 Tunis time.", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Checked on 7 Nov 2026. CPI is the inflation report — higher than expected usually pushes gold down, lower than expected usually helps gold.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Text("Recent News", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Source: Google News, search \"gold price\". Tap a headline to read it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.news.isEmpty()) {
            item {
                Text("No news loaded yet. Check your connection and tap Refresh (top right).")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://news.google.com/search?q=gold%20price&hl=en-US&gl=US&ceid=US:en")))
                }, shape = RoundedCornerShape(16.dp)) { Text("Open gold news in browser") }
            }
        } else {
            items(state.news) { item: NewsItem ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.link)))
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${item.source}  •  ${item.time}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
            Spacer(Modifier.height(4.dp))
            Text(
                "v0.3 does not fetch the calendar live. The events below are the ones to watch, in simple words. Tap the button at the bottom for the live calendar in your browser.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            ModernCard(
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
            ) {
                Text("⭐ Next verified event", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("US CPI (inflation) — Tue 10 Nov 2026, 14:30 Tunis time.")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Checked on 7 Nov 2026. CPI is the inflation report — higher than expected usually pushes gold down, lower than expected usually helps gold.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            ModernCard {
                Text("CPI — inflation", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Prices in the US. High inflation can mean interest rates stay high, which usually hurts gold.")
            }
        }
        item {
            ModernCard {
                Text("NFP — US jobs", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Jobs added in the US, first Friday of the month, 14:30 Tunis time. Strong jobs → dollar up → gold usually down.")
            }
        }
        item {
            ModernCard {
                Text("Fed decision — interest rate", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("The US central bank sets the rate. Rate cut → gold usually up. Rate hike → gold usually down.")
            }
        }
        item {
            ModernCard {
                Text("Fed speeches", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("What Fed officials say about future rates can move gold in minutes. Avoid trading right before them.")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.forexfactory.com/calendar")))
                }, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) { Text("Forex Factory") }
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.investing.com/economic-calendar/")))
                }, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) { Text("Investing.com") }
            }
        }
    }
}

@Composable
fun DailyCheckScreen(state: GoldState, viewModel: GoldViewModel) {
    val spot = state.spotPrice
    val targetProgress = if (spot == null) 0f else (spot / REFERENCE_TARGET).toFloat().coerceIn(0f, 1f)
    val targetPercent = if (spot == null) null else (spot / REFERENCE_TARGET * 100).toInt()
    val rangeLow = state.chartCloses.minOrNull()
    val rangeHigh = state.chartCloses.maxOrNull()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Target", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            ModernCard {
                Text(
                    if (targetPercent == null) "— of ${fmtPrice(REFERENCE_TARGET)} reference level"
                    else "$targetPercent% of ${fmtPrice(REFERENCE_TARGET)} reference level",
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (spot == null) "Gold price not loaded yet — tap Refresh."
                    else "Gold is ${fmtPrice(REFERENCE_TARGET - spot)} away from ${fmtPrice(REFERENCE_TARGET)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = targetProgress,
                    color = GoldAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(6.dp))
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your progress • ${fmtPrice(spot)} of ${fmtPrice(REFERENCE_TARGET)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Reference level only — not a prediction, not advice. Gold may never reach it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            ModernCard {
                Text("30-day range", style = MaterialTheme.typography.labelLarge, color = GoldPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (rangeLow == null || rangeHigh == null) "—"
                    else "${fmtPrice(rangeLow.toDouble())} — ${fmtPrice(rangeHigh.toDouble())}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Low — High, from the last 30 days of live chart data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Text("Your Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Daily Check", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Gold: ${fmtPrice(spot)} per ounce. ${viewModel.trendWords()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Last updated", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (state.fromCache) "Last saved price — ${state.lastUpdatedTunis ?: "—"} (Tunis time)."
                            else "Updated ${state.lastUpdatedTunis ?: "—"} (Tunis time).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            ModernCard {
                Text("Why it moved", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(viewModel.whyItMoved())
                if (state.news.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Top headline: ${state.news.first().title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            ModernCard {
                Text("What to watch today", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("US CPI (inflation) — Tue 10 Nov 2026, 14:30 Tunis time. Check the Calendar tab for the live calendar before trading.")
            }
        }
        item {
            Text(
                "Educational only — not financial advice. No buy / sell signal. No app can predict where gold will go. Practice on demo first and never risk money you cannot afford to lose.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
