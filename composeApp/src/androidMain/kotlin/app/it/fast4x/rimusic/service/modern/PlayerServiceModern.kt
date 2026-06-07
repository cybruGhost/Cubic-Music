package app.it.fast4x.rimusic.service.modern

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.app.WallpaperManager.FLAG_LOCK
import android.app.WallpaperManager.FLAG_SYSTEM
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.MainThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioOffloadSupportProvider
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionToken
import app.kreate.android.R
import app.kreate.android.service.PlaybackSourceMonitor
import app.kreate.android.service.createDataSourceFactory
import app.kreate.android.widget.Widget
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.MoreExecutors
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.NavigationEndpoint
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.nextPage
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.MainActivity
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.enums.AudioQualityFormat
import app.it.fast4x.rimusic.enums.DurationInMilliseconds
import app.it.fast4x.rimusic.enums.ExoPlayerCacheLocation
import app.it.fast4x.rimusic.enums.ExoPlayerDiskCacheMaxSize
import app.it.fast4x.rimusic.enums.ExoPlayerMinTimeForEvent
import app.it.fast4x.rimusic.enums.NotificationButtons
import app.it.fast4x.rimusic.enums.NotificationType
import app.it.fast4x.rimusic.enums.PresetsReverb
import app.it.fast4x.rimusic.enums.QueueLoopType
import app.it.fast4x.rimusic.enums.WallpaperType
import app.it.fast4x.rimusic.extensions.audiovolume.AudioVolumeObserver
import app.it.fast4x.rimusic.extensions.audiovolume.OnAudioVolumeChangedListener
import app.it.fast4x.rimusic.extensions.connectivity.AndroidConnectivityObserverLegacy
import app.it.fast4x.rimusic.extensions.discord.DiscordPresenceManager
import app.it.fast4x.rimusic.isHandleAudioFocusEnabled
import app.it.fast4x.rimusic.models.Event
import app.it.fast4x.rimusic.models.PersistentQueue
import app.it.fast4x.rimusic.models.PersistentSong
import app.it.fast4x.rimusic.models.QueuedMediaItem
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.models.asMediaItem
import app.it.fast4x.rimusic.service.BitmapProvider
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.service.MyDownloadService
import app.it.fast4x.rimusic.service.NoInternetException
import app.it.fast4x.rimusic.service.PlayableFormatNotFoundException
import app.it.fast4x.rimusic.utils.CoilBitmapLoader
import app.it.fast4x.rimusic.utils.TimerJob
import app.it.fast4x.rimusic.utils.YouTubeRadio
import app.it.fast4x.rimusic.utils.activityPendingIntent
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.audioQualityFormatKey
import app.it.fast4x.rimusic.utils.audioReverbPresetKey
import app.it.fast4x.rimusic.utils.autoLoadSongsInQueueKey
import app.it.fast4x.rimusic.utils.bassboostEnabledKey
import app.it.fast4x.rimusic.utils.bassboostLevelKey
import app.it.fast4x.rimusic.utils.broadCastPendingIntent
import app.it.fast4x.rimusic.utils.closebackgroundPlayerKey
import app.it.fast4x.rimusic.utils.collect
import app.it.fast4x.rimusic.utils.discordPersonalAccessTokenKey
import app.it.fast4x.rimusic.utils.enableWallpaperKey
import app.it.fast4x.rimusic.utils.encryptedPreferences
import app.it.fast4x.rimusic.utils.exoPlayerCacheLocationKey
import app.it.fast4x.rimusic.utils.exoPlayerCustomCacheKey
import app.it.fast4x.rimusic.utils.exoPlayerDiskCacheMaxSizeKey
import app.it.fast4x.rimusic.utils.exoPlayerMinTimeForEventKey
import app.it.fast4x.rimusic.utils.fadeInEffect
import app.it.fast4x.rimusic.utils.fadeOutEffect
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_ENDED
import app.it.fast4x.rimusic.utils.forcePlay
import app.it.fast4x.rimusic.utils.getEnum
import app.it.fast4x.rimusic.utils.intent
import app.it.fast4x.rimusic.utils.isPlayable
import app.it.fast4x.rimusic.utils.isAtLeastAndroid10
import app.it.fast4x.rimusic.utils.isAtLeastAndroid6
import app.it.fast4x.rimusic.utils.isAtLeastAndroid7
import app.it.fast4x.rimusic.utils.isAtLeastAndroid8
import app.it.fast4x.rimusic.utils.isNetworkConnected
import app.it.fast4x.rimusic.utils.isPauseOnVolumeZeroEnabledKey
import app.it.fast4x.rimusic.utils.loudnessBaseGainKey
import app.it.fast4x.rimusic.utils.manageDownload
import app.it.fast4x.rimusic.utils.mediaItems
import app.it.fast4x.rimusic.utils.minimumSilenceDurationKey
import app.it.fast4x.rimusic.utils.notificationPlayerFirstIconKey
import app.it.fast4x.rimusic.utils.notificationPlayerSecondIconKey
import app.it.fast4x.rimusic.utils.notificationTypeKey
import app.it.fast4x.rimusic.utils.pauseListenHistoryKey
import app.it.fast4x.rimusic.utils.persistentQueueKey
import app.it.fast4x.rimusic.utils.playbackVideoIdOrNull
import app.it.fast4x.rimusic.utils.playNext
import app.it.fast4x.rimusic.utils.playPrevious
import app.it.fast4x.rimusic.utils.playbackFadeAudioDurationKey
import app.it.fast4x.rimusic.utils.playbackPitchKey
import app.it.fast4x.rimusic.utils.playbackSpeedKey
import app.it.fast4x.rimusic.utils.playbackVolumeKey
import app.it.fast4x.rimusic.utils.preferences
import app.it.fast4x.rimusic.utils.putEnum
import app.it.fast4x.rimusic.utils.offlineQueueNetworkRefillKey
import app.it.fast4x.rimusic.utils.queueLoopTypeKey
import app.it.fast4x.rimusic.utils.resumePlaybackOnStartKey
import app.it.fast4x.rimusic.utils.resumePlaybackWhenDeviceConnectedKey
import app.it.fast4x.rimusic.utils.sanitizePlaybackUri
import app.it.fast4x.rimusic.utils.safePrepare
import app.it.fast4x.rimusic.utils.safeRelease
import app.it.fast4x.rimusic.utils.safeSeekTo
import app.it.fast4x.rimusic.utils.setGlobalVolume
import app.it.fast4x.rimusic.utils.showDownloadButtonBackgroundPlayerKey
import app.it.fast4x.rimusic.utils.showLikeButtonBackgroundPlayerKey
import app.it.fast4x.rimusic.utils.skipBrokenMediaItem
import app.it.fast4x.rimusic.utils.skipMediaOnErrorKey
import app.it.fast4x.rimusic.utils.skipSilenceKey
import app.it.fast4x.rimusic.utils.timer
import app.it.fast4x.rimusic.utils.toggleRepeatMode
import app.it.fast4x.rimusic.utils.toggleShuffleMode
import app.it.fast4x.rimusic.utils.volumeNormalizationKey
import app.it.fast4x.rimusic.utils.wallpaperTypeKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import app.kreate.android.me.knighthat.utils.Toaster
import timber.log.Timber
import java.io.InterruptedIOException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.math.roundToInt
import kotlin.system.exitProcess
import app.it.fast4x.rimusic.repository.QuickPicksRepository
import android.os.Binder as AndroidBinder
import androidx.compose.ui.util.fastMap
import app.it.fast4x.rimusic.utils.isDiscordPresenceEnabledKey
import android.app.Notification
import android.app.NotificationChannel
import app.it.fast4x.rimusic.utils.safeSetMediaItems

const val LOCAL_KEY_PREFIX = "local:"

val MediaItem.isLocal get() = mediaId.startsWith(LOCAL_KEY_PREFIX) || localConfiguration?.uri?.scheme in setOf("content", "file")
val Song.isLocal get() = id.startsWith(LOCAL_KEY_PREFIX) || id.startsWith("content://") || id.startsWith("file://")

private fun Throwable.isNetworkUnavailablePlaybackFailure(): Boolean =
    generateSequence(this) { it.cause }.any { cause ->
        cause is NoInternetException ||
            cause is UnknownHostException ||
            cause is ConnectException ||
            cause is NoRouteToHostException ||
            cause is SocketTimeoutException ||
            cause is InterruptedIOException ||
            cause.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
            cause.message?.contains("Failed to connect", ignoreCase = true) == true ||
            cause.message?.contains("timeout", ignoreCase = true) == true ||
            cause.message?.contains("Read timed out", ignoreCase = true) == true ||
            cause.message?.contains("ENETUNREACH", ignoreCase = true) == true ||
            cause.message?.contains("EAI_NODATA", ignoreCase = true) == true ||
            cause.message?.contains("ECONNABORTED", ignoreCase = true) == true
    }

