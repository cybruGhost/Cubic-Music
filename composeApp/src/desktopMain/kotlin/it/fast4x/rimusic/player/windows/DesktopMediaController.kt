package windows

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import player.PlayerController
import player.PlayerState
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Base64
import java.util.concurrent.Executors

/**
 * Codec-capable Windows desktop audio controller.
 *
 * A small private PowerShell/WPF helper hosts Windows' modern MediaPlayer on an STA thread.
 * Commands travel over stdin, so signed remote URLs are never interpreted by a shell.
 */
class DesktopMediaController : PlayerController {
    private val stateFlow = MutableStateFlow(PlayerState(volume = 0.5f))
    override val state: StateFlow<PlayerState> = stateFlow.asStateFlow()

    private val helperFile = Files.createTempFile("cubic-music-player-", ".ps1").also { path ->
        Files.writeString(path, HELPER_SCRIPT, StandardCharsets.UTF_8)
        path.toFile().deleteOnExit()
    }
    private val process = ProcessBuilder(
        "powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-STA",
        "-ExecutionPolicy", "Bypass", "-File", helperFile.toString()
    ).start()
    private val writer = process.outputStream.bufferedWriter(StandardCharsets.UTF_8)
    private val readers = Executors.newFixedThreadPool(2) { task ->
        Thread(task, "cubic-desktop-media-helper").apply { isDaemon = true }
    }
    @Volatile private var disposed = false

