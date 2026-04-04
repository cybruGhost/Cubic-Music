package app.kreate.android.service

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import app.kreate.android.R
import app.kreate.android.Threads
import app.kreate.android.network.innertube.Store
import app.kreate.android.utils.CharUtils
import com.google.gson.Gson
import com.grack.nanojson.JsonObject
import io.ktor.client.statement.bodyAsText
import it.fast4x.innertube.Innertube
import app.it.fast4x.rimusic.models.Song
import it.fast4x.innertube.Innertube.createPoTokenChallenge
import it.fast4x.innertube.Innertube.SearchFilter
import it.fast4x.innertube.models.PlayerResponse
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.nextPage
import it.fast4x.innertube.requests.searchPage
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.enums.AudioQualityFormat
import app.it.fast4x.rimusic.isConnectionMeteredEnabled
import app.it.fast4x.rimusic.models.Format
import app.it.fast4x.rimusic.service.LoginRequiredException
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.service.UnknownException
import app.it.fast4x.rimusic.service.UnplayableException
import app.it.fast4x.rimusic.service.modern.PlayerServiceModern
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeSessionStore
import app.it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeRequestThrottler
import app.it.fast4x.rimusic.utils.isConnectionMetered
import app.it.fast4x.rimusic.utils.okHttpDataSourceFactory
import app.it.fast4x.rimusic.utils.getPipedSession
import com.dd3boh.outertune.utils.potoken.PoTokenGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import app.kreate.android.me.knighthat.utils.Toaster
import it.fast4x.invidious.Invidious
import it.fast4x.piped.Piped
import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.NonBlocking
import org.json.JSONArray
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import org.schabi.newpipe.extractor.services.youtube.YoutubeStreamHelper
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import it.fast4x.innertube.requests.player
import it.fast4x.innertube.utils.from

private const val CHUNK_LENGTH = 512 * 1024L     // 512Kb

/**
 * Store id of song just added to the database.
 * This is created to reduce load to Room
 */
@set:Synchronized
private var justInserted: String = ""

/**
 * Reach out to `next` endpoint for song's information.
 *
 * Info includes:
 * - Titles
 * - Artist(s)
 * - Album
 * - Thumbnails
 * - Duration
 *
 * ### If song IS already inside database
 *
 * It'll replace unmodified columns with fetched data
 *
 * ### If song IS NOT already inside database
 *
 * New record will be created and insert into database
 *
 */
@Blocking
private fun upsertSongInfo( videoId: String ) = runBlocking {       // Use this to prevent suspension of thread while waiting for response from YT

    // Skip adding if it's just added in previous call
    if( videoId == justInserted ) return@runBlocking

    Innertube.nextPage( NextBody(videoId = videoId) )?.fold(
        onSuccess = { nextPage ->
            val songItem = nextPage.itemsPage?.items?.firstOrNull() ?: return@fold
            Database.upsert( songItem )
        },
        onFailure = {
            when( it ) {
                // [UnknownHostException] means no internet connection in most cases
                // Set [justInserted] to this video will skip subsequence calls
                is UnknownHostException -> justInserted = videoId
                else                    -> Toaster.e( R.string.failed_to_fetch_original_property )
            }
        }
    )
}

/**
 * Upsert provided format to the database
 */
@NonBlocking
private fun upsertSongFormat( videoId: String, format: PlayerResponse.StreamingData.Format ) {
    // Skip adding if it's just added in previous call
    if( videoId == justInserted ) return

    runCatching {
        Database.asyncTransaction {
                  // Ensure Song exists first to satisfy Foreign Key constraint
            songTable.insertIgnore(Song.makePlaceholder(videoId))

            formatTable.insertIgnore(Format(
                videoId,
                format.itag,
                format.mimeType,
                format.bitrate.toLong(),
                format.contentLength,
                format.lastModified,
                format.loudnessDb?.toFloat()
            ))
        }

        // Format must be added successfully before setting variable
        justInserted = videoId
    }
}

//<editor-fold defaultstate="collapsed" desc="Extractors">
private val jsonParser =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        useArrayPolymorphism = true
        explicitNulls = false
    }

