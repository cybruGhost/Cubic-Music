package it.fast4x.rimusic.utils

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.kreate.android.Preferences
import app.kreate.android.themed.common.component.settings.SettingComponents
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.ui.components.themed.SecondaryTextButton
import me.knighthat.utils.Toaster

@OptIn(UnstableApi::class)
@Composable
fun RestartPlayerService(
    restartService: Boolean = false,
    onRestart: () -> Unit
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    AnimatedVisibility(visible = restartService) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingComponents.Description(
                textId = R.string.minimum_silence_length_warning,
                isImportant = true,
                modifier = Modifier.weight(2f)
            )
            SecondaryTextButton(
                text = stringResource(R.string.restart_service),
                onClick = {
                    binder?.restartForegroundOrStop().let { onRestart() }
                    Toaster.done()
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 24.dp)
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun RestartActivity(
    restart: Boolean = false,
    onRestart: () -> Unit
) {
    var restartActivity by Preferences.RESTART_ACTIVITY
    val context = LocalContext.current
    AnimatedVisibility(visible = restart) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingComponents.Description(
                textId = R.string.minimum_silence_length_warning,
                isImportant = true,
                modifier = Modifier.weight(2f)
            )
            SecondaryTextButton(
                text = stringResource(R.string.restart_service),
                onClick = {
                    restartActivity = !restartActivity
                    onRestart()
                    Toaster.done()
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 24.dp)
            )
        }
    }
}