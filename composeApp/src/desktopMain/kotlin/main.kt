import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.application
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import app.it.fast4x.rimusic.getAsyncImageLoader
import app.it.fast4x.rimusic.ui.CubicDesktopAppV2
import org.jetbrains.compose.resources.painterResource
import rimusic.composeapp.generated.resources.Res
import rimusic.composeapp.generated.resources.app_icon
import kotlin.system.exitProcess

@OptIn(ExperimentalCoilApi::class)
fun main() = application {
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    setSingletonImageLoaderFactory { context ->
        getAsyncImageLoader(context)
    }
    Window(
        icon = painterResource(Res.drawable.app_icon),
        onCloseRequest = {
            exitApplication()
            exitProcess(0)
        },
        state = windowState,
        title = "Cubic Music"
    ) {
        CubicDesktopAppV2(windowState)
    }
}
