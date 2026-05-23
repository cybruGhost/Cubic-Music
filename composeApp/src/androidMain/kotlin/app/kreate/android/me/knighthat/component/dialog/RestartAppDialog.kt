package app.kreate.android.me.knighthat.component.dialog

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.service.MyDownloadService
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.utils.intent
import app.it.fast4x.rimusic.utils.medium
import app.kreate.android.BuildConfig
import app.kreate.android.R
import kotlin.system.exitProcess

object RestartAppDialog : ConfirmDialog {

    override val dialogTitle: String
        @Composable
        get() = stringResource(R.string.title_restart_required)

    override var isActive: Boolean by mutableStateOf(false)

    override fun hideDialog() {
        isActive = false
    }

    override fun onConfirm() {
        appContext().stopService(appContext().intent<PlayerServiceModern>())
        appContext().stopService(appContext().intent<MyDownloadService>())
        (appContext() as? Activity)?.finishAffinity()
        exitProcess(0)
    }

    @Composable
    override fun Buttons() {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = ::hideDialog,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colorPalette().text
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                BasicText(
                    text = stringResource(R.string.cancel),
                    style = typography().s.medium
                )
            }
            Button(
                onClick = ::onConfirm,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorPalette().accent,
                    contentColor = colorPalette().background0
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                BasicText(
                    text = BuildConfig.APP_NAME,
                    style = typography().s.medium
                )
            }
        }
    }

    @Composable
    override fun DialogBody() {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colorPalette().background2
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .align(Alignment.CenterVertically),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(
                                containerColor = colorPalette().accent.copy(alpha = 0.16f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.size(46.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.refresh),
                                    contentDescription = null,
                                    tint = colorPalette().accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        BasicText(
                            text = stringResource(R.string.title_restart_required),
                            style = typography().m.medium.copy(color = colorPalette().text)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BasicText(
                            text = stringResource(R.string.restart_dialog_body, BuildConfig.APP_NAME),
                            style = typography().xs.copy(color = colorPalette().textSecondary)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        BasicText(
                            text = "Restart now to reload downloads, storage sources, and playback safely.",
                            style = typography().xxs.copy(color = colorPalette().textSecondary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            BasicText(
                text = "Your music data stays intact. The app just needs a clean relaunch to apply the storage change.",
                style = typography().xxs.copy(color = colorPalette().textSecondary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
