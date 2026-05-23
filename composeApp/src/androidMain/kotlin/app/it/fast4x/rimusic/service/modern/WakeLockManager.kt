package app.it.fast4x.rimusic.service.modern

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import timber.log.Timber

class WakeLockManager(
    context: Context,
    private val tag: String,
    private val maxDurationMs: Long = 30_000L,
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null
    private var acquiredAtMs = 0L

    fun acquire() {
        try {
            if (wakeLock != null) {
                try {
                    if (wakeLock!!.isHeld) return
                } catch (_: Exception) {
                    wakeLock = null
                }
            }
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    tag
                ).apply { setReferenceCounted(false) }
            }
            val lock = wakeLock ?: return
            if (!lock.isHeld) {
                acquiredAtMs = SystemClock.elapsedRealtime()
                lock.acquire(maxDurationMs)
                Timber.d("WakeLockManager acquired")
            }
        } catch (e: Exception) {
            Timber.e(e, "WakeLockManager acquire failed")
            wakeLock = null
        }
    }

    fun release() {
        try {
            val lock = wakeLock
            if (lock != null) {
                try {
                    if (lock.isHeld) {
                        runCatching { lock.release() }
                            .onFailure { Timber.e(it, "WakeLockManager release failed") }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "WakeLockManager release check failed")
                }
            }
        } finally {
            wakeLock = null
            acquiredAtMs = 0L
        }
    }

    fun releaseIfHeldTooLong() {
        val lock = wakeLock ?: return
        if (!lock.isHeld) return
        val elapsed = SystemClock.elapsedRealtime() - acquiredAtMs
        if (elapsed >= maxDurationMs) {
            Timber.w("WakeLockManager releasing after %sms", elapsed)
            release()
        }
    }
}
