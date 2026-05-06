package app.it.fast4x.rimusic.recognition

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.max

class AudioRecorder(private val scope: CoroutineScope) {

    private var instance: AudioRecord? = null
    private var job: Job? = null
    private val mutex = Mutex()

    private val _active   = MutableStateFlow(false)
    private val _duration = MutableStateFlow(0)
    private val _buffer   = MutableStateFlow(ByteArray(0))

    val active   = _active.asStateFlow()
    val duration = _duration.asStateFlow()
    val buffer   = _buffer.asStateFlow()

    fun start() {
        scope.launch {
            mutex.withLock {
                if (_active.value) return@launch
                runCatching {
                    instance = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        BUFFER_SIZE
                    ).also { it.startRecording() }
                    reset(true)
                    job = scope.launch(Dispatchers.IO) { loop() }
                }.onFailure { stop() }
            }
        }
    }

    fun stop() {
        scope.launch {
            mutex.withLock {
                if (!_active.value) return@launch
                job?.cancelAndJoin()
                runCatching { instance?.stop(); instance?.release() }
                instance = null
                reset(false)
            }
        }
    }

    private suspend fun loop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        runCatching {
            val out = ByteArrayOutputStream()
            while (coroutineContext.isActive) {
                val chunk = ByteArray(BUFFER_SIZE)
                val read  = instance?.read(chunk, 0, chunk.size) ?: 0
                if (read > 0) {
                    out.write(chunk, 0, read)
                    val bytes = out.toByteArray()
                    _buffer.emit(bytes)
                    _duration.emit(bytes.size / (SAMPLE_RATE * SAMPLE_WIDTH * CHANNEL_COUNT))
                }
            }
        }.onFailure { reset(false) }
    }

    private suspend fun reset(active: Boolean) {
        _active.emit(active)
        _duration.emit(0)
        _buffer.emit(ByteArray(0))
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val CHANNEL_CONFIG  = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT    = AudioFormat.ENCODING_PCM_16BIT
        private const val CHANNEL_COUNT   = 1
        private const val SAMPLE_WIDTH    = 2  // bytes per 16-bit sample
        private val BUFFER_SIZE = max(
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2,
            SAMPLE_RATE * SAMPLE_WIDTH * CHANNEL_COUNT
        )
    }
}