package it.fast4x.rimusic

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader

import app.kreate.android.R
import it.fast4x.rimusic.service.modern.PlayerServiceModern
import it.fast4x.rimusic.service.MyDownloadHelper
import it.fast4x.rimusic.utils.CaptureCrash
import it.fast4x.rimusic.utils.FileLoggingTree
import it.fast4x.rimusic.utils.logDebugEnabledKey
import it.fast4x.rimusic.utils.preferences
import timber.log.Timber
import java.io.File

class MainApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        //DatabaseInitializer()
        Dependencies.init(this)

        createNotificationChannels()

        /**** LOG *********/
        val logEnabled = preferences.getBoolean(logDebugEnabledKey, false)
        
        // Always create logs directory and set up crash handler
        val dir = filesDir.resolve("logs").also {
            if (it.exists()) return@also
            it.mkdir()
        }
        
        // Always set up crash handler regardless of debug mode
        Thread.setDefaultUncaughtExceptionHandler(CaptureCrash(dir.absolutePath))
        
        if (logEnabled) {
            Timber.plant(FileLoggingTree(File(dir, "N-Zik_log.txt")))
            Timber.d("Log enabled at ${dir.absolutePath}")
        } else {
            Timber.uprootAll()
            Timber.plant(Timber.DebugTree())
        }
        /**** LOG *********/
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Channel for music player
            val playerChannel = NotificationChannel(
                PlayerServiceModern.NotificationChannelId,
                applicationContext.getString(R.string.player),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = applicationContext.getString(R.string.player)
                setShowBadge(false)
            }

            // Channel for sleep timer
            val sleepTimerChannel = NotificationChannel(
                PlayerServiceModern.SleepTimerNotificationChannelId,
                applicationContext.getString(R.string.sleep_timer),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(R.string.sleep_timer)
                setShowBadge(false)
            }

            // Channel for downloads
            val downloadChannel = NotificationChannel(
                MyDownloadHelper.DOWNLOAD_NOTIFICATION_CHANNEL_ID,
                applicationContext.getString(R.string.download),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = applicationContext.getString(R.string.download)
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(listOf(playerChannel, sleepTimerChannel, downloadChannel))
        }
    }

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context).build()

}

object Dependencies {
    lateinit var application: MainApplication
        private set

    internal fun init(application: MainApplication) {
        this.application = application
    }
}