@UnstableApi
private fun checkPlayability( playabilityStatus: PlayerResponse.PlayabilityStatus? ) {
    if( playabilityStatus?.status != "OK" )
        when( playabilityStatus?.status ) {
            "LOGIN_REQUIRED"    -> throw LoginRequiredException()
            "UNPLAYABLE"        -> throw UnplayableException()
            else                -> throw UnknownException()
        }
}

private fun extractFormat(
    streamingData: PlayerResponse.StreamingData?,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): PlayerResponse.StreamingData.Format? =
    when (audioQualityFormat) {
        AudioQualityFormat.High -> streamingData?.highestQualityFormat
        AudioQualityFormat.Medium -> streamingData?.mediumQualityFormat
        AudioQualityFormat.Low -> streamingData?.lowestQualityFormat
        AudioQualityFormat.Auto ->
            if (connectionMetered && isConnectionMeteredEnabled())
                streamingData?.mediumQualityFormat
            else
                streamingData?.autoMaxQualityFormat
    }

@UnstableApi
private fun getFormatUrl(
    videoId: String,
    cpn: String,
    responseJson: JsonObject,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean,
): Uri {
    val jsonString = Gson().toJson( responseJson )
    val playerResponse = jsonParser.decodeFromString<PlayerResponse>( jsonString )

    checkPlayability( playerResponse.playabilityStatus )

    val format = extractFormat( playerResponse.streamingData, audioQualityFormat, connectionMetered )
    format?.let {
        CoroutineScope( Threads.DATASPEC_DISPATCHER ).launch { upsertSongFormat( videoId, it ) }
    }

    return YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated( videoId, format?.url.orEmpty() )
                                         .toUri()
                                         .buildUpon()
                                         .appendQueryParameter( "range", "0-${format?.contentLength ?: 1_000_000}" )
                                         .appendQueryParameter( "cpn", cpn )
                                         .build()
}

@UnstableApi
fun getAndroidReelFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri {
    val cpn = CharUtils.randomString( 16 )
    val response = YoutubeStreamHelper.getAndroidReelPlayerResponse( ContentCountry.DEFAULT, Localization.DEFAULT, videoId, cpn )
    return getFormatUrl( videoId, cpn, response, audioQualityFormat, connectionMetered )
}

private fun String.getPoToken(): String? =
    this.replace("[", "")
        .replace("]", "")
        .split(",")
        .findLast { it.contains("\"") }
        ?.replace("\"", "")

private suspend fun generateIosPoToken() =
    createPoTokenChallenge().bodyAsText()
        .let { challenge ->
            val listChallenge = jsonParser.decodeFromString<List<String?>>(challenge)
            listChallenge.filterIsInstance<String>().firstOrNull()
        }?.let { poTokenChallenge ->
            Innertube.generatePoToken(poTokenChallenge)
                .bodyAsText()
                .getPoToken()
        }

@UnstableApi
suspend fun getIosFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri {
    if (isYouTubeLoggedIn()) {
        YouTubeSessionStore.applyCurrentSession()
        val authenticated = runCatching {
            val poToken = PoTokenGenerator().getWebClientPoToken(videoId)?.playerRequestPoToken
            val response = YouTubeRequestThrottler.run {
                Innertube.player(videoId = videoId, poToken = poToken)
            }?.getOrThrow() ?: throw IllegalStateException("Null player response")
            val jsonString = Gson().toJson(response)
            return@getIosFormatUrl getFormatUrl(
                videoId = videoId,
                cpn = CharUtils.randomString(16),
                responseJson = Gson().fromJson(jsonString, JsonObject::class.java),
                audioQualityFormat = audioQualityFormat,
                connectionMetered = connectionMetered
            )
        }

        authenticated.getOrElse {
            it.printStackTrace()
        }
    }

    val cpn = CharUtils.randomString( 16 )
    val visitorData = Store.getIosVisitorData()
    val playerRequestToken = generateIosPoToken().orEmpty()
    val poTokenResult = PoTokenResult(visitorData, playerRequestToken, null )
    val response = YoutubeStreamHelper.getIosPlayerResponse( ContentCountry.DEFAULT, Localization.DEFAULT, videoId, cpn, poTokenResult )
    return getFormatUrl( videoId, cpn, response, audioQualityFormat, connectionMetered )
}

