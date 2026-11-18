package app.cubic.android.lastfm

import android.content.Context
import androidx.core.content.edit
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.utils.encryptedPreferences
import app.it.fast4x.rimusic.utils.preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.min

/**
 * Playback-independent Last.fm observer. It watches Media3 but never controls or blocks playback.
 * Runtime setting changes apply to the active track without requiring an app or service restart.
 */
class CubicLastFmScrobbler(private val context: Context) {
    private enum class Phase { Idle, Active, Paused, Done }

    private data class TrackMetadata(
        val artist: String,
        val title: String,
        val album: String?,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var timerJob: Job? = null
    @Volatile private var generation = 0L
    @Volatile private var phase = Phase.Idle
    @Volatile private var trackedMediaId: String? = null
    @Volatile private var trackedItem: MediaItem? = null
    @Volatile private var durationMs = C.TIME_UNSET
    @Volatile private var remainingMs = 0L
    @Volatile private var timerStartedAtMs = 0L
    @Volatile private var trackStartedAtEpochSeconds = 0L
    @Volatile private var isCurrentlyPlaying = false
    @Volatile private var nowPlayingSent = false
    @Volatile private var scrobbleSubmitted = false
    @Volatile private var destroyed = false

    fun onTrackTransition(mediaItem: MediaItem?, duration: Long) = synchronized(this) {
        if (destroyed) return@synchronized
        cancelTimer()
        phase = Phase.Idle
        trackedMediaId = mediaItem?.mediaId
        trackedItem = mediaItem
        durationMs = duration
        remainingMs = 0L
        timerStartedAtMs = 0L
        trackStartedAtEpochSeconds = 0L
        isCurrentlyPlaying = false
        nowPlayingSent = false
        scrobbleSubmitted = false
    }

    fun onPlayingStateChanged(isPlaying: Boolean, mediaItem: MediaItem?, duration: Long) =
        synchronized(this) {
            if (destroyed) return@synchronized
            if (mediaItem == null) {
                cancelTimer()
                trackedMediaId = null
                trackedItem = null
                phase = Phase.Idle
                return@synchronized
            }
            if (mediaItem.mediaId != trackedMediaId) return@synchronized

            trackedItem = mediaItem
            isCurrentlyPlaying = isPlaying
            if (duration > 0L && duration != C.TIME_UNSET) durationMs = duration

            if (!isPlaying) {
                if (phase == Phase.Active) pauseTimer()
                return@synchronized
            }

            when (phase) {
                Phase.Idle -> startEligibleTrack(mediaItem, resume = false)
                Phase.Paused -> resumeTimer(mediaItem)
                Phase.Done -> {
                    if (!scrobbleSubmitted && scrobblingEnabled()) {
                        startEligibleTrack(mediaItem, resume = false)
                    } else if (!nowPlayingSent && nowPlayingEnabled()) {
                        sendNowPlayingOnly(mediaItem)
                    }
                }
                Phase.Active -> Unit
            }
        }

    /** Apply account-page switches immediately to the current track. */
    fun onConfigurationChanged() = synchronized(this) {
        if (destroyed) return@synchronized
        val mediaItem = trackedItem ?: return@synchronized

        if (!scrobblingEnabled() && (phase == Phase.Active || phase == Phase.Paused)) {
            cancelTimer()
            remainingMs = 0L
            phase = Phase.Done
        }

        if (isCurrentlyPlaying && nowPlayingEnabled() && !nowPlayingSent) {
            sendNowPlayingOnly(mediaItem)
        }

        if (
            isCurrentlyPlaying &&
            scrobblingEnabled() &&
            !scrobbleSubmitted &&
            phase != Phase.Active
        ) {
            startEligibleTrack(mediaItem, resume = false)
        }
    }

    fun destroy() = synchronized(this) {
        if (destroyed) return@synchronized
        destroyed = true
        cancelTimer()
        trackedMediaId = null
        trackedItem = null
        scope.cancel()
    }

    private fun startEligibleTrack(mediaItem: MediaItem, resume: Boolean) {
        if (durationMs <= minimumTrackDurationMs || durationMs == C.TIME_UNSET) return
        if (!resume) {
            if (trackStartedAtEpochSeconds == 0L) {
                trackStartedAtEpochSeconds = System.currentTimeMillis() / 1000L
            }
            remainingMs = min(durationMs / 2L, maximumScrobbleDelayMs)
        }
        phase = Phase.Active
        launchTimer(mediaItem, resume)
    }

    private fun resumeTimer(mediaItem: MediaItem) {
        if (remainingMs <= 0L) {
            phase = Phase.Done
            return
        }
        startEligibleTrack(mediaItem, resume = true)
    }

    private fun launchTimer(mediaItem: MediaItem, resume: Boolean) {
        generation++
        val activeGeneration = generation
        val mediaId = mediaItem.mediaId
        timerStartedAtMs = System.currentTimeMillis()
        timerJob = scope.launch {
            val metadata = resolveMetadata(mediaItem)
            if (metadata.artist.isBlank() || metadata.title.isBlank()) {
                Timber.tag(tag).d("Skipped Last.fm update because track metadata is incomplete")
                phase = Phase.Done
                return@launch
            }
            if (activeGeneration != generation || trackedMediaId != mediaId || destroyed) return@launch

            if (!resume) sendNowPlaying(metadata)

            if (!scrobblingEnabled()) {
                phase = Phase.Done
                return@launch
            }

            delay(remainingMs)
            if (activeGeneration != generation || trackedMediaId != mediaId || destroyed) return@launch
            phase = Phase.Done
            scrobbleSubmitted = true
            CubicLastFmClient.scrobble(
                artist = metadata.artist,
                track = metadata.title,
                album = metadata.album,
                startedAtEpochSeconds = trackStartedAtEpochSeconds,
                durationSeconds = durationSeconds(),
            ).onSuccess {
                context.preferences.edit {
                    putLong(lastFmLastScrobbleAtKey, System.currentTimeMillis())
                }
                Timber.tag(tag).d("Scrobbled '%s' by '%s'", metadata.title, metadata.artist)
            }.onFailure(::handleFailure)
        }
    }

    private fun sendNowPlayingOnly(mediaItem: MediaItem) {
        if (nowPlayingSent || !nowPlayingEnabled()) return
        nowPlayingSent = true
        val mediaId = mediaItem.mediaId
        scope.launch {
            val metadata = resolveMetadata(mediaItem)
            if (metadata.artist.isBlank() || metadata.title.isBlank()) return@launch
            if (trackedMediaId != mediaId || destroyed) return@launch
            submitNowPlaying(metadata)
        }
    }

    private suspend fun sendNowPlaying(metadata: TrackMetadata) {
        if (nowPlayingSent || !nowPlayingEnabled()) return
        nowPlayingSent = true
        submitNowPlaying(metadata)
    }

    private suspend fun submitNowPlaying(metadata: TrackMetadata) {
        CubicLastFmClient.updateNowPlaying(
            artist = metadata.artist,
            track = metadata.title,
            album = metadata.album,
            durationSeconds = durationSeconds(),
        ).onSuccess {
            Timber.tag(tag).d("Now playing '%s' by '%s'", metadata.title, metadata.artist)
        }.onFailure(::handleFailure)
    }

    private suspend fun resolveMetadata(mediaItem: MediaItem): TrackMetadata {
        var artist = mediaItem.mediaMetadata.artist?.toString()?.trim().orEmpty()
            .ifBlank { mediaItem.mediaMetadata.albumArtist?.toString()?.trim().orEmpty() }
        var title = mediaItem.mediaMetadata.title?.toString()?.trim().orEmpty()
        val album = mediaItem.mediaMetadata.albumTitle?.toString()?.trim()?.ifBlank { null }

        if (artist.isBlank() || title.isBlank()) {
            val databaseSong = runCatching {
                Database.songTable.findById(mediaItem.mediaId).first()
            }.getOrNull()
            if (artist.isBlank()) artist = databaseSong?.artistsText?.trim().orEmpty()
            if (title.isBlank()) title = databaseSong?.title?.trim().orEmpty()
        }
        return TrackMetadata(artist = artist, title = title, album = album)
    }

    private fun pauseTimer() {
        if (timerStartedAtMs > 0L) {
            remainingMs = (remainingMs - (System.currentTimeMillis() - timerStartedAtMs))
                .coerceAtLeast(0L)
        }
        cancelTimer()
        phase = if (remainingMs > 0L) Phase.Paused else Phase.Done
    }

    private fun cancelTimer() {
        generation++
        timerJob?.cancel()
        timerJob = null
        timerStartedAtMs = 0L
    }

    private fun nowPlayingEnabled(): Boolean =
        context.preferences.getBoolean(lastFmNowPlayingEnabledKey, true)

    private fun scrobblingEnabled(): Boolean =
        context.preferences.getBoolean(lastFmScrobblingEnabledKey, true)

    private fun durationSeconds(): Long =
        durationMs.takeIf { it > 0L && it != C.TIME_UNSET }?.div(1000L) ?: 0L

    private fun handleFailure(error: Throwable) {
        Timber.tag(tag).w(error, "Last.fm background update failed")
        if (error is LastFmApiException && error.errorCode == invalidSessionError) {
            context.encryptedPreferences.edit {
                remove(lastFmSessionKey)
                remove(lastFmUsernameKey)
                remove(lastFmAvatarUrlKey)
            }
            CubicLastFmClient.sessionKey = null
        }
    }

    private companion object {
        const val tag = "CubicLastFm"
        const val invalidSessionError = 9
        const val minimumTrackDurationMs = 30_000L
        const val maximumScrobbleDelayMs = 4 * 60_000L
    }
}
