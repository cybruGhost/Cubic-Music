package app.kreate.android.me.knighthat.updater

import android.os.Looper
import android.util.Xml
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
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.enums.CheckUpdateState
import app.it.fast4x.rimusic.ui.components.themed.SecondaryTextButton
import app.it.fast4x.rimusic.ui.screens.settings.EnumValueSelectorSettingsEntry
import app.it.fast4x.rimusic.ui.screens.settings.SettingsDescription
import app.it.fast4x.rimusic.utils.checkUpdateStateKey
import app.it.fast4x.rimusic.utils.checkBetaUpdatesKey
import app.it.fast4x.rimusic.utils.SecureApiConfig
import app.it.fast4x.rimusic.utils.updateCancelledKey
import app.it.fast4x.rimusic.utils.lastUpdateCheckKey
import app.it.fast4x.rimusic.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import androidx.core.text.HtmlCompat
import app.kreate.android.me.knighthat.utils.Repository
import app.kreate.android.me.knighthat.utils.Toaster
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.UnknownHostException
import java.net.URLEncoder
import java.nio.file.NoSuchFileException
import java.io.StringReader
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

object Updater {
    private const val TRANSIENT_FAILURE_RETRY_MS = 15L * 60L * 1_000L
    private const val RATE_LIMIT_RETRY_MS = 60L * 60L * 1_000L

    private val updaterScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val automaticCheckStarted = AtomicBoolean(false)
    private var automaticRetryJob: Job? = null

    private class UpdateHttpException(
        val statusCode: Int,
        message: String
    ) : IOException(message)

    private lateinit var tagName: String
    lateinit var build: GithubRelease.Build
    var githubRelease: GithubRelease? = null
    val latestVersionName: String
        get() = if (::tagName.isInitialized) tagName else githubRelease?.tagName.orEmpty()
    private val updateHttpClient by lazy {
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()
    }

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

    private fun directGithubFullBuild(createdAt: String = "1970-01-01T00:00:00Z"): GithubRelease.Build =
        GithubRelease.Build(
            id = 0u,
            url = SecureApiConfig.githubLatestFullApkUrl,
            name = "${BuildConfig.APP_NAME}-full.apk",
            size = 0u,
            createdAt = createdAt,
            downloadUrl = SecureApiConfig.githubLatestFullApkUrl
        )

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

    private data class UpdateCandidate(
        val release: GithubRelease,
        val build: GithubRelease.Build,
        val source: String
    )

    private fun applyUpdateCandidate(candidate: UpdateCandidate) {
        githubRelease = candidate.release
        build = candidate.build
        tagName = candidate.release.tagName
    }

    /**
     * Checks all available update sources and uses the newest valid release.
     * Order still gives Update Buddy the first network attempt, but a stale dashboard
     * cannot hide a newer GitHub release.
     */
    private suspend fun fetchUpdate(checkBetaUpdates: Boolean = false) = withContext(Dispatchers.IO) {
        val results = supervisorScope {
            listOf<suspend () -> UpdateCandidate>(
                { fetchGithubAtomUpdateCandidate(checkBetaUpdates) },
                { fetchUpdateBuddyReleaseCandidate(checkBetaUpdates, viaGithubFallback = false) },
                { fetchUpdateBuddyReleaseCandidate(checkBetaUpdates, viaGithubFallback = true) },
                { fetchGithubUpdateCandidate(checkBetaUpdates) }
            ).map { fetcher ->
                async { runCatching { fetcher() } }
            }.awaitAll()
        }

        val candidates = results.mapNotNull { it.getOrNull() }
        val errors = results.mapNotNull { it.exceptionOrNull() }
        val best = candidates
            .filter { candidate -> isVersionNewer(candidate.release.tagName, BuildConfig.VERSION_NAME) }
            .maxWithOrNull { left, right ->
                compareVersionStrings(left.release.tagName, right.release.tagName)
            }

        if (best == null) {
            val receivedValidResult =
                candidates.isNotEmpty() || errors.any { error -> error is NoSuchFileException }
            if (receivedValidResult) throw NoSuchFileException("")
            throw (errors.firstOrNull() ?: NoSuchFileException(""))
        }

        applyUpdateCandidate(best)
    }

