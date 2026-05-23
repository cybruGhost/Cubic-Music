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
        private const val MIN_VALID_APK_SIZE_BYTES = 32 * 1024L
        private var progressUpdateJob: Job? = null
        private var currentDownloadId: Long = -1
        private var currentContext: Context? = null
        private var currentDownloadManager: DownloadManager? = null
        private var currentReceiver: BroadcastReceiver? = null
        private var currentTargetFile: File? = null
        private var pollingActive = false

        private fun getDownloadsDir(): File = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "CubicMusic"
        )

        private fun getTargetFile(fileName: String): File = File(getDownloadsDir(), fileName)

        private fun displayPath(file: File): String = file.absolutePath

        fun getDownloadFolderPath(): String = displayPath(getDownloadsDir())

        fun hasDownloadedArtifacts(): Boolean =
            getDownloadsDir()
                .takeIf(File::exists)
                ?.listFiles()
                ?.any { file ->
                    file.isFile && (
                        file.name.endsWith(".apk", ignoreCase = true) ||
                            file.name.endsWith(".apk.bak", ignoreCase = true)
                        )
                } == true

        private fun isValidApkFile(file: File?): Boolean =
            file?.exists() == true && file.length() >= MIN_VALID_APK_SIZE_BYTES

        private fun cleanupStaleApkFiles(fileName: String) {
            val downloadsDir = getDownloadsDir()
            if (!downloadsDir.exists()) return

            downloadsDir.listFiles()?.forEach { file ->
                if (!file.isFile) return@forEach
                val isTarget = file.name == fileName
                val isStaleApk = file.name.endsWith(".apk", ignoreCase = true) && !isTarget
                val isBackupApk = file.name.endsWith(".apk.bak", ignoreCase = true)
                val isPartial = file.name.endsWith(".part", ignoreCase = true)
                    || file.name.endsWith(".partial", ignoreCase = true)
                    || file.name.endsWith(".tmp", ignoreCase = true)
                val isBrokenTarget = isTarget && file.length() < MIN_VALID_APK_SIZE_BYTES
                if (isStaleApk || isBackupApk || isPartial || isBrokenTarget) {
                    file.delete()
                }
            }
        }

        fun isApkDownloaded(fileName: String): Boolean = isValidApkFile(getDownloadedApkFile(fileName))
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
        val downloadsDir = getDownloadsDir()

        // If directory doesn't exist, nothing to delete
        if (!downloadsDir.exists()) return false

        val apkFile = getTargetFile(fileName)
        val backupFile = File(downloadsDir, "$fileName.bak")
        val deletedMain = if (apkFile.exists()) apkFile.delete() else false
        val deletedBackup = if (backupFile.exists()) backupFile.delete() else false

        cleanupStaleApkFiles(fileName)

        // Remove notification if present
        currentContext?.let { context ->
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
            nm.cancel(NOTIFICATION_ID + 1)
        }

        if (currentTargetFile?.name == fileName) {
            currentTargetFile = null
        }

        deletedMain || deletedBackup || !hasDownloadedArtifacts()
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
        // Get downloaded APK file
        fun getDownloadedApkFile(fileName: String): File? {
            val downloadsDir = getDownloadsDir()
            
            if (!downloadsDir.exists()) {
                return null
            }
            
            val apkFile = getTargetFile(fileName)
            return apkFile.takeIf(::isValidApkFile)
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
                val downloadsDir = getDownloadsDir()
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                cleanupStaleApkFiles(fileName)

                val targetFile = getTargetFile(fileName)
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                currentTargetFile = targetFile
                
                // Create request
                val request = DownloadManager.Request(Uri.parse(downloadUrl))
                    .setTitle(context.getString(R.string.apk_update_title))
                    .setDescription(
                        context.getString(
                            R.string.apk_download_started_message,
                            displayPath(targetFile)
                        )
                    )
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationUri(Uri.fromFile(targetFile))
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
                showDownloadStartedNotification(context, targetFile)
                
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMessage = context.getString(
                    R.string.apk_download_failed_with_reason,
                    e.message ?: context.getString(R.string.apk_unknown_error)
                )
                onError(errorMessage)
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
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
                }

                currentTargetFile?.delete()
                
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
                        val downloadedFile = currentTargetFile

                        if (!isValidApkFile(downloadedFile)) {
                            downloadedFile?.delete()
                            onError(context.getString(R.string.apk_download_invalid_file))
                            Toast.makeText(context, context.getString(R.string.apk_download_invalid_file), Toast.LENGTH_LONG).show()
                            cleanup()
                            cursor.close()
                            return
                        }

                        // Show success notification
                        showInstallationCompleteNotification(context, fileUri, downloadedFile)
                        
                        // Call completion callback - DON'T CLOSE APP!
                        onComplete()
                        
                        // Show toast that app is downloaded
                        Toast.makeText(
                            context, 
                            context.getString(
                                R.string.apk_download_ready_message,
                                displayPath(downloadedFile ?: currentTargetFile ?: getDownloadsDir())
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    DownloadManager.STATUS_FAILED -> {
                        @Suppress("DEPRECATION")
                        val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                        val errorMsg = when (reason) {
                            DownloadManager.ERROR_CANNOT_RESUME -> context.getString(R.string.apk_download_error_cannot_resume)
                            DownloadManager.ERROR_DEVICE_NOT_FOUND -> context.getString(R.string.apk_download_error_device_not_found)
                            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> context.getString(R.string.apk_download_error_file_exists)
                            DownloadManager.ERROR_FILE_ERROR -> context.getString(R.string.apk_download_error_file)
                            DownloadManager.ERROR_HTTP_DATA_ERROR -> context.getString(R.string.apk_download_error_http_data)
                            DownloadManager.ERROR_INSUFFICIENT_SPACE -> context.getString(R.string.apk_download_error_insufficient_space)
                            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> context.getString(R.string.apk_download_error_too_many_redirects)
                            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> context.getString(R.string.apk_download_error_unhandled_http_code)
                            else -> context.getString(R.string.apk_download_failed)
                        }
                        currentTargetFile?.delete()
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
                                status == DownloadManager.STATUS_FAILED) {
                                cursor.close()
                                break
                            } else if (status == DownloadManager.STATUS_PAUSED) {
                                currentTargetFile?.delete()
                                launch(Dispatchers.Main) {
                                    onError(context.getString(R.string.apk_download_error_paused))
                                }
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
            currentTargetFile = null
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
        
        private fun showDownloadStartedNotification(context: Context, targetFile: File) {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.downloading_update))
                .setContentText(
                    context.getString(
                        R.string.apk_download_progress_message,
                        0,
                        displayPath(targetFile)
                    )
                )
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
                .setContentText(
                    context.getString(
                        R.string.apk_download_progress_message,
                        progress,
                        displayPath(currentTargetFile ?: getDownloadsDir())
                    )
                )
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
        
        private fun showInstallationCompleteNotification(context: Context, fileUri: Uri, file: File?) {
            val safeUri = when {
                file != null && file.exists() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ->
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                file != null && file.exists() -> Uri.fromFile(file)
                else -> fileUri
            }
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.download_complete))
                .setContentText(
                    context.getString(
                        R.string.apk_download_ready_message,
                        displayPath(file ?: getDownloadsDir())
                    )
                )
                .setSmallIcon(R.drawable.install)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
            
            // Create intent to install APK
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(safeUri, "application/vnd.android.package-archive")
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
                
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
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
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.apk_install_failed_with_reason,
                        e.message ?: context.getString(R.string.apk_download_failed)
                    ),
                    Toast.LENGTH_LONG
                ).show()
                false
            }
        }
        
        private fun installApk(context: Context, fileUri: Uri) {
            try {
                // Convert content:// URI to file:// URI if needed
                val finalUri = if (fileUri.scheme == "content") {
                    // For Android 10+, we need to use FileProvider
                    val file = getDownloadsDir()
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
                
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
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
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.apk_install_failed_with_reason,
                        e.message ?: context.getString(R.string.apk_download_failed)
                    ),
                    Toast.LENGTH_LONG
                ).show()
                
                // Fallback: Show notification that user can tap
                showInstallationCompleteNotification(context, fileUri, currentTargetFile)
            }
        }
    }
}
