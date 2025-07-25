package it.fast4x.rimusic.ui.screens.player

import android.annotation.SuppressLint
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import app.kreate.android.R
import app.kreate.android.Preferences
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.PlayerBackgroundColors
import it.fast4x.rimusic.enums.PlayerType
import it.fast4x.rimusic.models.Format
import it.fast4x.rimusic.service.modern.LOCAL_KEY_PREFIX
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.themed.IconButton
import it.fast4x.rimusic.ui.styling.onOverlay
import it.fast4x.rimusic.ui.styling.overlay
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.isLandscape
import it.fast4x.rimusic.utils.medium
import kotlinx.coroutines.Dispatchers
import kotlin.math.roundToInt

@SuppressLint("LongLogTag")
@UnstableApi
@Composable
fun StatsForNerds(
    mediaId: String,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current ?: return

//    val audioQualityFormat by rememberPreference(audioQualityFormatKey, AudioQualityFormat.High)
//
//    val connectivityManager = getSystemService(context, ConnectivityManager::class.java) as ConnectivityManager

    AnimatedVisibility(
        visible = isDisplayed,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        var cachedBytes by remember(mediaId) {
            mutableStateOf(binder.cache.getCachedBytes(mediaId, 0, -1))
        }

        var downloadCachedBytes by remember(mediaId) {
            mutableStateOf(binder.downloadCache.getCachedBytes(mediaId, 0, -1))
        }

        val format by remember {
            Database.formatTable.findBySongId( mediaId )
        }.collectAsState( null, Dispatchers.IO )
        val showThumbnail by Preferences.PLAYER_SHOW_THUMBNAIL
        val statsForNerds by Preferences.PLAYER_STATS_FOR_NERDS
        val playerType by Preferences.PLAYER_TYPE
        val transparentBackgroundActionBarPlayer by Preferences.PLAYER_TRANSPARENT_ACTIONS_BAR
        var blackgradient by Preferences.BLACK_GRADIENT
        val playerBackgroundColors by Preferences.PLAYER_BACKGROUND
        var statsfornerdsfull by remember {mutableStateOf(false)}
        val rotationAngle by animateFloatAsState(
            targetValue = if (statsfornerdsfull) 180f else 0f,
            animationSpec = tween(durationMillis = 500)
        )

        DisposableEffect(mediaId) {
            val listener = object : Cache.Listener {
                override fun onSpanAdded(cache: Cache, span: CacheSpan) {
                    cachedBytes += span.length
                }

                override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
                    cachedBytes -= span.length
                }

                override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) =
                    Unit
            }

            binder.cache.addListener(mediaId, listener)

            onDispose {
                binder.cache.removeListener(mediaId, listener)
            }
        }

    if (showThumbnail && (!statsForNerds || playerType == PlayerType.Essential)) {
        Box(
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            onDismiss()
                        }
                    )
                }
                .background(colorPalette().overlay)
                .fillMaxSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(all = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    BasicText(
                        text = stringResource(R.string.id),
                        style = typography().xs.medium.color(colorPalette().onOverlay)
                    )
                    if (format?.songId?.startsWith(LOCAL_KEY_PREFIX) == false) {
                        BasicText(
                            text = stringResource(R.string.itag),
                            style = typography().xs.medium.color(colorPalette().onOverlay)
                        )
                        BasicText(
                            text = stringResource(R.string.quality),
                            style = typography().xs.medium.color(colorPalette().onOverlay)
                        )
                    }
                    BasicText(
                        text = stringResource(R.string.bitrate),
                        style = typography().xs.medium.color(colorPalette().onOverlay)
                    )
                    BasicText(
                        text = stringResource(R.string.size),
                        style = typography().xs.medium.color(colorPalette().onOverlay)
                    )

                    if (format?.songId?.startsWith(LOCAL_KEY_PREFIX) == true)
                        BasicText(
                            text = stringResource(R.string.cached),
                            style = typography().xs.medium.color(colorPalette().onOverlay)
                        )

                    if (format?.songId?.startsWith(LOCAL_KEY_PREFIX) == false) {
                        BasicText(
                            text = if (downloadCachedBytes == 0L) stringResource(R.string.cached)
                            else stringResource(R.string.downloaded),
                            style = typography().xs.medium.color(colorPalette().onOverlay)
                        )

                        BasicText(
                            text = stringResource(R.string.loudness),
                            style = typography().xs.medium.color(colorPalette().onOverlay)
                        )
                    }
                }

                Column {
                    BasicText(
                        text = mediaId,
                        maxLines = 1,
                        style = typography().xs.medium.color(colorPalette().onOverlay)
                    )

                    if (format?.songId?.startsWith(LOCAL_KEY_PREFIX) == false) {
                        BasicText(
                            text = format?.itag?.toString()
                                ?: stringResource(R.string.audio_quality_format_unknown),
                            maxLines = 1,
                            style = typography().xs.medium.color(colorPalette().onOverlay)
                        )
                        BasicText(
                            text = getQuality(format!!),
                            maxLines = 1,
                            style = typography().xs.medium.color(colorPalette().onOverlay)
                        )
                    }
                    BasicText(
                        text = format?.bitrate?.let { "${it / 1000} kbps" } ?: stringResource(R.string.audio_quality_format_unknown),
                        maxLines = 1,
                        style = typography().xs.medium.color(colorPalette().onOverlay)
                    )
                    BasicText(
//                        text = format?.contentLength
//                            ?.let { Formatter.formatShortFileSize(context, it) } ?: stringResource(R.string.audio_quality_format_unknown),
                        text = when (format?.songId?.startsWith(LOCAL_KEY_PREFIX)){
                            true -> "100%"
                            else -> {
                                if (downloadCachedBytes == 0L)
                                    Formatter.formatShortFileSize(context, cachedBytes) + format?.contentLength?.let {
                                        " (${(cachedBytes.toFloat() / it * 100).roundToInt()}%)"
                                }
                                else Formatter.formatShortFileSize(
                                    context,
                                    downloadCachedBytes
                                ) + format?.contentLength?.let {
                                     " (${(downloadCachedBytes.toFloat() / it * 100).roundToInt()}%)"
                                }
                            }
                        },
                        maxLines = 1,
                        style = typography().xs.medium.color(colorPalette().onOverlay)
                    )
//                    if (format?.songId?.startsWith(LOCAL_KEY_PREFIX) == true) {
//                        BasicText(
//                            text = "100%",
//                            maxLines = 1,
//                            style = typography().xs.medium.color(colorPalette().onOverlay)
//                        )
//                    }
                    if (format?.songId?.startsWith(LOCAL_KEY_PREFIX) == false) {
//                        BasicText(
//                            text = if (cachedBytes > downloadCachedBytes)
//                                Formatter.formatShortFileSize(context, cachedBytes)
//                            else Formatter.formatShortFileSize(
//                                    context,
//                                    downloadCachedBytes
//                                )
//                            + format?.contentLength?.let {
//                                    if (cachedBytes > downloadCachedBytes)
//                                        " (${(cachedBytes.toFloat() / it * 100).roundToInt()}%)"
//                                    else " (${(downloadCachedBytes.toFloat() / it * 100).roundToInt()}%)"
//                                }
//                            ,
//                            maxLines = 1,
//                            style = typography().xs.medium.color(colorPalette().onOverlay)
//                        )
                        BasicText(
                            text = format?.loudnessDb?.let { "%.2f dB".format(it) }
                                ?: stringResource(R.string.audio_quality_format_unknown),
                            maxLines = 1,
                            style = typography().xs.medium.color(colorPalette().onOverlay)
                        )
                    }
                }
            }
        }
    }
        if ((statsForNerds) && (!showThumbnail || playerType == PlayerType.Modern)) {
            Column(

            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = modifier
                        .background(colorPalette().background2.copy(alpha = if ((transparentBackgroundActionBarPlayer) || ((playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient) || (playerBackgroundColors == PlayerBackgroundColors.ThemeColorGradient)) && blackgradient) 0.0f else 0.7f))
                        .padding(vertical = 5.dp)
                        .fillMaxWidth(if (isLandscape) 0.8f else 1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = modifier.weight(1f)
                            .padding(end = 4.dp)
                    ) {
                        if (format?.songId?.startsWith(LOCAL_KEY_PREFIX) == false) {
                            BasicText(
                                text = stringResource(R.string.quality) + " : " + getQuality(format!!),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = typography().xs.medium.color(colorPalette().text)
                            )
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = modifier.weight(1f)
                    ) {
                        println("StatsForNerds modern player bitrate: ${format?.bitrate}")
                        BasicText(
                            text = format?.bitrate?.let { stringResource(R.string.bitrate) + " : " + "${it / 1000} kbps" }
                                ?: (stringResource(R.string.bitrate) + " : " + stringResource(R.string.audio_quality_format_unknown)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = typography().xs.medium.color(colorPalette().text)
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = modifier.weight(1f)
                    ) {
                        BasicText(
                            text = format?.contentLength
                                ?.let {stringResource(R.string.size) + " : " + Formatter.formatShortFileSize(context,it)}
                                ?: (stringResource(R.string.size) + " : " + stringResource(R.string.audio_quality_format_unknown)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = typography().xs.medium.color(colorPalette().text)
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = modifier.weight(0.2f)
                    ) {
                        IconButton(
                            icon = R.drawable.chevron_up,
                            color = colorPalette().text,
                            onClick = {statsfornerdsfull = !statsfornerdsfull},
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(rotationAngle)
                        )
                    }
                }
                AnimatedVisibility(visible = statsfornerdsfull) {
                  Column {
                      Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.Center,
                          modifier = modifier
                              .background(colorPalette().background2.copy(alpha = if ((transparentBackgroundActionBarPlayer) || ((playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient) || (playerBackgroundColors == PlayerBackgroundColors.ThemeColorGradient)) && blackgradient) 0.0f else 0.7f))
                              .padding(vertical = 5.dp)
                              .fillMaxWidth(if (isLandscape) 0.8f else 1f)
                      ) {
                          Box(
                              contentAlignment = Alignment.Center,
                              modifier = modifier.weight(1f)
                          ) {
                              BasicText(
                                  text = stringResource(R.string.id) + " : " + mediaId,
                                  maxLines = 1,
                                  style = typography().xs.medium.color(colorPalette().text)
                              )
                          }
                          if (format?.songId?.startsWith(LOCAL_KEY_PREFIX) == false) {
                              Box(
                                  contentAlignment = Alignment.Center,
                                  modifier = modifier.weight(1f)
                              ) {
                                  BasicText(
                                      text = (stringResource(R.string.itag) + " : " + format?.itag?.toString())
                                          ?: (stringResource(R.string.itag) + " : " + stringResource(
                                              R.string.audio_quality_format_unknown
                                          )),
                                      maxLines = 1,
                                      style = typography().xs.medium.color(colorPalette().text)
                                  )
                              }
                          }
                      }
                      Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.Center,
                          modifier = modifier
                              .background(colorPalette().background2.copy(alpha = if ((transparentBackgroundActionBarPlayer) || ((playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient) || (playerBackgroundColors == PlayerBackgroundColors.ThemeColorGradient)) && blackgradient) 0.0f else 0.7f))
                              .padding(vertical = 5.dp)
                              .fillMaxWidth(if (isLandscape) 0.8f else 1f)
                      ) {
                          if (format?.songId?.startsWith(LOCAL_KEY_PREFIX) == true) {
                              Box(
                                  contentAlignment = Alignment.Center,
                                  modifier = modifier.weight(1f)
                              ) {
                                  BasicText(
                                      text = stringResource(R.string.cached) + " : " + "100%",
                                      maxLines = 1,
                                      style = typography().xs.medium.color(colorPalette().text)
                                  )
                              }
                          }
                          if (format?.songId?.startsWith(LOCAL_KEY_PREFIX) == false) {
                              Box(
                                  contentAlignment = Alignment.Center,
                                  modifier = modifier.weight(1f)
                              ) {
                                  BasicText(
                                      text =  if (downloadCachedBytes == 0L)
                                                  stringResource(R.string.cached) + " : " + Formatter.formatShortFileSize(
                                                      context,
                                                      cachedBytes
                                                  )
                                                  + format?.contentLength?.let {
                                                          " (${(cachedBytes.toFloat() / it * 100).roundToInt()}%)"
                                                  }
                                          else stringResource(R.string.downloaded) + " : " + Formatter.formatShortFileSize(
                                                  context,
                                                  downloadCachedBytes
                                              )
                                          + format?.contentLength?.let {
                                               " (${(downloadCachedBytes.toFloat() / it * 100).roundToInt()}%)"
                                          }
                                      ,
                                      maxLines = 1,
                                      style = typography().xs.medium.color(colorPalette().text)
                                  )
                              }
                              Box(
                                  contentAlignment = Alignment.Center,
                                  modifier = modifier.weight(1f)
                              ) {
                                  BasicText(
                                      text = format?.loudnessDb?.let {
                                          stringResource(R.string.loudness) + " : " + "%.2f dB".format(
                                              it
                                          )
                                      }
                                          ?: (stringResource(R.string.loudness) + " : " + stringResource(
                                              R.string.audio_quality_format_unknown
                                          )),
                                      maxLines = 1,
                                      style = typography().xs.medium.color(colorPalette().text)
                                  )
                              }
                          }
                      }
                  }
                }
            }

        }
    }
}


@Composable
fun getQuality(format: Format): String {
    return when (format.itag?.toString()) {
        "251", "141" -> stringResource(R.string.audio_quality_format_high)
        "250", "140", "171" -> stringResource(R.string.audio_quality_format_medium)
        "249", "139" -> stringResource(R.string.audio_quality_format_low)
        else -> format.itag.toString()
    }
}