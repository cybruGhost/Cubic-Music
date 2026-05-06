package app.it.fast4x.rimusic.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.kreate.android.BuildConfig
import app.kreate.android.R
import app.it.fast4x.rimusic.MainActivity
import app.it.fast4x.rimusic.utils.SecureApiConfig
import app.it.fast4x.rimusic.utils.newReleaseNotificationsEnabledKey
import app.it.fast4x.rimusic.utils.preferences
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * AppAnnouncementNotifier
 *
 * Fetches system notifications from the Supabase config endpoint and displays
 * rich, adaptive Android notifications with full category support.
 *
 * Endpoint:
 *   https://....../functions/v1/app-config
 *     ?app=cubic_music&key=system_notification
 *
 * Supported categories (maps to Android notification style + priority):
 *   "announcement" → 📢  PRIORITY_DEFAULT  | BigTextStyle
 *   "promo"        → 🎁  PRIORITY_LOW       | BigPictureStyle preferred
 *   "emergency"    → 🚨  PRIORITY_MAX       | ongoing, full-screen intent
 *   "update"       → 🔄  PRIORITY_HIGH      | BigTextStyle + action button
 *   "callout"      → 📣  PRIORITY_HIGH      | InboxStyle summary
 */
object AppAnnouncementNotifier {

    // ── Channel ──────────────────────────────────────────────────────────────
    const val CHANNEL_ID = "app_system_notifications"

    // ── Notification IDs (one slot per logical category so they don't collide)
    private val NOTIFICATION_IDS = mapOf(
        Category.ANNOUNCEMENT to 2001,
        Category.PROMO        to 2002,
        Category.EMERGENCY    to 2003,
        Category.UPDATE       to 2004,
        Category.CALLOUT      to 2005,
        Category.NEW_RELEASE  to 2006,
    )

    // ── Storage ───────────────────────────────────────────────────────────────
    private const val PREFS_NAME           = "cubic_announcement_notifier"
    private const val KEY_LAST_SIGNATURE   = "last_signature"
    private const val KEY_LAST_SHOWN_AT    = "last_shown_at"

    // ── API ───────────────────────────────────────────────────────────────────
    private val executor = Executors.newSingleThreadExecutor()

    // ── Category enum ─────────────────────────────────────────────────────────
    enum class Category(val key: String) {
        ANNOUNCEMENT("announcement"),
        PROMO("promo"),
        EMERGENCY("emergency"),
        UPDATE("update"),
        CALLOUT("callout"),
        NEW_RELEASE("new_release");

        companion object {
            fun from(raw: String): Category {
                val normalized = raw.lowercase().trim().replace('-', '_').replace(' ', '_')
                return entries.firstOrNull { it.key == normalized } ?: ANNOUNCEMENT
            }
        }
    }

    // ── Payload ───────────────────────────────────────────────────────────────
    private data class Payload(
        val title: String,
        val contents: String,
        val url: String,
        val show: Boolean,
        val showImage: Boolean,
        val showText: Boolean,
        val category: Category,
        val frequencyHours: Long,
        val image_url: String?,
    ) {
        /** Fingerprint — changes whenever visible content changes. */
        val signature: String
            get() = listOf(title, contents, url, category.key, image_url.orEmpty(), showText, showImage)
                .joinToString("|")
    }

    // ── Public entry-point ────────────────────────────────────────────────────
    fun maybeShow(context: Context) {
        executor.execute {
            runCatching {
                val payload = fetchPayload() ?: return@runCatching
                if (!payload.show) return@runCatching
                if (payload.category == Category.NEW_RELEASE &&
                    !context.preferences.getBoolean(newReleaseNotificationsEnabledKey, false)
                ) return@runCatching

                val prefs      = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastSig    = prefs.getString(KEY_LAST_SIGNATURE, null)
                val lastShown  = prefs.getLong(KEY_LAST_SHOWN_AT, 0L)
                val now        = System.currentTimeMillis()
                val minHours   = if (payload.category == Category.NEW_RELEASE) 24L else 1L
                val freqMs     = payload.frequencyHours.coerceAtLeast(minHours) * 3_600_000L

                if (payload.signature == lastSig && now - lastShown < freqMs) return@runCatching

                val notificationId = NOTIFICATION_IDS[payload.category] ?: 2001
                NotificationManagerCompat.from(context)
                    .notify(notificationId, buildNotification(context, payload))

                prefs.edit()
                    .putString(KEY_LAST_SIGNATURE, payload.signature)
                    .putLong(KEY_LAST_SHOWN_AT, now)
                    .apply()

            }.onFailure { Timber.w(it, "AppAnnouncementNotifier: fetch failed") }
        }
    }

