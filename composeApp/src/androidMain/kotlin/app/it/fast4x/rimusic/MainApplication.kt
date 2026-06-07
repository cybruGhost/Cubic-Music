package app.it.fast4x.rimusic

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.content.Context
import coil3.SingletonImageLoader
import coil3.ImageLoader

import app.kreate.android.R
import app.cubic.android.core.network.NetworkClientFactory
import app.cubic.android.core.network.Store
import app.cubic.android.core.utils.cipher.CipherDeobfuscator
import app.cubic.android.core.utils.potoken.PoTokenGenerator
import app.it.fast4x.rimusic.notifications.AppAnnouncementNotifier
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.utils.CaptureCrash
import app.it.fast4x.rimusic.utils.FileLoggingTree
import app.it.fast4x.rimusic.utils.logDebugEnabledKey
import app.it.fast4x.rimusic.utils.preferences
import app.it.fast4x.rimusic.utils.ytCookieKey
import app.it.fast4x.rimusic.utils.ytDataSyncIdKey
import app.it.fast4x.rimusic.utils.ytVisitorDataKey
import it.fast4x.innertube.Innertube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import kotlin.random.Random

class MainApplication : Application(), SingletonImageLoader.Factory {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        //DatabaseInitializer()
        Dependencies.init(this)
        CipherDeobfuscator.initialize(this)
        NetworkClientFactory.configure(proxy = null, cacheDir = cacheDir)
        initializeYouTubeSession()

        createNotificationChannels()

        /**** LOG *********/
        val logEnabled = preferences.getBoolean(logDebugEnabledKey, false)
        
        // Always create logs directory and set up crash handler
        val dir = filesDir.resolve("logs").also {
            if (it.exists()) return@also
            it.mkdir()
        }
        val runtimeLogFile = File(dir, "Cubic-Music_log.txt").also {
            if (!it.exists()) {
                it.createNewFile()
            }
        }
        
        // Always set up crash handler regardless of debug mode
        Thread.setDefaultUncaughtExceptionHandler(CaptureCrash(dir.absolutePath))
        
        if (logEnabled) {
            Timber.uprootAll()
            Timber.plant(Timber.DebugTree())
            Timber.plant(FileLoggingTree(runtimeLogFile))
            Timber.d("Debug log enabled at ${runtimeLogFile.absolutePath}")
        } else {
            Timber.uprootAll()
            Timber.plant(Timber.DebugTree())
        }
        /**** LOG *********/

        AppAnnouncementNotifier.maybeShow(this)
        AppAnnouncementNotifier.scheduleBackgroundChecks(this)
        prewarmPoToken()
    }

    private fun prewarmPoToken() {
        applicationScope.launch {
            delay(Random.nextLong(3_000L, 5_001L))
            runCatching {
                withContext(Dispatchers.IO) {
                    val sessionId = Store.getIosVisitorData()
                        .ifBlank { Innertube.visitorData.ifBlank { Innertube.DEFAULT_VISITOR_DATA } }
                    PoTokenGenerator().getWebClientPoToken("", sessionId)
                }
            }.onSuccess {
                Timber.d("PoToken pre-warmed")
            }.onFailure {
                Timber.w(it, "PoToken pre-warm skipped; on-demand generation will be used")
            }
        }
    }

    private fun initializeYouTubeSession() {
        val savedCookie = preferences.getString(ytCookieKey, "").orEmpty()
        if (savedCookie.isBlank()) return

        Innertube.cookie = savedCookie
        Innertube.visitorData = preferences.getString(ytVisitorDataKey, "").orEmpty()
            .ifBlank { Innertube.DEFAULT_VISITOR_DATA }
        Innertube.dataSyncId = preferences.getString(ytDataSyncIdKey, "").orEmpty()
            .ifBlank { null }
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

            val announcementChannel = NotificationChannel(
                AppAnnouncementNotifier.CHANNEL_ID,
                applicationContext.getString(R.string.app_announcements_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(R.string.app_announcements_channel_description)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(playerChannel, sleepTimerChannel, downloadChannel, announcementChannel)
            )
        }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return if (Dependencies.isInitialized) {
            app.kreate.android.me.knighthat.coil.ImageCacheFactory.LOADER
        } else {
            ImageLoader.Builder(context).build()
        }
    }



}

object Dependencies {
    lateinit var application: MainApplication
        private set

    val isInitialized: Boolean
        get() = ::application.isInitialized

    internal fun init(application: MainApplication) {

        this.application = application
    }
}
