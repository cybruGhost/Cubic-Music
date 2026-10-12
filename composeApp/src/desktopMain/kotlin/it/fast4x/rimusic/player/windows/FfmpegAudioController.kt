package windows

import app.it.fast4x.rimusic.net.CubicRangeTransfer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import player.PlayerController
import player.PlayerState
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator
import java.io.BufferedInputStream
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.math.roundToInt

/** Desktop-only FFmpeg decoder feeding Java Sound PCM. No installed media player is required. */
class FfmpegAudioController(
    private val httpClient: OkHttpClient = OkHttpClient()
) : PlayerController {
    private val stateFlow = MutableStateFlow(PlayerState(volume = 0.5f))
    override val state: StateFlow<PlayerState> = stateFlow.asStateFlow()

    private val workers = Executors.newCachedThreadPool { task ->
        Thread(task, "cubic-ffmpeg-audio").apply { isDaemon = true }
    }
    private val format = AudioFormat(SAMPLE_RATE.toFloat(), 16, CHANNELS, true, false)
    private val durationPattern = Regex("Duration: (\\d+):(\\d+):(\\d+(?:\\.\\d+)?)")
    private val lock = Any()

    @Volatile private var generation = 0L
    @Volatile private var currentUrl: String? = null
    @Volatile private var process: Process? = null
    @Volatile private var audioLine: SourceDataLine? = null
    @Volatile private var paused = false
    @Volatile private var disposed = false
    @Volatile private var expectedDurationMs = 0L
    @Volatile private var seekBaseMs = 0L
    @Volatile private var volume = 0.5f
    @Volatile private var muted = false
    @Volatile var onStreamFailure: ((url: String, positionMs: Long) -> Unit)? = null

    override fun load(url: String) = loadAt(url, 0L)

    fun loadAt(url: String, positionMs: Long) {
        currentUrl = url
        paused = false
        startDecoder(url, positionMs.coerceAtLeast(0L))
    }

    override fun play() {
        paused = false
        runCatching { audioLine?.start() }
    }

    override fun pause() {
        paused = true
        runCatching { audioLine?.stop() }
        stateFlow.update { it.copy(isPlaying = false) }
    }

    override fun stop() {
        currentUrl = null
        stopDecoder(resetTimestamp = true)
    }

    override fun seekTo(timestamp: Long) {
        val safe = timestamp.coerceIn(0L, expectedDurationMs.takeIf { it > 0L } ?: Long.MAX_VALUE)
        currentUrl?.let { startDecoder(it, safe) }
    }

    fun setExpectedDuration(durationMs: Long) {
        expectedDurationMs = durationMs.coerceAtLeast(0L)
        stateFlow.update { it.copy(duration = expectedDurationMs) }
    }

    override fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        muted = false
        stateFlow.update { it.copy(volume = volume, isMuted = false) }
    }

    override fun toggleSound() {
        muted = !muted
        stateFlow.update { it.copy(isMuted = muted) }
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        currentUrl = null
        onStreamFailure = null
        stopDecoder(resetTimestamp = true)
        workers.shutdownNow()
    }

    private fun startDecoder(url: String, startAtMs: Long) {
        val runGeneration = synchronized(lock) {
            generation += 1
            process?.destroyForcibly()
            process = null
            runCatching { audioLine?.stop() }
            runCatching { audioLine?.close() }
            audioLine = null
            seekBaseMs = startAtMs
            stateFlow.update { it.copy(isPlaying = false, timestamp = startAtMs, duration = expectedDurationMs) }
            generation
        }
        workers.submit { decode(url, startAtMs, runGeneration) }
    }

    private fun decode(url: String, startAtMs: Long, runGeneration: Long) {
        var localProcess: Process? = null
        var localLine: SourceDataLine? = null
        try {
            val args = mutableListOf(
                DefaultFFMPEGLocator().executablePath,
                "-hide_banner", "-loglevel", "info"
            )
            val remote = url.startsWith("http", ignoreCase = true)
            args += listOf("-i", if (remote) "pipe:0" else url)
            if (startAtMs > 0) args += listOf("-ss", "%.3f".format(java.util.Locale.US, startAtMs / 1000.0))
            args += listOf(
                "-vn", "-sn", "-dn", "-f", "s16le", "-acodec", "pcm_s16le",
                "-ac", CHANNELS.toString(), "-ar", SAMPLE_RATE.toString(), "pipe:1"
            )
            localProcess = ProcessBuilder(args).start()
            val runningProcess = checkNotNull(localProcess)
            synchronized(lock) {
                if (runGeneration != generation || disposed) {
                    runningProcess.destroyForcibly()
                    return
                }
                process = runningProcess
            }
            workers.submit { readDecoderErrors(runningProcess, runGeneration) }
            if (remote) workers.submit {
                try {
                    runningProcess.outputStream.buffered(64 * 1024).use { output ->
                        CubicRangeTransfer.copy(
                            client = httpClient,
                            url = url,
                            output = output,
                            shouldContinue = { runGeneration == generation && !disposed }
                        )
                    }
                } catch (error: Throwable) {
                    if (runGeneration == generation && !disposed) {
                        System.err.println("Cubic range input failed: ${error.message}")
                    }
                }
            }

            val info = DataLine.Info(SourceDataLine::class.java, format)
            localLine = (AudioSystem.getLine(info) as SourceDataLine).apply {
                open(format, SAMPLE_RATE * CHANNELS * 2)
                start()
            }
            synchronized(lock) {
                if (runGeneration != generation || disposed) {
                    localLine.close()
                    localProcess.destroyForcibly()
                    return
                }
                audioLine = localLine
            }

            val input = BufferedInputStream(localProcess.inputStream, 64 * 1024)
            val buffer = ByteArray(16 * 1024)
            var lastStateUpdate = 0L
            while (runGeneration == generation && !disposed) {
                while (paused && runGeneration == generation && !disposed) Thread.sleep(20)
                val count = input.read(buffer)
                if (count <= 0) break
                applyGain(buffer, count, if (muted) 0f else volume)
                var written = 0
                while (written < count && runGeneration == generation && !disposed) {
                    written += localLine.write(buffer, written, count - written)
                }
                val now = System.nanoTime()
                if (now - lastStateUpdate >= 100_000_000L) {
                    val position = seekBaseMs + localLine.microsecondPosition / 1000L
                    stateFlow.update { it.copy(isPlaying = !paused, timestamp = position) }
                    lastStateUpdate = now
                }
            }
            if (runGeneration == generation && !disposed) {
                localLine.drain()
                val position = seekBaseMs + localLine.microsecondPosition / 1000L
                stateFlow.update { it.copy(isPlaying = false, timestamp = position) }
                val endedEarly = expectedDurationMs > 0L && position < expectedDurationMs - 1_500L
                if (endedEarly && !paused) {
                    System.err.println("Cubic FFmpeg input ended early at ${position}ms of ${expectedDurationMs}ms")
                    onStreamFailure?.invoke(url, position)
                }
            }
        } catch (error: Throwable) {
            if (runGeneration == generation && !disposed) {
                val position = stateFlow.value.timestamp
                System.err.println("Cubic FFmpeg player failed: ${error.message}")
                stateFlow.update { it.copy(isPlaying = false) }
                onStreamFailure?.invoke(url, position)
            }
        } finally {
            runCatching { localLine?.stop() }
            runCatching { localLine?.close() }
            runCatching { localProcess?.destroy() }
            synchronized(lock) {
                if (runGeneration == generation) {
                    audioLine = null
                    process = null
                }
            }
        }
    }

    private fun readDecoderErrors(runningProcess: Process, runGeneration: Long) {
        runningProcess.errorStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                durationPattern.find(line)?.let { match ->
                    val (hours, minutes, seconds) = match.destructured
                    val duration = ((hours.toLong() * 3600 + minutes.toLong() * 60 + seconds.toDouble()) * 1000).toLong()
                    if (runGeneration == generation) stateFlow.update { it.copy(duration = duration) }
                }
                if (line.contains("error", ignoreCase = true) || line.contains("failed", ignoreCase = true) || line.contains("403")) {
                    System.err.println("Cubic FFmpeg: $line")
                }
            }
        }
    }

    private fun stopDecoder(resetTimestamp: Boolean) {
        synchronized(lock) {
            generation += 1
            runCatching { process?.destroyForcibly() }
            process = null
            runCatching { audioLine?.stop() }
            runCatching { audioLine?.close() }
            audioLine = null
        }
        stateFlow.update { it.copy(isPlaying = false, timestamp = if (resetTimestamp) 0L else it.timestamp) }
    }

    private fun applyGain(data: ByteArray, size: Int, gain: Float) {
        if (gain >= 0.999f) return
        var index = 0
        while (index + 1 < size) {
            val sample = ((data[index].toInt() and 0xff) or (data[index + 1].toInt() shl 8)).toShort().toInt()
            val scaled = (sample * gain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            data[index] = scaled.toByte()
            data[index + 1] = (scaled shr 8).toByte()
            index += 2
        }
    }

    private companion object {
        const val SAMPLE_RATE = 48_000
        const val CHANNELS = 2
    }
}
