package app.kreate.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.graphics.Color as AndroidColor
import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.media3.common.util.UnstableApi
import androidx.palette.graphics.Palette
import androidx.core.content.ContextCompat
import app.kreate.android.R
import android.content.BroadcastReceiver
import app.kreate.android.drawable.APP_ICON_BITMAP
import app.it.fast4x.rimusic.MainActivity
import app.it.fast4x.rimusic.RescueCenterActivity
import app.it.fast4x.rimusic.cleanPrefix
import java.io.File
import androidx.compose.ui.graphics.Color
import android.content.Intent
import android.os.SystemClock
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern

// ─────────────────────────────────────────────────────────────────────────────
// Broadcast action strings — must match PlayerServiceModern.Action exactly.
// PlayerServiceModern registers NotificationActionReceiver with these strings
// via ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED). We set the
// package on every Intent so the OS routes to the correct private receiver.
// ─────────────────────────────────────────────────────────────────────────────
// Add this class after imports, before sealed class Widget
class WidgetActionReceiver : BroadcastReceiver() {
    companion object {
        private var lastAction: String? = null
        private var lastActionAtMs: Long = 0L
        private const val ACTION_DEBOUNCE_MS = 700L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val incomingAction = intent.action ?: return
        val now = SystemClock.elapsedRealtime()
        if (incomingAction == lastAction && now - lastActionAtMs < ACTION_DEBOUNCE_MS) {
            return
        }
        lastAction = incomingAction
        lastActionAtMs = now

        val playerAction = when (intent.action) {
            ACTION_PLAY -> PlayerServiceModern.Action.play.value
            ACTION_PAUSE -> PlayerServiceModern.Action.pause.value
            ACTION_NEXT -> PlayerServiceModern.Action.next.value
            ACTION_PREVIOUS -> PlayerServiceModern.Action.previous.value
            else -> null
        } ?: return

        // Make sure the service is alive before forwarding the transport action.
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, PlayerServiceModern::class.java)
            )
        }

        val forwardIntent = Intent(playerAction).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(forwardIntent)
    }
}
private const val ACTION_PLAY     = "it.fast4x.rimusic.play"
private const val ACTION_PAUSE    = "it.fast4x.rimusic.pause"
private const val ACTION_NEXT     = "it.fast4x.rimusic.next"
private const val ACTION_PREVIOUS = "it.fast4x.rimusic.previous"

// ─────────────────────────────────────────────────────────────────────────────
// Preference keys
// ─────────────────────────────────────────────────────────────────────────────

private val PREF_TITLE        = stringPreferencesKey("songTitleKey")
private val PREF_ARTIST       = stringPreferencesKey("songArtistKey")
private val PREF_PLAYING      = booleanPreferencesKey("isPlayingKey")
private val PREF_BITMAP_PATH  = stringPreferencesKey("thumbnailPathKey")
private val PREF_THUMB_READY  = booleanPreferencesKey("thumbReady")
private val PREF_PAL_READY    = booleanPreferencesKey("paletteReady")
private val PREF_DYN_BG_D     = stringPreferencesKey("dynBgDark")
private val PREF_DYN_SURF_D   = stringPreferencesKey("dynSurfDark")
private val PREF_DYN_ACC_D    = stringPreferencesKey("dynAccDark")
private val PREF_DYN_BG_L     = stringPreferencesKey("dynBgLight")
private val PREF_DYN_SURF_L   = stringPreferencesKey("dynSurfLight")
private val PREF_DYN_ACC_L    = stringPreferencesKey("dynAccLight")

// ─────────────────────────────────────────────────────────────────────────────
// Palette extraction
// ─────────────────────────────────────────────────────────────────────────────

private data class WidgetPalette(
    @ColorInt val bgDark:    Int,
    @ColorInt val surfDark:  Int,
    @ColorInt val accDark:   Int,
    @ColorInt val bgLight:   Int,
    @ColorInt val surfLight: Int,
    @ColorInt val accLight:  Int,
)