@UnstableApi
class PlayerServiceModern : MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback,
    SharedPreferences.OnSharedPreferenceChangeListener,
    OnAudioVolumeChangedListener,
    AudioManager.OnAudioFocusChangeListener {

    private val coroutineScope = CoroutineScope(Dispatchers.IO) + Job()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var mediaSession: MediaLibrarySession
    private var mediaLibrarySessionCallback: MediaLibrarySessionCallback =
        MediaLibrarySessionCallback(this, Database, MyDownloadHelper)
    lateinit var player: ExoPlayer
    private lateinit var sessionPlayer: ExoPlayer
    lateinit var cache: Cache
    lateinit var downloadCache: Cache
    private lateinit var audioVolumeObserver: AudioVolumeObserver
    private lateinit var bitmapProvider: BitmapProvider
    private var volumeNormalizationJob: Job? = null
    private var isPersistentQueueEnabled: Boolean = false
    private var isclosebackgroundPlayerEnabled = false
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasPlayingBeforeFocusLoss = false
    private var volumeBeforeDuck = 1f
    private lateinit var wakeLockManager: WakeLockManager
    private var audioDeviceCallback: AudioDeviceCallback? = null
    private lateinit var downloadListener: DownloadManager.Listener

    // FIX: Use a nullable reference instead of lateinit so we can safely check before first init
    private var connectivityObserverOrNull: AndroidConnectivityObserverLegacy? = null

    // Tracks whether onCreate completed successfully to guard callbacks
    private var isServiceReady = false

    /**
     * Discord presence
     */
    private var discordPresenceManager: DiscordPresenceManager? = null
    private var lastReportedNotificationMediaId: String? = null
    private var lastPlaybackSurfaceRefreshMs = 0L
    private var lastPlaybackSurfaceMediaId: String? = null
    private var lastArtworkRefreshKey: String? = null
    private var lastNoInternetToastMs = 0L
    private var lastSmartMessageMs = 0L
    private var lastAutoSourceRecoveryMediaId: String? = null
    private var lastAutoSourceRecoveryMs = 0L
    private var lastExtractorRecoveryMediaId: String? = null
    private var lastExtractorRecoveryMs = 0L
    private var lastEndedRecoveryMediaId: String? = null
    private var lastEndedRecoveryMs = 0L
    private var currentSongRetryCount = 0
    private var resumeOnNetworkRestore = false
    private var pauseTriggeredByNetworkWait = false
    private var networkRecoveryMediaId: String? = null
    private var lastPlaybackPositionMs = 0L
    private var lastBufferedPositionMs = 0L
    private val playbackRecoveryHelper = PlaybackRecoveryHelper()
    private var consecutiveErrorSkipCount = 0
    private val maxConsecutiveErrorSkips = 5
    private val maxCurrentSongRetries = 2
    private var lastWidgetUpdateMs = 0L
    private var widgetUpdateJob: Job? = null
    private var cacheCompletionJob: Job? = null
    private var cacheCompletionMediaId: String? = null

    // FIX: Track whether audio focus was successfully granted so we know if we should
    // respond to AUDIOFOCUS_GAIN after an AUDIOFOCUS_LOSS. This prevents the race
    // condition where a stale LOSS from a previous instance fires on the new one.
    private var audioFocusGranted = false

    // FIX: Debounce audio focus loss to ignore transient loss that arrives immediately
    // after requesting focus (caused by the previous crashed instance's ghost focus).
    private var audioFocusLossHandled = false
    private var audioFocusRequestTimeMs = 0L
    private val audioFocusStabilizationWindowMs = 1500L

    var loudnessEnhancer: LoudnessEnhancer? = null
    private var binder = Binder()
    private var bassBoost: BassBoost? = null
    private var reverbPreset: PresetReverb? = null
    private var showLikeButton = true
    private var showDownloadButton = true


    lateinit var audioQualityFormat: AudioQualityFormat
    lateinit var sleepTimer: SleepTimer
    private lateinit var playbackStatsListener: PlaybackStatsListener
    private var timerJob: TimerJob? = null
    private var radio: YouTubeRadio? = null

    val currentMediaItem = MutableStateFlow<MediaItem?>(null)

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    private val currentSong = currentMediaItem.flatMapLatest { mediaItem ->
        val songId = mediaItem?.mediaId?.split("/")?.lastOrNull() ?: mediaItem?.mediaId ?: ""
        Database.songTable.findById(songId)
    }.stateIn(coroutineScope, SharingStarted.Lazily, null)

    var currentSongStateDownload = MutableStateFlow(Download.STATE_STOPPED)

    // FIX: connectivityObserver is now a property backed by the nullable field
    lateinit var connectivityObserver: AndroidConnectivityObserverLegacy

    private val isNetworkAvailable = MutableStateFlow(true)
    private val waitingForNetwork = MutableStateFlow(false)

    private var notificationManager: NotificationManager? = null

    private lateinit var notificationActionReceiver: NotificationActionReceiver

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d(
            "PlayerServiceModern.onStartCommand action=%s flags=%s startId=%s playbackState=%s playWhenReady=%s mediaId=%s",
            intent?.action,
            flags,
            startId,
            if (isServiceReady) player.playbackState else -1,
            if (isServiceReady) player.playWhenReady else false,
            if (isServiceReady) player.currentMediaItem?.mediaId else null
        )
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    @kotlin.OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (isAtLeastAndroid8) {
            val channel = NotificationChannel(
                NotificationChannelId,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows currently playing music"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)

            val sleepChannel = NotificationChannel(
                SleepTimerNotificationChannelId,
                "Sleep Timer",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Sleep timer notifications"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(sleepChannel)
        }

        // FIX: Use the nullable reference to safely unregister before re-creating.
        // The old code called connectivityObserver.unregister() on a lateinit that
        // was never initialized yet, causing an UninitializedPropertyAccessException.
        connectivityObserverOrNull?.runCatching { unregister() }
        connectivityObserver = AndroidConnectivityObserverLegacy(this@PlayerServiceModern)
        connectivityObserverOrNull = connectivityObserver

        coroutineScope.launch {
            connectivityObserver.networkStatus.collect { isAvailable ->
                isNetworkAvailable.value = isAvailable
                Timber.d("PlayerServiceModern network status: $isAvailable")
                if (isAvailable && waitingForNetwork.value) {
                    applyRecoveryDecision(
                        playbackRecoveryHelper.onNetworkRestored(recoverySnapshot())
                    )
                }
            }
        }

        val notificationType = preferences.getEnum(notificationTypeKey, NotificationType.Default)
        when (notificationType) {
            NotificationType.Default -> {
                setMediaNotificationProvider(CustomMediaNotificationProvider(this)
                    .apply {
                        setSmallIcon(R.drawable.ic_launcher_monochrome)
                    }
                )
            }
            NotificationType.Advanced -> {
                setMediaNotificationProvider(object : MediaNotification.Provider {
                    override fun createNotification(
                        mediaSession: MediaSession,
                        customLayout: ImmutableList<CommandButton>,
                        actionFactory: MediaNotification.ActionFactory,
                        onNotificationChangedCallback: MediaNotification.Provider.Callback
                    ): MediaNotification {
                        return updateCustomNotification(mediaSession)
                    }

                    override fun handleCustomCommand(
                        session: MediaSession,
                        action: String,
                        extras: Bundle
                    ): Boolean {
                        return false
                    }
                })
            }
        }

        runCatching {
            bitmapProvider = BitmapProvider(
                scope = coroutineScope,
                bitmapSize = (512 * resources.displayMetrics.density).roundToInt(),
                colorProvider = { isSystemInDarkMode ->
                    if (isSystemInDarkMode) Color.BLACK else Color.WHITE
                }
            )
        }.onFailure {
            Timber.e("Failed init bitmap provider in PlayerService ${it.stackTraceToString()}")
        }

        preferences.registerOnSharedPreferenceChangeListener(this)

        isPersistentQueueEnabled = preferences.getBoolean(persistentQueueKey, false)
        audioQualityFormat = preferences.getEnum(audioQualityFormatKey, AudioQualityFormat.Auto)
        showLikeButton = preferences.getBoolean(showLikeButtonBackgroundPlayerKey, true)
        showDownloadButton = preferences.getBoolean(showDownloadButtonBackgroundPlayerKey, true)

        val cacheSize =
            preferences.getEnum(exoPlayerDiskCacheMaxSizeKey, ExoPlayerDiskCacheMaxSize.`2GB`)

        val cacheEvictor = when (cacheSize) {
            ExoPlayerDiskCacheMaxSize.Unlimited -> NoOpCacheEvictor()
            ExoPlayerDiskCacheMaxSize.Custom -> {
                val customCacheSize = preferences.getInt(exoPlayerCustomCacheKey, 32) * 1000 * 1000L
                LeastRecentlyUsedCacheEvictor(customCacheSize)
            }
            else -> LeastRecentlyUsedCacheEvictor(cacheSize.bytes)
        }

        val cacheDir = when (cacheSize) {
            ExoPlayerDiskCacheMaxSize.Disabled -> createTempDirectory(CACHE_DIRNAME).toFile()
            else ->
                when (preferences.getEnum(exoPlayerCacheLocationKey, ExoPlayerCacheLocation.System)) {
                    ExoPlayerCacheLocation.System -> super.getCacheDir()
                    ExoPlayerCacheLocation.Private -> filesDir
                }.resolve(CACHE_DIRNAME)
        }

        cacheDir.mkdirs()
        cache = SimpleCache(cacheDir, cacheEvictor, StandaloneDatabaseProvider(this))
        downloadCache = MyDownloadHelper.getDownloadCache(applicationContext)

        playbackStatsListener = PlaybackStatsListener(false, this@PlayerServiceModern)

        // FIX: ExoPlayer is built WITHOUT internal audio focus management (false).
        // We manage audio focus ourselves below to avoid double-request conflicts.
        // Previously, ExoPlayer was internally requesting focus AND we were requesting it manually,
        // which caused the OS to immediately fire AUDIOFOCUS_LOSS back to the old instance,
        // which then propagated as a spurious loss to us.
        wakeLockManager = WakeLockManager(this, "CubicMusic::Playback")

        val playerSet = PlayerInitializer.createPlayers(
            context = this,
            mediaSourceFactory = createMediaSourceFactory(),
            renderersFactory = createRendersFactory(),
        )
        player = playerSet.player
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()
        sessionPlayer = player

        // FIX: Request audio focus ONCE. Abandon any stale focus from previous crashed
        // instance before requesting to prevent the OS immediately sending AUDIOFOCUS_LOSS.
        audioManager = audioManager ?: (getSystemService(AUDIO_SERVICE) as AudioManager)
        if (isHandleAudioFocusEnabled()) {
            // Abandon previous stale request unconditionally before making a new one
            audioFocusRequest?.let { stale ->
                runCatching { audioManager?.abandonAudioFocusRequest(stale) }
            }
            audioFocusRequest = null
            audioFocusGranted = false
            audioFocusLossHandled = false

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(this, handler)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(false)
                .build()

            // FIX: Record the time of the focus request so we can suppress spurious
            // AUDIOFOCUS_LOSS callbacks that arrive within the stabilization window.
            audioFocusRequestTimeMs = SystemClock.elapsedRealtime()
            val focusRequestResult = audioManager?.requestAudioFocus(audioFocusRequest!!)
            audioFocusGranted = (focusRequestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            Timber.d("PlayerServiceModern audio focus request result=%s granted=%s", focusRequestResult, audioFocusGranted)
        }

        sleepTimer = SleepTimer(coroutineScope, sessionPlayer)
        sessionPlayer.addListener(sleepTimer)
        player.addListener(this@PlayerServiceModern)
        player.addAnalyticsListener(playbackStatsListener)

        val forwardingPlayer =
            object : ForwardingPlayer(sessionPlayer) {
                override fun getAvailableCommands(): Player.Commands {
                    return super.getAvailableCommands()
                        .buildUpon()
                        .addAllCommands()
                        .build()
                }

                override fun getBufferedPercentage(): Int {
                    return runCatching { super.getBufferedPercentage().coerceIn(0, 100) }
                        .getOrDefault(0)
                }
            }

        mediaLibrarySessionCallback.apply {
            binder = this@PlayerServiceModern.binder
            toggleLike = ::toggleLike
            toggleDownload = ::toggleDownload
            toggleRepeat = ::toggleRepeat
            toggleShuffle = ::toggleShuffle
            startRadio = ::startRadio
            callPause = binder::gracefulPause
            actionSearch = ::actionSearch
        }

        mediaSession =
            MediaLibrarySession.Builder(this, forwardingPlayer, mediaLibrarySessionCallback)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java)
                            .putExtra("expandPlayerBottomSheet", true),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setBitmapLoader(
                    CoilBitmapLoader(
                        this,
                        coroutineScope,
                        512 * resources.displayMetrics.density.toInt()
                    )
                )
                .build()
        mediaLibrarySessionCallback.observeRepository(mediaSession)
        player.skipSilenceEnabled = preferences.getBoolean(skipSilenceKey, false)
        player.repeatMode = preferences.getEnum(queueLoopTypeKey, QueueLoopType.Default).type

        sessionPlayer.playbackParameters = PlaybackParameters(
            preferences.getFloat(playbackSpeedKey, 1f),
            preferences.getFloat(playbackPitchKey, 1f)
        )
        sessionPlayer.volume = preferences.getFloat(playbackVolumeKey, 1f)
        sessionPlayer.setGlobalVolume(sessionPlayer.volume)

        val sessionToken = SessionToken(this, ComponentName(this, PlayerServiceModern::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())

        audioVolumeObserver = AudioVolumeObserver(this)
        audioVolumeObserver.register(AudioManager.STREAM_MUSIC, this)

        downloadListener = object : DownloadManager.Listener {
            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?
            ) = run {
                if (download.request.id != currentMediaItem.value?.mediaId) return@run
                Timber.d("PlayerServiceModern onDownloadChanged current song ${currentMediaItem.value?.mediaId} state ${download.state} key ${download.request.id}")
                updateDownloadedState()
            }
        }
        MyDownloadHelper.getDownloadManager(this).addListener(downloadListener)

        notificationActionReceiver = NotificationActionReceiver(sessionPlayer)
        QuickPicksRepository.refreshIfNeeded()

        val filter = IntentFilter().apply {
            addAction(Action.play.value)
            addAction(Action.pause.value)
            addAction(Action.next.value)
            addAction(Action.previous.value)
            addAction(Action.like.value)
            addAction(Action.download.value)
            addAction(Action.playradio.value)
            addAction(Action.shuffle.value)
            addAction(Action.repeat.value)
            addAction(Action.search.value)
        }

        ContextCompat.registerReceiver(
            this,
            notificationActionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        coroutineScope.launch {
            currentSong.debounce(1000).collect { song ->
                updateDownloadedState()
                updateDefaultNotification()
                withContext(Dispatchers.Main) {
                    updateWidgets()
                }
            }
        }

        // Mark service as fully initialized before restoring queue
        isServiceReady = true

        maybeRestorePlayerQueue()
        maybeResumePlaybackWhenDeviceConnected()
        maybeBassBoost()
        maybeReverb()

        if (isPersistentQueueEnabled) {
            maybeResumePlaybackOnStart()

            val scheduler = Executors.newScheduledThreadPool(1)
            scheduler.scheduleWithFixedDelay({
                if (isServiceReady) maybeSavePlayerQueue()
            }, 0, 30, TimeUnit.SECONDS)
        }

        if (preferences.getBoolean(isDiscordPresenceEnabledKey, false)) {
            val token = encryptedPreferences.getString(discordPersonalAccessTokenKey, "")
            if (token?.isNotEmpty() == true) {
                discordPresenceManager = DiscordPresenceManager(
                    context = this,
                    getToken = { token },
                )
            }
        }
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaSession

 override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
    if (!isServiceReady) return
    try {
        maybeSavePlayerQueue()
        if (!playWhenReady) {
            releasePlaybackWakeLock()
            if (waitingForNetwork.value) {
                // Keep restoration armed. Network-recovery pauses also trigger this callback;
                // clearing the flags here makes playback stay paused after the stream returns.
                pauseTriggeredByNetworkWait = false
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Error in onPlayWhenReadyChanged")
    }
}

    override fun onRepeatModeChanged(repeatMode: Int) {
        if (!isServiceReady) return
        updateDefaultNotification()
        preferences.edit {
            putEnum(queueLoopTypeKey, QueueLoopType.from(repeatMode))
        }
    }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats
    ) {
        if (!isServiceReady) return
        if (preferences.getBoolean(pauseListenHistoryKey, false)) return

        val mediaItem =
            eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem
        if (mediaItem.mediaId.isBlank()) return
        val songId = mediaItem.mediaId.substringAfterLast("/", mediaItem.mediaId).ifBlank { return }

        val totalPlayTimeMs = playbackStats.totalPlayTimeMs

        if (songId.isBlank()) return

        if (totalPlayTimeMs > 5000)
            Database.asyncTransaction {
                songTable.insertIgnore(Song.makePlaceholder(songId))
                songTable.updateTotalPlayTime(songId, totalPlayTimeMs, true)
            }

        val minTimeForEvent =
            preferences.getEnum(exoPlayerMinTimeForEventKey, ExoPlayerMinTimeForEvent.`20s`)

        if (totalPlayTimeMs > minTimeForEvent.asMillis) {
            Database.asyncTransaction {
                runCatching {
                    songTable.insertIgnore(Song.makePlaceholder(songId))
                    eventTable.insertIgnore(
                        Event(
                            songId = songId,
                            timestamp = System.currentTimeMillis(),
                            playTime = totalPlayTimeMs
                        )
                    )
                }.onFailure {
                    Timber.e(
                        it,
                        "PlayerServiceModern failed to insert playback event for mediaId=%s songId=%s",
                        mediaItem.mediaId,
                        songId
                    )
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        isclosebackgroundPlayerEnabled = preferences.getBoolean(closebackgroundPlayerKey, false)
        Timber.d(
            "PlayerServiceModern.onTaskRemoved closebackgroundPlayerEnabled=%s isPlaying=%s playWhenReady=%s playbackState=%s mediaId=%s queueSize=%s",
            isclosebackgroundPlayerEnabled,
            if (isServiceReady) player.isPlaying else false,
            if (isServiceReady) player.playWhenReady else false,
            if (isServiceReady) player.playbackState else -1,
            if (isServiceReady) player.currentMediaItem?.mediaId else null,
            if (isServiceReady) player.mediaItemCount else 0
        )
        if (isclosebackgroundPlayerEnabled) {
            if (shouldKeepServiceAlive()) {
                Timber.d("PlayerServiceModern.onTaskRemoved keeping service alive because playback session is still active")
                super.onTaskRemoved(rootIntent)
                return
            }
            runCatching {
                broadCastPendingIntent<NotificationDismissReceiver>().send()
            }.onFailure {
                Timber.e(it, "PlayerServiceModern failed to send dismiss broadcast from onTaskRemoved")
            }
            runCatching {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }.onFailure {
                Timber.e(it, "PlayerServiceModern failed to stop foreground from onTaskRemoved")
            }
            stopSelf()
            return
        }
        super.onTaskRemoved(rootIntent)
    }

@UnstableApi
override fun onDestroy() {
    isServiceReady = false
    try {
        releasePlaybackWakeLock()
    } catch (e: Exception) {
        Timber.e(e, "Failed to release wake lock in onDestroy")
    }
    
    Timber.d(
        "PlayerServiceModern.onDestroy isPlaying=%s playWhenReady=%s playbackState=%s mediaId=%s queueSize=%s",
        if (isServiceReady) player.isPlaying else false,
        if (isServiceReady) player.playWhenReady else false,
        if (isServiceReady) player.playbackState else -1,
        if (isServiceReady) player.currentMediaItem?.mediaId else null,
        if (isServiceReady) player.mediaItemCount else 0
    )
        runCatching {
            if (preferences.getBoolean(isDiscordPresenceEnabledKey, false)) {
                Toaster.i("[DiscordPresence] onStop: call the manager (close discord presence)")
                discordPresenceManager?.onStop()
            }
            if (isPersistentQueueEnabled) maybeSavePlayerQueue()
            preferences.unregisterOnSharedPreferenceChangeListener(this)
            if (::player.isInitialized) {
                player.removeListener(this)
            }
            cacheCompletionJob?.cancel()
            if (::player.isInitialized) {
                runCatching { player.stop() }
                    .onFailure { Timber.e(it, "Failed to stop player during onDestroy") }
                player.safeRelease()
            }
            try {
                unregisterReceiver(notificationActionReceiver)
            } catch (e: Exception) {
                Timber.e("PlayerServiceModern onDestroy unregisterReceiver notificationActionReceiver " + e.stackTraceToString())
            }
            runCatching { mediaLibrarySessionCallback.release() }
            if (::mediaSession.isInitialized) {
                mediaSession.release()
            }
            if (::cache.isInitialized) {
                cache.release()
            }
            if (::downloadListener.isInitialized) {
                MyDownloadHelper.getDownloadManager(this).removeListener(downloadListener)
            }
            loudnessEnhancer?.release()
            audioFocusRequest?.let { request ->
                audioManager?.abandonAudioFocusRequest(request)
            }
            audioFocusRequest = null
            audioFocusGranted = false
            // FIX: unregister using the nullable reference — safe even if never initialized
            connectivityObserverOrNull?.runCatching { unregister() }
            connectivityObserverOrNull = null
            audioVolumeObserver?.unregister()
            timerJob?.cancel()
            timerJob = null
            notificationManager?.cancel(NotificationId)
            notificationManager?.cancelAll()
            notificationManager = null
            coroutineScope.cancel()
        }.onFailure {
            Timber.e("Failed onDestroy in PlayerService " + it.stackTraceToString())
        }
        super.onDestroy()
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (!isServiceReady) return
        Timber.d("PlayerServiceModern.onAudioFocusChange=%s", focusChange)

        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // FIX: Suppress AUDIOFOCUS_LOSS callbacks that arrive suspiciously soon after
                // this instance requested focus. These are ghost callbacks from the OS clearing
                // the previous crashed instance's focus — they are NOT directed at us.
                val timeSinceRequest = SystemClock.elapsedRealtime() - audioFocusRequestTimeMs
                if (timeSinceRequest < audioFocusStabilizationWindowMs) {
                    Timber.w(
                        "PlayerServiceModern.onAudioFocusChange LOSS arrived %dms after focus request — suppressing (likely stale ghost from previous instance)",
                        timeSinceRequest
                    )
                    return
                }
                Timber.w("Audio focus LOST - another app took over playback")
                wasPlayingBeforeFocusLoss = player.isPlaying || player.playWhenReady
                if (wasPlayingBeforeFocusLoss) {
                    binder.gracefulPause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Timber.d("Audio focus lost transient - notification or alarm")
                wasPlayingBeforeFocusLoss = player.isPlaying || player.playWhenReady
                if (wasPlayingBeforeFocusLoss) {
                    binder.gracefulPause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Timber.d("Audio focus lost transient - ducking volume")
                if (player.volume > 0.15f) {
                    volumeBeforeDuck = player.volume
                }
                player.volume = (player.volume * 0.3f).coerceAtLeast(0.05f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Timber.d("Audio focus regained")
                val restoredVolume = preferences
                    .getFloat(playbackVolumeKey, volumeBeforeDuck.coerceAtLeast(0.5f))
                    .coerceIn(0.15f, 1f)
                player.volume = restoredVolume
                player.setGlobalVolume(restoredVolume)
                if (wasPlayingBeforeFocusLoss) {
                    binder.gracefulPlay()
                }
                handler.postDelayed({
                    if (isServiceReady && (player.isPlaying || player.playWhenReady) && player.volume < 0.15f) {
                        player.volume = restoredVolume
                        player.setGlobalVolume(restoredVolume)
                    }
                }, 350L)
                wasPlayingBeforeFocusLoss = false
                audioFocusGranted = true
            }
        }
    }

private fun acquirePlaybackWakeLock() {
    if (!isServiceReady) return
    wakeLockManager.acquire()
}

private fun releasePlaybackWakeLock() {
    wakeLockManager.release()
}

private fun shouldHoldPlaybackWakeLock(): Boolean {
    if (!isServiceReady) return false
    return player.isPlaying ||
        (player.playWhenReady && (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING))
}

private fun syncPlaybackWakeLockState() {
    if (shouldHoldPlaybackWakeLock()) {
        acquirePlaybackWakeLock()
    } else {
        releasePlaybackWakeLock()
    }
}
    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?
    ) {
        if (!isServiceReady) return

        when (key) {
            persistentQueueKey -> {
                sharedPreferences?.let {
                    isPersistentQueueEnabled =
                        it.getBoolean(key, isPersistentQueueEnabled)
                }
            }

            volumeNormalizationKey, loudnessBaseGainKey -> maybeNormalizeVolume()

            resumePlaybackWhenDeviceConnectedKey ->
                maybeResumePlaybackWhenDeviceConnected()

            skipSilenceKey -> {
                sharedPreferences?.let {
                    val enabled = it.getBoolean(key, false)
                    player.skipSilenceEnabled = enabled
                }
            }

            queueLoopTypeKey -> {
                player.repeatMode =
                    sharedPreferences?.getEnum(queueLoopTypeKey, QueueLoopType.Default)?.type
                        ?: QueueLoopType.Default.type
            }

            bassboostLevelKey, bassboostEnabledKey -> maybeBassBoost()
            audioReverbPresetKey -> maybeReverb()
        }
    }

    private var pausedByZeroVolume = false
    override fun onAudioVolumeChanged(currentVolume: Int, maxVolume: Int) {
        if (!isServiceReady) return
        if (preferences.getBoolean(isPauseOnVolumeZeroEnabledKey, false)) {
            if (player.isPlaying && currentVolume < 1) {
                binder.gracefulPause()
                pausedByZeroVolume = true
            } else if (pausedByZeroVolume && currentVolume >= 1) {
                binder.gracefulPlay()
                pausedByZeroVolume = false
            }
        }
    }

    override fun onAudioVolumeDirectionChanged(direction: Int) {}

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (!isServiceReady) return
        currentSongRetryCount = 0
        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            consecutiveErrorSkipCount = 0
        }
        waitingForNetwork.value = false
        resumeOnNetworkRestore = false
        pauseTriggeredByNetworkWait = false
        networkRecoveryMediaId = null
        capturePlaybackSnapshot()

        val displayMediaItem = displayedMediaItem() ?: mediaItem
        currentMediaItem.update { displayMediaItem }
        maybeNormalizeVolume()
        loadFromRadio(reason)
        requestArtworkPlaybackSurfaceRefresh(displayMediaItem, minIntervalMs = 0L)
        warmCurrentSongCache(displayMediaItem)

        val now = System.currentTimeMillis()
        val presenceSnapshot = currentPresenceSnapshot()
        if (preferences.getBoolean(isDiscordPresenceEnabledKey, false)) {
            val token = encryptedPreferences.getString(discordPersonalAccessTokenKey, "")
            if (token?.isNotEmpty() == true) {
                discordPresenceManager?.onPlayingStateChanged(
                    presenceSnapshot.mediaItem ?: mediaItem,
                    presenceSnapshot.isPlaying,
                    presenceSnapshot.position,
                    presenceSnapshot.duration,
                    now,
                    getCurrentPosition = { currentPresenceSnapshot().position },
                    isPlayingProvider = { currentPresenceSnapshot().isPlaying }
                )
            }
        }
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (!isServiceReady) return
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
            maybeSavePlayerQueue()
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        if (!isServiceReady) return
        updateDefaultNotification()
        if (shuffleModeEnabled && !binder.isManuallyShuffled) {
            val shuffledIndices = IntArray(player.mediaItemCount) { it }
            shuffledIndices.shuffle()
            shuffledIndices[shuffledIndices.indexOf(player.currentMediaItemIndex)] = shuffledIndices[0]
            shuffledIndices[0] = player.currentMediaItemIndex
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        }
    }

 @UnstableApi
override fun onIsPlayingChanged(isPlaying: Boolean) {
    if (!isServiceReady) return
    try {
        if (isPlaying) {
            acquirePlaybackWakeLock()
        } else {
            releasePlaybackWakeLock()
        }
    } catch (e: Exception) {
        Timber.e(e, "Error in onIsPlayingChanged")
    }
    
    val presenceSnapshot = currentPresenceSnapshot()
    val item = presenceSnapshot.mediaItem
    val now = System.currentTimeMillis()

    if (preferences.getBoolean(isDiscordPresenceEnabledKey, false)) {
        val token = encryptedPreferences.getString(discordPersonalAccessTokenKey, "")
        if (token?.isNotEmpty() == true) {
            discordPresenceManager?.onPlayingStateChanged(
                item,
                presenceSnapshot.isPlaying,
                presenceSnapshot.position,
                presenceSnapshot.duration,
                now,
                getCurrentPosition = { currentPresenceSnapshot().position },
                isPlayingProvider = { currentPresenceSnapshot().isPlaying }
            )
        }
    }

    requestPlaybackSurfaceRefresh(item)
}
    override fun onPlayerError(error: PlaybackException) {
        if (!isServiceReady) return
        super.onPlayerError(error)

        val currentMediaId = player.currentMediaItem?.mediaId.orEmpty()
        val errorCauses = generateSequence(error as Throwable?) { it.cause }.toList()
        val deepestCause = errorCauses.lastOrNull()
        val isInvalidResponse = errorCauses.any { it.javaClass.simpleName == "InvalidResponseCodeException" }
        val isRefreshableStreamFailure =
            isInvalidResponse ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
        val isPlayableFormatMissing = errorCauses.any { it is PlayableFormatNotFoundException }
        val isNetworkUnavailable = error.isNetworkUnavailablePlaybackFailure()

        Timber.e(
            error,
            "PlayerServiceModern onPlayerError mediaId=%s retryCount=%d errorCode=%s errorName=%s rootCause=%s",
            currentMediaId,
            currentSongRetryCount,
            error.errorCode,
            error.errorCodeName,
            deepestCause?.javaClass?.simpleName
        )

        if (!isNetworkAvailable.value || isNetworkUnavailable) {
            applyRecoveryDecision(
                PlaybackRecoveryHelper.Decision.WaitForNetwork(
                    mediaId = currentMediaId.ifBlank { player.currentMediaItem?.mediaId.orEmpty() },
                    positionMs = lastPlaybackPositionMs.takeIf { it > 0L }
                        ?: player.currentPosition.coerceAtLeast(0L),
                    message = "No internet. Will retry when connection returns.",
                    resumeWhenNetworkReturns = player.isPlaying || player.playWhenReady
                        || player.playbackState == Player.STATE_BUFFERING
                )
            )
            return
        }

        val isDecoderIssue =
            error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                error.errorCodeName.contains("DECOD", ignoreCase = true) ||
                deepestCause?.javaClass?.simpleName?.contains("Codec", ignoreCase = true) == true

        if (isDecoderIssue) {
            Timber.w("Decoder failure for %s - skipping.", currentMediaId)
            skipOrPause("Codec error - skipping song.")
            return
        }

        if (isNearEndPlaybackFailure()) {
            Timber.w(
                "Playback failed near the end for %s at %d/%dms - advancing instead of retrying tail chunk.",
                currentMediaId,
                player.currentPosition,
                player.duration
            )
            skipOrPause("Stream failed near the end - moving on.")
            return
        }

        if (currentMediaId.isNotBlank() && isRefreshableStreamFailure) {
            val failedSource = PlaybackSourceMonitor.status.value
                .takeIf { it.videoId == currentMediaId }
                ?.source
            Timber.w(
                "Refreshable stream failure for %s from source=%s - retrying Innertube resolver",
                currentMediaId,
                failedSource?.label
            )
        }

        if (isRefreshableStreamFailure && currentMediaId.isNotBlank() && currentSongRetryCount < maxCurrentSongRetries) {
            val shouldAutoResume = player.playWhenReady || player.isPlaying || player.playbackState == Player.STATE_BUFFERING
            currentSongRetryCount++
            val delayMs = 450L
            Timber.i(
                "Refreshing blocked stream URL for %s (attempt %d/%d)",
                currentMediaId,
                currentSongRetryCount,
                maxCurrentSongRetries
            )
            showSmartMessage("Refreshing stream link...")
            runCatching { player.pause() }
            player.playWhenReady = shouldAutoResume
            val prepared = player.safePrepare()
            player.playWhenReady = shouldAutoResume
            if (!prepared) {
                skipOrPause("Source unrecoverable for $currentMediaId.")
                return
            }
            handler.postDelayed({
                if (!isServiceReady) return@postDelayed
                if (player.currentMediaItem?.mediaId != currentMediaId) return@postDelayed
                player.safeSeekTo(lastPlaybackPositionMs.coerceAtLeast(0L))
                if (shouldAutoResume) {
                    player.playWhenReady = true
                    runCatching { binder.gracefulPlay() }
                        .onFailure { Timber.e(it, "Failed to resume after blocked stream refresh for %s", currentMediaId) }
                }
            }, delayMs)
            return
        }

        if (isRefreshableStreamFailure || isPlayableFormatMissing) {
            skipOrPause("Source unavailable for this song.")
            return
        }

        if (currentMediaId.isNotBlank() && currentSongRetryCount < maxCurrentSongRetries) {
            val shouldAutoResume = player.playWhenReady || player.isPlaying || player.playbackState == Player.STATE_BUFFERING
            currentSongRetryCount++
            val delayMs = when (currentSongRetryCount) {
                1 -> 800L
                2 -> 1_500L
                3 -> 2_500L
                else -> 3_500L
            }
            Timber.i(
                "Retrying %s (attempt %d/%d) in %dms",
                currentMediaId, currentSongRetryCount, maxCurrentSongRetries, delayMs
            )
            showSmartMessage("Retrying... ($currentSongRetryCount/$maxCurrentSongRetries)")

            runCatching { player.pause() }
            player.playWhenReady = shouldAutoResume
            val prepared = player.safePrepare()
            player.playWhenReady = shouldAutoResume
            if (!prepared) {
                skipOrPause("Source unrecoverable for $currentMediaId.")
                return
            }
            handler.postDelayed({
                if (!isServiceReady) return@postDelayed
                if (player.currentMediaItem?.mediaId != currentMediaId) return@postDelayed
                player.safeSeekTo(lastPlaybackPositionMs.coerceAtLeast(0L))
                if (shouldAutoResume) {
                    player.playWhenReady = true
                    runCatching { binder.gracefulPlay() }
                        .onFailure { Timber.e(it, "Failed to resume after retry for %s", currentMediaId) }
                }
            }, delayMs)
            return
        }

        Timber.w("All %d retries exhausted for %s", maxCurrentSongRetries, currentMediaId)
        if (!preferences.getBoolean(skipMediaOnErrorKey, true)) {
            applyRecoveryDecision(
                PlaybackRecoveryHelper.Decision.Pause("Playback stopped. Skip on error is disabled.")
            )
            return
        }

        consecutiveErrorSkipCount++
        if (consecutiveErrorSkipCount >= maxConsecutiveErrorSkips) {
            applyRecoveryDecision(
                PlaybackRecoveryHelper.Decision.Pause(
                    "Several songs failed in a row. Paused to avoid looping through the queue."
                )
            )
            return
        }

        val prev = player.currentMediaItem
        val skipped = player.skipBrokenMediaItem("onPlayerError ${error.errorCodeName}")
        if (skipped && prev != null) {
            showSmartMessage(
                getString(R.string.skip_media_on_error_message, prev.mediaMetadata.title)
            )
        } else if (!skipped) {
            applyRecoveryDecision(
                PlaybackRecoveryHelper.Decision.Pause("Playback stopped - no more tracks to try.")
            )
        }
    }

    private fun isNearEndPlaybackFailure(): Boolean {
        val durationMs = runCatching { player.duration }.getOrDefault(C.TIME_UNSET)
        if (durationMs <= 0L || durationMs == C.TIME_UNSET) return false

        val positionMs = maxOf(
            lastPlaybackPositionMs,
            runCatching { player.currentPosition }.getOrDefault(0L)
        ).coerceAtLeast(0L)
        val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)

        return positionMs >= 30_000L && remainingMs <= 12_000L
    }

    private fun skipOrPause(reason: String) {
        if (!preferences.getBoolean(skipMediaOnErrorKey, true)) {
            applyRecoveryDecision(PlaybackRecoveryHelper.Decision.Pause(reason))
        } else if (player.hasNextMediaItem()) {
            val prev = player.currentMediaItem
            val skipped = player.skipBrokenMediaItem(reason)
            if (skipped && prev != null) {
                showSmartMessage(getString(R.string.skip_media_on_error_message, prev.mediaMetadata.title))
            }
        } else {
            applyRecoveryDecision(PlaybackRecoveryHelper.Decision.Pause(reason))
        }
    }

    private fun handleNetworkLossDuringPlayback(currentMediaId: String): Boolean {
        if (currentMediaId.isBlank()) return false
        waitingForNetwork.value = true
        networkRecoveryMediaId = currentMediaId
        resumeOnNetworkRestore =
            player.isPlaying || player.playWhenReady || player.playbackState == Player.STATE_BUFFERING
        pauseTriggeredByNetworkWait = resumeOnNetworkRestore

        if (resumeOnNetworkRestore) {
            binder.gracefulPause()
        } else {
            player.pause()
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastNoInternetToastMs >= 3_000L) {
            lastNoInternetToastMs = now
            showSmartMessage("Connection lost. Check Wi-Fi or mobile data. Cubic will retry automatically.")
        }
        return true
    }

    private fun shouldRecoverCurrentSong(
        error: PlaybackException,
        deepestCause: Throwable?
    ): Boolean {
        val recoverableCodes = setOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
            416
        )
        val messageBlob = buildString {
            append(error.message.orEmpty())
            append(' ')
            append(deepestCause?.message.orEmpty())
            append(' ')
            append(deepestCause?.javaClass?.simpleName.orEmpty())
        }

        return error.errorCode in recoverableCodes ||
            messageBlob.contains("EOF", ignoreCase = true) ||
            messageBlob.contains("end of file", ignoreCase = true) ||
            messageBlob.contains("Connection reset", ignoreCase = true) ||
            messageBlob.contains("Broken pipe", ignoreCase = true)
    }

override fun onPlaybackStateChanged(playbackState: Int) {
    if (!isServiceReady) return
    try {
        capturePlaybackSnapshot()
        when (playbackState) {
            Player.STATE_READY -> {
                waitingForNetwork.value = false
                consecutiveErrorSkipCount = 0
                syncPlaybackWakeLockState()
            }
            Player.STATE_BUFFERING -> {
                syncPlaybackWakeLockState()
                applyRecoveryDecision(
                    playbackRecoveryHelper.onPlaybackStateChanged(
                        playbackState = playbackState,
                        snapshot = recoverySnapshot(),
                    )
                )
            }
            Player.STATE_ENDED -> {
                syncPlaybackWakeLockState()
                if (!player.hasNextMediaItem()) {
                    runCatching {
                        player.playWhenReady = false
                        player.pause()
                    }.onFailure {
                        Timber.e(it, "Failed to keep ended player paused without clearing the queue")
                    }
                }
            }
            Player.STATE_IDLE -> {
                syncPlaybackWakeLockState()
                // Do not clear the queue here. Media3 can transiently report IDLE during
                // recovery, and clearing items from this callback can make the player UI
                // disappear or the service stop even though playback should simply pause.
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Error in onPlaybackStateChanged")
    }
}

    override fun onEvents(player: Player, events: Player.Events) {
        if (!isServiceReady) return
        if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
            capturePlaybackSnapshot()
            syncPlaybackWakeLockState()
            val isBufferingOrReady = player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            if (isBufferingOrReady && player.playWhenReady) {
                sendOpenEqualizerIntent()
            } else {
                sendCloseEqualizerIntent()
                if (!player.playWhenReady && !pauseTriggeredByNetworkWait) {
                    waitingForNetwork.value = false
                }
            }
        }
    }

    private fun ensureWakeLockHeld() {
        if (!isServiceReady) return
        syncPlaybackWakeLockState()
    }

    private fun loadFromRadio(reason: Int) {
        if (!isServiceReady) return
        val isEnabled = preferences.getBoolean(autoLoadSongsInQueueKey, true)
        val isRepeatTransition = reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
        val remainingQueueItems = (player.mediaItemCount - player.currentMediaItemIndex - 1).coerceAtLeast(0)
        val isDownloadedQueue = player.mediaItemCount > 0 &&
            (0 until player.mediaItemCount).all { index ->
                MyDownloadHelper.isSongDownloaded(player.getMediaItemAt(index).mediaId)
            }
        val canRefillDownloadedQueue = !isDownloadedQueue ||
            preferences.getBoolean(offlineQueueNetworkRefillKey, false)

        if (
            isEnabled &&
            !isRepeatTransition &&
            canRefillDownloadedQueue &&
            !binder.isLoadingRadio &&
            preferences.getBoolean(autoLoadSongsInQueueKey, true) &&
            remainingQueueItems <= 5
        )
            player.currentMediaItem?.let {
                binder.startRadio(it, true)
            }
    }

    private fun maybeBassBoost() {
        if (!isServiceReady) return
        if (!preferences.getBoolean(bassboostEnabledKey, false)) {
            runCatching {
                bassBoost?.enabled = false
                bassBoost?.release()
            }
            bassBoost = null
            maybeNormalizeVolume()
            return
        }

        runCatching {
            if (bassBoost == null) bassBoost = BassBoost(0, player.audioSessionId)
            val bassboostLevel =
                (preferences.getFloat(bassboostLevelKey, 0.5f) * 1000f).toInt().toShort()
            Timber.d("PlayerServiceModern maybeBassBoost bassboostLevel $bassboostLevel")
            bassBoost?.enabled = false
            bassBoost?.setStrength(bassboostLevel)
            bassBoost?.enabled = true
        }.onFailure {
            Toaster.e("Can't enable bass boost")
        }
    }

    private fun maybeReverb() {
        if (!isServiceReady) return
        val presetType = preferences.getEnum(audioReverbPresetKey, PresetsReverb.NONE)

        if (presetType == PresetsReverb.NONE) {
            runCatching {
                reverbPreset?.enabled = false
                player.clearAuxEffectInfo()
                reverbPreset?.release()
            }
            reverbPreset = null
            return
        }

        runCatching {
            if (reverbPreset == null) reverbPreset = PresetReverb(1, player.audioSessionId)
            reverbPreset?.enabled = false
            reverbPreset?.preset = presetType.preset
            reverbPreset?.enabled = true
            reverbPreset?.id?.let { player.setAuxEffectInfo(AuxEffectInfo(it, 1f)) }
        }
    }

    @UnstableApi
    private fun maybeNormalizeVolume() {
        if (!isServiceReady) return
        if (!preferences.getBoolean(volumeNormalizationKey, false)) {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            volumeNormalizationJob?.cancel()
            return
        }

        runCatching {
            if (loudnessEnhancer == null) {
                loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
            }
        }.onFailure {
            Timber.e("PlayerService maybeNormalizeVolume load loudnessEnhancer ${it.stackTraceToString()}")
            return
        }

        val baseGain = preferences.getFloat(loudnessBaseGainKey, 5.00f)
        player.currentMediaItem?.mediaId?.let { songId ->
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob = coroutineScope.launch(Dispatchers.Main) {
                fun Float?.toMb() = ((this ?: 0f) * 100).toInt()

                Database.formatTable
                    .findBySongId(songId)
                    .cancellable()
                    .collectLatest { format ->
                        val loudnessMb = format?.loudnessDb.toMb().let {
                            if (it !in -2000..2000) {
                                Toaster.w("Extreme loudness detected")
                                0
                            } else it
                        }

                        try {
                            loudnessEnhancer?.setTargetGain(baseGain.toMb() - loudnessMb)
                            loudnessEnhancer?.enabled = true
                        } catch (e: Exception) {
                            Timber.e("PlayerService maybeNormalizeVolume apply targetGain ${e.stackTraceToString()}")
                        }
                    }
            }
        }
    }

    @SuppressLint("NewApi")
    private fun maybeResumePlaybackWhenDeviceConnected() {
        if (!isAtLeastAndroid6) return

        if (preferences.getBoolean(resumePlaybackWhenDeviceConnectedKey, false)) {
            if (audioManager == null) {
                audioManager = getSystemService(AUDIO_SERVICE) as AudioManager?
            }

            audioDeviceCallback = object : AudioDeviceCallback() {
                private fun canPlayMusic(audioDeviceInfo: AudioDeviceInfo): Boolean {
                    if (!audioDeviceInfo.isSink) return false
                    return audioDeviceInfo.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        audioDeviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        audioDeviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        audioDeviceInfo.type == AudioDeviceInfo.TYPE_USB_HEADSET
                }

                override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                    if (isServiceReady && player.playWhenReady && !player.isPlaying && addedDevices.any(::canPlayMusic)) {
                        runCatching { player.play() }
                            .onFailure { Timber.e(it, "Failed to resume playback after device connection") }
                    }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) = Unit
            }

            audioManager?.registerAudioDeviceCallback(audioDeviceCallback, handler)
        } else {
            audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
            audioDeviceCallback = null
        }
    }

    private fun createTrackSelector() = DefaultTrackSelector(this).apply {
        setParameters(
            buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
        )
    }

    private fun createRendersFactory() = object : DefaultRenderersFactory(this) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink {
            val minimumSilenceDuration = preferences.getLong(
                minimumSilenceDurationKey, 2_000_000L
            ).coerceIn(1000L..2_000_000L)

            return DefaultAudioSink.Builder(applicationContext)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioOffloadSupportProvider(
                    DefaultAudioOffloadSupportProvider(applicationContext)
                )
                .setAudioProcessorChain(
                    DefaultAudioProcessorChain(
                        arrayOf(),
                        SilenceSkippingAudioProcessor(
                            2_000_000L,
                            0.01f,
                            2_000_000L,
                            0,
                            256
                        ),
                        SonicAudioProcessor()
                    )
                )
                .build()
                .apply {
                    if (isAtLeastAndroid10) setOffloadMode(AudioSink.OFFLOAD_MODE_DISABLED)
                }
        }
    }.apply {
        setEnableDecoderFallback(true)
    }

    private fun createMediaSourceFactory() = DefaultMediaSourceFactory(
        createDataSourceFactory(),
        DefaultExtractorsFactory()
    ).setLoadErrorHandlingPolicy(
        object : DefaultLoadErrorHandlingPolicy() {
            override fun isEligibleForFallback(exception: IOException) = true
            override fun getMinimumLoadableRetryCount(dataType: Int): Int = 2
        }
    )

    private fun warmCurrentSongCache(mediaItem: MediaItem?) {
        if (!isServiceReady) return
        val mediaId = mediaItem
            ?.takeUnless { it.isLocal }
            ?.playbackVideoIdOrNull()
            ?: run {
                cacheCompletionJob?.cancel()
                cacheCompletionJob = null
                cacheCompletionMediaId = null
                return
            }

        if (!isNetworkConnected(this) || MyDownloadHelper.isSongDownloaded(mediaId)) {
            cacheCompletionJob?.cancel()
            cacheCompletionJob = null
            cacheCompletionMediaId = null
            return
        }

        if (player.currentMediaItem?.mediaId == mediaItem?.mediaId && (player.isPlaying || player.playWhenReady)) {
            cacheCompletionJob?.cancel()
            cacheCompletionJob = null
            cacheCompletionMediaId = null
            return
        }

        if (cacheCompletionMediaId == mediaId && cacheCompletionJob?.isActive == true) return

        cacheCompletionJob?.cancel()
        cacheCompletionMediaId = mediaId
        cacheCompletionJob = coroutineScope.launch {
            runCatching {
                val dataSource = createDataSourceFactory().createDataSource() as? CacheDataSource
                    ?: error("Failed to create cache data source")
                val dataSpec = DataSpec.Builder()
                    .setUri("https://music.youtube.com/watch?v=$mediaId")
                    .setKey(mediaId)
                    .build()

                CacheWriter(dataSource, dataSpec, null, null).cache()
                Timber.d("PlayerServiceModern completed cache warmup for %s", mediaId)
            }.onFailure { error ->
                if (cacheCompletionMediaId == mediaId) {
                    Timber.w(error, "PlayerServiceModern cache warmup failed for %s", mediaId)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun buildCustomCommandButtons(): MutableList<CommandButton> {
        val notificationPlayerFirstIcon = preferences.getEnum(notificationPlayerFirstIconKey, NotificationButtons.Download)
        val notificationPlayerSecondIcon = preferences.getEnum(notificationPlayerSecondIconKey, NotificationButtons.Favorites)

        val commandButtonsList = mutableListOf<CommandButton>()
        val firstCommandButton = NotificationButtons.entries.let { buttons ->
            buttons
                .filter { it == notificationPlayerFirstIcon }
                .map {
                    val displayName = appContext().resources.getString(it.textId)
                    CommandButton.Builder()
                        .setDisplayName(displayName)
                        .setIconResId(
                            it.getStateIcon(
                                it,
                                currentSong.value?.likedAt,
                                currentSongStateDownload.value,
                                player.repeatMode,
                                player.shuffleModeEnabled
                            )
                        )
                        .setSessionCommand(it.sessionCommand)
                        .build()
                }
        }

        val secondCommandButton = NotificationButtons.entries.let { buttons ->
            buttons
                .filter { it == notificationPlayerSecondIcon }
                .map {
                    val displayName = appContext().resources.getString(it.textId)
                    CommandButton.Builder()
                        .setDisplayName(displayName)
                        .setIconResId(
                            it.getStateIcon(
                                it,
                                currentSong.value?.likedAt,
                                currentSongStateDownload.value,
                                player.repeatMode,
                                player.shuffleModeEnabled
                            )
                        )
                        .setSessionCommand(it.sessionCommand)
                        .build()
                }
        }

        val otherCommandButtons = NotificationButtons.entries.let { buttons ->
            buttons
                .filterNot { it == notificationPlayerFirstIcon || it == notificationPlayerSecondIcon }
                .map {
                    val displayName = appContext().resources.getString(it.textId)
                    CommandButton.Builder()
                        .setDisplayName(displayName)
                        .setIconResId(
                            it.getStateIcon(
                                it,
                                currentSong.value?.likedAt,
                                currentSongStateDownload.value,
                                player.repeatMode,
                                player.shuffleModeEnabled
                            )
                        )
                        .setSessionCommand(it.sessionCommand)
                        .build()
                }
        }

        commandButtonsList += firstCommandButton + secondCommandButton + otherCommandButtons
        return commandButtonsList
    }

    @Suppress("DEPRECATION")
    private fun updateCustomNotification(session: MediaSession): MediaNotification {
        val playIntent = Action.play.pendingIntent
        val pauseIntent = Action.pause.pendingIntent
        val nextIntent = Action.next.pendingIntent
        val prevIntent = Action.previous.pendingIntent

        val displayMediaItem = displayedMediaItem()
        if (displayMediaItem == null) {
            val fallbackNotification = NotificationCompat.Builder(this, NotificationChannelId)
                .setContentTitle("Cubic Music")
                .setContentText("Loading...")
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setStyle(MediaStyleNotificationHelper.MediaStyle(session))
                .build()
            return MediaNotification(NotificationId, fallbackNotification)
        }
        val mediaMetadata = displayMediaItem.mediaMetadata

        bitmapProvider.load(mediaMetadata.artworkUri) {}

        val customNotify = if (isAtLeastAndroid8) {
            NotificationCompat.Builder(this, NotificationChannelId)
        } else {
            NotificationCompat.Builder(this)
        }
            .setContentTitle(cleanPrefix(cleanPrefix(mediaMetadata.title?.toString() ?: "")))
            .setContentText(
                if (mediaMetadata.albumTitle != null && mediaMetadata.artist != "")
                    "${cleanPrefix(cleanPrefix(mediaMetadata.artist?.toString() ?: ""))} | ${cleanPrefix(cleanPrefix(mediaMetadata.albumTitle?.toString() ?: ""))}"
                else cleanPrefix(cleanPrefix(mediaMetadata.artist?.toString() ?: ""))
            )
            .setSubText(
                if (mediaMetadata.albumTitle != null && mediaMetadata.artist != "")
                    "${cleanPrefix(cleanPrefix(mediaMetadata.artist?.toString() ?: ""))} | ${cleanPrefix(cleanPrefix(mediaMetadata.albumTitle?.toString() ?: ""))}"
                else cleanPrefix(cleanPrefix(mediaMetadata.artist?.toString() ?: ""))
            )
            .setLargeIcon(bitmapProvider.bitmap)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSmallIcon(
                player.playerError?.let { R.drawable.alert_circle }
                    ?: R.drawable.ic_launcher_monochrome
            )
            .setOngoing(
                sessionPlayer.playWhenReady ||
                    sessionPlayer.isPlaying
            )
            .setContentIntent(
                activityPendingIntent<MainActivity>(flags = PendingIntent.FLAG_UPDATE_CURRENT) {
                    putExtra("expandPlayerBottomSheet", true)
                }
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(session))
            .addAction(R.drawable.play_skip_back, "Skip back", prevIntent)
            .addAction(
                if (sessionPlayer.isPlaying) R.drawable.pause else R.drawable.play,
                if (sessionPlayer.isPlaying) "Pause" else "Play",
                if (sessionPlayer.isPlaying) pauseIntent else playIntent
            )
            .addAction(R.drawable.play_skip_forward, "Skip forward", nextIntent)

        val notificationPlayerFirstIcon = preferences.getEnum(notificationPlayerFirstIconKey, NotificationButtons.Download)
        val notificationPlayerSecondIcon = preferences.getEnum(notificationPlayerSecondIconKey, NotificationButtons.Favorites)

        NotificationButtons.entries.filter { it == notificationPlayerFirstIcon }.map {
            customNotify.addAction(
                it.getStateIcon(it, currentSong.value?.likedAt, currentSongStateDownload.value, player.repeatMode, player.shuffleModeEnabled),
                appContext().resources.getString(it.textId),
                it.pendingIntent()
            )
        }

        NotificationButtons.entries.filter { it == notificationPlayerSecondIcon }.map {
            customNotify.addAction(
                it.getStateIcon(it, currentSong.value?.likedAt, currentSongStateDownload.value, player.repeatMode, player.shuffleModeEnabled),
                appContext().resources.getString(it.textId),
                it.pendingIntent()
            )
        }

        NotificationButtons.entries.filterNot { it == notificationPlayerFirstIcon || it == notificationPlayerSecondIcon }.map {
            customNotify.addAction(
                it.getStateIcon(it, currentSong.value?.likedAt, currentSongStateDownload.value, player.repeatMode, player.shuffleModeEnabled),
                appContext().resources.getString(it.textId),
                it.pendingIntent()
            )
        }

        updateWallpaper()
        return MediaNotification(NotificationId, customNotify.build())
    }

    private fun updateWallpaper() {
        val wallpaperEnabled = preferences.getBoolean(enableWallpaperKey, false)
        val wallpaperType = preferences.getEnum(wallpaperTypeKey, WallpaperType.Lockscreen)
        if (isAtLeastAndroid7 && wallpaperEnabled) {
            coroutineScope.launch(Dispatchers.IO) {
                val wpManager = WallpaperManager.getInstance(this@PlayerServiceModern)
                wpManager.setBitmap(
                    bitmapProvider.bitmap, null, true,
                    when (wallpaperType) {
                        WallpaperType.Both -> (FLAG_LOCK or FLAG_SYSTEM)
                        WallpaperType.Lockscreen -> FLAG_LOCK
                        WallpaperType.Home -> FLAG_SYSTEM
                    }
                )
            }
        }
    }

    private fun updateDefaultNotification() {
        if (!isServiceReady) return
        coroutineScope.launch(Dispatchers.Main) {
            if (::mediaSession.isInitialized) {
                mediaSession.setCustomLayout(buildCustomCommandButtons())
            }
        }
    }

    private fun requestPlaybackSurfaceRefresh(
        mediaItem: MediaItem? = displayedMediaItem(),
        includeWidgets: Boolean = true,
        minIntervalMs: Long = 250L,
    ) {
        if (!isServiceReady) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastPlaybackSurfaceRefreshMs < minIntervalMs) return
        lastPlaybackSurfaceRefreshMs = now

        mediaItem?.let {
            if (it.mediaId != lastPlaybackSurfaceMediaId) {
                lastPlaybackSurfaceMediaId = it.mediaId
                currentMediaItem.value = it
            }
        }
        updateDefaultNotification()
        if (includeWidgets) {
            updateWidgets()
        }
    }

    private fun requestArtworkPlaybackSurfaceRefresh(
        mediaItem: MediaItem? = displayedMediaItem(),
        includeWidgets: Boolean = true,
        minIntervalMs: Long = 250L,
    ) {
        if (!isServiceReady) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastPlaybackSurfaceRefreshMs < minIntervalMs) return
        lastPlaybackSurfaceRefreshMs = now
        mediaItem?.let {
            if (it.mediaId != lastPlaybackSurfaceMediaId) {
                lastPlaybackSurfaceMediaId = it.mediaId
                currentMediaItem.value = it
            }
        }

        val artworkKey = "${mediaItem?.mediaId.orEmpty()}|${mediaItem?.mediaMetadata?.artworkUri}"
        if (::bitmapProvider.isInitialized && artworkKey != lastArtworkRefreshKey) {
            lastArtworkRefreshKey = artworkKey
            bitmapProvider.load(mediaItem?.mediaMetadata?.artworkUri) {
                updateDefaultNotification()
                if (includeWidgets) {
                    updateWidgets()
                }
            }
            return
        }

        updateDefaultNotification()
        if (includeWidgets) {
            updateWidgets()
        }
    }

    fun toggleLike() {
        binder.toggleLike()
    }

    fun toggleDownload() {
        binder.toggleDownload()
    }

    fun toggleRepeat() {
        binder.toggleRepeat()
    }

    fun toggleShuffle() {
        binder.toggleShuffle()
    }

    fun startRadio() {
        player.currentMediaItem?.let(binder::startRadio)
    }

    private fun showSmartMessage(message: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSmartMessageMs < 1_500L) return
        lastSmartMessageMs = now
        Toaster.i(message)
    }

    private fun isExtractorDriftError(
        error: PlaybackException,
        deepestCause: Throwable?
    ): Boolean {
        val errorText = buildString {
            append(error.errorCodeName)
            append(' ')
            append(error.message.orEmpty())
            append(' ')
            append(error.cause?.javaClass?.name.orEmpty())
            append(' ')
            append(error.cause?.message.orEmpty())
            append(' ')
            append(deepestCause?.javaClass?.name.orEmpty())
            append(' ')
            append(deepestCause?.message.orEmpty())
        }

        return error.errorCodeName.contains("PARSING", ignoreCase = true) ||
            error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
            error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ||
            errorText.contains("JSON response is too short", ignoreCase = true) ||
            errorText.contains("ParsingException", ignoreCase = true) ||
            errorText.contains("NewPipe", ignoreCase = true) ||
            errorText.contains("extractor", ignoreCase = true) ||
            errorText.contains("Unable to connect to the server", ignoreCase = true)
    }

    @MainThread
    fun updateWidgets() {
        if (!isServiceReady) return
        val now = System.currentTimeMillis()
        if (now - lastWidgetUpdateMs < 750L) return
        lastWidgetUpdateMs = now

        val displayMediaMetadata = displayedMediaItem()?.mediaMetadata ?: binder.player.mediaMetadata
        val status = Triple(
            cleanPrefix(cleanPrefix(displayMediaMetadata.title.toString())),
            cleanPrefix(cleanPrefix(displayMediaMetadata.artist.toString())),
            binder.player.isPlaying
        )

        val actions = Triple(
            if (status.third) binder::gracefulPause else binder::gracefulPlay,
            binder.player::seekToPrevious,
            binder.player::seekToNext
        )

        widgetUpdateJob?.cancel()
        widgetUpdateJob = coroutineScope.launch {
            val file = File(cacheDir, "widget_thumbnail.png")
            FileOutputStream(file).use { outStream ->
                bitmapProvider.bitmap.compress(Bitmap.CompressFormat.PNG, 50, outStream)
            }

            withContext(Dispatchers.Default) {
                Widget.Vertical.update(applicationContext, actions, status, file)
                Widget.Horizontal.update(applicationContext, actions, status, file)
            }
        }
    }

    @UnstableApi
    private fun sendOpenEqualizerIntent() {
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        )
    }

    @UnstableApi
    private fun sendCloseEqualizerIntent() {
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
        )
    }

    private fun actionSearch() {
        binder.actionSearch()
    }

    private fun displayedMediaItem(): MediaItem? {
        return player.currentMediaItem
    }

    private data class PresenceSnapshot(
        val mediaItem: MediaItem?,
        val isPlaying: Boolean,
        val position: Long,
        val duration: Long,
    )

    private fun currentPresenceSnapshot(): PresenceSnapshot {
        val displayMediaItem = displayedMediaItem()
        val duration = when {
            sessionPlayer.duration > 0L -> sessionPlayer.duration
            player.duration > 0L -> player.duration
            else -> 0L
        }
        val position = when {
            sessionPlayer.currentPosition >= 0L -> sessionPlayer.currentPosition
            else -> player.currentPosition.coerceAtLeast(0L)
        }.coerceIn(0L, duration.coerceAtLeast(1L))
        val isPlaying = sessionPlayer.isPlaying || player.isPlaying

        return PresenceSnapshot(
            mediaItem = displayMediaItem,
            isPlaying = isPlaying,
            position = position,
            duration = duration
        )
    }

    private fun capturePlaybackSnapshot() {
        if (!isServiceReady) return
        lastPlaybackPositionMs = currentPresenceSnapshot().position.coerceAtLeast(0L)
        lastBufferedPositionMs = runCatching { player.bufferedPosition.coerceAtLeast(0L) }
            .getOrDefault(lastPlaybackPositionMs)
    }

    private fun recoverySnapshot(): PlaybackRecoveryHelper.Snapshot {
        capturePlaybackSnapshot()
        val currentItem = displayedMediaItem() ?: player.currentMediaItem
        return PlaybackRecoveryHelper.Snapshot(
            mediaId = currentItem?.mediaId,
            title = currentItem?.mediaMetadata?.title?.toString(),
            currentPositionMs = runCatching { player.currentPosition.coerceAtLeast(0L) }.getOrDefault(lastPlaybackPositionMs),
            bufferedPositionMs = lastBufferedPositionMs.coerceAtLeast(0L),
            playWhenReady = player.playWhenReady || (waitingForNetwork.value && resumeOnNetworkRestore),
            isNetworkAvailable = isNetworkAvailable.value,
            hasNextMediaItem = player.hasNextMediaItem(),
            skipOnErrorEnabled = preferences.getBoolean(skipMediaOnErrorKey, true),
            lastPlaybackPositionMs = lastPlaybackPositionMs,
            currentRetryCount = currentSongRetryCount,
        )
    }

    private fun applyRecoveryDecision(decision: PlaybackRecoveryHelper.Decision) {
        when (decision) {
            PlaybackRecoveryHelper.Decision.None -> Unit
            is PlaybackRecoveryHelper.Decision.WaitForNetwork -> {
                waitingForNetwork.value = true
                networkRecoveryMediaId = decision.mediaId
                resumeOnNetworkRestore = decision.resumeWhenNetworkReturns
                pauseTriggeredByNetworkWait = decision.resumeWhenNetworkReturns
                lastPlaybackPositionMs = decision.positionMs.coerceAtLeast(0L)
                if (decision.resumeWhenNetworkReturns) {
                    runCatching { binder.gracefulPause() }
                        .onFailure { Timber.e(it, "Failed to pause while waiting for network") }
                } else {
                    runCatching { player.pause() }
                        .onFailure { Timber.e(it, "Failed to pause player while waiting for network") }
                }
                showSmartMessage(decision.message)
            }

            is PlaybackRecoveryHelper.Decision.RetryCurrent -> {
                val isNetworkResume = waitingForNetwork.value && networkRecoveryMediaId == decision.mediaId
                waitingForNetwork.value = false
                networkRecoveryMediaId = null
                resumeOnNetworkRestore = false
                pauseTriggeredByNetworkWait = false
                if (!isNetworkResume) {
                    currentSongRetryCount = (currentSongRetryCount + 1).coerceAtMost(maxCurrentSongRetries)
                }
                lastPlaybackPositionMs = decision.positionMs.coerceAtLeast(0L)
                showSmartMessage(decision.message)
                runCatching { player.pause() }
                    .onFailure { Timber.e(it, "Failed to pause before retrying current song") }
                if (!player.safePrepare()) return
                handler.postDelayed(
                    {
                        if (!isServiceReady) return@postDelayed
                        if (player.currentMediaItem?.mediaId != decision.mediaId) return@postDelayed
                        player.safeSeekTo(lastPlaybackPositionMs)
                        runCatching { binder.gracefulPlay() }
                            .onFailure { Timber.e(it, "Failed to resume current song retry for %s", decision.mediaId) }
                    },
                    decision.delayMs
                )
            }

            is PlaybackRecoveryHelper.Decision.SkipNext -> {
                waitingForNetwork.value = false
                networkRecoveryMediaId = null
                resumeOnNetworkRestore = false
                pauseTriggeredByNetworkWait = false
                currentSongRetryCount = 0
                showSmartMessage(decision.message)
                runCatching { player.playNext() }
                    .onFailure { Timber.e(it, "Failed to skip to next song after recovery failure") }
            }

            is PlaybackRecoveryHelper.Decision.Pause -> {
                waitingForNetwork.value = false
                networkRecoveryMediaId = null
                resumeOnNetworkRestore = false
                pauseTriggeredByNetworkWait = false
                currentSongRetryCount = 0
                showSmartMessage(decision.message)
                runCatching { player.pause() }
                    .onFailure { Timber.e(it, "Failed to pause player after unrecoverable playback issue") }
            }
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (!isServiceReady) return
        capturePlaybackSnapshot()
        Timber.d("PlayerServiceModern onPositionDiscontinuity oldPosition ${oldPosition.mediaItemIndex} newPosition ${newPosition.mediaItemIndex} reason $reason")
        if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SKIP) {
            if (preferences.getBoolean(isDiscordPresenceEnabledKey, false)) {
                val token = encryptedPreferences.getString(discordPersonalAccessTokenKey, "")
                if (token?.isNotEmpty() == true) {
                    val presenceSnapshot = currentPresenceSnapshot()
                    val now = System.currentTimeMillis()
                    discordPresenceManager?.onPlayingStateChanged(
                        presenceSnapshot.mediaItem,
                        presenceSnapshot.isPlaying,
                        presenceSnapshot.position,
                        presenceSnapshot.duration,
                        now,
                        getCurrentPosition = { currentPresenceSnapshot().position },
                        isPlayingProvider = { currentPresenceSnapshot().isPlaying }
                    )
                }
            }
        }
        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
    }

    private fun maybeSavePlayerQueue() {
        if (!isPersistentQueueEnabled || !isServiceReady) return
        Timber.d("PlayerServiceModern maybeSavePlayerQueue is enabled")

        CoroutineScope(Dispatchers.Main).launch {
            if (!isServiceReady || !::player.isInitialized) return@launch
            val mediaItems = player.currentTimeline.mediaItems
            val mediaItemIndex = player.currentMediaItemIndex
            val mediaItemPosition = player.currentPosition

            if (mediaItems.isEmpty()) return@launch

            mediaItems.mapIndexed { index, mediaItem ->
                QueuedMediaItem(
                    mediaItem = mediaItem,
                    position = if (index == mediaItemIndex) mediaItemPosition else null
                )
            }.let { queuedMediaItems ->
                if (queuedMediaItems.isEmpty()) return@let

                Database.asyncTransaction {
                    queueTable.deleteAll()
                    queueTable.insert(queuedMediaItems)
                }

                Timber.d("PlayerServiceModern QueuePersistentEnabled Saved queue")
            }
        }
    }

    private fun maybeResumePlaybackOnStart() {
        if (isPersistentQueueEnabled && preferences.getBoolean(resumePlaybackOnStartKey, false)) {
            Timber.d("PlayerServiceModern: resumePlaybackOnStart is disabled for wake/start safety")
        }
    }

    @ExperimentalCoroutinesApi
    @FlowPreview
    @UnstableApi
    private fun maybeRestorePlayerQueue() {
        if (!isPersistentQueueEnabled) return

        Database.asyncQuery {
            val queuedSong = runBlocking {
                queueTable.all().first()
            }

            if (queuedSong.isEmpty()) return@asyncQuery

            val index = queuedSong.indexOfFirst { it.position != null }.coerceAtLeast(0)

            runBlocking(Dispatchers.Main) {
                if (!isServiceReady) return@runBlocking
                if (!player.safeSetMediaItems(
                    queuedSong.map { mediaItem ->
                        mediaItem.mediaItem.buildUpon()
                            .setUri(sanitizePlaybackUri(mediaItem.mediaItem.mediaId))
                            .setCustomCacheKey(mediaItem.mediaItem.mediaId)
                            .build().apply {
                                mediaMetadata.extras?.putBoolean("isFromPersistentQueue", true)
                            }
                    },
                    index,
                    queuedSong[index].position ?: C.TIME_UNSET
                )) return@runBlocking
                player.safePrepare()
            }
        }
    }

    @ExperimentalCoroutinesApi
    @FlowPreview
    @UnstableApi
    private fun maybeRestoreFromDiskPlayerQueue() {
        runCatching {
            filesDir.resolve("persistentQueue.data").inputStream().use { fis ->
                ObjectInputStream(fis).use { oos ->
                    oos.readObject() as PersistentQueue
                }
            }
        }.onSuccess { queue ->
            runBlocking(Dispatchers.Main) {
                if (!isServiceReady) return@runBlocking
                if (!player.safeSetMediaItems(
                    queue.songMediaItems.map { song ->
                        song.asMediaItem.buildUpon()
                            .setUri(sanitizePlaybackUri(song.asMediaItem.mediaId))
                            .setCustomCacheKey(song.asMediaItem.mediaId)
                            .build().apply {
                                mediaMetadata.extras?.putBoolean("isFromPersistentQueue", true)
                            }
                    },
                    queue.mediaItemIndex,
                    queue.position
                )) return@runBlocking
                player.safePrepare()
            }
        }.onFailure {
            Timber.e(it.stackTraceToString())
        }
    }

    private fun maybeSaveToDiskPlayerQueue() {
        val persistentQueue = PersistentQueue(
            title = "title",
            songMediaItems = player.currentTimeline.mediaItems.map {
                PersistentSong(
                    id = it.mediaId,
                    title = it.mediaMetadata.title.toString(),
                    durationText = it.mediaMetadata.extras?.getString("durationText").toString(),
                    thumbnailUrl = it.mediaMetadata.artworkUri.toString()
                )
            },
            mediaItemIndex = player.currentMediaItemIndex,
            position = player.currentPosition
        )

        runCatching {
            filesDir.resolve("persistentQueue.data").outputStream().use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(persistentQueue)
                }
            }
        }.onFailure {
            Timber.e(it.stackTraceToString())
        }.onSuccess {
            Log.d("mediaItem", "QueuePersistentEnabled Saved $persistentQueue")
        }
    }

    fun updateDownloadedState() {
        if (currentSong.value == null) return
        val mediaId = currentSong.value!!.id
        val downloads = MyDownloadHelper.downloads.value
        currentSongStateDownload.value = downloads[mediaId]?.state ?: Download.STATE_STOPPED
        updateDefaultNotification()
    }

    fun restartForegroundOrStop() {
        binder.restartForegroundOrStop()
    }

    private fun shouldKeepServiceAlive(): Boolean {
        if (!isServiceReady) return false
        val hasMediaItem = player.currentMediaItem != null || displayedMediaItem() != null
        return player.isPlaying ||
            player.playWhenReady ||
            player.playbackState == Player.STATE_BUFFERING ||
            player.playbackState == Player.STATE_READY ||
            hasMediaItem ||
            player.mediaItemCount > 0
    }

    @UnstableApi
    class CustomMediaNotificationProvider(context: Context) : DefaultMediaNotificationProvider(context) {
        override fun getNotificationContentTitle(metadata: MediaMetadata): CharSequence? {
            val customMetadata = MediaMetadata.Builder()
                .setTitle(cleanPrefix(metadata.title?.toString() ?: ""))
                .build()
            return super.getNotificationContentTitle(customMetadata)
        }

        override fun getNotificationContentText(metadata: MediaMetadata): CharSequence? {
            val customMetadata = MediaMetadata.Builder()
                .setArtist(cleanPrefix(metadata.artist?.toString() ?: ""))
                .setAlbumTitle(cleanPrefix(metadata.albumTitle?.toString() ?: ""))
                .build()
            return super.getNotificationContentText(customMetadata)
        }
    }

    class NotificationDismissReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            kotlin.runCatching {
                context.stopService(context.intent<PlayerServiceModern>())
            }.onFailure {
                Timber.e("Failed NotificationDismissReceiver stopService in PlayerServiceModern ${it.stackTraceToString()}")
            }
        }
    }

    inner class NotificationActionReceiver(private val player: Player) : BroadcastReceiver() {
        @ExperimentalCoroutinesApi
        @FlowPreview
        override fun onReceive(context: Context, intent: Intent) {
            if (!isServiceReady) return
            when (intent.action) {
                Action.pause.value -> binder.gracefulPause()
                Action.play.value -> binder.gracefulPlay()
                Action.next.value -> player.playNext()
                Action.previous.value -> player.playPrevious()
                Action.like.value -> binder.toggleLike()
                Action.download.value -> binder.toggleDownload()
                Action.playradio.value -> startRadio()
                Action.shuffle.value -> binder.toggleShuffle()
                Action.search.value -> binder.actionSearch()
                Action.repeat.value -> binder.toggleRepeat()
            }
        }
    }

    open inner class Binder : AndroidBinder() {
        var isManuallyShuffled = false
            internal set

        val service: PlayerServiceModern
            get() = this@PlayerServiceModern

        val bitmap: Bitmap
            get() = bitmapProvider.bitmap

        val player: ExoPlayer
            get() = this@PlayerServiceModern.player

        val sessionPlayer: ExoPlayer
            get() = this@PlayerServiceModern.sessionPlayer

        val cache: Cache
            get() = this@PlayerServiceModern.cache

        val downloadCache: Cache
            get() = this@PlayerServiceModern.downloadCache

        val sleepTimerMillisLeft: StateFlow<Long?>?
            get() = timerJob?.millisLeft

        val displayedMediaItem: MediaItem?
            get() = this@PlayerServiceModern.displayedMediaItem()

        val displayedPositionAndDuration: Pair<Long, Long>
            get() {
                val duration = player.duration.coerceAtLeast(1L)
                val position = player.currentPosition.coerceIn(0L, duration)
                return position to duration
            }

        fun startSleepTimer(delayMillis: Long) {
            timerJob?.cancel()
            timerJob = coroutineScope.timer(delayMillis) {
                val notification = NotificationCompat
                    .Builder(this@PlayerServiceModern, SleepTimerNotificationChannelId)
                    .setContentTitle(getString(R.string.sleep_timer_ended))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(true)
                    .setSmallIcon(R.drawable.ic_launcher_monochrome)
                    .build()

                notificationManager?.notify(SleepTimerNotificationId, notification)

                coroutineScope.launch {
                    delay(1000)
                    stopSelf()
                    exitProcess(0)
                }
            }
        }

        fun cancelSleepTimer() {
            timerJob?.cancel()
            timerJob = null
        }

        private var radioJob: Job? = null

        var isLoadingRadio by mutableStateOf(false)
            private set

        fun startRadio(
            mediaItem: MediaItem,
            append: Boolean = false,
            endpoint: NavigationEndpoint.Endpoint.Watch? = null
        ) {
            this.stopRadio()
            if (!mediaItem.isPlayable()) {
                Toaster.w("This song source is invalid and cannot be played")
                Timber.w("startRadio skipped invalid media item: %s", mediaItem.mediaId)
                return
            }
            val sanitizedMediaItem = mediaItem.buildUpon()
                .setUri(
                    sanitizePlaybackUri(
                        mediaItem.localConfiguration?.uri?.toString()
                            ?.takeIf { it.isNotBlank() }
                            ?: mediaItem.mediaId
                    )
                )
                .build()
            val sourceVideoId = sanitizedMediaItem.mediaId.substringAfterLast("/")

            if (player.currentMediaItem?.mediaId != sanitizedMediaItem.mediaId)
                player.forcePlay(sanitizedMediaItem)

            radioJob = coroutineScope.launch {
                isLoadingRadio = true

                var playlistId = endpoint?.playlistId

                if (playlistId == null)
                    Innertube.nextPage(NextBody(videoId = sourceVideoId))
                        ?.getOrNull()
                        ?.itemsPage
                        ?.items
                        ?.firstOrNull()
                        ?.let { it.info?.endpoint?.playlistId }
                        ?.also { playlistId = it }

                if (!playlistId.isNullOrBlank())
                    Innertube.nextPage(NextBody(videoId = sourceVideoId, playlistId = playlistId))
                        ?.getOrNull()
                        ?.itemsPage
                        ?.items
                        ?.map(Innertube.SongItem::asMediaItem)
                        ?.distinctBy { it.mediaId }
                        ?.let { relatedSongs ->
                            Database.asyncTransaction {
                                relatedSongs.forEach(::insertIgnore)
                            }

                            val currentQueue = withContext(Dispatchers.Main) {
                                player.mediaItems.fastMap(MediaItem::mediaId)
                            }

                            relatedSongs.filter { it.mediaId != sanitizedMediaItem.mediaId && it.mediaId !in currentQueue }
                        }
                        ?.also {
                            withContext(Dispatchers.Main) {
                                if (!isServiceReady) return@withContext
                                val curIndex = player.currentMediaItemIndex
                                val endIndex = player.mediaItemCount
                                if (!append && player.mediaItemCount > 1) {
                                    player.moveMediaItem(curIndex, 0)
                                    player.removeMediaItems(curIndex + 1, endIndex)
                                }
                                player.addMediaItems(it)
                            }
                        }

                isLoadingRadio = false
            }
        }

        fun startRadio(
            song: Song,
            append: Boolean = false,
            endpoint: NavigationEndpoint.Endpoint.Watch? = null
        ) = startRadio(song.asMediaItem, append, endpoint)

        fun stopRadio() {
            isLoadingRadio = false
            radioJob?.cancel()
            radio = null
        }

        @MainThread
        fun gracefulPause() {
            val duration = preferences.getEnum(playbackFadeAudioDurationKey, DurationInMilliseconds.Disabled)
            player.fadeOutEffect(duration.asMillis)
        }

        @MainThread
        fun gracefulPlay() {
            val duration = preferences.getEnum(playbackFadeAudioDurationKey, DurationInMilliseconds.Disabled)
            player.fadeInEffect(duration.asMillis)
        }

        fun restartForegroundOrStop() {
            runCatching {
                updateDefaultNotification()
                if (!shouldKeepServiceAlive()) {
                    Timber.d(
                        "PlayerServiceModern.restartForegroundOrStop stopping service playbackState=%s playWhenReady=%s mediaId=%s queueSize=%s",
                        if (isServiceReady) player.playbackState else -1,
                        if (isServiceReady) player.playWhenReady else false,
                        if (isServiceReady) player.currentMediaItem?.mediaId else null,
                        if (isServiceReady) player.mediaItemCount else 0
                    )
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    Timber.d(
                        "PlayerServiceModern.restartForegroundOrStop keeping service playbackState=%s playWhenReady=%s mediaId=%s queueSize=%s",
                        if (isServiceReady) player.playbackState else -1,
                        if (isServiceReady) player.playWhenReady else false,
                        if (isServiceReady) player.currentMediaItem?.mediaId else null,
                        if (isServiceReady) player.mediaItemCount else 0
                    )
                }
            }.onFailure {
                Timber.e(it, "PlayerServiceModern restartForegroundOrStop failed")
            }
        }

        @kotlin.OptIn(FlowPreview::class)
        fun toggleLike() {
            coroutineScope.launch(Dispatchers.IO) {
                currentMediaItem.value?.let { mediaItem ->
                    app.kreate.android.me.knighthat.sync.YouTubeSync.toggleSongLike(
                        this@PlayerServiceModern,
                        mediaItem
                    )
                    updateDefaultNotification()
                }
            }
        }

        fun toggleDownload() {
            manageDownload(
                context = this@PlayerServiceModern,
                mediaItem = currentMediaItem.value ?: return,
                downloadState = currentSongStateDownload.value == Download.STATE_COMPLETED
            )
        }

        fun toggleRepeat() {
            player.toggleRepeatMode()
            updateDefaultNotification()
        }

        fun toggleShuffle() {
            player.toggleShuffleMode()
            updateDefaultNotification()
        }

        fun actionSearch() {
            startActivity(
                Intent(applicationContext, MainActivity::class.java)
                    .setAction(MainActivity.action_search)
                    .setFlags(FLAG_ACTIVITY_NEW_TASK + FLAG_ACTIVITY_CLEAR_TASK)
            )
            Timber.d("PlayerServiceModern actionSearch")
        }
    }

    @JvmInline
    value class Action(val value: String) {
        val pendingIntent: PendingIntent
            get() = PendingIntent.getBroadcast(
                appContext(),
                100,
                Intent(value).setPackage(appContext().packageName),
                PendingIntent.FLAG_UPDATE_CURRENT.or(if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0)
            )

        companion object {
            val pause = Action("it.fast4x.rimusic.pause")
            val play = Action("it.fast4x.rimusic.play")
            val next = Action("it.fast4x.rimusic.next")
            val previous = Action("it.fast4x.rimusic.previous")
            val like = Action("it.fast4x.rimusic.like")
            val download = Action("it.fast4x.rimusic.download")
            val playradio = Action("it.fast4x.rimusic.playradio")
            val shuffle = Action("it.fast4x.rimusic.shuffle")
            val search = Action("it.fast4x.rimusic.search")
            val repeat = Action("it.fast4x.rimusic.repeat")
        }
    }

    companion object {
        const val NotificationId = 1001
        const val NotificationChannelId = "default_channel_id"

        const val SleepTimerNotificationId = 1002
        const val SleepTimerNotificationChannelId = "sleep_timer_channel_id"

        val PlayerErrorsToReload = arrayOf(416, 4003)
        val PlayerErrorsToSkip = arrayOf(2000)

        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"
        const val SEARCHED = "searched"

        const val CACHE_DIRNAME = "exoplayer"
    }
}