@UnstableApi
suspend fun getInnertubePlayerFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri {
    val response = YouTubeRequestThrottler.run {
        Innertube.player(videoId = videoId)
    }?.getOrThrow() ?: throw IllegalStateException("Null Innertube player response")

    val jsonString = Gson().toJson(response)
    return getFormatUrl(
        videoId = videoId,
        cpn = CharUtils.randomString(16),
        responseJson = Gson().fromJson(jsonString, JsonObject::class.java),
        audioQualityFormat = audioQualityFormat,
        connectionMetered = connectionMetered
    )
}

private suspend fun resolvePrimaryFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri =
    try {
        getAndroidReelFormatUrl(videoId, audioQualityFormat, connectionMetered)
    } catch (primaryError: Exception) {
        runCatching {
            getIosFormatUrl(videoId, audioQualityFormat, connectionMetered)
        }.recoverCatching {
            getInnertubePlayerFormatUrl(videoId, audioQualityFormat, connectionMetered)
        }.getOrElse {
            throw primaryError
        }
    }

private fun <T> pickPreferredFormat(
    high: T?,
    medium: T?,
    low: T?,
    auto: T?,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): T? = when (audioQualityFormat) {
    AudioQualityFormat.High -> high
    AudioQualityFormat.Medium -> medium
    AudioQualityFormat.Low -> low
    AudioQualityFormat.Auto -> if (connectionMetered && isConnectionMeteredEnabled()) medium else auto
}

private suspend fun getInvidiousFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri {
    val response = Invidious.api.videos(videoId)?.getOrThrow()
        ?: throw IllegalStateException("Invidious response unavailable")
    val format = pickPreferredFormat(
        high = response.highestQualityFormat,
        medium = response.mediumQualityFormat,
        low = response.lowestQualityFormat,
        auto = response.autoMaxQualityFormat,
        audioQualityFormat = audioQualityFormat,
        connectionMetered = connectionMetered
    ) ?: throw IllegalStateException("No playable Invidious format found")

    return format.url?.toUri() ?: throw IllegalStateException("Invidious format URL unavailable")
}

private suspend fun getPipedFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri {
    val pipedSession = getPipedSession()
    if (pipedSession.token.isBlank() || pipedSession.apiBaseUrl.toString().isBlank()) {
        throw IllegalStateException("Piped session unavailable")
    }

    val streams = Piped.media.audioStreams(pipedSession.toApiSession(), videoId)?.getOrThrow()
        ?: throw IllegalStateException("Piped audio streams unavailable")
    val sortedStreams = streams
        .filter { !it.videoOnly && it.url.isNotBlank() }
        .sortedBy { it.bitrate }

    val format = pickPreferredFormat(
        high = sortedStreams.lastOrNull(),
        medium = sortedStreams.getOrNull(sortedStreams.lastIndex / 2),
        low = sortedStreams.firstOrNull(),
        auto = sortedStreams.lastOrNull(),
        audioQualityFormat = audioQualityFormat,
        connectionMetered = connectionMetered
    ) ?: throw IllegalStateException("No playable Piped format found")

    return format.url.toUri()
}

private suspend fun resolveAlternateProviderFormatUrl(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): Uri =
    runCatching {
        getInvidiousFormatUrl(videoId, audioQualityFormat, connectionMetered)
    }.recoverCatching {
        getPipedFormatUrl(videoId, audioQualityFormat, connectionMetered)
    }.getOrThrow()