    /**
     * The Atom feed is public and avoids GitHub's small unauthenticated REST quota.
     * This keeps startup checks working while the update service or REST API is down.
     */
    private suspend fun fetchGithubAtomUpdateCandidate(
        checkBetaUpdates: Boolean = false
    ): UpdateCandidate = withContext(Dispatchers.IO) {
        val body = executeUpdateRequest("${Repository.REPO_URL}/releases.atom")
            .orEmpty()
            .ifBlank { throw NoSuchFileException("") }
        val parser = Xml.newPullParser().apply {
            setInput(StringReader(body))
        }
        val releases = mutableListOf<GithubRelease>()
        var inEntry = false
        var entryId = ""
        var entryTitle = ""
        var entryContent = ""
        var entryUpdated = "1970-01-01T00:00:00Z"
        var entryLink = ""

        while (parser.eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> when (parser.name) {
                    "entry" -> {
                        inEntry = true
                        entryId = ""
                        entryTitle = ""
                        entryContent = ""
                        entryUpdated = "1970-01-01T00:00:00Z"
                        entryLink = ""
                    }
                    "id" -> if (inEntry) entryId = parser.nextText()
                    "title" -> if (inEntry) entryTitle = parser.nextText()
                    "content" -> if (inEntry) entryContent = parser.nextText()
                    "updated" -> if (inEntry) entryUpdated = parser.nextText()
                    "link" -> if (
                        inEntry &&
                        parser.getAttributeValue(null, "rel") == "alternate"
                    ) {
                        entryLink = parser.getAttributeValue(null, "href").orEmpty()
                    }
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> if (
                    parser.name == "entry" &&
                    inEntry
                ) {
                    inEntry = false
                    val tag = entryLink.substringAfterLast("/tag/", "")
                        .ifBlank { entryId.substringAfterLast('/') }
                    if (tag.isNotBlank()) {
                        val notes = HtmlCompat.fromHtml(
                            entryContent,
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                        ).toString().trim()
                        releases += GithubRelease(
                            id = 0u,
                            tagName = tag,
                            name = entryTitle.ifBlank { tag },
                            body = notes,
                            prerelease = extractVersionSuffix(tag) == "b",
                            builds = listOf(directGithubFullBuild(entryUpdated))
                        )
                    }
                }
            }
            parser.next()
        }

        val bestRelease = findBestRelease(releases, checkBetaUpdates)
            ?: throw NoSuchFileException("")
        UpdateCandidate(
            release = bestRelease,
            build = directGithubFullBuild(
                bestRelease.builds.firstOrNull()?.createdAt.orEmpty()
                    .ifBlank { "1970-01-01T00:00:00Z" }
            ),
            source = "github-atom"
        )
    }
    private suspend fun fetchGithubUpdate(checkBetaUpdates: Boolean = false) = withContext(Dispatchers.IO) {
        applyUpdateCandidate(fetchGithubUpdateCandidate(checkBetaUpdates))
    }

    private suspend fun fetchGithubUpdateCandidate(checkBetaUpdates: Boolean = false): UpdateCandidate = withContext(Dispatchers.IO) {
        assert(Looper.myLooper() != Looper.getMainLooper()) {
            "Cannot run fetch update on main thread"
        }

        val url = "${Repository.GITHUB_API}/repos/${Repository.REPO}/releases"
        val resBody = executeUpdateRequest(url)
        if (resBody.isNullOrBlank()) throw NoSuchFileException("")

        val json = Json { ignoreUnknownKeys = true }
        val releases = json.decodeFromString<List<GithubRelease>>(resBody)
        val bestRelease = findBestRelease(releases, checkBetaUpdates) ?: throw NoSuchFileException("")
        val bestBuild = runCatching { extractBuild(bestRelease.builds, checkBetaUpdates) }
            .getOrElse {
                if (extractBuildType(BuildConfig.VERSION_NAME) == "full") {
                    directGithubFullBuild(bestRelease.builds.firstOrNull()?.createdAt ?: "1970-01-01T00:00:00Z")
                } else {
                    throw it
                }
            }

        UpdateCandidate(bestRelease, bestBuild, "github")
    }

    private suspend fun fetchUpdateBuddyRelease(checkBetaUpdates: Boolean = false) = withContext(Dispatchers.IO) {
        applyUpdateCandidate(fetchUpdateBuddyReleaseCandidate(checkBetaUpdates, viaGithubFallback = false))
    }

    private suspend fun fetchUpdateBuddyReleaseCandidate(
        checkBetaUpdates: Boolean = false,
        viaGithubFallback: Boolean = false
    ): UpdateCandidate = withContext(Dispatchers.IO) {
        val channel = if (checkBetaUpdates || extractVersionSuffix(BuildConfig.VERSION_NAME) == "b") "beta" else "stable"
        val current = URLEncoder.encode(BuildConfig.VERSION_NAME, Charsets.UTF_8.name())
        val url = if (viaGithubFallback) {
            val repo = URLEncoder.encode(Repository.REPO, Charsets.UTF_8.name())
            "${SecureApiConfig.updateBuddyGithubReleaseEndpoint}?repo=$repo&current=$current"
        } else {
            "${SecureApiConfig.updateBuddyLatestReleaseEndpoint}?channel=$channel&current=$current"
        }

        val body = executeUpdateRequest(url, treat404AsNoFile = true).orEmpty()
        if (body.isBlank()) throw NoSuchFileException("")

        val root = JSONObject(body)
        if (!root.optBoolean("updateAvailable", false)) throw NoSuchFileException("")

        val latest = root.optJSONObject("latest") ?: throw NoSuchFileException("")
        val version = latest.optString("version").ifBlank { throw NoSuchFileException("") }
        val buildType = if (channel == "beta") "beta" else extractBuildType(BuildConfig.VERSION_NAME)
        val downloadUrl = latest.optString("downloadUrl")
            .takeIf { it.isNotBlank() }
            ?: if (buildType == "full") SecureApiConfig.githubLatestFullApkUrl else throw NoSuchFileException("")
        val createdAt = latest.optString("publishedAt").ifBlank { "1970-01-01T00:00:00Z" }
        val release = GithubRelease(
            id = 0u,
            tagName = version,
            name = latest.optString("name").ifBlank { version },
            body = latest.optString("notes"),
            prerelease = latest.optBoolean("isPrerelease", channel != "stable"),
            builds = listOf(
                GithubRelease.Build(
                    id = 0u,
                    url = root.optString("htmlUrl").ifBlank { downloadUrl },
                    name = "${BuildConfig.APP_NAME}-$buildType.apk",
                    size = 0u,
                    createdAt = createdAt,
                    downloadUrl = downloadUrl
                )
            )
        )

        UpdateCandidate(release, release.builds.first(), if (viaGithubFallback) "update-buddy-github" else "update-buddy")
    }

