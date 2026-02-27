package app.kreate.android.me.knighthat.component.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.media3.common.PlaybackParameters
import app.kreate.android.R
import app.kreate.android.constant.Speed
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.kreate.android.me.knighthat.component.dialog.Dialog
import app.kreate.android.me.knighthat.component.menu.ListMenu

class PlaybackSpeed: Dialog, MenuIcon, Descriptive {

    override val iconId: Int = R.drawable.speedometer_outline
    override val messageId: Int = R.string.description_playback_speed
    override val color: Color
        @Composable
        get() = Color.Gray
    override val menuIconTitle: String
        @Composable
        get() = stringResource( R.string.title_playback_speed )
    override val dialogTitle: String
        @Composable
        get() = menuIconTitle

    var speedState: Float by mutableFloatStateOf( 1f )
    override var isActive: Boolean by mutableStateOf( false )

    override fun onShortClick() = showDialog()

    @Composable
    override fun Render() {
        super.Render()

        val player = LocalPlayerServiceBinder.current?.player
        LaunchedEffect( speedState ) {
            player?.playbackParameters = PlaybackParameters( speedState, 1f )
        }
    }

    @Composable
    override fun DialogBody() {
        ListMenu.Menu {
            Speed.entries.forEach {
                ListMenu.Entry(
                    text = it.text,
                    icon = {},
                    onClick = {
                        speedState = it.value
                    },
                )
            }
        }
    }
}