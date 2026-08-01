package app.it.fast4x.rimusic.ui.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal object CubicColors {
    val Background = Color(0xFF090A09)
    val Window = Color(0xFF101110)
    val Sidebar = Color(0xFF171817)
    val Panel = Color(0xFF141514)
    val PanelRaised = Color(0xFF1D1F1D)
    val Selection = Color(0xFF292B27)
    val Border = Color(0xFF2A2D29)
    val Accent = Color(0xFFC9F34A)
    val AccentSoft = Color(0xFF303C18)
    val Green = Color(0xFF18C968)
    val Success = Color(0xFFB98CFF)
    val Text = Color(0xFFF5F6F2)
    val TextSecondary = Color(0xFFA8AAA5)
    val TextMuted = Color(0xFF6E716B)
    val Danger = Color(0xFFFF6B6B)
}

@Composable
internal fun CubicDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CubicColors.Accent,
            onPrimary = CubicColors.Background,
            background = CubicColors.Background,
            onBackground = CubicColors.Text,
            surface = CubicColors.Panel,
            onSurface = CubicColors.Text,
            surfaceVariant = CubicColors.PanelRaised,
            onSurfaceVariant = CubicColors.TextSecondary,
            outline = CubicColors.Border,
            error = CubicColors.Danger
        ),
        content = content
    )
}
