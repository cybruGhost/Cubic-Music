package app.it.fast4x.rimusic.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.kreate.android.R
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.utils.formatAsDuration
import app.it.fast4x.rimusic.utils.semiBold

private const val MINUTES_TO_MILLIS = 60 * 1000L
private const val STEP_MINUTES = 5
private const val MAX_SLIDER_VALUE = 120f

@OptIn(ExperimentalTextApi::class)
@Composable
fun SleepTimerDialog(
    sleepTimerMillisLeft: Long?,
    timeRemaining: Long,
    onDismiss: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onStartSleepTimer: (Long) -> Unit
) {
    if (sleepTimerMillisLeft != null) {
        ConfirmationDialog(
            text = stringResource(R.string.stop_sleep_timer),
            cancelText = stringResource(R.string.no),
            confirmText = stringResource(R.string.stop),
            onDismiss = onDismiss,
            onConfirm = {
                onCancelSleepTimer()
                onDismiss()
            }
        )
    } else {
        val colorPalette = colorPalette()
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorPalette.background1
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        var showCircularSlider by remember { mutableStateOf(false) }
                        var amount by remember { mutableStateOf(1) }

                        // Header
                        BasicText(
                            text = stringResource(R.string.set_sleep_timer),
                            style = typography().l.semiBold.copy(color = colorPalette.text),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(
                                space = 16.dp,
                                alignment = Alignment.CenterHorizontally
                            ),
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .fillMaxWidth()
                        ) {
                            val sleepTimeMillis = amount * STEP_MINUTES * MINUTES_TO_MILLIS

                            if (!showCircularSlider) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .alpha(if (amount <= 1) 0.5f else 1f)
                                        .clip(CircleShape)
                                        .clickable(enabled = amount > 1) { amount-- }
                                        .size(48.dp)
                                        .background(colorPalette.background0)
                                ) {
                                    BasicText(
                                        text = "-",
                                        style = typography().xs.semiBold.copy(color = colorPalette.text)
                                    )
                                }

                                Box(contentAlignment = Alignment.Center) {
                                    BasicText(
                                        text = stringResource(
                                            R.string.left,
                                            formatAsDuration(sleepTimeMillis)
                                        ),
                                        style = typography().s.semiBold.copy(color = colorPalette.text),
                                        modifier = Modifier.clickable {
                                            showCircularSlider = !showCircularSlider
                                        }
                                    )
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .alpha(if (amount >= MAX_SLIDER_VALUE.toInt()) 0.5f else 1f)
                                        .clip(CircleShape)
                                        .clickable(enabled = amount < MAX_SLIDER_VALUE.toInt()) { amount++ }
                                        .size(48.dp)
                                        .background(colorPalette.background0)
                                ) {
                                    BasicText(
                                        text = "+",
                                        style = typography().xs.semiBold.copy(color = colorPalette.text)
                                    )
                                }

                            } else {
                                CircularSlider(
                                    stroke = 40f,
                                    thumbColor = colorPalette.accent,
                                    text = formatAsDuration(sleepTimeMillis),
                                    modifier = Modifier.size(300.dp),
                                    onChange = { percentage ->
                                        amount = (percentage * MAX_SLIDER_VALUE).toInt()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (timeRemaining > 0) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier
                                    .padding(bottom = 20.dp)
                                    .fillMaxWidth()
                            ) {
                                SecondaryTextButton(
                                    text = stringResource(R.string.set_to) + " "
                                            + formatAsDuration(timeRemaining)
                                            + " " + stringResource(R.string.end_of_song),
                                    onClick = {
                                        onStartSleepTimer(timeRemaining)
                                        onDismiss()
                                    }
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.material3.Button(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorPalette.background2,
                                    contentColor = colorPalette.text
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                androidx.compose.material3.Text(
                                    text = stringResource(R.string.cancel),
                                    style = typography().s.semiBold
                                )
                            }

                            androidx.compose.material3.Button(
                                enabled = amount > 0,
                                onClick = {
                                    onStartSleepTimer(amount * STEP_MINUTES * MINUTES_TO_MILLIS)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorPalette.accent,
                                    contentColor = colorPalette.textSecondary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                androidx.compose.material3.Text(
                                    text = stringResource(R.string.confirm),
                                    style = typography().s.semiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