    // ── Fetch & parse ─────────────────────────────────────────────────────────
    private fun fetchPayload(): Payload? {
        val conn = URL(SecureApiConfig.cubicSystemNotificationConfigUrl).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod  = "GET"
            conn.connectTimeout = 5_000
            conn.readTimeout    = 5_000
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)

            // Support both flat payload and nested {"system_notification":{...}}
            val n = root.optJSONObject("system_notification") ?: root

            Payload(
                title          = n.optString("title").trim(),
                contents       = n.optString("contents").trim(),
                url            = n.optString("url").trim(),
                show           = n.optBoolean("show", false),
                showImage      = n.optBoolean("show_image", true),
                showText       = n.optBoolean("show_text", true),
                category       = Category.from(n.optString("category", "announcement")),
                frequencyHours = n.optLong("frequency_hours").takeIf { it > 0L } ?: 24L,
                image_url       = n.optString("image_url")
                    .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) },
            )
        } finally {
            conn.disconnect()
        }
    }

    // ── Build notification ────────────────────────────────────────────────────
    private fun buildNotification(context: Context, p: Payload): Notification {
        val tapIntent = p.url
            .takeIf(String::isNotBlank)
            ?.let { Intent(Intent.ACTION_VIEW, Uri.parse(it)) }
            ?: Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val contentPi = PendingIntent.getActivity(
            context, p.category.ordinal + 100, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ── Category-specific settings ────────────────────────────────────────
        val cfg = categoryConfig(context, p.category)

        val displayTitle = p.title.ifBlank { cfg.defaultTitle }
        val displayText  = when {
            p.showText && p.contents.isNotBlank() -> p.contents.replace('\n', ' ')
            else                                  -> cfg.defaultText
        }
        val displayHeading = cfg.emoji
            .takeIf(String::isNotBlank)
            ?.let { "$it  $displayTitle" }
            ?: displayTitle

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(cfg.smallIcon)
            .setContentTitle(displayHeading)
            .setContentText(displayText)
            .setPriority(cfg.priority)
            .setCategory(cfg.androidCategory)
            .setAutoCancel(!cfg.isOngoing)
            .setOngoing(cfg.isOngoing)
            .setContentIntent(contentPi)
            .setColor(cfg.accentColor)
            .setColorized(cfg.colorized)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // ── Optional action button (e.g. "Update Now" / "Learn More") ─────────
        cfg.actionLabel?.let { label ->
            builder.addAction(
                NotificationCompat.Action(
                    cfg.actionIcon,
                    label,
                    contentPi
                )
            )
        }

        // ── Rich style selection ──────────────────────────────────────────────
        val image = p.image_url
            ?.takeIf { p.showImage }
            ?.let(::loadBitmap)

        when {
            // Always prefer BigPicture when there's an image
            image != null -> {
                builder.setLargeIcon(image)
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(image)
                        .bigLargeIcon(null as Bitmap?)
                        .setBigContentTitle(displayHeading)
                        .setSummaryText(
                            p.contents.takeIf { p.showText && it.isNotBlank() }
                        )
                )
            }

            // Callout → InboxStyle (treats newline-separated lines as bullets)
            p.category == Category.CALLOUT && p.contents.contains('\n') -> {
                val style = NotificationCompat.InboxStyle()
                    .setBigContentTitle(displayHeading)
                p.contents.lines().take(6).forEach { style.addLine(it) }
                builder.setStyle(style)
            }

            // Default: BigText for anything with body copy
            p.showText && p.contents.isNotBlank() -> {
                builder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(displayHeading)
                        .bigText(p.contents)
                )
            }
        }

        return builder.build()
    }

    // ── Category config ───────────────────────────────────────────────────────
    private data class CategoryConfig(
        val emoji: String,
        val defaultTitle: String,
        val defaultText: String,
        val priority: Int,
        val androidCategory: String,
        val smallIcon: Int,
        val accentColor: Int,
        val colorized: Boolean,
        val isOngoing: Boolean,
        val actionLabel: String?,
        val actionIcon: Int,
    )

    private fun categoryConfig(context: Context, cat: Category): CategoryConfig = when (cat) {

        Category.ANNOUNCEMENT -> CategoryConfig(
            emoji           = "📢",
            defaultTitle    = context.getString(R.string.app_announcements_channel_name),
            defaultText     = context.getString(R.string.app_announcements_channel_description),
            priority        = NotificationCompat.PRIORITY_DEFAULT,
            androidCategory = NotificationCompat.CATEGORY_MESSAGE,
            smallIcon       = R.drawable.notifications,
            accentColor     = 0xFF4A90D9.toInt(),
            colorized       = false,
            isOngoing       = false,
            actionLabel     = null,
            actionIcon      = 0,
        )

        Category.PROMO -> CategoryConfig(
            emoji           = "🎁",
            defaultTitle    = context.getString(R.string.app_announcements_channel_name),
            defaultText     = context.getString(R.string.app_announcements_channel_description),
            priority        = NotificationCompat.PRIORITY_LOW,
            androidCategory = NotificationCompat.CATEGORY_PROMO,
            smallIcon       = R.drawable.star,
            accentColor     = 0xFFE8A838.toInt(),
            colorized       = false,
            isOngoing       = false,
            actionLabel     = context.getString(R.string.learn_more),
            actionIcon      = R.drawable.arrow_forward,
        )

        Category.EMERGENCY -> CategoryConfig(
            emoji           = "🚨",
            defaultTitle    = "Emergency",
            defaultText     = "Important notice from Cubic Music.",
            priority        = NotificationCompat.PRIORITY_MAX,
            androidCategory = NotificationCompat.CATEGORY_ALARM,
            smallIcon       = R.drawable.heart_breaked_yes,
            accentColor     = 0xFFD93025.toInt(),
            colorized       = true,
            isOngoing       = true,
            actionLabel     = context.getString(R.string.learn_more),
            actionIcon      = R.drawable.arrow_forward,
        )

        Category.UPDATE -> CategoryConfig(
            emoji           = "🔄",
            defaultTitle    = context.getString(R.string.update_available),
            defaultText     = context.getString(R.string.update_available),
            priority        = NotificationCompat.PRIORITY_HIGH,
            androidCategory = NotificationCompat.CATEGORY_RECOMMENDATION,
            smallIcon       = R.drawable.download,
            accentColor     = 0xFF34A853.toInt(),
            colorized       = false,
            isOngoing       = false,
            actionLabel     = context.getString(R.string.update_available),
            actionIcon      = R.drawable.download,
        )

        Category.CALLOUT -> CategoryConfig(
            emoji           = "📣",
            defaultTitle    = context.getString(R.string.app_announcements_channel_name),
            defaultText     = context.getString(R.string.app_announcements_channel_description),
            priority        = NotificationCompat.PRIORITY_HIGH,
            androidCategory = NotificationCompat.CATEGORY_SOCIAL,
            smallIcon       = R.drawable.notifications,
            accentColor     = 0xFFAB47BC.toInt(),
            colorized       = false,
            isOngoing       = false,
            actionLabel     = null,
            actionIcon      = 0,
        )

        Category.NEW_RELEASE -> CategoryConfig(
            emoji           = "",
            defaultTitle    = context.getString(R.string.new_releases_notifications),
            defaultText     = context.getString(R.string.new_releases_notifications_description),
            priority        = NotificationCompat.PRIORITY_DEFAULT,
            androidCategory = NotificationCompat.CATEGORY_RECOMMENDATION,
            smallIcon       = R.drawable.album,
            accentColor     = 0xFF34A853.toInt(),
            colorized       = false,
            isOngoing       = false,
            actionLabel     = context.getString(R.string.new_albums),
            actionIcon      = R.drawable.album,
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun loadBitmap(url: String): Bitmap? = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 4_000
        conn.readTimeout    = 4_000
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
            conn.inputStream.use(BitmapFactory::decodeStream)
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}
// u see this app ha my configs and  mostly features use my servers to make sure the app works.  as this is open source then you guys may just remove what u dont like but first try to understand my code coz it gets nasty
