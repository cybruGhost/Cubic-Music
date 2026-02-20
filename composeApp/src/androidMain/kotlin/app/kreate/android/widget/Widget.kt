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
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.kreate.android.drawable.APP_ICON_BITMAP
import it.fast4x.rimusic.MainActivity
import it.fast4x.rimusic.cleanPrefix
import java.io.File

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

            if (it[bitmapPath].isNullOrEmpty())
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
                    .background(GlanceTheme.colors.widgetBackground)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                Thumbnail(
                    GlanceModifier
                        .size(72.dp)
                        .padding(end = 12.dp)
                )

                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.Start
                ) {
                    // Simple text without styling
                    Text(
                        text = currentState(songTitleKey).orEmpty(),
                        modifier = GlanceModifier.padding(bottom = 2.dp)
                    )

                    Text(
                        text = currentState(songArtistKey).orEmpty(),
                        modifier = GlanceModifier.padding(bottom = 8.dp)
                    )

                    Controller()
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
                    .background(GlanceTheme.colors.widgetBackground)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Thumbnail(
                    GlanceModifier
                        .size(80.dp)
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = currentState(songTitleKey).orEmpty(),
                    modifier = GlanceModifier.padding(bottom = 2.dp)
                )
                
                Text(
                    text = currentState(songArtistKey).orEmpty(),
                    modifier = GlanceModifier.padding(bottom = 8.dp)
                )

                Controller()
            }
        }
    }
}