private fun FloatArray.setLightness(l: Float): FloatArray {
    this[2] = l.coerceIn(0f, 1f)
    return this
}

private fun FloatArray.setSaturation(s: Float): FloatArray {
    this[1] = s.coerceIn(0f, 1f)
    return this
}

private fun extractPalette(bitmap: Bitmap): WidgetPalette {
    val p = Palette.from(bitmap).maximumColorCount(8).generate()

    val fallbackDark  = AndroidColor.parseColor("#0F1014")
    val fallbackLight = AndroidColor.parseColor("#F0EDE6")
    val fallbackAcc   = AndroidColor.parseColor("#2EE59D")

    fun hsl(@ColorInt c: Int, block: (FloatArray) -> Unit): Int {
        val arr = FloatArray(3)
        ColorUtils.colorToHSL(c, arr)
        block(arr)
        return ColorUtils.HSLToColor(arr)
    }

    val rawDarkBg    = p.getDarkMutedColor(fallbackDark)
    val rawDarkSurf  = p.getDarkVibrantColor(fallbackDark)
    val rawDarkAcc   = p.getVibrantColor(fallbackAcc)
    val rawLightBg   = p.getLightMutedColor(fallbackLight)
    val rawLightSurf = p.getLightVibrantColor(fallbackLight)
    val rawLightAcc  = p.getVibrantColor(fallbackAcc)

    return WidgetPalette(
        bgDark    = hsl(rawDarkBg)    { arr -> arr.setLightness(0.08f); arr.setSaturation(arr[1].coerceAtMost(0.35f)) },
        surfDark  = hsl(rawDarkSurf)  { arr -> arr.setLightness(0.14f); arr.setSaturation(arr[1].coerceAtMost(0.4f)) },
        accDark   = hsl(rawDarkAcc)   { arr -> arr.setSaturation(arr[1].coerceAtLeast(0.5f)); arr.setLightness(0.55f) },
        bgLight   = hsl(rawLightBg)   { arr -> arr.setLightness(0.94f); arr.setSaturation(arr[1].coerceAtMost(0.2f)) },
        surfLight = hsl(rawLightSurf) { arr -> arr.setLightness(0.98f) },
        accLight  = hsl(rawLightAcc)  { arr -> arr.setSaturation(arr[1].coerceAtLeast(0.5f)); arr.setLightness(0.48f) },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Bitmap decode helper — fast two-pass, targets maxDim px
// ─────────────────────────────────────────────────────────────────────────────

private fun decodeFast(path: String, maxDim: Int = 256): Bitmap? {
    val b = Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, b)
    if (b.outWidth <= 0) return null
    var s = 1
    while (maxOf(b.outWidth, b.outHeight) / s > maxDim) s *= 2
    return BitmapFactory.decodeFile(path, Options().apply {
        inSampleSize      = s.coerceAtLeast(1)
        inPreferredConfig = Bitmap.Config.RGB_565
    })
}

// ─────────────────────────────────────────────────────────────────────────────
// Color encode/decode helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun Color.encode(): String = value.toString()
private fun String.decodeColor(): Color? = toLongOrNull()?.let { Color(it.toULong()) }

// Convert an Android @ColorInt (signed ARGB Int) to Compose Color.
// The mask avoids sign-extension that would produce the wrong ULong value.
private fun colorIntToComposeColor(@ColorInt argb: Int): Color =
    Color(argb.toLong() and 0xFFFFFFFFL)

// ─────────────────────────────────────────────────────────────────────────────
// Sealed Widget base
// ─────────────────────────────────────────────────────────────────────────────

sealed class Widget : GlanceAppWidget() {

    // Kept for external callers (e.g. service code) that reference these by name
    val songTitleKey  = PREF_TITLE
    val songArtistKey = PREF_ARTIST
    val isPlayingKey  = PREF_PLAYING
    var bitmapPath    = PREF_BITMAP_PATH

    // ── Static fallback colours ───────────────────────────────────────────

    private val fbBgL   = Color(0xFFF0EDE6)
    private val fbBgD   = Color(0xFF0F1014)
    private val fbSurfL = Color(0xFFFFFFFF)
    private val fbSurfD = Color(0xFF1A1B22)
    private val fbAccL  = Color(0xFFD97706)
    private val fbAccD  = Color(0xFF2EE59D)

    protected val textColor    = ColorProvider(Color(0xFF0F1014), Color(0xFFE8E8EA))
    protected val subtextColor = ColorProvider(Color(0xFF6B7280), Color(0xFF8A8B96))

    // ── Dynamic colour reads ──────────────────────────────────────────────

    @Composable @GlanceComposable
    protected fun dynBg() = ColorProvider(
        currentState(PREF_DYN_BG_L)?.decodeColor()   ?: fbBgL,
        currentState(PREF_DYN_BG_D)?.decodeColor()   ?: fbBgD
    )

    @Composable @GlanceComposable
    protected fun dynSurf() = ColorProvider(
        currentState(PREF_DYN_SURF_L)?.decodeColor() ?: fbSurfL,
        currentState(PREF_DYN_SURF_D)?.decodeColor() ?: fbSurfD
    )

    @Composable @GlanceComposable
    protected fun dynAcc() = ColorProvider(
        currentState(PREF_DYN_ACC_L)?.decodeColor()  ?: fbAccL,
        currentState(PREF_DYN_ACC_D)?.decodeColor()  ?: fbAccD
    )

    // Pill always uses the dark surface for contrast regardless of system theme
    @Composable @GlanceComposable
    protected fun dynPill() = ColorProvider(
        currentState(PREF_DYN_SURF_D)?.decodeColor() ?: Color(0xFF1A1A1A),
        currentState(PREF_DYN_SURF_D)?.decodeColor() ?: Color(0xFF2A2B35)
    )

    // ── Shared composables ────────────────────────────────────────────────

    @Composable @GlanceComposable
    protected fun Thumbnail(modifier: GlanceModifier) {
        val bmp = currentState(bitmapPath)?.let { decodeFast(it) } ?: APP_ICON_BITMAP
        Image(
            provider = ImageProvider(bmp),
            contentDescription = "Album art",
            modifier = modifier.clickable(actionStartActivity<MainActivity>())
        )
    }

    @Composable @GlanceComposable
    protected fun ShimmerArt(size: Int) {
        Box(
            modifier = GlanceModifier
                .size(size.dp)
                .background(ColorProvider(Color(0xFFDEDEDE), Color(0xFF252630)))
                .cornerRadius(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier
                    .width((size * 0.55f).toInt().dp)
                    .height((size * 0.12f).toInt().dp)
                    .background(ColorProvider(Color(0xFFEEEEEE), Color(0xFF32333F)))
                    .cornerRadius(6.dp)
            ) { /* empty */ }
        }
    }

    @Composable @GlanceComposable
    protected fun ShimmerText(widthDp: Int, heightDp: Int) {
        Box(
            modifier = GlanceModifier
                .width(widthDp.dp)
                .height(heightDp.dp)
                .background(ColorProvider(Color(0xFFDEDEDE), Color(0xFF2E2F3A)))
                .cornerRadius(4.dp)
        ) { /* empty */ }
    }

    @Composable @GlanceComposable
    protected fun StatusBadge() {
        val isPlaying = currentState(isPlayingKey) ?: false
        Text(
            text = if (isPlaying) "▶ NOW PLAYING" else "PAUSED",
            style = TextStyle(
                color = dynAcc(),
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp
            )
        )
    }

    /**
     * Transport pill.
     *
     * WHY broadcast: Glance's clickable() with an arbitrary lambda does NOT
     * work — the lambda is serialised for the RemoteViews system and the
     * receiver is never actually called at tap time. The only correct approach
     * is actionSendBroadcast() with an explicit Intent, which the OS delivers
     * to PlayerServiceModern.NotificationActionReceiver — the same receiver
     * that the notification buttons already use.
     *
     * The package is set on every Intent so Android routes it to our private
     * (RECEIVER_NOT_EXPORTED) receiver instead of broadcasting globally.
     */
    @Composable @GlanceComposable
    protected fun ControlsPill(context: Context) {
        val isPlaying = currentState(isPlayingKey) ?: false
        val pkg = context.packageName

        Row(
            modifier = GlanceModifier
                .background(dynPill())
                .cornerRadius(50.dp)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(R.drawable.play_skip_back),
                contentDescription = "Previous",
                modifier = GlanceModifier
                    .size(20.dp)
                    .clickable(
                        actionSendBroadcast(
                            Intent(context, WidgetActionReceiver::class.java)
                                .setAction(ACTION_PREVIOUS)
                                .setPackage(pkg)
                        )
                    )
            )

            Spacer(GlanceModifier.width(6.dp))

            Box(
                modifier = GlanceModifier
                    .size(42.dp)
                    .background(dynAcc())
                    .cornerRadius(50.dp)
                    .clickable(
                        actionSendBroadcast(
                            Intent(context, WidgetActionReceiver::class.java)
                                .setAction(if (isPlaying) ACTION_PAUSE else ACTION_PLAY)
                                .setPackage(pkg)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(if (isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = GlanceModifier.size(20.dp)
                )
            }

            Spacer(GlanceModifier.width(6.dp))

            Image(
                provider = ImageProvider(R.drawable.play_skip_forward),
                contentDescription = "Next",
                modifier = GlanceModifier
                    .size(20.dp)
                    .clickable(
                        actionSendBroadcast(
                            Intent(context, WidgetActionReceiver::class.java)
                                .setAction(ACTION_NEXT)
                                .setPackage(pkg)
                        )
                    )
            )
        }
    }

    // ── Update called from PlayerService ─────────────────────────────────

    @UnstableApi
    suspend fun update(
        context: Context,
        // The actions Triple is kept for API compat but is no longer used —
        // widget buttons fire broadcasts directly to PlayerServiceModern.
        @Suppress("UNUSED_PARAMETER")
        actions: Triple<() -> Unit, () -> Unit, () -> Unit>,
        status: Triple<String, String, Boolean>,
        bitmapFile: File
    ) {
        val glanceId = GlanceAppWidgetManager(context)
            .getGlanceIds(this::class.java).firstOrNull() ?: return

        val bmp = decodeFast(bitmapFile.absolutePath, maxDim = 128)

        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[PREF_TITLE]       = cleanPrefix(status.first)
            prefs[PREF_ARTIST]      = cleanPrefix(status.second)
            prefs[PREF_PLAYING]     = status.third
            prefs[PREF_BITMAP_PATH] = bitmapFile.absolutePath
            prefs[PREF_THUMB_READY] = (bmp != null)

            if (bmp != null) {
                val wp = extractPalette(bmp)
                prefs[PREF_DYN_BG_D]   = colorIntToComposeColor(wp.bgDark).encode()
                prefs[PREF_DYN_SURF_D] = colorIntToComposeColor(wp.surfDark).encode()
                prefs[PREF_DYN_ACC_D]  = colorIntToComposeColor(wp.accDark).encode()
                prefs[PREF_DYN_BG_L]   = colorIntToComposeColor(wp.bgLight).encode()
                prefs[PREF_DYN_SURF_L] = colorIntToComposeColor(wp.surfLight).encode()
                prefs[PREF_DYN_ACC_L]  = colorIntToComposeColor(wp.accLight).encode()
                prefs[PREF_PAL_READY]  = true
            }
        }

        update(context, glanceId)
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { GlanceTheme { Content(context) } }
    }

    @Composable
    protected abstract fun Content(context: Context)

    // ════════════════════════════════════════════════════════════════════════
    // HORIZONTAL  —  artwork left · badge + title + artist + controls right
    // ════════════════════════════════════════════════════════════════════════

    data object Horizontal : Widget() {

        @Composable
        override fun Content(context: Context) {
            val ready  = currentState(PREF_THUMB_READY) ?: false
            val title  = currentState(PREF_TITLE).orEmpty()
            val artist = currentState(PREF_ARTIST).orEmpty()

            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(dynBg())
                    .cornerRadius(20.dp)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                if (ready) {
                    Thumbnail(GlanceModifier.size(90.dp).cornerRadius(14.dp))
                } else {
                    ShimmerArt(90)
                }

                Spacer(GlanceModifier.width(14.dp))

                Column(
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.Start
                ) {
                    StatusBadge()
                    Spacer(GlanceModifier.height(5.dp))

                    if (ready) {
                        Text(
                            text = title.ifBlank { "Cubic Music" },
                            style = TextStyle(
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            maxLines = 1
                        )
                        Spacer(GlanceModifier.height(3.dp))
                        Text(
                            text = artist.ifBlank { "Tap to open player" },
                            style = TextStyle(color = subtextColor, fontSize = 12.sp),
                            maxLines = 1
                        )
                    } else {
                        ShimmerText(widthDp = 140, heightDp = 14)
                        Spacer(GlanceModifier.height(6.dp))
                        ShimmerText(widthDp = 100, heightDp = 10)
                    }

                    Spacer(GlanceModifier.height(12.dp))
                    ControlsPill(context)
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // VERTICAL  —  badge top · artwork center · title · artist · controls
    // ════════════════════════════════════════════════════════════════════════

    data object Vertical : Widget() {

        @Composable
        override fun Content(context: Context) {
            val ready  = currentState(PREF_THUMB_READY) ?: false
            val title  = currentState(PREF_TITLE).orEmpty()
            val artist = currentState(PREF_ARTIST).orEmpty()

            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(dynBg())
                    .cornerRadius(20.dp)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatusBadge()
                Spacer(GlanceModifier.height(12.dp))

                Box(
                    modifier = GlanceModifier
                        .size(114.dp)
                        .background(dynAcc())
                        .cornerRadius(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (ready) {
                        Thumbnail(GlanceModifier.size(106.dp).cornerRadius(14.dp))
                    } else {
                        ShimmerArt(106)
                    }
                }

                Spacer(GlanceModifier.height(14.dp))

                if (ready) {
                    Text(
                        text = title.ifBlank { "Cubic Music" },
                        style = TextStyle(
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = artist.ifBlank { "Tap to open your player" },
                        style = TextStyle(color = subtextColor, fontSize = 12.sp),
                        maxLines = 1
                    )
                } else {
                    ShimmerText(widthDp = 140, heightDp = 14)
                    Spacer(GlanceModifier.height(6.dp))
                    ShimmerText(widthDp = 100, heightDp = 10)
                }

                Spacer(GlanceModifier.height(16.dp))
                ControlsPill(context)
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // RESCUE  —  crash + log center shortcut
    // ════════════════════════════════════════════════════════════════════════

    data object Rescue : Widget() {

        @Composable
        override fun Content(context: Context) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(dynBg())
                    .cornerRadius(20.dp)
                    .padding(16.dp)
                    .clickable(actionStartActivity<RescueCenterActivity>()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(52.dp)
                        .background(dynAcc())
                        .cornerRadius(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.rescue),
                        contentDescription = "Rescue Center",
                        modifier = GlanceModifier.size(26.dp)
                    )
                }
                Spacer(GlanceModifier.height(10.dp))
                Text(
                    text = "RESCUE CENTER",
                    style = TextStyle(
                        color = dynAcc(),
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = "Crash & playback logs",
                    style = TextStyle(
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
                Spacer(GlanceModifier.height(3.dp))
                Text(
                    text = "Works even when the player is unhappy",
                    style = TextStyle(color = subtextColor, fontSize = 11.sp)
                )
            }
        }
    }
}
