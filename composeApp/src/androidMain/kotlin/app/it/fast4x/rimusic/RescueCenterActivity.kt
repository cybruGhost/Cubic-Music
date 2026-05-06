package app.it.fast4x.rimusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import app.it.fast4x.rimusic.enums.FontType
import app.it.fast4x.rimusic.ui.screens.settings.DebugRescueCenterDialog
import app.it.fast4x.rimusic.ui.styling.Appearance
import app.it.fast4x.rimusic.ui.styling.DefaultDarkColorPalette
import app.it.fast4x.rimusic.ui.styling.LocalAppearance
import app.it.fast4x.rimusic.ui.styling.typographyOf

class RescueCenterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appearance = Appearance(
                colorPalette = DefaultDarkColorPalette,
                typography = typographyOf(
                    color = DefaultDarkColorPalette.text,
                    useSystemFont = false,
                    applyFontPadding = false,
                    fontType = FontType.Rubik
                ),
                thumbnailShape = RoundedCornerShape(16.dp)
            )

            CompositionLocalProvider(LocalAppearance provides appearance) {
                DebugRescueCenterDialog(
                    context = this,
                    onDismiss = ::finish
                )
            }
        }
    }
}
