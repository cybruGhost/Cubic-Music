package app.cubic.android.cast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import app.it.fast4x.rimusic.colorPalette
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import app.kreate.android.R
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

import timber.log.Timber
import kotlin.coroutines.resume

private class CastCommandException(val code: Int) : Exception("Cast command status=$code")

private suspend fun PendingResult<RemoteMediaClient.MediaChannelResult>.awaitSuccess() {
    val result = suspendCancellableCoroutine<RemoteMediaClient.MediaChannelResult> { continuation ->
        setResultCallback { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        continuation.invokeOnCancellation { cancel() }
    }
    if (!result.status.isSuccess) throw CastCommandException(result.status.statusCode)
}

@Composable
fun CubicCastDialog(
    mediaItem: MediaItem,
    startPositionMs: Long,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current ?: return
    val busy by binder.service.castBusy.collectAsState()
    val serviceError by binder.service.castError.collectAsState()
    val casting by binder.service.casting.collectAsState()
    val scope = rememberCoroutineScope()
    val palette = colorPalette()
    val dialogShape = RoundedCornerShape(24.dp)
    val displayedPlayer = app.it.fast4x.rimusic.ui.screens.player.rememberDisplayedPlayerState(binder)
    val selectedItem = displayedPlayer.mediaItem ?: mediaItem
    val controlColor = if (palette.isDark && palette.accent.luminance() < 0.18f) {
        lerp(palette.accent, Color.White, 0.55f)
    } else palette.accent
    var error by remember { mutableStateOf<Int?>(null) }
    val castContext = remember {
        runCatching {
            check(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
                ConnectionResult.SUCCESS)
            CastContext.getSharedInstance(context)
        }.getOrNull()
    }
    val router = remember(castContext) { castContext?.let { MediaRouter.getInstance(context) } }
    var routeSelector by remember(castContext) {
        mutableStateOf(castContext?.mergedSelector?.takeUnless { it.isEmpty }
            ?: MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(
                    CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
                ))
                .build())
    }
    var routes by remember { mutableStateOf<List<MediaRouter.RouteInfo>>(emptyList()) }
    var connectingRouteId by remember { mutableStateOf<String?>(null) }
    var session by remember { mutableStateOf<CastSession?>(null) }
    var scanAttempt by remember { mutableStateOf(0) }
    var scanning by remember { mutableStateOf(true) }
    LaunchedEffect(castContext, session != null, scanAttempt) {
        scanning = castContext != null && session == null
        if (scanning) {
            delay(12_000L)
            scanning = false
        }
    }
    DisposableEffect(router, routeSelector, session != null, scanning, scanAttempt) {
        val callback = object : MediaRouter.Callback() {}
        if (castContext != null && session == null) {
            router?.addCallback(routeSelector, callback, if (scanning) {
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
            } else {
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY
            })
        }
        onDispose { router?.removeCallback(callback) }
    }
    LaunchedEffect(connectingRouteId) {
        if (connectingRouteId != null) {
            delay(20_000)
            if (session == null) {
                connectingRouteId = null
                error = R.string.cast_control_failed
            }
        }
    }
    var remoteTitle by remember { mutableStateOf("") }
    var remoteArtist by remember { mutableStateOf("") }
    var remoteArtwork by remember { mutableStateOf<android.net.Uri?>(null) }
    var playing by remember { mutableStateOf(false) }
    var hasMedia by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(castContext) {
        while (isActive && castContext != null) {
            runCatching {
                // The SDK can replace its selector after asynchronous initialization.
                // Re-register discovery and filter routes with the same current selector.
                castContext.mergedSelector?.takeUnless { it.isEmpty }?.let { currentSelector ->
                    if (routeSelector != currentSelector) {
                        routeSelector = currentSelector
                        Timber.d("Cast discovery selector refreshed")
                    }
                }
                session = castContext.sessionManager.currentCastSession?.takeIf { it.isConnected }
                if (session != null) connectingRouteId = null
                routes = router?.routes.orEmpty().filter {
                    !it.isDefault && !it.isBluetooth && it.isEnabled && it.matchesSelector(routeSelector)
                }.distinctBy { it.id }
                val remote = session?.remoteMediaClient
                hasMedia = remote?.hasMediaSession() == true
                playing = if (binder.service.casting.value) binder.player.isPlaying else remote?.isPlaying == true
                remoteTitle = remote?.mediaInfo?.metadata?.getString(MediaMetadata.KEY_TITLE).orEmpty()
                remoteArtist = remote?.mediaInfo?.metadata?.getString(MediaMetadata.KEY_ARTIST).orEmpty()
                remoteArtwork = remote?.mediaInfo?.metadata?.images?.firstOrNull()?.url
                duration = if (binder.service.casting.value) binder.player.duration.coerceAtLeast(0L) else remote?.streamDuration?.coerceAtLeast(0L) ?: 0L
                if (!seeking) position = if (binder.service.casting.value) binder.player.currentPosition.coerceAtLeast(0L) else remote?.approximateStreamPosition?.coerceAtLeast(0L) ?: 0L
                volume = session?.volume?.toFloat()?.coerceIn(0f, 1f) ?: 0f
            }.onFailure { error = R.string.cast_control_failed }
            delay(300)
        }
    }

    fun sendSong() {
        if (busy) return
        val target = session ?: return
        if (!Regex("[A-Za-z0-9_-]{11}").matches(binder.player.currentMediaItem?.mediaId.orEmpty())) {
            error = R.string.cast_online_only
            return
        }
        error = null
        binder.service.castPlayerSession(target)
    }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = controlColor,
            onSurface = palette.text,
            onSurfaceVariant = palette.textSecondary,
            surface = palette.background1,
        )
    ) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.background1.copy(alpha = 0.98f),
        titleContentColor = palette.text,
        textContentColor = palette.textSecondary,
        tonalElevation = 0.dp,
        shape = dialogShape,
        modifier = Modifier.border(
            1.dp,
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), palette.accent.copy(alpha = 0.24f))),
            dialogShape
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.cast_title), modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.baseline_cast_24),
                    contentDescription = null,
                    tint = controlColor,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    session?.castDevice?.friendlyName ?: stringResource(
                        if (castContext == null) R.string.cast_unavailable else R.string.cast_choose_device
                    )
                )
                if (session == null && castContext != null) {
                    if (routes.isEmpty()) {
                        if (scanning) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text(stringResource(R.string.cast_searching), style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            Text(stringResource(R.string.cast_no_devices), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = {
                                error = null
                                scanAttempt++
                            }) {
                                Text(stringResource(R.string.cast_scan_again))
                            }
                        }
                    }
                    routes.forEach { route ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = connectingRouteId == null) {
                                    error = null
                                    connectingRouteId = route.id
                                    runCatching { route.select() }.onFailure {
                                        connectingRouteId = null
                                        error = R.string.cast_control_failed
                                    }
                                }.padding(vertical = 14.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(R.drawable.baseline_cast_24), null, tint = controlColor, modifier = Modifier.size(24.dp))
                            Text(route.name, modifier = Modifier.weight(1f), color = palette.text,
                                style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (connectingRouteId == route.id) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = (if (hasMedia) remoteArtwork else null) ?: selectedItem.mediaMetadata.artworkUri
                            ?: "https://i.ytimg.com/vi/${selectedItem.mediaId}/hqdefault.jpg",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (hasMedia && remoteTitle.isNotBlank()) remoteTitle
                            else selectedItem.mediaMetadata.title?.toString().orEmpty(),
                            color = palette.text,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if (hasMedia && remoteArtist.isNotBlank()) remoteArtist
                            else selectedItem.mediaMetadata.artist?.toString().orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (busy) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.cast_loading), style = MaterialTheme.typography.bodyMedium)
                }
                (error ?: serviceError)?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                if (hasMedia && !busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            scope.launch {
                                runCatching {
                                    if (binder.service.casting.value) {
                                        if (playing) binder.player.pause() else binder.player.play()
                                    } else {
                                        val remote = session?.remoteMediaClient ?: return@launch
                                        if (playing) remote.pause().awaitSuccess() else remote.play().awaitSuccess()
                                    }
                                }.onFailure { error = R.string.cast_control_failed }
                            }
                        }) {
                            Icon(
                                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                stringResource(if (playing) R.string.cast_pause else R.string.play),
                            )
                        }
                        if (duration > 0L) Slider(
                            modifier = Modifier.weight(1f),
                            value = (position.toFloat() / duration).coerceIn(0f, 1f),
                            onValueChange = { seeking = true; position = (it * duration).toLong() },
                            onValueChangeFinished = {
                                val seekTo = position
                                seeking = false
                                scope.launch {
                                    runCatching {
                                        if (binder.service.casting.value) binder.player.seekTo(seekTo)
                                        else session?.remoteMediaClient?.seek(
                                            MediaSeekOptions.Builder().setPosition(seekTo).build()
                                        )?.awaitSuccess()
                                    }.onFailure { error = R.string.cast_control_failed }
                                }
                            },
                        )
                    }
                }
                if (session != null) {
                    Text(stringResource(R.string.cast_volume), style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            runCatching { session?.volume = it.toDouble() }
                                .onFailure { error = R.string.cast_control_failed }
                        },
                    )
                }
                if (session != null) TextButton(
                    enabled = !busy,
                    onClick = {
                        binder.service.endCastPlayerSession()
                        runCatching { castContext?.sessionManager?.endCurrentSession(true) }
                            .onFailure { error = R.string.cast_control_failed }
                    },
                ) { Text(stringResource(R.string.cast_disconnect)) }
            }
        },
        confirmButton = {
            if (session != null && !casting) TextButton(enabled = !busy, onClick = ::sendSong) {
                Text(stringResource(R.string.cast_this_song))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
    }
}
