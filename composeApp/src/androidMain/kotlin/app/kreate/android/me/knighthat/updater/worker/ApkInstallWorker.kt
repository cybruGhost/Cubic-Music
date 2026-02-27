package app.kreate.android.me.knighthat.updater.worker

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import app.kreate.android.R
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class ApkInstallWorker {

    companion object {
        private const val CHANNEL_ID = "apk_download_channel"
        private const val NOTIFICATION_ID = 1001
        private var progressUpdateJob: Job? = null
        private var currentDownloadId: Long = -1
        private var currentContext: Context? = null
        private var currentDownloadManager: DownloadManager? = null
        private var currentReceiver: BroadcastReceiver? = null
        private var pollingActive = false
        
        // Check if APK is already downloaded
// Check if APK is already downloaded
fun isApkDownloaded(fileName: String): Boolean {
    val downloadsDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "CubicMusic"
    )
    
    // If directory doesn't exist
    if (!downloadsDir.exists()) {
        return false
    }
    
    val apkFile = File(downloadsDir, fileName)
    
    // Check if file exists AND has reasonable size (not empty/corrupt)
    return apkFile.exists() && apkFile.length() > 1000 // At least 1KB
}
        // Add this function to ApkInstallWorker companion object
        fun reDownloadApk(
            context: Context,
            downloadUrl: String,
            fileName: String,
            onProgress: (Float) -> Unit = {},
            onComplete: () -> Unit = {},
            onError: (String) -> Unit = {}
        ) {
            // Cancel any existing download first
            cancelDownload()
            
            // Delete existing file
            deleteDownloadedApk(fileName)
            
            // Start fresh download
            startDownloadAndInstall(context, downloadUrl, fileName, onProgress, onComplete, onError)
        }
   // Delete downloaded APK manually (NOT cancel)
fun deleteDownloadedApk(fileName: String): Boolean {
    return try {
        val downloadsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "CubicMusic"
        )

        // If directory doesn't exist, nothing to delete
        if (!downloadsDir.exists()) return false

        val apkFile = File(downloadsDir, fileName)

        // Check if file exists before trying to delete
        if (!apkFile.exists()) return false

        val deleted = apkFile.delete()

        // Also clean up any other APK files in the directory
        downloadsDir.listFiles()?.forEach { file ->
            if (file.isFile && (file.name.endsWith(".apk") || file.name.contains("Cubic-Music"))) {
                if (file.name != fileName) { // Don't delete the one we just tried
                    file.delete()
                }
            }
        }

        // Remove notification if present
        currentContext?.let { context ->
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
            nm.cancel(NOTIFICATION_ID + 1)
        }

        deleted
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
        // Get downloaded APK file
        fun getDownloadedApkFile(fileName: String): File? {
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "CubicMusic"
            )
            
            if (!downloadsDir.exists()) {
                return null
            }
            
            val apkFile = File(downloadsDir, fileName)
            return if (apkFile.exists() && apkFile.length() > 0) apkFile else null
        }
        
        // Install already downloaded APK
        fun installDownloadedApk(context: Context, fileName: String): Boolean {
            return try {
                val apkFile = getDownloadedApkFile(fileName)
                if (apkFile != null) {
                    installApk(context, apkFile)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        
        fun startDownloadAndInstall(
            context: Context,
            downloadUrl: String,
            fileName: String,
            onProgress: (Float) -> Unit = {},
            onComplete: () -> Unit = {},
            onError: (String) -> Unit = {}
        ) {
            try {
                // Clean up any previous downloads
                cleanupPreviousDownload()
                
                // Store context for later cleanup
                currentContext = context
                
                // Create notification channel
                createNotificationChannel(context)
                
                // Start download using DownloadManager
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                currentDownloadManager = downloadManager
                
                // Create downloads directory path
                val downloadsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "CubicMusic"
                )
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                
                // Delete existing file if it exists
                val existingFile = File(downloadsDir, fileName)
                if (existingFile.exists()) {
                    existingFile.delete()
                }
                
                // Create request
                val request = DownloadManager.Request(Uri.parse(downloadUrl))
                    .setTitle("Cubic Music Update")
                    .setDescription("Downloading new version")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationUri(
                        Uri.fromFile(File(downloadsDir, fileName))
                    )
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                
                // Enqueue download
                val downloadId = downloadManager.enqueue(request)
                currentDownloadId = downloadId
                
                // Start polling for progress
                startProgressPolling(context, downloadManager, downloadId, onProgress, onComplete, onError)
                
                // Register receiver to handle download completion
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                        if (id == downloadId) {
                            handleDownloadCompletion(context, downloadManager, id, onComplete, onError)
                            unregisterReceiver(context)
                        }
                    }
                }
                
                currentReceiver = receiver
                
                // Register receiver with proper flags for Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(
                        receiver,
                        IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                        Context.RECEIVER_EXPORTED
                    )
                } else {
                    context.registerReceiver(
                        receiver,
                        IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                    )
                }
                
                // Show starting notification
                showDownloadStartedNotification(context)
                
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Unknown error")
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                cleanup()
            }
        }
        
        fun cancelDownload() {
            try {
                // Cancel progress polling
                progressUpdateJob?.cancel()
                progressUpdateJob = null
                pollingActive = false
                
                // Remove download from DownloadManager
                if (currentDownloadId != -1L && currentDownloadManager != null) {
                    currentDownloadManager?.remove(currentDownloadId)
                    
                    // Delete downloaded file if it exists
                    val downloadsDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "CubicMusic"
                    )
                    if (downloadsDir.exists()) {
                        downloadsDir.listFiles()?.forEach { file ->
                            if (file.isFile && (file.name.endsWith(".apk") || file.name.contains("Cubic-Music"))) {
                                file.delete()
                            }
                        }
                    }
                }
                
                // Cancel notification
                currentContext?.let { context ->
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(NOTIFICATION_ID)
                    notificationManager.cancel(NOTIFICATION_ID + 1)
                }
                
                // Unregister receiver
                unregisterReceiver(currentContext)
                
                // Clean up
                cleanup()
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        private fun handleDownloadCompletion(
            context: Context,
            downloadManager: DownloadManager,
            downloadId: Long,
            onComplete: () -> Unit,
            onError: (String) -> Unit
        ) {
            // Stop progress polling
            progressUpdateJob?.cancel()
            progressUpdateJob = null
            pollingActive = false
            
            // Check download status
            val query = DownloadManager.Query()
            query.setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            
            if (cursor.moveToFirst()) {
                @Suppress("DEPRECATION")
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        // Get downloaded file path
                        @Suppress("DEPRECATION")
                        val uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                        val fileUri = Uri.parse(uriString)
                        
                        // Show success notification
                        showInstallationCompleteNotification(context, fileUri)
                        
                        // Call completion callback - DON'T CLOSE APP!
                        onComplete()
                        
                        // Show toast that app is downloaded
                        Toast.makeText(
                            context, 
                            "Update downloaded! Click 'Install Now' to update.", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    DownloadManager.STATUS_FAILED -> {
                        @Suppress("DEPRECATION")
                        val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                        val errorMsg = when (reason) {
                            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume download"
                            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Device not found"
                            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
                            DownloadManager.ERROR_FILE_ERROR -> "File error"
                            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
                            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient space"
                            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
                            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP code"
                            else -> "Download failed"
                        }
                        onError(errorMsg)
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
            cursor.close()
            
            // Clean up
            cleanup()
        }
        
        private fun startProgressPolling(
            context: Context,
            downloadManager: DownloadManager,
            downloadId: Long,
            onProgress: (Float) -> Unit,
            onComplete: () -> Unit,
            onError: (String) -> Unit
        ) {
            progressUpdateJob?.cancel()
            pollingActive = true
            
            progressUpdateJob = CoroutineScope(Dispatchers.IO).launch {
                var lastProgress = -1
                
                while (pollingActive) {
                    try {
                        delay(500) // Poll every 500ms
                        
                        val query = DownloadManager.Query()
                        query.setFilterById(downloadId)
                        val cursor = downloadManager.query(query)
                        
                        if (cursor.moveToFirst()) {
                            @Suppress("DEPRECATION")
                            val bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            @Suppress("DEPRECATION")
                            val bytesTotal = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            
                            if (bytesTotal > 0) {
                                val progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                                val progressPercent = (progress * 100).toInt()
                                
                                // Only update if progress changed
                                if (progressPercent != lastProgress) {
                                    lastProgress = progressPercent
                                    
                                    // Update UI on main thread
                                    launch(Dispatchers.Main) {
                                        onProgress(progress)
                                    }
                                    
                                    // Update notification
                                    updateDownloadNotification(context, progressPercent)
                                }
                            }
                            
                            // Check if download is complete or failed
                            @Suppress("DEPRECATION")
                            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                            if (status == DownloadManager.STATUS_SUCCESSFUL || 
                                status == DownloadManager.STATUS_FAILED || 
                                status == DownloadManager.STATUS_PAUSED) {
                                cursor.close()
                                break
                            }
                        }
                        
                        cursor.close()
                    } catch (e: Exception) {
                        // Handle any errors during polling
                        e.printStackTrace()
                        break
                    }
                }
            }
        }
        
        private fun unregisterReceiver(context: Context?) {
            currentReceiver?.let { receiver ->
                context?.let {
                    try {
                        it.unregisterReceiver(receiver)
                    } catch (e: IllegalArgumentException) {
                        // Receiver was already unregistered
                    }
                }
                currentReceiver = null
            }
        }
        
        private fun cleanup() {
            progressUpdateJob = null
            pollingActive = false
            currentDownloadId = -1
            currentContext = null
            currentDownloadManager = null
            currentReceiver = null
        }
        
        private fun cleanupPreviousDownload() {
            cancelDownload()
        }
        
        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.apk_download_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.apk_download_channel_description)
                }
                
                val notificationManager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
        
        private fun showDownloadStartedNotification(context: Context) {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.downloading_update))
                .setContentText("0%")
                .setSmallIcon(R.drawable.download)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, 0, false)
            
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
        
        private fun updateDownloadNotification(context: Context, progress: Int) {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.downloading_update))
                .setContentText("$progress%")
                .setSmallIcon(R.drawable.download)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, progress, false)
            
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
        
        private fun showInstallationCompleteNotification(context: Context, fileUri: Uri) {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.download_complete))
                .setContentText(context.getString(R.string.tap_to_install))
                .setSmallIcon(R.drawable.install)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
            
            // Create intent to install APK
            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(fileUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                // Add flag to replace existing app
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
            }
            
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            
            builder.setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    installIntent,
                    flags
                )
            )
            
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID + 1, builder.build())
        }
        
        private fun installApk(context: Context, apkFile: File): Boolean {
            return try {
                val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        apkFile
                    )
                } else {
                    Uri.fromFile(apkFile)
                }
                
                val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    setDataAndType(fileUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    // Add flag to replace existing app
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                    putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
                }
                
                // Start installation
                context.startActivity(installIntent)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to install: ${e.message}", Toast.LENGTH_LONG).show()
                false
            }
        }
        
        private fun installApk(context: Context, fileUri: Uri) {
            try {
                // Convert content:// URI to file:// URI if needed
                val finalUri = if (fileUri.scheme == "content") {
                    // For Android 10+, we need to use FileProvider
                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CubicMusic")
                    val apkFile = file.listFiles()?.find { it.name.endsWith(".apk") }
                    apkFile?.let {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            it
                        )
                    } ?: fileUri
                } else {
                    fileUri
                }
                
                val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    setDataAndType(finalUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    // Add flag to replace existing app
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                    putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
                }
                
                // Start installation
                context.startActivity(installIntent)
                
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to install: ${e.message}", Toast.LENGTH_LONG).show()
                
                // Fallback: Show notification that user can tap
                showInstallationCompleteNotification(context, fileUri)
            }
        }
    }
}