package app.it.fast4x.rimusic.ui.components

import androidx.compose.runtime.Composable
import database.entities.Song
import player.PlayerController
import player.frame.FrameRenderer
import vlcj.VlcjFrameController

@Composable
fun MiniPlayerLegacy(
    frameController: VlcjFrameController,
    url: String?,
    song: Song?,
    onExpandAction: () -> Unit
) = MiniPlayer(
    frameController = frameController as PlayerController,
    frameRenderer = frameController as FrameRenderer,
    url = url,
    song = song,
    onExpandAction = onExpandAction
)
