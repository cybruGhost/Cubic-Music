package app.it.fast4x.rimusic.ui.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable

@Composable
@NonRestartableComposable
internal fun PortraitPlayerContent(
    content: @Composable () -> Unit
) {
    content()
}

@Composable
@NonRestartableComposable
internal fun LandscapePlayerContent(
    content: @Composable () -> Unit
) {
    content()
}
