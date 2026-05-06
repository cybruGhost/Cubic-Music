package app.it.fast4x.rimusic.service

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import app.kreate.android.R
import app.it.fast4x.rimusic.service.MyDownloadHelper.DOWNLOAD_NOTIFICATION_CHANNEL_ID

private const val JOB_ID = 8888
private const val FOREGROUND_NOTIFICATION_ID = 8989

@UnstableApi
class MyDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.download, 0
) {

    override fun getDownloadManager(): DownloadManager {

        // This will only happen once, because getDownloadManager is guaranteed to be called only once
        // in the life cycle of the process.
        val downloadManager: DownloadManager = MyDownloadHelper.getDownloadManager(this)
        return downloadManager
    }

    override fun getScheduler(): PlatformScheduler? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) PlatformScheduler(this, JOB_ID) else null
    }

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ) = NotificationCompat
        .Builder(
            /* context = */ this,
            /* notification = */ MyDownloadHelper
                .getDownloadNotificationHelper(this)
                .buildProgressNotification(
                /* context            = */ this,
                /* smallIcon          = */ R.drawable.download_progress,
                /* contentIntent      = */ null,
                /* message            = */ "${downloads.size} in progress",
                /* downloads          = */ downloads,
                /* notMetRequirements = */ notMetRequirements
            )
        )
        .setChannelId(DOWNLOAD_NOTIFICATION_CHANNEL_ID)
        /*
        // Add action in notification
        .addAction(
            NotificationCompat.Action.Builder(
                /* icon = */ R.drawable.close,
                /* title = */ getString(R.string.cancel),
                /* intent = */ null //TODO notificationActionReceiver.cancel.pendingIntent
            ).build()
        )
        */
        .build()
}
