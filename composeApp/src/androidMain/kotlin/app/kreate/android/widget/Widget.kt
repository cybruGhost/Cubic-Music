package app.kreate.android.widget

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
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
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.kreate.android.drawable.APP_ICON_BITMAP
import app.it.fast4x.rimusic.MainActivity
import app.it.fast4x.rimusic.cleanPrefix
import java.io.File
import androidx.compose.ui.graphics.Color

sealed class Widget : GlanceAppWidget() {

    val songTitleKey = stringPreferencesKey("songTitleKey")
    val songArtistKey = stringPreferencesKey("songArtistKey")
    val isPlayingKey = booleanPreferencesKey("isPlayingKey")
    var bitmapPath = stringPreferencesKey("thumbnailPathKey")

    private var onPlayPauseAction: () -> Unit = {}
    private var onPreviousAction: () -> Unit = {}
    private var onNextAction: () -> Unit = {}

    @Composable
    protected abstract fun Content(context: Context)

    @Composable
    @GlanceComposable
    protected fun Thumbnail(modifier: GlanceModifier) {
        val bitmap = currentState(bitmapPath)?.let(BitmapFactory::decodeFile) ?: APP_ICON_BITMAP
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "cover",
            modifier = modifier.clickable(actionStartActivity<MainActivity>())
        )
    }

    @Composable
    @GlanceComposable
    protected fun Controller() {
        val isPlaying = currentState(isPlayingKey) ?: false

        Row(
            modifier = GlanceModifier.padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.play_skip_back),
                contentDescription = "Previous",
                modifier = GlanceModifier
                    .size(32.dp)
                    .clickable(onPreviousAction)
            )

            Image(
                provider = ImageProvider(
                    if (isPlaying) R.drawable.pause else R.drawable.play
                ),
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = GlanceModifier
                    .size(40.dp)
                    .padding(horizontal = 16.dp)
                    .clickable(onPlayPauseAction)
            )

            Image(
                provider = ImageProvider(R.drawable.play_skip_forward),
                contentDescription = "Next",
                modifier = GlanceModifier
                    .size(32.dp)
                    .clickable(onNextAction)
            )
        }
    }

    @UnstableApi
    suspend fun update(
        context: Context,
        actions: Triple<() -> Unit, () -> Unit, () -> Unit>,
        status: Triple<String, String, Boolean>,
        bitmapFile: File
    ) {
        val glanceId =
            GlanceAppWidgetManager(context).getGlanceIds(this::class.java).firstOrNull() ?: return

        updateAppWidgetState(context, glanceId) {
            it[songTitleKey] = cleanPrefix(status.first)
            it[songArtistKey] = cleanPrefix(status.second)
            it[isPlayingKey] = status.third
            it[bitmapPath] = bitmapFile.absolutePath
        }

        onPlayPauseAction = actions.first
        onPreviousAction = actions.second
        onNextAction = actions.third

        update(context, glanceId)
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme { Content(context) }
        }
    }

    data object Horizontal : Widget() {

        @Composable
        override fun Content(context: Context) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(Color(0xFFF5EFE6), Color(0xFF1B2430)))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                Thumbnail(
                    GlanceModifier
                        .background(ColorProvider(Color(0x26D97706), Color(0x2600C2A8)))
                        .padding(6.dp)
                        .size(78.dp)
                        .padding(end = 14.dp)
                )

                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = if ((currentState(isPlayingKey) ?: false)) "NOW PLAYING" else "READY",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFB45309), Color(0xFF5EEAD4)),
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = GlanceModifier.padding(bottom = 4.dp)
                    )

                    Text(
                        text = currentState(songTitleKey).orEmpty().ifBlank { "Cubic Music" },
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF111827), Color.White),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.padding(bottom = 2.dp)
                    )

                    Text(
                        text = currentState(songArtistKey).orEmpty().ifBlank { "Tap to reopen your queue" },
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF6B7280), Color(0xFFD1D5DB))
                        ),
                        modifier = GlanceModifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = GlanceModifier
                            .background(ColorProvider(Color(0x1FEAB308), Color(0x1F38BDF8)))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Controller()
                    }
                }
            }
        }
    }

    data object Vertical : Widget() {

        @Composable
        override fun Content(context: Context) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(Color(0xFFF8F3EC), Color(0xFF172033)))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if ((currentState(isPlayingKey) ?: false)) "CUBIC NOW PLAYING" else "CUBIC PLAYER",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFB45309), Color(0xFF67E8F9)),
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = GlanceModifier.padding(bottom = 6.dp)
                )

                Thumbnail(
                    GlanceModifier
                        .background(ColorProvider(Color(0x26D97706), Color(0x2600C2A8)))
                        .padding(6.dp)
                        .size(96.dp)
                        .padding(bottom = 10.dp)
                )

                Text(
                    text = currentState(songTitleKey).orEmpty().ifBlank { "Cubic Music" },
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF111827), Color.White),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.padding(bottom = 2.dp)
                )
                
                Text(
                    text = currentState(songArtistKey).orEmpty().ifBlank { "Tap to open your player" },
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF6B7280), Color(0xFFD1D5DB))
                    ),
                    modifier = GlanceModifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = GlanceModifier
                        .background(ColorProvider(Color(0x1FEAB308), Color(0x1F38BDF8)))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Controller()
                }
            }
        }
    }
}
