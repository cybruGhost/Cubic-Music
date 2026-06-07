package app.it.fast4x.rimusic.ui.screens.player.components.controls

import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.utils.PlaybackContextStore
import app.it.fast4x.rimusic.utils.color
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.showPlayerOutputDeviceKey
import app.it.fast4x.rimusic.utils.showPlayerPlaybackContextKey

@Composable
fun PlayerContextBadges(
    disableScrollingText: Boolean,
    modifier: Modifier = Modifier
) {
    val showSource by rememberPreference(showPlayerPlaybackContextKey, true)
    val showDevice by rememberPreference(showPlayerOutputDeviceKey, true)
    val info by PlaybackContextStore.info.collectAsState()
    val context = LocalContext.current
    val outputDevice = remember(showDevice) {
        if (showDevice) context.currentAudioOutputName() else ""
    }

    val parts = buildList {
        if (showSource && info.source.isNotBlank()) {
            add(listOf(info.source, info.detail).filter { it.isNotBlank() }.joinToString(" - "))
        }
        if (showDevice && outputDevice.isNotBlank()) {
            add(outputDevice)
        }
    }

    if (parts.isEmpty()) return

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        BasicText(
            text = parts.joinToString("  |  "),
            style = typography().xxs.semiBold
                .color(colorPalette().accent.copy(alpha = 0.88f))
                .copy(textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (disableScrollingText) Modifier else Modifier.basicMarquee(iterations = Int.MAX_VALUE))
        )
    }
}

private fun android.content.Context.currentAudioOutputName(): String {
    val audioManager = getSystemService(AudioManager::class.java) ?: return ""
    val device = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
        ?: audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_DEVICE
            }
        ?: return ""

    val name = device.productName?.toString()?.trim().orEmpty()
    return if (name.isBlank()) "" else "Listening on $name"
}