private suspend fun findReplacementVideoId(videoId: String): String? {
    val song = Database.songTable.findById(videoId).first() ?: return null
    val title = cleanPrefix(song.title).trim()
    if (title.isBlank()) return null

    val artists = song.artistsText
        ?.split(",")
        ?.map { cleanPrefix(it).trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()

    val queries = buildList {
        add(title)
        artists.firstOrNull()?.let { add("$title $it") }
        if (artists.size > 1) {
            add((listOf(title) + artists.take(2)).joinToString(" "))
        }
    }.distinct()

    fun normalizeMatchText(value: String): String =
        cleanPrefix(value)
            .lowercase()
            .replace(Regex("\\b(official|music video|video|audio|lyrics|visualizer|topic|vevo|hd|4k)\\b"), " ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    fun scoreCandidate(candidateTitle: String, candidateArtist: String): Int {
        val expectedTitle = normalizeMatchText(title)
        val expectedArtists = artists.map(::normalizeMatchText).filter { it.isNotBlank() }
        val normalizedCandidateTitle = normalizeMatchText(candidateTitle)
        val normalizedCandidateArtist = normalizeMatchText(candidateArtist)

        if (normalizedCandidateTitle.isBlank()) return Int.MIN_VALUE
        if (
            normalizedCandidateTitle.contains("mix") ||
            normalizedCandidateTitle.contains("playlist") ||
            normalizedCandidateTitle.contains("full album")
        ) return Int.MIN_VALUE

        var score = 0
        if (normalizedCandidateTitle == expectedTitle) score += 120
        else if (
            normalizedCandidateTitle.contains(expectedTitle) ||
            expectedTitle.contains(normalizedCandidateTitle)
        ) score += 80
        else {
            val expectedTokens = expectedTitle.split(" ").filter { it.length > 1 }.toSet()
            val candidateTokens = normalizedCandidateTitle.split(" ").filter { it.length > 1 }.toSet()
            score += expectedTokens.intersect(candidateTokens).size * 18
        }

        expectedArtists.forEach { artist ->
            if (artist == normalizedCandidateArtist) score += 70
            else if (
                normalizedCandidateArtist.contains(artist) ||
                artist.contains(normalizedCandidateArtist)
            ) score += 45
            else {
                val artistTokens = artist.split(" ").filter { it.length > 1 }.toSet()
                val candidateArtistTokens = normalizedCandidateArtist.split(" ").filter { it.length > 1 }.toSet()
                score += artistTokens.intersect(candidateArtistTokens).size * 14
            }
        }

        return score
    }

    suspend fun searchReplacement(filter: SearchFilter): String? {
        var bestCandidate: Pair<String, Int>? = null

        for (query in queries) {
            val itemsPage = Innertube.searchPage(
                body = SearchBody(query = query, params = filter.value),
                fromMusicShelfRendererContent = { content ->
                    when (filter) {
                        SearchFilter.Song -> Innertube.SongItem.from(content)
                        SearchFilter.Video -> Innertube.VideoItem.from(content)
                        else -> null
                    }
                }
            )?.getOrNull()

            itemsPage?.items?.forEach { item ->
                if (item.key.isBlank() || item.key == videoId) return@forEach

                val candidateTitle = when (item) {
                    is Innertube.SongItem -> item.info?.name.orEmpty()
                    is Innertube.VideoItem -> item.info?.name.orEmpty()
                    else -> ""
                }
                val candidateArtist = when (item) {
                    is Innertube.SongItem -> item.authors?.joinToString(", ") { it.name.orEmpty() }.orEmpty()
                    is Innertube.VideoItem -> item.authors?.joinToString(", ") { it.name.orEmpty() }.orEmpty()
                    else -> ""
                }
                val score = scoreCandidate(candidateTitle, candidateArtist)
                if (score > (bestCandidate?.second ?: Int.MIN_VALUE)) {
                    bestCandidate = item.key to score
                }
            }
        }

        val minimumScore = if (filter == SearchFilter.Song) 80 else 65
        return bestCandidate?.takeIf { it.second >= minimumScore }?.first
    }

    fun searchOmadaReplacement(): String? {
        var bestCandidate: Pair<String, Int>? = null

        queries.forEach { query ->
            runCatching {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val connection = URL("https://yt.omada.cafe/api/v1/search?q=$encodedQuery")
                    .openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val results = JSONArray(response)
                for (index in 0 until results.length()) {
                    val item = results.optJSONObject(index) ?: continue
                    if (item.optString("type") != "video") continue

                    val candidateVideoId = item.optString("videoId").trim()
                    if (candidateVideoId.isBlank() || candidateVideoId == videoId) continue

                    val score = scoreCandidate(
                        candidateTitle = item.optString("title"),
                        candidateArtist = item.optString("author")
                    )
                    if (score > (bestCandidate?.second ?: Int.MIN_VALUE)) {
                        bestCandidate = candidateVideoId to score
                    }
                }
            }
        }

        return bestCandidate?.takeIf { it.second >= 60 }?.first
    }

    return searchReplacement(SearchFilter.Song)
        ?: searchReplacement(SearchFilter.Video)
        ?: searchOmadaReplacement()
}
//</editor-fold>

@UnstableApi
fun DataSpec.process(
    videoId: String,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): DataSpec = runBlocking( Dispatchers.IO ) {
    val formatUri = runBlocking( Dispatchers.IO ) {
        var replacementVideoId: String? = null
        try {
            resolvePrimaryFormatUrl(videoId, audioQualityFormat, connectionMetered)
        } catch (primaryError: Exception) {
            if (replacementVideoId.isNullOrBlank()) {
                replacementVideoId = findReplacementVideoId(videoId)
            }

            val candidateVideoIds = buildList {
                add(videoId)
                replacementVideoId
                    ?.takeIf { it.isNotBlank() && it != videoId }
                    ?.let(::add)
            }

            candidateVideoIds.firstNotNullOfOrNull { candidateVideoId ->
                runCatching {
                    resolveAlternateProviderFormatUrl(candidateVideoId, audioQualityFormat, connectionMetered)
                }.getOrNull()
            } ?: runCatching {
                replacementVideoId?.takeIf { it.isNotBlank() }?.let { fallbackVideoId ->
                    resolvePrimaryFormatUrl(fallbackVideoId, audioQualityFormat, connectionMetered)
                }
            }.getOrNull() ?: throw primaryError
        }
    }

    withUri( formatUri ).subrange( uriPositionOffset )
}

//<editor-fold defaultstate="collapsed" desc="Data source factories">
@UnstableApi
fun PlayerServiceModern.createDataSourceFactory(): DataSource.Factory {
    val upstreamFactory = appContext().okHttpDataSourceFactory

    // This factory resolves the Video ID to a real URL when needed (cache miss)
    val resolvingDataSourceFactory = ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
        val videoId = dataSpec.uri.toString().substringAfter("watch?v=")
        val isLocal = dataSpec.uri.scheme == ContentResolver.SCHEME_CONTENT || dataSpec.uri.scheme == ContentResolver.SCHEME_FILE

        if (isLocal) {
            return@Factory dataSpec
        }

        // Only upsert info if we are actually resolving (cache miss)
        CoroutineScope(Threads.DATASPEC_DISPATCHER).launch { upsertSongInfo(videoId) }

        // Always resolve URL for non-local files and ensure key is set to videoId
        // This ensures CacheDataSource uses the correct key even if URI changes
        dataSpec.process(videoId, audioQualityFormat, applicationContext.isConnectionMetered())
            .buildUpon()
            .setKey(videoId)
            .build()
    }

    // LRU Cache (Writable) - Upstream is the Resolver
    val lruCacheFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(resolvingDataSourceFactory)

    // Download Cache (Read-Only during playback) - Upstream is LRU Cache
    return CacheDataSource.Factory()
        .setCache(downloadCache)
        .setUpstreamDataSourceFactory(lruCacheFactory)
        .setCacheWriteDataSinkFactory(null)
        .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)
}

@UnstableApi
fun MyDownloadHelper.createDataSourceFactory(): DataSource.Factory {
    val upstreamFactory = appContext().okHttpDataSourceFactory

    val resolvingDataSourceFactory = ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
        val videoId = dataSpec.uri.toString().substringAfter("watch?v=")
        
        CoroutineScope(Threads.DATASPEC_DISPATCHER).launch { upsertSongInfo(videoId) }

        dataSpec.process(videoId, audioQualityFormat, appContext().isConnectionMetered())
            .buildUpon()
            .setKey(videoId)
            .build()
    }

    // Download Cache (Writable for downloads? No, CacheWriter handles writing)
    // We expose it as a source that can read from cache if available, or resolve upstream.
    return CacheDataSource.Factory()
        .setCache(getDownloadCache(appContext()))
        .setUpstreamDataSourceFactory(resolvingDataSourceFactory)
        .setCacheWriteDataSinkFactory(null)
}
//</editor-fold>
