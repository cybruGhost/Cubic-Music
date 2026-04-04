package app.it.fast4x.rimusic.extensions.youtubelogin

import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max
import kotlin.random.Random

object YouTubeRequestThrottler {
    private val mutex = Mutex()
    private var nextAllowedAt = 0L

    suspend fun <T> run(block: suspend () -> T): T {
        repeat(3) { attempt ->
            waitForTurn()
            try {
                return block()
            } catch (throwable: Throwable) {
                if (!throwable.isRateLimited() || attempt == 2) throw throwable
                val backoffMs = (1500L shl attempt) + Random.nextLong(250L, 900L)
                postpone(backoffMs)
            }
        }

        error("Request throttler exhausted retries")
    }

    private suspend fun waitForTurn() {
        val waitMs = mutex.withLock {
            max(0L, nextAllowedAt - System.currentTimeMillis())
        }
        if (waitMs > 0) delay(waitMs)
        delay(Random.nextLong(125L, 375L))
    }

    private suspend fun postpone(durationMs: Long) {
        mutex.withLock {
            nextAllowedAt = max(nextAllowedAt, System.currentTimeMillis() + durationMs)
        }
        delay(durationMs)
    }

    private fun Throwable.isRateLimited(): Boolean =
        this is ClientRequestException && response.status.value == 429 ||
            message?.contains("429") == true ||
            message?.contains("Too Many Requests", ignoreCase = true) == true
}
