# GoldApp v0.4 — Android gold dashboard

## What's new in v0.4 (animated nav + local notifications)
- **Animated bottom nav:** the plain grey-purple NavigationBar is gone.
  A custom bottom bar has a gold pill that slides between Home / News /
  Calendar / Daily Check on a spring, the selected icon pops (about
  1.15×, slight lift) and its label turns bold gold, and the screen
  content fades + slides in (AnimatedContent) instead of swapping
  instantly. Same 4 tabs, same order, same screens and data.
- **News notifications (local, on-device only — no server, no Firebase,
  no account):** a WorkManager job checks the gold news feed every
  30 minutes (only with network) using the existing repository fetch.
  When the top headline is new, it posts ONE notification — title
  "Gold news", text = the headline. Tap it and the app opens on the
  News tab. At most one notification per check; the first run after
  install only records the current headline and does not notify for it.
- **Event reminder:** the same job posts ONE reminder for US CPI,
  Tue 10 Nov 2026, 14:30 Tunis time — "tomorrow" if it is the day
  before, "today" on the day — only in the 24 hours before the event.
  Tap it and the app opens on the Calendar tab.
- **Allow notifications step:** on first launch (Android 13+) the app
  asks once "Allow GoldApp to send notifications?". Tap Allow, or no
  notifications will appear. If blocked, the News tab says so and
  tells you where to turn it on (Settings → Apps → GoldApp →
  Notifications). A switch on the News tab turns notifications on/off
  (default ON; OFF cancels the background job).
- **Limits, honestly:** checks run every 30 minutes, so a headline can
  take up to 30 minutes to notify. Android may delay background work
  when the battery is low. If the news fetch fails, the job posts
  nothing. No fake notifications, no test headline.

## What's new in v0.3 (UI only — same data, same features)
- **Light finance template, as approved from the mockup:** light
  background (#F6F6F8), white rounded cards, gold accent instead of
  blue. Dark theme is kept and follows the system.
- **Home:** "Gold / You are on Top of your Gold", white price card
  with change chip, 30-day chart, Spot / Futures as two mini-cards,
  and a "Your Activity" section with cards that open the News and
  Daily Check tabs.
- **News:** "Markets" at the top — DXY and 10-year yield as two white
  cards — then a gold Daily Check summary card and the CPI event card,
  then the same live Recent News list.
- **Daily Check:** "Target" card with a progress bar toward a $4,500
  reference level (computed from the live price, labelled "Reference
  level only — not a prediction, not advice"), a "30-day range" card
  with low/high computed from the live 30-day chart data, and "Your
  Stats" — today's Daily Check summary and the real last-updated time.
  No streak, no saved amount, no fake numbers.
- **Calendar:** same content, restyled light.
- No new features, no new permissions, no new dependencies, no
  price/news logic changed.

## What's new in v0.2 (UI only — same data, same features)
- **Modern logo:** a clean minimalist mark — deep charcoal rounded square
  with three rising gold bars (light / mid / deep gold) and a rising trend
  line. Vector drawable only, no PNGs. Same mark is shown in the app's
  top bar next to "GoldApp".
- **Modern dashboard:** dark-first Material 3 theme (background #0F0F0F,
  surface #1C1C1E, gold #D4AF37, green #34C759 for up, red #FF453A for
  down), with a light theme that follows the system. Hero price card
  with a coloured change chip, 30-day chart with a soft gold fill and
  faint gridlines, and DXY / 10-year yield as two side-by-side mini-cards.
  News, Calendar and Daily Check are restyled with the same rounded
  cards and spacing. No new features, no new permissions, no new
  dependencies, no price/news logic changed.

Your own gold app, for a beginning trader. Live gold price, a simple
chart, the dollar and US yields that move gold, gold news, the US events
to watch, and a short Daily Check — in one place, in simple English.

No account, no API key, no login needed in v0.4. Only INTERNET
permission, plus POST_NOTIFICATIONS (Android 13+) for the news alerts.

## What it does
- **Home:** live XAU/USD spot gold price (per ounce) + daily change,
  last-updated time in Tunis time, Refresh button, and a simple line
  chart of the last 30 days. The last price is saved on the phone and
  shown with its timestamp when you are offline.
- **Dollar & Rates card (Home):** US Dollar Index (DXY) and US 10-year
  yield, each with one plain line explaining how it usually affects gold.
- **News:** gold headlines with source + time. Tap one to open the
  article in your browser.
- **Calendar:** the US events that move gold most — CPI, NFP/jobs, Fed
  decision, Fed speeches — explained in simple English, plus buttons
  opening the live Forex Factory / Investing.com calendar in the browser.
- **Daily Check:** Price, Trend in simple words, Why it moved,
  What to watch today — the same short format as the WhatsApp check.

## Verified data sources (checked 7 Nov 2026 with curl — all HTTP 200, real data)
- Spot gold price: `https://api.gold-api.com/price/XAU`
  (returned $4,072.10 on 7 Nov 2026, no key needed)
- Gold futures price + change + 30-day chart, DXY, 10-year yield:
  Yahoo Finance chart API, no key needed:
  `query1.finance.yahoo.com/v8/finance/chart/GC=F?range=1mo&interval=1d`
  `query1.finance.yahoo.com/v8/finance/chart/DX-Y.NYB?range=5d&interval=1d`
  `query2.finance.yahoo.com/v8/finance/chart/%5ETNX?range=5d&interval=1d`
  (returned GC=F $4,074.90 / -0.94%, DXY 102.28, 10Y 4.86% on 7 Nov 2026)
- News: Google News RSS, `news.google.com/rss/search?q=gold+price`
  (real headlines, source, time and link, no key needed)

## Honest limits — v0.3
- **No prediction.** No app can tell you where gold will go. GoldApp
  shows the price, the trend and the news — it never gives a buy/sell
  signal. Educational only, not financial advice.
- **No live calendar fetch in v0.3.** The Calendar tab explains the
  events and links to the live calendar in your browser. One event is
  listed as verified, clearly labelled: US CPI, Tue 10 Nov 2026,
  14:30 Tunis time (checked 7 Nov 2026).
- **Prices are indicative.** Spot price is from gold-api.com, the chart
  and daily change are from gold futures (GC=F) on Yahoo Finance.
  They are not a buy/sell price from a shop or a broker.
- **If a source fails**, the app says so honestly ("couldn't load, tap
  refresh") and keeps showing the last saved price — it never shows a
  fake or hardcoded price as if it were live.
- **Not used, because they failed when checked on 7 Nov 2026:**
  Kitco RSS (404), gold-api history (401, needs a key), Investing.com
  RSS news_301 (cryptocurrency news, not gold).

## Build from your phone
See `GITHUB_BUILD.md`. Short version: unzip, create an empty GitHub
repo named `goldapp`, push, wait for Actions, download the
`goldapp-debug-apk` artifact, unzip and install `app-debug.apk`.

## Project layout
- `android/` — the Android project (package `app.goldapp`, minSdk 26,
  targetSdk 34, Kotlin + Jetpack Compose + Material 3)
- `.github/workflows/android.yml` — GitHub Actions build (JDK 17, Gradle 8.7)
