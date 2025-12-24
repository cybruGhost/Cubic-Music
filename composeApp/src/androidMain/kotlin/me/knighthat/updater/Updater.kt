package me.knighthat.updater

import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import app.kreate.android.BuildConfig
import app.kreate.android.R
import it.fast4x.rimusic.appContext
import it.fast4x.rimusic.enums.CheckUpdateState
import it.fast4x.rimusic.ui.components.themed.SecondaryTextButton
import it.fast4x.rimusic.ui.screens.settings.EnumValueSelectorSettingsEntry
import it.fast4x.rimusic.ui.screens.settings.SettingsDescription
import it.fast4x.rimusic.utils.checkUpdateStateKey
import it.fast4x.rimusic.utils.checkBetaUpdatesKey
import it.fast4x.rimusic.utils.updateCancelledKey
import it.fast4x.rimusic.utils.lastUpdateCheckKey
import it.fast4x.rimusic.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.knighthat.utils.Repository
import me.knighthat.utils.Toaster
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.UnknownHostException
import java.nio.file.NoSuchFileException
import kotlin.math.pow

object Updater {
    private lateinit var tagName: String
    lateinit var build: GithubRelease.Build
    var githubRelease: GithubRelease? = null

    /**
     * Extracts the build type from version string
     * e.g., "1.0.0-f" returns "full", "1.0.0-b" returns "beta"
     */
    private fun extractBuildType(versionStr: String): String {
        return when {
            versionStr.endsWith("-f") -> "full"
            versionStr.endsWith("-b") -> "beta"
            versionStr.endsWith("-m") -> "minified"
            else -> "full" // Default to full if no suffix
        }
    }

    /**
     * Extracts the version suffix from version string
     * e.g., "1.0.0-f" returns "f", "1.0.0-b" returns "b"
     */
    private fun extractVersionSuffix(versionStr: String): String {
        val parts = versionStr.removePrefix("v").split("-")
        return if (parts.size > 1) parts[1] else ""
    }

    private fun extractBuild(assets: List<GithubRelease.Build>, checkBetaUpdates: Boolean = false): GithubRelease.Build {
        val appName = BuildConfig.APP_NAME
        val currentBuildType = extractBuildType(BuildConfig.VERSION_NAME)
        val currentSuffix = extractVersionSuffix(BuildConfig.VERSION_NAME)

        // Determine which build types to look for based on current build and beta preferences
        val targetBuildTypes = when {
            // Full users with beta enabled: check beta and full
            currentSuffix == "f" && checkBetaUpdates -> listOf("beta", "full")
            // Full users with beta disabled: check only full
            currentSuffix == "f" && !checkBetaUpdates -> listOf("full")
            // Beta users with beta enabled: check beta and full
            currentSuffix == "b" && checkBetaUpdates -> listOf("beta", "full")
            // Beta users with beta disabled: check only full
            currentSuffix == "b" && !checkBetaUpdates -> listOf("full")
            // Minified users: check only minified
            currentSuffix == "m" -> listOf("minified")
            // Default: check only full
            else -> listOf("full")
        }

        // Try to find the best matching build
        for (buildType in targetBuildTypes) {
            val fileName = "$appName-$buildType.apk"
            val foundBuild = assets.fastFirstOrNull { it.name == fileName }
            if (foundBuild != null) {
                return foundBuild
            }
        }

        // Fallback to the original logic
        val fileName = "$appName-$currentBuildType.apk"
        val fallbackBuild = assets.fastFirstOrNull {
            it.name == fileName
        }
        
        if (fallbackBuild != null) {
            return fallbackBuild
        } else {
            throw NoSuchFileException("")
        }
    }