    init {
        readers.submit {
            process.inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
                lines.forEach(::consumeHelperLine)
            }
        }
        readers.submit {
            process.errorStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
                lines.forEach { System.err.println("Cubic desktop media: $it") }
            }
        }
    }

    override fun load(url: String) {
        val encoded = Base64.getEncoder().encodeToString(url.toByteArray(StandardCharsets.UTF_8))
        send("LOAD|$encoded")
        stateFlow.update { PlayerState(volume = it.volume, isMuted = it.isMuted) }
    }

    override fun play() = send("PLAY")
    override fun pause() = send("PAUSE")

    override fun stop() {
        send("STOP")
        stateFlow.update { it.copy(isPlaying = false, timestamp = 0L) }
    }

    override fun seekTo(timestamp: Long) {
        val value = timestamp.coerceAtLeast(0L)
        send("SEEK|$value")
        stateFlow.update { it.copy(timestamp = value) }
    }

    override fun setVolume(value: Float) {
        val safe = value.coerceIn(0f, 1f)
        send("VOLUME|${(safe * 100).toInt()}")
        stateFlow.update { it.copy(volume = safe, isMuted = false) }
    }

    override fun toggleSound() {
        send("MUTE")
        stateFlow.update { it.copy(isMuted = !it.isMuted) }
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        runCatching { send("QUIT", allowDisposed = true) }
        runCatching { writer.close() }
        runCatching { if (!process.waitFor(750, java.util.concurrent.TimeUnit.MILLISECONDS)) process.destroy() }
        readers.shutdownNow()
        runCatching { Files.deleteIfExists(helperFile) }
    }

    @Synchronized
    private fun send(command: String, allowDisposed: Boolean = false) {
        if (disposed && !allowDisposed) return
        runCatching {
            writer.write(command)
            writer.newLine()
            writer.flush()
        }.onFailure { System.err.println("Cubic desktop media command failed: ${it.message}") }
    }

    private fun consumeHelperLine(line: String) {
        val values = line.split('|')
        if (values.firstOrNull() != "STATE" || values.size < 7) return
        val opened = values[1] == "1"
        val playing = values[2] == "1"
        val position = values[3].toLongOrNull() ?: 0L
        val duration = values[4].toLongOrNull() ?: 0L
        val volume = ((values[5].toIntOrNull() ?: 50) / 100f).coerceIn(0f, 1f)
        val muted = values[6] == "1"
        stateFlow.value = PlayerState(
            isPlaying = opened && playing,
            isMuted = muted,
            volume = volume,
            timestamp = position.coerceAtLeast(0L),
            duration = duration.coerceAtLeast(0L)
        )
    }

    private companion object {
        val HELPER_SCRIPT = """
            Add-Type -AssemblyName PresentationCore
            Add-Type -AssemblyName WindowsBase
            Add-Type -AssemblyName PresentationFramework
            ${'$'}application = New-Object System.Windows.Application
            §window = New-Object System.Windows.Window
            §window.Width = 1
            §window.Height = 1
            §window.Left = -10000
            §window.Top = -10000
            §window.ShowInTaskbar = §false
            §window.WindowStyle = [System.Windows.WindowStyle]::None
            §window.Opacity = 0
            §player = New-Object System.Windows.Controls.MediaElement
            §player.LoadedBehavior = [System.Windows.Controls.MediaState]::Manual
            §player.UnloadedBehavior = [System.Windows.Controls.MediaState]::Manual
            §window.Content = §player
            §window.Show()
            §dispatcher = [System.Windows.Threading.Dispatcher]::CurrentDispatcher
            §script:opened = §false
            §script:wantsPlayback = §false
            §script:muted = §false
            §script:volume = 50
            §script:running = §true
            §player.Volume = 0.5
            §player.add_MediaOpened({
                §script:opened = §true
                if (§script:wantsPlayback) { §player.Play() }
            })
            §player.add_MediaEnded({ §script:wantsPlayback = §false })
            §player.add_MediaFailed({
                param(§sender, §eventArgs)
                §script:opened = §false
                §script:wantsPlayback = §false
                [Console]::Error.WriteLine("Media failed: " + §eventArgs.ErrorException.Message)
            })
            §pending = [Console]::In.ReadLineAsync()
            §lastState = [DateTime]::UtcNow
            while (§script:running) {
                if (§pending.IsCompleted) {
                    §line = §pending.GetAwaiter().GetResult()
                    if (§null -eq §line) { break }
                    §parts = §line.Split('|', 2)
                    switch (§parts[0]) {
                        'LOAD' {
                            §url = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String(§parts[1]))
                            §script:opened = §false
                            §script:wantsPlayback = §true
                            §player.Stop()
                            §player.Source = §null
                            §player.Source = [Uri]§url
                            §player.Play()
                        }
                        'PLAY' { §script:wantsPlayback = §true; §player.Play() }
                        'PAUSE' { §script:wantsPlayback = §false; §player.Pause() }
                        'STOP' { §script:wantsPlayback = §false; §player.Stop() }
                        'SEEK' { §player.Position = [TimeSpan]::FromMilliseconds([double]§parts[1]) }
                        'VOLUME' {
                            §script:volume = [Math]::Max(0, [Math]::Min(100, [int]§parts[1]))
                            §script:muted = §false
                            §player.IsMuted = §false
                            §player.Volume = §script:volume / 100.0
                        }
                        'MUTE' { §script:muted = -not §script:muted; §player.IsMuted = §script:muted }
                        'QUIT' { §script:running = §false }
                    }
                    if (§script:running) { §pending = [Console]::In.ReadLineAsync() }
                }
                §dispatcher.Invoke([Action]{}, [System.Windows.Threading.DispatcherPriority]::Background)
                if (([DateTime]::UtcNow - §lastState).TotalMilliseconds -ge 200) {
                    §position = [long]§player.Position.TotalMilliseconds
                    §duration = 0
                    if (§player.NaturalDuration.HasTimeSpan) { §duration = [long]§player.NaturalDuration.TimeSpan.TotalMilliseconds }
                    §state = 'STATE|{0}|{1}|{2}|{3}|{4}|{5}' -f [int]§script:opened,[int]§script:wantsPlayback,§position,§duration,§script:volume,[int]§script:muted
                    [Console]::Out.WriteLine(§state)
                    [Console]::Out.Flush()
                    §lastState = [DateTime]::UtcNow
                }
                Start-Sleep -Milliseconds 15
            }
            §player.Stop()
            §window.Close()
        """.trimIndent().replace('§', '$')
    }
}
