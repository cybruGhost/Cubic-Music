package app.cubic.android.cast

import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import timber.log.Timber
import java.util.Locale
import kotlin.coroutines.resume

/** The local deck retains queue ownership but never decodes while the receiver is the output. */
class CastQueuePlayer(
    val local: ExoPlayer,
    private val session: CastSession,
    private val scope: CoroutineScope,
    private val onDisconnected: () -> Unit,
) : ExoPlayer by local {
    private val remote: RemoteMediaClient = requireNotNull(session.remoteMediaClient)
    private val listeners = linkedSetOf<Player.Listener>()
    private var active = false
    private var changingQueue = false
    private var requestedPlay = true
    private var loading = false
    private var failure: androidx.media3.exoplayer.ExoPlaybackException? = null
    private val receiverIdle: Boolean get() = remote.playerState == MediaStatus.PLAYER_STATE_IDLE
    private var relay: CastStreamRelay? = null
    private var loadGeneration = 0L
    private var pendingPlay: Boolean? = null
    private var pendingPlayAt = 0L
    private var loadJob: Job? = null
    private var preloadJob: Job? = null
    private var monitorJob: Job? = null
    private var preparedIndex = C.INDEX_UNSET
    private var preparedId: String? = null
    private var preparedStream: CastStream? = null
    private var lastPosition = 0L
    private var lastDuration = C.TIME_UNSET
    private var lastState = Player.STATE_IDLE
    private var lastPlaying = false
    private var lastRequestedPlay = true
    private var recoveryAttempts = 0
    private var finishedHandled = false
    private var pendingLocalSyncIndex = C.INDEX_UNSET
    private var pendingLocalSyncId: String? = null
    private val queueListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (!active) return
            if (changingQueue ||
                (local.currentMediaItemIndex == pendingLocalSyncIndex && mediaItem?.mediaId == pendingLocalSyncId)
            ) {
                pendingLocalSyncIndex = C.INDEX_UNSET
                pendingLocalSyncId = null
                return
            }
            recoveryAttempts = 0
            loadCurrent(0L)
            publishTransition(reason)
        }
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (!active) return
            listeners.toList().forEach { it.onTimelineChanged(timeline, reason) }
            publishEvents(Player.EVENT_TIMELINE_CHANGED)
            if (!changingQueue) invalidatePreparedNext()
        }
        override fun onRepeatModeChanged(repeatMode: Int) {
            listeners.toList().forEach { it.onRepeatModeChanged(repeatMode) }
            invalidatePreparedNext()
        }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            listeners.toList().forEach { it.onShuffleModeEnabledChanged(shuffleModeEnabled) }
            invalidatePreparedNext()
        }
    }

    suspend fun loadInitial(): Boolean {
        val index = local.currentMediaItemIndex
        val item = local.currentMediaItem ?: return false
        return load(item, index, initial = true, position = local.currentPosition)
    }

    fun activate() {
        active = true
        local.addListener(queueListener)
        pendingPlay = true
        pendingPlayAt = android.os.SystemClock.elapsedRealtime()
        remote.play()
        monitorJob = scope.launch {
            delay(250L)
            var disconnectedAt = 0L
            while (isActive && active) {
                if (!session.isConnected) {
                    if (disconnectedAt == 0L) disconnectedAt = android.os.SystemClock.elapsedRealtime()
                    if (!session.isResuming && android.os.SystemClock.elapsedRealtime() - disconnectedAt > 5_000L) {
                        onDisconnected()
                        break
                    }
                } else {
                    disconnectedAt = 0L
                    try { updateFromReceiver() }
                    catch (e: Exception) {
                        Timber.w("Cast status update failed: %s", e.javaClass.simpleName)
                        onDisconnected()
                        break
                    }
                }
                delay(250L)
            }
        }
        publishState()
    }

    fun detach() {
        active = false
        ++loadGeneration
        loadJob?.cancel()
        preloadJob?.cancel()
        monitorJob?.cancel()
        local.removeListener(queueListener)
        listeners.clear()
        val relayToClose = relay
        relay = null
        preparedStream = null
        // Closing live OkHttp sockets can perform network I/O on some Android builds.
        // Cast teardown is initiated from the UI, so release the relay away from main.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            relayToClose?.stop()
        }
        scope.coroutineContext[Job]?.cancel()
    }

    private fun updateFromReceiver() {
        val info = remote.mediaInfo
        val queuedIndex = info?.customData?.optInt("cubicIndex", C.INDEX_UNSET) ?: C.INDEX_UNSET
        val id = info?.customData?.optString("cubicMediaId")
        val index = when {
            queuedIndex in 0 until local.mediaItemCount && local.getMediaItemAt(queuedIndex).mediaId == id -> queuedIndex
            local.currentMediaItem?.mediaId == id -> local.currentMediaItemIndex
            else -> (0 until local.mediaItemCount).firstOrNull { local.getMediaItemAt(it).mediaId == id } ?: C.INDEX_UNSET
        }
        if (!loading && index in 0 until local.mediaItemCount) {
            if (index != local.currentMediaItemIndex) {
                lastPosition = remote.approximateStreamPosition.coerceAtLeast(0L)
                remote.streamDuration.takeIf { it > 0L }?.let { lastDuration = it }
                syncLocalQueuePosition(index, lastPosition)
                preparedIndex = C.INDEX_UNSET
                preparedId = null
                preparedStream = null
                recoveryAttempts = 0
                finishedHandled = false
                publishTransition(Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
            }
            val receiverPosition = remote.approximateStreamPosition.coerceAtLeast(0L)
            if (!receiverIdle || receiverPosition > lastPosition) lastPosition = receiverPosition
            remote.streamDuration.takeIf { it > 0L }?.let { lastDuration = it }
            if (remote.isPlaying || remote.isPaused) {
                val desired = pendingPlay
                if (desired == null || desired == remote.isPlaying || android.os.SystemClock.elapsedRealtime() - pendingPlayAt > 3_000L) {
                    requestedPlay = remote.isPlaying
                    pendingPlay = null
                }
                finishedHandled = false
                failure = null
            }
            if (receiverIdle && remote.mediaStatus?.idleReason == MediaStatus.IDLE_REASON_ERROR && failure == null) {
                if (recoveryAttempts++ < 1) loadCurrent(lastPosition)
                else reportFailure()
            } else if (receiverIdle && remote.mediaStatus?.idleReason == MediaStatus.IDLE_REASON_FINISHED && !finishedHandled) {
                finishedHandled = true
                val duration = lastDuration.takeIf { it > 0L }
                val finishSlack = duration?.let { maxOf(3_000L, it / 100L) } ?: 0L
                val reachedRealEnd = duration != null && lastPosition >= duration - finishSlack
                if (!reachedRealEnd) {
                    // A receiver can report FINISHED after abandoning a short/probed HTTP range.
                    // Recover the same song instead of incorrectly advancing Cubic's queue.
                    if (recoveryAttempts++ < 2) loadCurrent(lastPosition) else reportFailure()
                } else if (local.repeatMode == Player.REPEAT_MODE_ONE) loadCurrent(0L)
                else if (local.hasNextMediaItem()) seekToNextMediaItem()
                else requestedPlay = false
            }
            val remaining = lastDuration.takeIf { it > 0L }?.minus(lastPosition)
            if (!receiverIdle && remaining != null && remaining in 0L..45_000L) preloadNext()
        }
        publishState()
    }

    private fun info(
        item: MediaItem,
        index: Int,
        stream: CastStream,
        lyricsTrack: MediaTrack? = null,
    ): MediaInfo {
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_TITLE, item.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() } ?: stream.title)
            putString(MediaMetadata.KEY_ARTIST, item.mediaMetadata.artist?.toString()?.takeIf { it.isNotBlank() } ?: stream.artist)
            putString(MediaMetadata.KEY_ALBUM_TITLE, "Cubic Music")
            val art = item.mediaMetadata.artworkUri?.takeIf { it.scheme == "https" }
                ?: android.net.Uri.parse("https://i.ytimg.com/vi/${item.mediaId}/hqdefault.jpg")
            addImage(WebImage(art))
        }
        return MediaInfo.Builder(stream.url).setContentType(stream.mimeType)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED).setMetadata(metadata)
            .setCustomData(JSONObject().put("cubicMediaId", item.mediaId).put("cubicIndex", index))
            .apply { lyricsTrack?.let { setMediaTracks(listOf(it)) } }
            .apply { if (stream.durationMs > 0L) setStreamDuration(stream.durationMs) }.build()
    }

    private suspend fun load(item: MediaItem, index: Int, initial: Boolean, position: Long): Boolean = withTimeout(60_000L) {
        val prepared = preparedStream?.takeIf { preparedIndex == index && preparedId == item.mediaId }
        if (prepared != null) {
            preparedStream = null
            preparedIndex = C.INDEX_UNSET
            preparedId = null
            if (loadOnReceiver(item, index, prepared, initial, position)) return@withTimeout true
        }
        var attempts = 0
        CastStreamResolver.tryCandidates(item.mediaId, skipPlaybackCache = recoveryAttempts > 0) { stream ->
            check(session.isConnected) { "Cast session disconnected" }
            check(local.currentMediaItemIndex == index && local.currentMediaItem?.mediaId == item.mediaId) { "Queue changed during Cast load" }
            check(attempts++ < 4) { "Cast attempts exhausted" }
            // A receiver cannot reliably reproduce the headers/range semantics of a
            // short-lived googlevideo URL. Prefer Cubic's LAN relay, then retain the
            // direct URL as a compatibility fallback when the relay is unavailable.
            val forwarded = relayStream(stream)
            when {
                forwarded != null && loadOnReceiver(item, index, forwarded, initial, position) -> true
                !stream.requiresRelay -> loadOnReceiver(item, index, stream, initial, position)
                else -> false
            }
        }
    }

    private suspend fun relayStream(stream: CastStream): CastStream? = try {
        if (relay == null) {
            relay = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                CastStreamRelay.create(app.it.fast4x.rimusic.appContext())
            }
        }
        relay?.add(stream, remote.mediaInfo?.contentId)
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) {
        Timber.w("Cast relay unavailable: %s", e.javaClass.simpleName)
        null
    }

    private suspend fun loadOnReceiver(
        item: MediaItem,
        index: Int,
        stream: CastStream,
        initial: Boolean,
        position: Long,
    ): Boolean {
        val lyricsTrack = createLyricsTrack(item, stream.durationMs)
        if (tryLoadOnReceiver(item, index, stream, initial, position, lyricsTrack)) return true
        if (lyricsTrack != null) {
            // A few third-party receivers reject text tracks entirely. Retry this exact
            // audio source without lyrics so captions can never prevent playback.
            Timber.w("Cast receiver rejected timed lyrics for id=%s; retrying audio only", item.mediaId)
            return tryLoadOnReceiver(item, index, stream, initial, position, null)
        }
        return false
    }

    private suspend fun tryLoadOnReceiver(
        item: MediaItem,
        index: Int,
        stream: CastStream,
        initial: Boolean,
        position: Long,
        lyricsTrack: MediaTrack?,
    ): Boolean {
        try {
            return withTimeoutOrNull(15_000L) {
                val start = if (initial) local.currentPosition else position
                val request = MediaLoadRequestData.Builder()
                    .setMediaInfo(info(item, index, stream, lyricsTrack))
                    .setAutoplay(!initial && requestedPlay)
                    .setCurrentTime(start.coerceAtLeast(0L))
                    .apply { lyricsTrack?.let { setActiveTrackIds(longArrayOf(it.id)) } }
                    .build()
                remote.load(request).awaitCastResult()
                delay(350L)
                while (true) {
                    check(session.isConnected) { "Cast disconnected during load" }
                    if (remote.mediaInfo?.contentId == stream.url) {
                        if (remote.isPlaying || remote.isPaused) break
                        check(!receiverIdle || remote.mediaStatus?.idleReason != MediaStatus.IDLE_REASON_ERROR) { "Receiver rejected media" }
                    }
                    delay(150L)
                }
                lastPosition = remote.approximateStreamPosition.coerceAtLeast(0L)
                lastDuration = stream.durationMs.takeIf { it > 0 } ?: C.TIME_UNSET
                true
            } == true
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            Timber.w("Cast receiver rejected id=%s candidate=%s failure=%s", item.mediaId, stream.candidateId, e.javaClass.simpleName)
            return false
        }
    }

    private suspend fun createLyricsTrack(item: MediaItem, durationMs: Long): MediaTrack? {
        val activeRelay = relay ?: return null
        val syncedLyrics = try {
            withContext(Dispatchers.IO) {
                app.it.fast4x.rimusic.Database.lyricsTable.findBySongId(item.mediaId).first()?.synced
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w("Cast lyrics lookup failed for id=%s: %s", item.mediaId, error.javaClass.simpleName)
            null
        }?.takeIf { it.isNotBlank() } ?: return null
        val webVtt = syncedLyrics.toWebVtt(durationMs) ?: return null
        val url = activeRelay.addLyrics(item.mediaId, webVtt)
        return MediaTrack.Builder(CAST_LYRICS_TRACK_ID, MediaTrack.TYPE_TEXT)
            .setContentId(url)
            .setContentType("text/vtt")
            .setName("Live lyrics")
            .setLanguage(Locale.getDefault())
            .setSubtype(MediaTrack.SUBTYPE_CAPTIONS)
            .build()
    }


    private fun loadCurrent(position: Long) {
        val item = local.currentMediaItem ?: run { stop(); return }
        val generation = ++loadGeneration
        loadJob?.cancel()
        preloadJob?.cancel()
        if (preparedIndex != local.currentMediaItemIndex || preparedId != item.mediaId) {
            preparedIndex = C.INDEX_UNSET
            preparedId = null
            preparedStream = null
        }
        loading = true
        failure = null
        finishedHandled = false
        lastPosition = position
        lastDuration = C.TIME_UNSET
        publishState()
        loadJob = scope.launch {
            try {
                remote.pause()
                if (!load(item, local.currentMediaItemIndex, false, position)) reportFailure()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { reportFailure() }
            finally { if (generation == loadGeneration) { loading = false; publishState() } }
        }
    }

    private fun invalidatePreparedNext() {
        preloadJob?.cancel()
        preparedIndex = C.INDEX_UNSET
        preparedId = null
        preparedStream = null
    }

    private fun preloadNext() {
        if (!active || loading) return
        val next = if (local.repeatMode == Player.REPEAT_MODE_ONE) C.INDEX_UNSET else local.nextMediaItemIndex
        val item = next.takeIf { it in 0 until local.mediaItemCount }?.let(local::getMediaItemAt)
        if (next == preparedIndex && item?.mediaId == preparedId) return
        preloadJob?.cancel()
        preparedIndex = next
        preparedId = item?.mediaId
        preparedStream = null
        preloadJob = scope.launch {
            try {
                if (item == null || !Regex("[A-Za-z0-9_-]{11}").matches(item.mediaId)) return@launch
                withTimeout(45_000L) {
                    CastStreamResolver.tryCandidates(item.mediaId) { stream ->
                        if (!active || local.nextMediaItemIndex != next || local.getMediaItemAt(next).mediaId != item.mediaId) return@tryCandidates false
                        val source = relayStream(stream)
                            ?: stream.takeUnless { it.requiresRelay }
                            ?: return@tryCandidates false
                        preparedStream = source
                        true
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Timber.w("Cast next-item preload failed: %s", e.javaClass.simpleName) }
        }
    }

    private fun reportFailure() {
        failure = androidx.media3.exoplayer.ExoPlaybackException.createForRemote("The Cast receiver could not play this track")
        requestedPlay = false
        listeners.toList().forEach { it.onPlayerError(failure!!) }
        publishEvents(Player.EVENT_PLAYER_ERROR)
    }

    private fun syncLocalQueuePosition(index: Int, positionMs: Long) {
        pendingLocalSyncIndex = index
        pendingLocalSyncId = local.getMediaItemAt(index).mediaId
        changingQueue = true
        try {
            local.seekTo(index, positionMs.coerceAtLeast(0L))
        } finally {
            changingQueue = false
        }
    }

    private fun publishTransition(reason: Int) {
        listeners.toList().forEach { it.onMediaItemTransition(local.currentMediaItem, reason); it.onMediaMetadataChanged(local.mediaMetadata) }
        publishEvents(
            Player.EVENT_MEDIA_ITEM_TRANSITION,
            Player.EVENT_MEDIA_METADATA_CHANGED,
            Player.EVENT_POSITION_DISCONTINUITY,
        )
    }
    private fun publishState() {
        val state = playbackState
        val playing = isPlaying
        val changed = mutableListOf<Int>()
        if (lastState != state) { lastState = state; listeners.toList().forEach { it.onPlaybackStateChanged(state) }; changed += Player.EVENT_PLAYBACK_STATE_CHANGED }
        if (lastRequestedPlay != requestedPlay) { lastRequestedPlay = requestedPlay; listeners.toList().forEach { it.onPlayWhenReadyChanged(requestedPlay, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }; changed += Player.EVENT_PLAY_WHEN_READY_CHANGED }
        if (lastPlaying != playing) { lastPlaying = playing; listeners.toList().forEach { it.onIsPlayingChanged(playing) }; changed += Player.EVENT_IS_PLAYING_CHANGED }
        if (changed.isNotEmpty()) publishEvents(*changed.toIntArray())
    }
    private fun publishEvents(vararg events: Int) {
        val flags = androidx.media3.common.FlagSet.Builder().addAll(*events).build()
        listeners.toList().forEach { it.onEvents(this, Player.Events(flags)) }
    }

    override fun addListener(listener: Player.Listener) { listeners.add(listener) }
    override fun removeListener(listener: Player.Listener) { listeners.remove(listener) }
    override fun getApplicationLooper(): Looper = local.applicationLooper
    override fun getPlaybackState(): Int = when { failure != null -> Player.STATE_IDLE; loading -> Player.STATE_BUFFERING; remote.isBuffering || remote.isLoadingNextItem -> Player.STATE_BUFFERING; receiverIdle && finishedHandled -> Player.STATE_ENDED; else -> Player.STATE_READY }
    override fun getPlayWhenReady() = requestedPlay
    override fun isPlaying() = active && !loading && remote.isPlaying
    override fun isLoading() = loading || remote.isBuffering
    override fun getPlaybackSuppressionReason() = Player.PLAYBACK_SUPPRESSION_REASON_NONE
    override fun getPlayerError() = failure
    override fun getCurrentPosition() = if (loading || !session.isConnected) lastPosition else remote.approximateStreamPosition.coerceAtLeast(0L)
    override fun getDuration() = remote.streamDuration.takeIf { !loading && it > 0 } ?: lastDuration
    override fun getContentPosition() = currentPosition
    override fun getContentDuration() = duration
    override fun getBufferedPosition() = currentPosition
    override fun getContentBufferedPosition() = currentPosition
    override fun getTotalBufferedDuration() = 0L
    override fun getBufferedPercentage() = 0
    override fun play() = setPlayWhenReady(true)
    override fun pause() = setPlayWhenReady(false)
    override fun setPlayWhenReady(playWhenReady: Boolean) {
        requestedPlay = playWhenReady
        pendingPlay = playWhenReady
        pendingPlayAt = android.os.SystemClock.elapsedRealtime()
        if (failure != null && playWhenReady) loadCurrent(lastPosition)
        else if (!loading) { if (playWhenReady) remote.play() else remote.pause() }
        publishState()
    }
    override fun prepare() { if (failure != null) loadCurrent(lastPosition) }
    override fun stop() { loadJob?.cancel(); preloadJob?.cancel(); loading = false; requestedPlay = false; remote.stop(); finishedHandled = true; publishState() }
    override fun release() { detach(); local.release() }
    override fun seekTo(positionMs: Long) {
        lastPosition = positionMs.coerceAtLeast(0L)
        if (loading) loadCurrent(lastPosition)
        else remote.seek(MediaSeekOptions.Builder().setPosition(lastPosition).build())
    }
    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        if (mediaItemIndex == local.currentMediaItemIndex) seekTo(positionMs)
        else {
            syncLocalQueuePosition(mediaItemIndex, positionMs)
            recoveryAttempts = 0
            loadCurrent(positionMs.coerceAtLeast(0))
            publishTransition(Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
        }
    }
    override fun seekToDefaultPosition() = seekTo(0L)
    override fun seekToDefaultPosition(mediaItemIndex: Int) = seekTo(mediaItemIndex, 0L)
    override fun seekToNextMediaItem() { if (local.hasNextMediaItem()) seekTo(local.nextMediaItemIndex, 0L) }
    override fun seekToPreviousMediaItem() { if (local.hasPreviousMediaItem()) seekTo(local.previousMediaItemIndex, 0L) }
    override fun seekToNext() = seekToNextMediaItem()
    override fun seekToPrevious() { if (currentPosition > maxSeekToPreviousPosition) seekTo(0L) else seekToPreviousMediaItem() }
    override fun seekBack() = seekTo((currentPosition - seekBackIncrement).coerceAtLeast(0))
    override fun seekForward() = seekTo((currentPosition + seekForwardIncrement).coerceAtMost(duration.coerceAtLeast(0)))
}

private const val CAST_LYRICS_TRACK_ID = 73L
private val lrcTimestamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

private fun String.toWebVtt(trackDurationMs: Long): String? {
    val timedLines = buildList {
        lineSequence().forEach lineLoop@ { rawLine ->
            val timestamps = lrcTimestamp.findAll(rawLine).toList()
            val text = lrcTimestamp.replace(rawLine, "").trim()
            if (timestamps.isEmpty() || text.isBlank()) return@lineLoop
            timestamps.forEach timestampLoop@ { timestamp ->
                val minutes = timestamp.groupValues[1].toLongOrNull() ?: return@timestampLoop
                val seconds = timestamp.groupValues[2].toLongOrNull() ?: return@timestampLoop
                val fraction = timestamp.groupValues[3]
                val millis = when (fraction.length) {
                    1 -> fraction.toLongOrNull()?.times(100L)
                    2 -> fraction.toLongOrNull()?.times(10L)
                    else -> fraction.take(3).padEnd(3, '0').toLongOrNull()
                } ?: 0L
                add(((minutes * 60L + seconds) * 1_000L + millis) to text)
            }
        }
    }.sortedBy { it.first }
        .distinctBy { it.first }
    if (timedLines.isEmpty()) return null

    return buildString {
        append("WEBVTT\n\n")
        timedLines.forEachIndexed { index, (startMs, rawText) ->
            val nextStartMs = timedLines.getOrNull(index + 1)?.first
            val naturalEndMs = nextStartMs
                ?: trackDurationMs.takeIf { it > startMs }
                ?: (startMs + 5_000L)
            val endMs = naturalEndMs.coerceAtLeast(startMs + 250L)
            append(startMs.asWebVttTimestamp())
            append(" --> ")
            append(endMs.asWebVttTimestamp())
            append('\n')
            append(
                rawText
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
            )
            append("\n\n")
        }
    }
}

private fun Long.asWebVttTimestamp(): String {
    val safe = coerceAtLeast(0L)
    val hours = safe / 3_600_000L
    val minutes = safe / 60_000L % 60L
    val seconds = safe / 1_000L % 60L
    val millis = safe % 1_000L
    return String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
}

internal suspend fun PendingResult<RemoteMediaClient.MediaChannelResult>.awaitCastResult() {
    val result = suspendCancellableCoroutine<RemoteMediaClient.MediaChannelResult> { continuation ->
        setResultCallback { if (continuation.isActive) continuation.resume(it) }
        continuation.invokeOnCancellation { cancel() }
    }
    check(result.status.isSuccess) { "Cast command status=${result.status.statusCode}" }
}
