package windows

import com.sun.jna.platform.win32.COM.util.Factory
import com.sun.jna.platform.win32.COM.util.IUnknown
import com.sun.jna.platform.win32.COM.util.annotation.ComInterface
import com.sun.jna.platform.win32.COM.util.annotation.ComMethod
import com.sun.jna.platform.win32.COM.util.annotation.ComObject
import com.sun.jna.platform.win32.COM.util.annotation.ComProperty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import player.PlayerController
import player.PlayerState
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Windows-native audio controller. It uses the Media Player COM engine bundled with Windows. */
class WindowsMediaController : PlayerController {
    private val factory = Factory()
    private val player by lazy { factory.createObject(WindowsMediaPlayer::class.java) }
    private val poller = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "cubic-windows-player-state").apply { isDaemon = true }
    }
    private val _state = MutableStateFlow(PlayerState())
    private var requestedVolume = 0.5f

    @Volatile private var playWhenReady = false
    private var lastPlayState = Int.MIN_VALUE
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    init {
        poller.scheduleAtFixedRate(::refreshState, 0, 250, TimeUnit.MILLISECONDS)
    }

    override fun load(url: String) {
        runCatching {
            playWhenReady = true
            player.settings.autoStart = true
            player.settings.volume = (requestedVolume * 100).toInt()
            player.url = url
            _state.update { PlayerState(volume = requestedVolume) }
        }.onFailure { System.err.println("Cubic Windows player load failed: ${it.message}") }
    }

    override fun play() {
        playWhenReady = true
        runCatching { player.controls.play() }
            .onFailure { System.err.println("Cubic Windows player play failed: ${it.message}") }
    }

    override fun pause() {
        playWhenReady = false
        runCatching { player.controls.pause() }
    }

    override fun stop() {
        playWhenReady = false
        runCatching { player.controls.stop() }
        _state.update { it.copy(isPlaying = false, timestamp = 0L) }
    }

    override fun dispose() {
        playWhenReady = false
        poller.shutdownNow()
        runCatching { player.controls.stop() }
        runCatching { factory.disposeAll() }
    }

    override fun seekTo(timestamp: Long) {
        runCatching { player.controls.currentPosition = timestamp.coerceAtLeast(0L) / 1000.0 }
        _state.update { it.copy(timestamp = timestamp.coerceAtLeast(0L)) }
    }

    override fun setVolume(value: Float) {
        requestedVolume = value.coerceIn(0f, 1f)
        runCatching {
            player.settings.volume = (requestedVolume * 100).toInt()
            if (requestedVolume > 0f) player.settings.mute = false
        }
        _state.update { it.copy(volume = requestedVolume, isMuted = false) }
    }

    override fun toggleSound() {
        val muted = !_state.value.isMuted
        runCatching { player.settings.mute = muted }
        _state.update { it.copy(isMuted = muted) }
    }

    private fun refreshState() {
        runCatching {
            val playState = player.playState
            val controls = player.controls
            val settings = player.settings
            val duration = runCatching { player.currentMedia?.duration ?: 0.0 }.getOrDefault(0.0)
            if (playWhenReady && playState in setOf(1, 2, 10)) controls.play()
            if (playState != lastPlayState) {
                lastPlayState = playState
                println("Cubic Windows player state: $playState")
            }
            _state.value = PlayerState(
                isPlaying = playState == 3,
                isMuted = settings.mute,
                volume = (settings.volume / 100f).coerceIn(0f, 1f),
                timestamp = (controls.currentPosition * 1000.0).toLong().coerceAtLeast(0L),
                duration = (duration * 1000.0).toLong().coerceAtLeast(0L)
            )
        }
    }
}

@ComObject(clsId = "{6BF52A52-394A-11D3-B153-00C04F79FAA6}")
interface WindowsMediaPlayer : IUnknown {
    @get:ComProperty(name = "URL")
    @set:ComProperty(name = "URL")
    var url: String

    @get:ComProperty(name = "controls")
    val controls: WindowsMediaControls

    @get:ComProperty(name = "settings")
    val settings: WindowsMediaSettings

    @get:ComProperty(name = "currentMedia")
    val currentMedia: WindowsMediaItem?

    @get:ComProperty(name = "playState")
    val playState: Int
}

@ComInterface(iid = "{74C09E02-F828-11D2-A74B-00A0C905F36E}")
interface WindowsMediaControls : IUnknown {
    @ComMethod(name = "play") fun play()
    @ComMethod(name = "pause") fun pause()
    @ComMethod(name = "stop") fun stop()

    @get:ComProperty(name = "currentPosition")
    @set:ComProperty(name = "currentPosition")
    var currentPosition: Double
}

@ComInterface(iid = "{9104D1AB-80C9-4FED-ABF0-2E6417A6DF14}")
interface WindowsMediaSettings : IUnknown {
    @get:ComProperty(name = "autoStart")
    @set:ComProperty(name = "autoStart")
    var autoStart: Boolean

    @get:ComProperty(name = "mute")
    @set:ComProperty(name = "mute")
    var mute: Boolean

    @get:ComProperty(name = "volume")
    @set:ComProperty(name = "volume")
    var volume: Int
}

@ComInterface(iid = "{94D55E95-3FAC-11D3-B155-00C04F79FAA6}")
interface WindowsMediaItem : IUnknown {
    @get:ComProperty(name = "duration")
    val duration: Double
}
