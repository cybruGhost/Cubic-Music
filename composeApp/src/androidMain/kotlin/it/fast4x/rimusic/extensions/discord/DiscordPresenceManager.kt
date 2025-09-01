package it.fast4x.rimusic.extensions.discord

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import app.kreate.android.R
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets
import com.my.kizzyrpc.model.Timestamps
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import it.fast4x.innertube.Innertube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonArray
import me.knighthat.utils.ImageProcessor
import me.knighthat.utils.Toaster
import me.knighthat.utils.isNetworkAvailable
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.annotations.Contract
import timber.log.Timber

/**
     * THIS IS STILL IN BETA AND MAY NOT WORK AS EXPECTED AND CAUSE CRASH
     * Call this method when the playing state changes.
     * - isPlaying = true : send the "playing" presence and refresh it every 10s
     * - isPlaying = false : launch a timer, then send the "paused" presence (frozen time)
     */


class DiscordPresenceManager(
    private val context: Context,
    private val getToken: () -> String?,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    companion object {

        private const val APPLICATION_ID = "1379051016007454760"
        private const val TEMP_FILE_HOST = "https://litterbox.catbox.moe/resources/internals/api.php"
        private const val MAX_DIMENSION = 1024                           // Per Discord's guidelines
        private const val MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024     // 2 MB in bytes
    }

    private var rpc: KizzyRPC? = null
    private var lastToken: String? = null
    private var lastMediaItem: MediaItem? = null
    private var lastPosition: Long = 0L
    private var isStopped = false
    private val discordScope = externalScope
    private var refreshJob: Job? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    @Volatile
    private var smallImage: String? = null
    @Volatile
    private var largeImage: String? = null

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun uploadArtwork( artworkUri: Uri? ): Result<Uri> =
        runCatching {
            val uploadableUri = ImageProcessor.compressArtwork(
                context,
                artworkUri,
                MAX_DIMENSION,
                MAX_DIMENSION,
                MAX_FILE_SIZE_BYTES
            )!!
            if( uploadableUri.scheme!!.startsWith( "http" ) )
                return@runCatching uploadableUri

            Innertube.client
                .submitFormWithBinaryData(
                    url = TEMP_FILE_HOST,
                    formData = formData {
                        val (mimeType, fileData) = with( context.contentResolver ) {
                            getType( uploadableUri )!! to openInputStream( uploadableUri )!!.readBytes()
                        }

                        append("reqtype", "fileupload")
                        append("time", "1h")
                        append("fileToUpload", fileData, io.ktor.http.Headers.build {
                            append( HttpHeaders.ContentDisposition, "filename=\"${System.currentTimeMillis()}\"" )
                            append( HttpHeaders.ContentType, mimeType )
                        })
                    }
                )
                .bodyAsText()
                .toUri()
        }

    private suspend fun getDiscordAssetUri( imageUrl: String ): String? {
        if ( imageUrl.startsWith( "mp:" ) ) return imageUrl

        val token = rpc?.token ?: return null
        return runCatching {
            Innertube.client
                .post( "https://discord.com/api/v9/applications/$APPLICATION_ID/external-assets" ) {
                    headers.append( "Authorization", token )
                    setBody(
                        // Use this to ensure syntax
                        // {"urls":[imageUrl]}
                        buildJsonObject {
                            putJsonArray( "urls" ) { add( imageUrl ) }
                        }
                    )
                }
                .body<JsonArray>()
                .firstOrNull()
                ?.jsonObject["external_asset_path"]
                ?.jsonPrimitive
                ?.content
                ?.let { "mp:$it" }
        }.onFailure { exception ->
            // Handle rate limiting (429) silently to avoid disturbing the user
            if (exception.message?.contains("429") == true || exception.message?.contains("Too Many Requests") == true) {
                Timber.tag("DiscordPresence").d("Rate limited by Discord API, skipping asset upload")
            } else {
                // Log other errors but don't show them to the user to avoid disturbance
                Timber.tag("DiscordPresence").w("Failed to get Discord asset URI: ${exception.message}")
            }
        }.getOrNull()
    }

    @Contract("_,null->null")
    private suspend fun getLargeImageUrl( artworkUri: Uri? ): String? =
        uploadArtwork( artworkUri ).fold(
            onSuccess = {
                getDiscordAssetUri( it.toString() )
            },
            onFailure = {
                // Log the error but don't show it to the user to avoid disturbance
                Timber.tag("DiscordPresence").w("Failed to upload artwork: ${it.message}")

                getLargeImageFallback()
            }
        )

    private suspend fun getSmallImageUrl(): String? =
        if ( smallImage != null )
            smallImage
        else
            getDiscordAssetUri( "https://raw.githubusercontent.com/cybrughost/Cubic-Music/main/assets/discord/fallback_app.png" )
                ?.also { smallImage = it }

    private suspend fun getLargeImageFallback(): String? =
        if ( largeImage != null )
            largeImage
        else
            getDiscordAssetUri( "https://raw.githubusercontent.com/cybrughost/Cubic-Music/main/assets/discord/fallback_album.png" )
                ?.also { largeImage = it }


    /**
     * Validate the token
     */

     internal suspend fun validateToken(token: String): Boolean? = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable(context)) return@withContext null
        val request = Request.Builder()
            .url("https://discord.com/api/v9/users/@me")
            .header("Authorization", token)
            .get()
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        }.getOrElse { exception ->
            // Handle rate limiting and network errors silently
            if (exception.message?.contains("429") == true || exception.message?.contains("Too Many Requests") == true) {
                Timber.tag("DiscordPresence").d("Rate limited by Discord API during token validation")
                null // Treat as network error to retry later
            } else {
                Timber.tag("DiscordPresence").e(exception, "Error validating token: ${exception.message}")
                if (exception is java.io.IOException) {
                    null
                } else {
                    false
                }
            }
        }
    }


    fun onPlayingStateChanged(mediaItem: MediaItem?, isPlaying: Boolean, position: Long = 0L, duration: Long = 0L, now: Long = System.currentTimeMillis(), getCurrentPosition: (() -> Long)? = null, isPlayingProvider: (() -> Boolean)? = null) {
        if (isStopped) return
        val token = getToken() ?: return
        if (token.isEmpty()) return

        if (!isNetworkAvailable(context)) {
            return
        }

        refreshJob?.cancel()
        refreshJob = null

        if (token != lastToken) {
            rpc?.closeRPC()
            rpc = KizzyRPC(token)
            lastToken = token
        }

        lastMediaItem = mediaItem
        lastPosition = position
        if (mediaItem == null) {
            sendPausedPresence(duration, now, position)
            return
        }
        if (isPlaying) {
            sendPlayingPresence(mediaItem, position, duration, now)
            // Store current values to avoid calling lambdas later
            val currentIsPlaying = isPlaying
            val currentPosition = position
            startRefreshJob(
                isPlayingProvider = { currentIsPlaying },
                mediaItem = mediaItem,
                getCurrentPosition = { currentPosition },
                pausedPosition = position,
                duration = duration,
                startTime = now // Store the original start time
            )
        } else {
            sendPausedPresence(duration, now, position)
            // Store current values to avoid calling lambdas later
            val currentIsPlaying = isPlaying
            val currentPosition = position
            startRefreshJob(
                isPlayingProvider = { currentIsPlaying },
                mediaItem = mediaItem,
                getCurrentPosition = { currentPosition },
                pausedPosition = position,
                duration = duration,
                startTime = now
            )
        }
    }

    /**
     * Send the "Paused" presence with the frozen time.
     */
    private fun sendPausedPresence(duration: Long, now: Long, pausedPosition: Long) {
        if (isStopped) return
        val mediaItem = lastMediaItem ?: return
        val frozenTimestamp = now - pausedPosition
        val title = mediaItem.mediaMetadata.title?.toString().takeIf { !it.isNullOrBlank() } ?: context.getString(R.string.unknown_title)
        val artist = mediaItem.mediaMetadata.artist?.toString().takeIf { !it.isNullOrBlank() } ?: context.getString(R.string.unknown_artist)
        discordScope.launch {
            if (isStopped) return@launch
            sendActivity(
                mediaItem = mediaItem,
                details = "⏸️ Paused: $title",
                state = artist,
                start = frozenTimestamp,
                end = frozenTimestamp,
                status = "online",
                paused = true
            )
        }
    }

    /**
     * Send a custom discord activity
     */
    private suspend fun sendActivity(
        mediaItem: MediaItem,
        details: String,
        state: String,
        start: Long,
        end: Long,
        status: String,
        paused: Boolean
    ) {
        if (isStopped) return
        val token = getToken() ?: return
        if (token.isEmpty()) return

        when (validateToken(token)) {
            false -> {
                Timber.tag("DiscordPresence").e("Invalid token, stopping presence updates")
                Toaster.e(R.string.discord_token_text_invalid)
                return
            }
            null -> {
                Timber.tag("DiscordPresence").w("Network error while updating presence, skipping.")
                return
            }
            true -> { /* Token is valid, continue */ }
        }

        if (token != lastToken) {
            rpc?.closeRPC()
            rpc = KizzyRPC(token)
            lastToken = token
        }
        val largeImageUrl = getLargeImageUrl(mediaItem.mediaMetadata.artworkUri)
        val smallImageUrl = getSmallImageUrl()
        val largeTextValue = if (state.isNotBlank()) "$details - $state" else details
        runCatching {
            rpc?.setActivity(
                activity = Activity(
                    applicationId = APPLICATION_ID,
                    name = "Cubic-Music",
                    details = details,
                    state = state,
                    type = TypeDiscordActivity.LISTENING.value,
                    timestamps = Timestamps(
                        start = start,
                        end = end
                    ),
                    assets = Assets(
                        largeImage = largeImageUrl,
                        smallImage = smallImageUrl,
                        largeText = largeTextValue,
                        smallText = "v${getVersionName(context)}",
                    ),
                    buttons = listOf("Get Cubic-Music", "Listen to YTMusic"),
                    metadata = com.my.kizzyrpc.model.Metadata(
                        listOf(
                            "https://github.com/cybrughost/Cubic-Music/",
                            "https://music.youtube.com/watch?v=${mediaItem.mediaId}",
                        )
                    )
                ),
                status = status,
                since = System.currentTimeMillis()
            )
        }.onFailure {
            // Log the error but don't show it to the user to avoid disturbance
            Timber.tag("DiscordPresence").w("Error setting Discord activity: ${it.message}")
        }
    }

    /**
     * Close the discord presence (STOP)
     */
    fun onStop() {
        isStopped = true
        refreshJob?.cancel()
        rpc?.closeRPC()
        discordScope.cancel()
    }

    /**
     * Get the version name of the app
     */
    fun getVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Get the type of the discord activity
     */
    enum class TypeDiscordActivity (val value: Int) {
        PLAYING(0),
        STREAMING(1),
        LISTENING(2),
        WATCHING(3),
        COMPETING(5)
    }

    /**
     * Send a custom discord activity
     */
    private fun sendPlayingPresence(mediaItem: MediaItem, position: Long, duration: Long, now: Long) {
        val start = now - position
        val end = start + duration
        val title = mediaItem.mediaMetadata.title?.toString().takeIf { !it.isNullOrBlank() } ?: context.getString(R.string.unknown_title)
        val artist = mediaItem.mediaMetadata.artist?.toString().takeIf { !it.isNullOrBlank() } ?: context.getString(R.string.unknown_artist)
        discordScope.launch {
            sendActivity(
                mediaItem = mediaItem,
                details = title,
                state = artist,
                start = start,
                end = end,
                status = "online",
                paused = false
            )
        }
    }


    /**
     * Start the refresh job
     */
    private fun startRefreshJob(
        isPlayingProvider: () -> Boolean,
        mediaItem: MediaItem,
        getCurrentPosition: () -> Long,
        pausedPosition: Long,
        duration: Long,
        startTime: Long
    ) {
        refreshJob = discordScope.launch {
            while (isActive && !isStopped) {
                delay(15_000L)
                if (!isNetworkAvailable(context)) {
                    continue
                }
                val isPlaying = isPlayingProvider()
                if (isPlaying) {
                    val pos = getCurrentPosition()
                    sendPlayingPresence(mediaItem, pos, duration, startTime)
                } else {
                    sendPausedPresence(duration, System.currentTimeMillis(), pausedPosition)
                }
            }
        }
    }
}