    private fun compareVersionStrings(version1: String, version2: String): Int {
        val v1Parts = version1.removePrefix("v").substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = version2.removePrefix("v").substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(v1Parts.size, v2Parts.size)
        for (index in 0 until maxLength) {
            val left = v1Parts.getOrNull(index) ?: 0
            val right = v2Parts.getOrNull(index) ?: 0
            if (left != right) return left.compareTo(right)
        }
        return 0
    }

    private fun executeUpdateRequest(url: String, treat404AsNoFile: Boolean = false): String? {
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            try {
                updateHttpClient.newCall(
                    Request.Builder()
                        .url(url)
                        .header("User-Agent", "${BuildConfig.APP_NAME}/${BuildConfig.VERSION_NAME}")
                        .header(
                            "Accept",
                            "application/json, application/atom+xml, application/xml, text/xml, */*"
                        )
                        .header("Cache-Control", "no-cache")
                        .build()
                ).execute().use { response ->
                    if (treat404AsNoFile && response.code == 404) throw NoSuchFileException("")
                    if (!response.isSuccessful) {
                        throw UpdateHttpException(
                            statusCode = response.code,
                            message = "Update service unavailable (${response.code})"
                        )
                    }
                    return response.body?.string()
                }
            } catch (error: IOException) {
                lastError = error
                if (attempt == 1 || !error.message.orEmpty().contains("stream", ignoreCase = true)) throw error
            }
        }
        throw lastError ?: IOException("Update check failed")
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
        
        if (eligibleReleases.isEmpty()) return null

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
    ): Job = updaterScope.launch {
        if (!BuildConfig.IS_AUTOUPDATE) return@launch

        // The app shell can be composed more than once. Run exactly one automatic
        // check per process, while keeping every explicit manual check available.
        if (!isForced && !automaticCheckStarted.compareAndSet(false, true)) return@launch

        val sharedPrefs = appContext().getSharedPreferences("settings", 0)
        val now = System.currentTimeMillis()

        try {
            fetchUpdate(checkBetaUpdates)

            val hasUpdate = ::tagName.isInitialized &&
                isVersionNewer(tagName, BuildConfig.VERSION_NAME)
            withContext(Dispatchers.Main.immediate) {
                NewUpdateAvailableDialog.isActive = hasUpdate
                NewUpdateAvailableDialog.isCancelled = false
                if (!hasUpdate && isForced) {
                    Toaster.i(R.string.info_no_update_available)
                }
            }

            if (!hasUpdate) {
                sharedPrefs.edit().putBoolean(updateCancelledKey, false).apply()
            }
            sharedPrefs.edit().putLong(lastUpdateCheckKey, now).apply()
            automaticRetryJob?.cancel()
            automaticRetryJob = null
        } catch (error: NoSuchFileException) {
            withContext(Dispatchers.Main.immediate) {
                NewUpdateAvailableDialog.isActive = false
                NewUpdateAvailableDialog.isCancelled = false
                if (isForced) Toaster.i(R.string.info_no_update_available)
            }
            sharedPrefs.edit()
                .putLong(lastUpdateCheckKey, now)
                .putBoolean(updateCancelledKey, false)
                .apply()
            automaticRetryJob?.cancel()
            automaticRetryJob = null
        } catch (error: Exception) {
            val isRateLimited =
                error is UpdateHttpException && error.statusCode in setOf(403, 429)
            val retryDelay =
                if (isRateLimited) RATE_LIMIT_RETRY_MS else TRANSIENT_FAILURE_RETRY_MS

            if (isForced) {
                val message = when (error) {
                    is UnknownHostException -> appContext().getString(R.string.error_no_internet)
                    else -> appContext().getString(R.string.update_failed_message)
                }
                withContext(Dispatchers.Main.immediate) { Toaster.e(message) }
            } else {
                automaticRetryJob?.cancel()
                automaticRetryJob = updaterScope.launch {
                    delay(retryDelay)
                    automaticCheckStarted.set(false)
                    checkForUpdate(checkBetaUpdates = checkBetaUpdates)
                }
            }
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