    /**
     * Compares two version strings and returns true if version1 is newer than version2
     */
    private fun isVersionNewer(version1: String, version2: String): Boolean {
        val v1 = version1.removePrefix("v").substringBefore("-")
        val v2 = version2.removePrefix("v").substringBefore("-")
        
        val v1Parts = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = v2.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLength = maxOf(v1Parts.size, v2Parts.size)
        
        for (i in 0 until maxLength) {
            val v1Part = v1Parts.getOrNull(i) ?: 0
            val v2Part = v2Parts.getOrNull(i) ?: 0
            
            when {
                v1Part > v2Part -> return true
                v1Part < v2Part -> return false
            }
        }
        
        return false // Versions are equal
    }

    /**
     * Turns `v1.0.0` to `1.0.0`, `1.0.0-m` to `1.0.0`
     */
    private fun trimVersion(versionStr: String): String {
        return versionStr.removePrefix("v").substringBefore("-")
    }

    /**
     * Sends out requests to Github for latest version.
     *
     * Results are downloaded, filtered, and saved to [build]
     *
     * > **NOTE**: This is a blocking process, it should never run on UI thread
     */
    private suspend fun fetchUpdate(checkBetaUpdates: Boolean = false) = withContext(Dispatchers.IO) {
        assert(Looper.myLooper() != Looper.getMainLooper()) {
            "Cannot run fetch update on main thread"
        }

        // Get all releases to find the best one
        val url = "${Repository.GITHUB_API}/repos/${Repository.REPO}/releases"
        val request = Request.Builder().url(url).build()
        val response = OkHttpClient().newCall(request).execute()

        if (!response.isSuccessful) {
            Toaster.e(response.message)
            return@withContext
        }

        val resBody = response.body?.string()
        if (resBody.isNullOrBlank()) {
            Toaster.i(R.string.info_no_update_available)
            return@withContext
        }

        val json = Json {
            ignoreUnknownKeys = true
        }
        val releases = json.decodeFromString<List<GithubRelease>>(resBody)
        
        // Find the best release based on current version and beta preferences
        val bestRelease = findBestRelease(releases, checkBetaUpdates)
        
        if (bestRelease != null) {
            this@Updater.githubRelease = bestRelease
            build = extractBuild(bestRelease.builds, checkBetaUpdates)
            tagName = bestRelease.tagName
        } else {
            throw NoSuchFileException("")
        }
    }

    /**
     * Finds the best release based on current version and beta preferences
     */
    private fun findBestRelease(releases: List<GithubRelease>, checkBetaUpdates: Boolean): GithubRelease? {
        val currentVersion = BuildConfig.VERSION_NAME
        val currentSuffix = extractVersionSuffix(currentVersion)
        
        // Filter releases based on current build type and beta preferences
        val eligibleReleases = releases.filter { release ->
            val releaseSuffix = extractVersionSuffix(release.tagName)
            
            when {
                // Full users with beta enabled: accept both beta and full
                currentSuffix == "f" && checkBetaUpdates -> releaseSuffix == "" || releaseSuffix == "b"
                // Full users with beta disabled: only accept full
                currentSuffix == "f" && !checkBetaUpdates -> releaseSuffix == ""
                // Beta users with beta enabled: accept both beta and full
                currentSuffix == "b" && checkBetaUpdates -> releaseSuffix == "b" || releaseSuffix == ""
                // Beta users with beta disabled: only accept full
                currentSuffix == "b" && !checkBetaUpdates -> releaseSuffix == ""
                // Minified users: only accept minified
                currentSuffix == "m" -> releaseSuffix == ""
                // Default case: only accept full releases
                else -> releaseSuffix == ""
            }
        }
        
        // Find the release with the highest version number
        // Find the maximum number of version parts to normalize all versions
        val maxParts = eligibleReleases.maxOf { release ->
            val version = release.tagName.removePrefix("v").substringBefore("-")
            version.split(".").size
        }
        
        val bestRelease = eligibleReleases.maxByOrNull { release ->
            val version = release.tagName.removePrefix("v").substringBefore("-")
            val parts = version.split(".").map { it.toIntOrNull() ?: 0 }
            
            // Pad the parts array to have the same length as maxParts
            val normalizedParts = parts.toMutableList()
            while (normalizedParts.size < maxParts) {
                normalizedParts.add(0)
            }
            
            // Create a comparable version number (e.g., 1.2.3 -> 1002003)
            normalizedParts.foldIndexed(0L) { index, acc, part ->
                acc + (part * (1000.0.pow(maxParts - 1 - index)).toLong())
            }
        }
        
        return bestRelease
    }

