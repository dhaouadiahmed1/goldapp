package app.goldapp.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.goldapp.MainActivity
import app.goldapp.R
import app.goldapp.data.GoldRepository
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Local, on-device notifications only. No server, no Firebase, no account.
 *
 * - A WorkManager job runs every 30 minutes (with network), reuses the
 *   existing GoldRepository.refresh() news fetch, and posts at most ONE
 *   notification per run when the top headline is new.
 * - The same job posts at most ONE reminder for the US CPI event
 *   (Tue 10 Nov 2026, 14:30 Tunis time), only in the 24 hours before it.
 * - If the news fetch fails, the worker posts nothing.
 * - The first run after install only records the current headline; it
 *   does not notify for a headline that was already there.
 */
object GoldNotifications {
    const val CHANNEL_ID = "gold_news"
    const val WORK_NAME = "gold_news_check"
    const val EXTRA_TAB = "goldapp_tab"
    const val TAB_NEWS = "NEWS"
    const val TAB_CALENDAR = "CALENDAR"

    private const val PREFS = "goldapp_notifications"
    private const val KEY_LAST_NEWS_LINK = "last_news_link"
    private const val KEY_CPI_SENT = "cpi_reminder_sent"
    private const val KEY_ENABLED = "notifications_enabled"
    private const val NEWS_NOTIFICATION_ID = 1001
    private const val CPI_NOTIFICATION_ID = 1002

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) schedule(context) else cancel(context)
    }

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gold news",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "New gold headlines and reminders for big US events."
            manager.createNotificationChannel(channel)
        }
    }

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<GoldNewsWorker>(30, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun openTabIntent(context: Context, tab: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_TAB, tab)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun postNewsNotification(context: Context, headline: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Gold news")
            .setContentText(headline)
            .setStyle(Notification.BigTextStyle().bigText(headline))
            .setContentIntent(openTabIntent(context, TAB_NEWS, 1))
            .setAutoCancel(true)
            .build()
        manager.notify(NEWS_NOTIFICATION_ID, notification)
    }

    fun postCpiNotification(context: Context, title: String, text: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openTabIntent(context, TAB_CALENDAR, 2))
            .setAutoCancel(true)
            .build()
        manager.notify(CPI_NOTIFICATION_ID, notification)
    }

    internal fun lastNewsLink(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_NEWS_LINK, null)

    internal fun saveLastNewsLink(context: Context, link: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_NEWS_LINK, link).apply()
    }

    internal fun cpiReminderSent(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_CPI_SENT, false)

    internal fun markCpiReminderSent(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CPI_SENT, true).apply()
    }
}

class GoldNewsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    // US CPI, Tue 10 Nov 2026, 14:30 Tunis time. One reminder only,
    // in the 24 hours before the event, never after it.
    private val cpiTime: ZonedDateTime = ZonedDateTime.of(
        2026, 11, 10, 14, 30, 0, 0, ZoneId.of("Africa/Tunis")
    )

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!GoldNotifications.isEnabled(context)) return Result.success()
        try {
            // New gold headline, if any.
            val state = GoldRepository(context).refresh()
            val newest = state.news.firstOrNull()
            if (newest != null) {
                val lastLink = GoldNotifications.lastNewsLink(context)
                if (lastLink != null && newest.link != lastLink) {
                    GoldNotifications.postNewsNotification(context, newest.title)
                }
                GoldNotifications.saveLastNewsLink(context, newest.link)
            }

            // US CPI reminder, at most once, only in the 24h before it.
            if (!GoldNotifications.cpiReminderSent(context)) {
                val now = ZonedDateTime.now(ZoneId.of("Africa/Tunis"))
                if (!now.isAfter(cpiTime) && now.isAfter(cpiTime.minusHours(24))) {
                    val isToday = now.toLocalDate() == cpiTime.toLocalDate()
                    if (isToday) {
                        GoldNotifications.postCpiNotification(
                            context,
                            "US CPI today — 14:30 Tunis time",
                            "The inflation report is out today. Check the Calendar tab before trading."
                        )
                    } else {
                        GoldNotifications.postCpiNotification(
                            context,
                            "US CPI tomorrow — Tue 10 Nov, 14:30 Tunis time",
                            "The inflation report is tomorrow. It usually moves gold."
                        )
                    }
                    GoldNotifications.markCpiReminderSent(context)
                }
            }
        } catch (_: Exception) {
            // Fetch failed: post nothing, try again on the next run.
            return Result.success()
        }
        return Result.success()
    }
}
