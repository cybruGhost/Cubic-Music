package app.it.fast4x.rimusic.extensions.games.pacman.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColorScheme(
    primary = PacmanYellow,
    onPrimary = PacmanWhite,
    secondary = PacmanBackground,
)

private val LightColorPalette = lightColorScheme(
    primary = PacmanYellow,
    onPrimary = PacmanWhite,
    secondary = PacmanBackground,
)

@Composable
fun PacmanComposeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}