    fun checkForUpdate(
        isForced: Boolean = false,
        checkBetaUpdates: Boolean = false
    ) = CoroutineScope(Dispatchers.IO).launch {
        // Update the last check timestamp at the beginning
        val sharedPrefs = appContext().getSharedPreferences("settings", 0)
        sharedPrefs.edit()
            .putLong(lastUpdateCheckKey, System.currentTimeMillis())
            .apply()
            
        if (!BuildConfig.IS_AUTOUPDATE || NewUpdateAvailableDialog.isCancelled) return@launch

        try {
            if (!::build.isInitialized || isForced) {
                fetchUpdate(checkBetaUpdates)
            }

            // Check if the new version is actually newer
            val hasUpdate = if (::tagName.isInitialized) {
                isVersionNewer(tagName, BuildConfig.VERSION_NAME)
            } else {
                false
            }
            NewUpdateAvailableDialog.isActive = hasUpdate
            
            if (!NewUpdateAvailableDialog.isActive) {
                if(isForced) {
                    Toaster.i(R.string.info_no_update_available)
                }
                NewUpdateAvailableDialog.isCancelled = true
                // Also reset the cancelled state in SharedPreferences when no update is available
                sharedPrefs.edit()
                    .putBoolean(updateCancelledKey, false)
                    .apply()
            } else {
                // If there's an update available, reset the cancelled state
                NewUpdateAvailableDialog.isCancelled = false
            }
        } catch (e: Exception) {
            val message = when (e) {
                is UnknownHostException -> appContext().getString(R.string.error_no_internet)
                is NoSuchFileException -> appContext().getString(R.string.info_no_update_available)
                else -> e.message ?: appContext().getString(R.string.error_unknown)
            }
            
            // Use appropriate toast type based on exception
            when (e) {
                is NoSuchFileException -> Toaster.i(message) // Blue for no update available
                else -> Toaster.e(message) // Red for other errors
            }

            NewUpdateAvailableDialog.isCancelled = true
        }
    }

    @Composable
    fun SettingEntry() {
        var checkUpdateState by rememberPreference(checkUpdateStateKey, CheckUpdateState.Enabled)
        var checkBetaUpdates by rememberPreference(checkBetaUpdatesKey, extractVersionSuffix(BuildConfig.VERSION_NAME) == "b")
        if (!BuildConfig.IS_AUTOUPDATE)
            checkUpdateState = CheckUpdateState.Disabled

        Row(Modifier.fillMaxWidth()) {
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.enable_check_for_update),
                selectedValue = checkUpdateState,
                onValueSelected = { checkUpdateState = it },
                valueText = { it.text },
                isEnabled = BuildConfig.IS_AUTOUPDATE,
                modifier = Modifier.weight(1f)
            )

            AnimatedVisibility(
                visible = checkUpdateState != CheckUpdateState.Disabled && BuildConfig.IS_AUTOUPDATE,
                // Slide in from right + fade in effect.
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(initialAlpha = 0f),
                // Slide out from left + fade out effect.
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(targetAlpha = 0f)
            ) {
                SecondaryTextButton(
                    text = stringResource(R.string.info_check_update_now),
                    onClick = { checkForUpdate(true, checkBetaUpdates) },
                    modifier = Modifier.padding(end = 24.dp)
                )
            }
        }

        SettingsDescription(
            stringResource(
                if (BuildConfig.IS_AUTOUPDATE)
                    R.string.when_enabled_a_new_version_is_checked_and_notified_during_startup
                else
                    R.string.description_app_not_installed_by_apk
            )
        )
    }
}