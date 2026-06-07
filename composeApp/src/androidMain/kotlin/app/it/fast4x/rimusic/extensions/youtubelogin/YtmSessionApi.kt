package app.it.fast4x.rimusic.extensions.youtubelogin

import app.cubic.android.core.network.NetworkQualityHelper
import app.cubic.android.core.network.enum.NetworkQuality
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.appRunningInBackground
import app.it.fast4x.rimusic.appVisibilityInBackground
import app.it.fast4x.rimusic.utils.SecureApiConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Collections
import java.util.concurrent.TimeUnit

object YtmSessionApi {
    private val SESSION_ENDPOINT: String
        get() = SecureApiConfig.ytmSessionEndpoint
    private const val SESSION_ORIGIN = "https://ytm-cookie-sparkle.lovable.app"
    private const val SESSION_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val activeCalls = Collections.synchronizedSet(mutableSetOf<Call>())
    private val visibilityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        visibilityScope.launch {
            appVisibilityInBackground.collect { isInBackground ->
                if (isInBackground) {
                    cancelActiveCalls()
                }
            }
        }
    }

    suspend fun fetchAccountInfo(cookies: String): Result<YtmAccountInfo> = withContext(Dispatchers.IO) {
        post("fetch", cookies) { json ->
            YtmAccountInfo(
                hasSession = json.optBoolean("hasSession", false),
                accountName = json.optString("accountName"),
                accountEmail = json.optString("accountEmail"),
                accountChannelHandle = json.optString("accountChannelHandle"),
                accountThumbnail = json.optString("accountThumbnail")
            )
        }
    }

    suspend fun listAccounts(cookies: String): Result<List<YtmLinkedAccount>> = withContext(Dispatchers.IO) {
        post("list_accounts", cookies) { json ->
            val accounts = json.optJSONArray("accounts") ?: JSONArray()
            List(accounts.length()) { index ->
                parseLinkedAccount(accounts.optJSONObject(index) ?: JSONObject())
            }.distinctBy { account ->
                listOf(
                    account.accountEmail.trim().lowercase(),
                    account.channelHandle.trim().lowercase(),
                    account.pageId.trim(),
                    account.accountName.trim().lowercase()
                ).joinToString("|")
            }
        }
    }

    suspend fun switchAccount(
        cookies: String,
        authUser: String,
        pageId: String?
    ): Result<YtmApiSession> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedCookies = YouTubeSessionStore.normalizeCookieString(cookies)
            require(normalizedCookies.isNotBlank()) { "No YouTube Music cookies found yet" }
            require(authUser.isNotBlank()) { "Missing authUser for selected account" }

            val body = JSONObject().put("cookies", normalizedCookies)

            val url = buildString {
                append(SESSION_ENDPOINT)
                append("?action=switch_account&authuser=").append(authUser.queryValue())
                if (!pageId.isNullOrBlank()) append("&pageid=").append(pageId.queryValue())
            }

            val request = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody(jsonMediaType))
                .sessionHeaders()
                .build()

            execute(request).use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException(parseError(responseBody, response.code))
                }

                parseApiSession(JSONObject(responseBody), fallbackCookie = normalizedCookies)
            }
        }
    }

    suspend fun ensureScopedSession(session: YoutubeSession): YoutubeSession = withContext(Dispatchers.IO) {
        if (!YouTubeSessionStore.hasAuthCookies(session.cookie)) return@withContext session

        val accounts = listAccounts(session.cookie).getOrDefault(emptyList())
        val scopedAccount = accounts.firstOrNull { account ->
            session.authUser.isNotBlank() &&
                account.authUser == session.authUser &&
                account.pageId == session.pageId
        } ?: accounts.firstOrNull { it.isSelected }
            ?: return@withContext session

        if (scopedAccount.authUser == session.authUser && scopedAccount.pageId == session.pageId) {
            return@withContext session
        }

        val switched = scopedAccount.authUser
            .takeIf { it.isNotBlank() }
            ?.let { authUser ->
                switchAccount(
                    cookies = session.cookie,
                    authUser = authUser,
                    pageId = scopedAccount.pageId.ifBlank { null }
                ).getOrNull()
            }

        YouTubeSessionStore.saveSession(
            context = appContext(),
            session = YoutubeSession(
                cookie = switched?.cookie?.ifBlank { session.cookie } ?: session.cookie,
                visitorData = switched?.visitorData?.ifBlank { session.visitorData } ?: session.visitorData,
                dataSyncId = switched?.dataSyncId?.ifBlank { session.dataSyncId } ?: session.dataSyncId,
                authUser = switched?.authUser?.ifBlank { scopedAccount.authUser } ?: scopedAccount.authUser,
                pageId = switched?.pageId?.ifBlank { scopedAccount.pageId } ?: scopedAccount.pageId,
                accountName = switched?.accountName?.ifBlank { scopedAccount.accountName } ?: scopedAccount.accountName,
                accountEmail = switched?.accountEmail?.ifBlank { scopedAccount.accountEmail } ?: scopedAccount.accountEmail,
                accountChannelHandle = switched?.accountChannelHandle?.ifBlank { scopedAccount.channelHandle }
                    ?: scopedAccount.channelHandle,
                accountThumbnail = switched?.accountThumbnail?.ifBlank { scopedAccount.accountThumbnail }
                    ?: scopedAccount.accountThumbnail,
                lastAccountRefreshAt = System.currentTimeMillis()
            ),
            makePreferred = true
        )
    }

    suspend fun fetchPlaylists(
        cookies: String,
        authUser: String? = null,
        pageId: String? = null
    ): Result<List<YtmPlaylist>> = postLibrary("playlists", cookies, authUser, pageId) { json ->
        val arr = json.optJSONArray("playlists") ?: JSONArray()
        List(arr.length()) { index ->
            val item = arr.optJSONObject(index) ?: JSONObject()
            val subtitle = item.optString("subtitle")
            YtmPlaylist(
                playlistId = item.optString("playlistId").normalizedField(),
                browseId = item.optString("browseId").normalizedField(),
                rawPlaylistId = item.optString("rawPlaylistId").normalizedField(),
                title = item.optString("title").normalizedField(),
                name = item.optString("name").normalizedField(),
                thumbnail = item.optString("thumbnail").normalizedField(),
                thumbnailUrl = item.optString("thumbnailUrl").normalizedField(),
                songCount = item.optString("songCount").normalizedField().ifBlank { extractSongCount(subtitle) },
                subtitle = subtitle.normalizedField()
            )
        }
    }

    suspend fun fetchHomeFeed(
        cookies: String,
        authUser: String? = null,
        pageId: String? = null,
        continuation: String? = null,
        debug: Boolean = false,
        maxPages: Int = 50,
        guest: Boolean = false
    ): Result<YtmHomeFeed> = postHomeFeed(
        cookies = cookies,
        authUser = authUser,
        pageId = pageId,
        continuation = continuation,
        debug = debug,
        maxPages = maxPages,
        guest = guest
    ) { json ->
        val arr = json.optJSONArray("sections") ?: JSONArray()
        val sections = List(arr.length()) { index ->
            val section = arr.optJSONObject(index) ?: JSONObject()
            val items = section.optJSONArray("items") ?: JSONArray()
            YtmHomeSection(
                title = section.optString("title").normalizedField(),
                subtitle = section.optString("subtitle").normalizedField(),
                browseId = section.optString("browseId").normalizedField(),
                params = section.optString("params").normalizedField(),
                type = section.optString("type").normalizedField(),
                itemCount = section.optInt("itemCount"),
                hasMore = section.optBoolean("hasMore"),
                items = List(items.length()) { itemIndex ->
                    val item = items.optJSONObject(itemIndex) ?: JSONObject()
                    YtmHomeSectionItem(
                        id = item.optString("id").normalizedField(),
                        videoId = item.optString("videoId").normalizedField(),
                        playlistId = item.optString("playlistId").normalizedField(),
                        browseId = item.optString("browseId").normalizedField(),
                        title = item.optString("title").normalizedField(),
                        subtitle = item.optString("subtitle").normalizedField(),
                        artistsText = item.optString("artistsText").normalizedField(),
                        artistId = item.optString("artistId").normalizedField(),
                        artistIds = item.optJSONArray("artistIds")?.let { artistIds ->
                            List(artistIds.length()) { artistIndex -> artistIds.optString(artistIndex).normalizedField() }
                                .filter { it.isNotBlank() }
                        } ?: emptyList(),
                        album = item.optString("album").normalizedField(),
                        albumId = item.optString("albumId").normalizedField(),
                        thumbnail = item.optString("thumbnail").normalizedField(),
                        thumbnailUrl = item.optString("thumbnailUrl").normalizedField(),
                        type = item.optString("type").normalizedField()
                    )
                }
            )
        }
        YtmHomeFeed(
            sections = sections,
            continuation = json.optString("continuation")
                .normalizedField()
                .takeIf { it.isNotBlank() },
            hasMore = json.optBoolean("hasMore"),
            pagesFetched = json.optInt("pagesFetched"),
            rawJson = json.optJSONObject("raw")?.toString(),
        parseErrors = parseErrors(json)
        )
    }

    suspend fun fetchAllHomeFeed(
        cookies: String,
        authUser: String? = null,
        pageId: String? = null,
        debug: Boolean = false,
        maxPages: Int = 50,
        guest: Boolean = false,
        safetyCap: Int = 5
    ): Result<YtmHomeFeed> = withContext(Dispatchers.IO) {
        runCatching {
            val firstPage = fetchHomeFeed(
                cookies = cookies,
                authUser = authUser,
                pageId = pageId,
                debug = debug,
                maxPages = maxPages,
                guest = guest
            ).getOrThrow()

            val sections = firstPage.sections.toMutableList()
            val parseErrors = firstPage.parseErrors.toMutableList()
            var continuation = firstPage.continuation
            var hasMore = firstPage.hasMore
            var pagesFetched = firstPage.pagesFetched
            var iterations = 1

            while (hasMore && !continuation.isNullOrBlank() && iterations < safetyCap) {
                val nextPage = fetchHomeFeed(
                    cookies = cookies,
                    authUser = authUser,
                    pageId = pageId,
                    continuation = continuation,
                    debug = debug,
                    maxPages = maxPages,
                    guest = guest
                ).getOrThrow()

                sections += nextPage.sections
                parseErrors += nextPage.parseErrors
                continuation = nextPage.continuation
                hasMore = nextPage.hasMore
                pagesFetched += nextPage.pagesFetched
                iterations++
            }

            firstPage.copy(
                sections = sections.distinctBy { section ->
                    listOf(
                        section.title,
                        section.browseId,
                        section.params,
                        section.type,
                        section.items.take(3).joinToString(",") { item ->
                            item.id.ifBlank { item.videoId.ifBlank { item.playlistId.ifBlank { item.browseId } } }
                        }
                    ).joinToString("|")
                },
                continuation = continuation,
                hasMore = hasMore,
                pagesFetched = pagesFetched,
                parseErrors = parseErrors
            )
        }
    }

    suspend fun fetchPlaylistSongs(
        cookies: String,
        playlistId: String,
        authUser: String? = null,
        pageId: String? = null
    ): Result<List<YtmSong>> = withContext(Dispatchers.IO) {
        require(playlistId.isNotBlank()) { "Missing playlistId" }
        val url = buildString {
            append(SESSION_ENDPOINT)
            append("?action=playlist_songs")
            append("&playlistId=").append(playlistId.queryValue())
            if (!authUser.isNullOrBlank()) append("&authuser=").append(authUser.queryValue())
            if (!pageId.isNullOrBlank()) append("&pageid=").append(pageId.queryValue())
        }
        postUrl(url, cookies, ::parseSongs)
    }

    suspend fun fetchLikedSongs(
        cookies: String,
        authUser: String? = null,
        pageId: String? = null
    ): Result<List<YtmSong>> = postLibrary("liked_songs", cookies, authUser, pageId, ::parseSongs)

    suspend fun addVideosToPlaylist(
        cookies: String,
        playlistId: String,
        videoIds: List<String>,
        authUser: String? = null,
        pageId: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedCookies = YouTubeSessionStore.normalizeCookieString(cookies)
            require(normalizedCookies.isNotBlank()) { "No YouTube Music cookies found yet" }
            val normalizedPlaylistId = playlistId.removePrefix("VL").trim()
            require(normalizedPlaylistId.isNotBlank()) { "Missing playlistId" }
            val validVideoIds = videoIds
                .map { it.substringAfterLast("/").trim() }
                .filter { VIDEO_ID_REGEX.matches(it) }
                .distinct()
            require(validVideoIds.isNotEmpty()) { "No valid YouTube video ids to add" }

            var inserted = 0
            validVideoIds.chunked(100).forEach { chunk ->
                val request = Request.Builder()
                    .url("https://music.youtube.com/youtubei/v1/browse/edit_playlist?prettyPrint=false")
                    .post(buildEditPlaylistBody(normalizedPlaylistId, chunk).toString().toRequestBody(jsonMediaType))
                    .ytmEditPlaylistHeaders(
                        cookies = normalizedCookies,
                        authUser = authUser,
                        pageId = pageId
                    )
                    .build()

                execute(request).use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw IOException(parseError(body, response.code))
                    }
                    inserted += chunk.size
                }
            }
            inserted
        }
    }

    suspend fun fetchHistory(
        cookies: String,
        authUser: String? = null,
        pageId: String? = null
    ): Result<List<YtmSong>> = postLibrary("history", cookies, authUser, pageId, ::parseSongs)

    suspend fun fetchArtists(
        cookies: String,
        authUser: String? = null,
        pageId: String? = null
    ): Result<List<YtmArtist>> = postLibrary("artists", cookies, authUser, pageId) { json ->
        val arr = json.optJSONArray("artists") ?: JSONArray()
        List(arr.length()) { index ->
            val item = arr.optJSONObject(index) ?: JSONObject()
            YtmArtist(
                browseId = item.optString("browseId").normalizedField(),
                name = item.optString("name").normalizedField(),
                thumbnail = item.optString("thumbnail").normalizedField(),
                subscribers = item.optString("subscribers").normalizedField()
            )
        }
    }

    suspend fun fetchAlbums(
        cookies: String,
        authUser: String? = null,
        pageId: String? = null
    ): Result<List<YtmAlbum>> = postLibrary("albums", cookies, authUser, pageId) { json ->
        val arr = json.optJSONArray("albums") ?: JSONArray()
        List(arr.length()) { index ->
            val item = arr.optJSONObject(index) ?: JSONObject()
            YtmAlbum(
                browseId = item.optString("browseId").normalizedField(),
                playlistId = item.optString("playlistId").normalizedField(),
                title = item.optString("title").normalizedField(),
                artist = item.optString("artist").normalizedField(),
                thumbnail = item.optString("thumbnail").normalizedField(),
                year = item.optString("year").normalizedField(),
                type = item.optString("type").normalizedField()
            )
        }
    }

    suspend fun fetchArtist(
        cookies: String,
        artistId: String,
        authUser: String? = null,
        pageId: String? = null,
        guest: Boolean = false
    ): Result<YtmArtistDetail> {
        require(artistId.isNotBlank()) { "Missing artist id" }
        val action = if (guest) "guest_artist" else "artist"
        val url = buildString {
            append(SESSION_ENDPOINT)
            append("?action=").append(action)
            append("&id=").append(artistId.queryValue())
            if (!authUser.isNullOrBlank()) append("&authuser=").append(authUser.queryValue())
            if (!pageId.isNullOrBlank()) append("&pageid=").append(pageId.queryValue())
        }
        return postUrl(url, if (guest) "" else cookies, ::parseArtistDetail, allowEmptyCookies = guest)
    }

    suspend fun fetchAlbum(
        cookies: String,
        albumId: String,
        authUser: String? = null,
        pageId: String? = null,
        guest: Boolean = false
    ): Result<YtmAlbumDetail> {
        require(albumId.isNotBlank()) { "Missing album id" }
        val action = if (guest) "guest_album" else "album"
        val url = buildString {
            append(SESSION_ENDPOINT)
            append("?action=").append(action)
            append("&id=").append(albumId.queryValue())
            if (!authUser.isNullOrBlank()) append("&authuser=").append(authUser.queryValue())
            if (!pageId.isNullOrBlank()) append("&pageid=").append(pageId.queryValue())
        }
        return postUrl(url, if (guest) "" else cookies, ::parseAlbumDetail, allowEmptyCookies = guest)
    }

    private fun execute(request: Request): Response {
        ensureForeground("YtmSessionApi")
        val call = httpClient.newCall(request)
        activeCalls += call
        return try {
            call.execute().also {
                Timber.d("YtmSessionApi %s %s -> %s", request.method, request.url, it.code)
            }
        } catch (e: IOException) {
            throw IOException(describeConnectivityFailure(e.message), e)
        } finally {
            activeCalls -= call
        }
    }

    private fun parseLinkedAccount(json: JSONObject): YtmLinkedAccount =
        YtmLinkedAccount(
            accountName = json.optString("accountName").normalizedField(),
            accountEmail = json.optString("accountEmail").normalizedField(),
            channelHandle = json.optString("channelHandle").normalizedField(),
            accountThumbnail = json.optString("accountThumbnail").normalizedField(),
            subscribers = json.optString("subscribers").normalizedField(),
            isSelected = json.optBoolean("isSelected"),
            authUser = json.optString("authUser").normalizedField(),
            pageId = json.optString("pageId").normalizedField()
        )

    private fun parseApiSession(json: JSONObject, fallbackCookie: String): YtmApiSession =
        YtmApiSession(
            hasSession = json.optBoolean("hasSession", true),
            cookie = json.optString("cookies").normalizedField().ifBlank {
                json.optString("cookie").normalizedField().ifBlank { fallbackCookie }
            },
            visitorData = json.optString("visitorData").normalizedField(),
            dataSyncId = json.optString("dataSyncId").normalizedField(),
            authUser = json.optString("authUser").normalizedField(),
            pageId = json.optString("pageId").normalizedField(),
            accountName = json.optString("accountName").normalizedField(),
            accountEmail = json.optString("accountEmail").normalizedField(),
            accountChannelHandle = json.optString("accountChannelHandle").normalizedField(),
            accountThumbnail = json.optString("accountThumbnail").normalizedField()
        )

    private fun parseSongs(json: JSONObject): List<YtmSong> {
        val songs = json.optJSONArray("songs") ?: JSONArray()
        return List(songs.length()) { index ->
            val item = songs.optJSONObject(index) ?: JSONObject()
            YtmSong(
                id = item.optString("id").normalizedField().ifBlank { item.optString("videoId").normalizedField() },
                videoId = item.optString("videoId").normalizedField(),
                title = item.optString("title").normalizedField(),
                artist = item.optString("artist").normalizedField(),
                artistsText = item.optString("artistsText").normalizedField(),
                artistId = item.optString("artistId").normalizedField(),
                artistIds = item.optJSONArray("artistIds")?.let { artistIds ->
                    List(artistIds.length()) { artistIndex -> artistIds.optString(artistIndex).normalizedField() }
                        .filter { it.isNotBlank() }
                } ?: emptyList(),
                artists = item.optJSONArray("artists")?.let { artists ->
                    List(artists.length()) { artistIndex ->
                        val artist = artists.optJSONObject(artistIndex) ?: JSONObject()
                        YtmArtistRef(
                            id = artist.optString("id").normalizedField(),
                            name = artist.optString("name").normalizedField()
                        )
                    }
                        .filter { it.id.isNotBlank() || it.name.isNotBlank() }
                } ?: emptyList(),
                album = item.optString("album").normalizedField(),
                albumId = item.optString("albumId").normalizedField(),
                thumbnail = item.optString("thumbnail").normalizedField(),
                thumbnailUrl = item.optString("thumbnailUrl").normalizedField(),
                duration = item.optString("duration").normalizedField(),
                durationText = item.optString("durationText").normalizedField(),
                setVideoId = item.optString("setVideoId").normalizedField(),
                position = item.optInt("position", -1),
                dateAdded = item.optString("dateAdded").normalizedField(),
                isAvailable = item.optBoolean("isAvailable", true)
            )
        }
    }

    private fun parseArtistDetail(json: JSONObject): YtmArtistDetail =
        YtmArtistDetail(
            browseId = json.optString("browseId").normalizedField(),
            name = json.optString("name").normalizedField(),
            description = json.optString("description").normalizedField(),
            subscribers = json.optString("subscribers").normalizedField(),
            thumbnail = json.optString("thumbnail").normalizedField(),
            thumbnailUrl = json.optString("thumbnailUrl").normalizedField(),
            songs = parseArtistTracks(json.optJSONArray("songs")),
            albums = parseArtistAlbumRefs(json.optJSONArray("albums")),
            singles = parseArtistAlbumRefs(json.optJSONArray("singles")),
            videos = parseArtistTracks(json.optJSONArray("videos")),
            related = parseRelatedArtists(json.optJSONArray("related"))
        )

    private fun parseAlbumDetail(json: JSONObject): YtmAlbumDetail =
        YtmAlbumDetail(
            browseId = json.optString("browseId").normalizedField(),
            playlistId = json.optString("playlistId").normalizedField(),
            title = json.optString("title").normalizedField(),
            artist = json.optString("artist").normalizedField(),
            artistId = json.optString("artistId").normalizedField(),
            year = json.optString("year").normalizedField(),
            description = json.optString("description").normalizedField(),
            thumbnail = json.optString("thumbnail").normalizedField(),
            thumbnailUrl = json.optString("thumbnailUrl").normalizedField(),
            trackCount = json.optInt("trackCount"),
            durationText = json.optString("durationText").normalizedField(),
            tracks = parseAlbumTracks(json.optJSONArray("tracks"))
        )

    private fun parseArtistTracks(arr: JSONArray?): List<YtmArtistTrack> {
        arr ?: return emptyList()
        return List(arr.length()) { index ->
            val item = arr.optJSONObject(index) ?: JSONObject()
            YtmArtistTrack(
                videoId = item.optString("videoId").normalizedField(),
                title = item.optString("title").normalizedField(),
                album = item.optString("album").normalizedField(),
                albumId = item.optString("albumId").normalizedField(),
                thumbnail = item.optString("thumbnail").normalizedField(),
                thumbnailUrl = item.optString("thumbnailUrl").normalizedField(),
                duration = item.optString("duration").normalizedField()
            )
        }.filter { it.videoId.isNotBlank() && it.title.isNotBlank() }
    }

    private fun parseArtistAlbumRefs(arr: JSONArray?): List<YtmArtistAlbumRef> {
        arr ?: return emptyList()
        return List(arr.length()) { index ->
            val item = arr.optJSONObject(index) ?: JSONObject()
            YtmArtistAlbumRef(
                browseId = item.optString("browseId").normalizedField(),
                playlistId = item.optString("playlistId").normalizedField(),
                title = item.optString("title").normalizedField(),
                year = item.optString("year").normalizedField(),
                thumbnail = item.optString("thumbnail").normalizedField(),
                thumbnailUrl = item.optString("thumbnailUrl").normalizedField(),
                type = item.optString("type").normalizedField()
            )
        }.filter { it.browseId.isNotBlank() || it.playlistId.isNotBlank() || it.title.isNotBlank() }
    }

    private fun parseRelatedArtists(arr: JSONArray?): List<YtmRelatedArtist> {
        arr ?: return emptyList()
        return List(arr.length()) { index ->
            val item = arr.optJSONObject(index) ?: JSONObject()
            YtmRelatedArtist(
                browseId = item.optString("browseId").normalizedField(),
                name = item.optString("name").normalizedField(),
                thumbnail = item.optString("thumbnail").normalizedField(),
                thumbnailUrl = item.optString("thumbnailUrl").normalizedField(),
                subscribers = item.optString("subscribers").normalizedField()
            )
        }.filter { it.browseId.isNotBlank() || it.name.isNotBlank() }
    }

    private fun parseAlbumTracks(arr: JSONArray?): List<YtmAlbumTrack> {
        arr ?: return emptyList()
        return List(arr.length()) { index ->
            val item = arr.optJSONObject(index) ?: JSONObject()
            YtmAlbumTrack(
                videoId = item.optString("videoId").normalizedField(),
                title = item.optString("title").normalizedField(),
                artistsText = item.optString("artistsText").normalizedField(),
                artistIds = item.optJSONArray("artistIds")?.let { artistIds ->
                    List(artistIds.length()) { artistIndex -> artistIds.optString(artistIndex).normalizedField() }
                        .filter { it.isNotBlank() }
                } ?: emptyList(),
                duration = item.optString("duration").normalizedField(),
                trackNumber = item.optInt("trackNumber", index + 1),
                thumbnail = item.optString("thumbnail").normalizedField(),
                thumbnailUrl = item.optString("thumbnailUrl").normalizedField()
            )
        }.filter { it.videoId.isNotBlank() && it.title.isNotBlank() }
    }

    private fun extractSongCount(subtitle: String): String =
        Regex("(\\d+)\\s+(?:tracks?|songs?)", RegexOption.IGNORE_CASE)
            .find(subtitle)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()

    private suspend fun <T> postLibrary(
        action: String,
        cookies: String,
        authUser: String?,
        pageId: String?,
        parse: (JSONObject) -> T
    ): Result<T> {
        val url = buildString {
            append(SESSION_ENDPOINT)
            append("?action=").append(action)
            if (!authUser.isNullOrBlank()) append("&authuser=").append(authUser.queryValue())
            if (!pageId.isNullOrBlank()) append("&pageid=").append(pageId.queryValue())
        }
        return postUrl(url, cookies, parse)
    }

    private suspend fun <T> postHomeFeed(
        cookies: String,
        authUser: String?,
        pageId: String?,
        continuation: String?,
        debug: Boolean,
        maxPages: Int,
        guest: Boolean,
        parse: (JSONObject) -> T
    ): Result<T> {
        val action = if (guest) "guest_home" else "home"
        val url = buildString {
            append(SESSION_ENDPOINT)
            append("?action=").append(action)
            if (!authUser.isNullOrBlank()) append("&authuser=").append(authUser.queryValue())
            if (!pageId.isNullOrBlank()) append("&pageid=").append(pageId.queryValue())
            if (!continuation.isNullOrBlank()) append("&continuation=").append(continuation.queryValue())
            if (debug) append("&debug=1")
            append("&maxPages=").append(maxPages.coerceIn(1, 50))
        }
        return postUrl(url, if (guest) "" else cookies, parse, allowEmptyCookies = guest)
    }

    private suspend fun <T> post(
        action: String,
        cookies: String,
        parse: (JSONObject) -> T
    ): Result<T> = postUrl("$SESSION_ENDPOINT?action=$action", cookies, parse)

    private suspend fun <T> postUrl(
        url: String,
        cookies: String,
        parse: (JSONObject) -> T,
        allowEmptyCookies: Boolean = false
    ): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedCookies = YouTubeSessionStore.normalizeCookieString(cookies)
            require(allowEmptyCookies || normalizedCookies.isNotBlank()) { "No YouTube Music cookies found yet" }

            val request = Request.Builder()
                .url(url)
                .post(JSONObject().put("cookies", normalizedCookies).toString().toRequestBody(jsonMediaType))
                .sessionHeaders()
                .build()

            execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException(parseError(body, response.code))
                }
                parse(JSONObject(body))
            }
        }
    }

    private fun ensureForeground(owner: String) {
        if (appRunningInBackground) {
            throw CancellationException("$owner skipped while app is in background")
        }
    }

    private fun cancelActiveCalls() {
        val calls = synchronized(activeCalls) { activeCalls.toList() }
        calls.forEach { call ->
            runCatching { call.cancel() }
                .onFailure { Timber.w(it, "YtmSessionApi failed cancelling background call") }
        }
        activeCalls.clear()
    }

    private fun parseError(body: String, statusCode: Int): String =
        runCatching {
            JSONObject(body).optString("error").ifBlank {
                JSONObject(body).optString("message").ifBlank {
                    describeHttpFailure(statusCode)
                }
            }
        }.getOrDefault(describeHttpFailure(statusCode))

    private fun describeConnectivityFailure(originalMessage: String?): String {
        val context = appContext()
        val isAvailable = NetworkQualityHelper.isNetworkAvailable(context)
        val isConnected = NetworkQualityHelper.isNetworkConnected(context)
        val quality = NetworkQualityHelper.getCurrentNetworkQuality(context)

        return when {
            !isAvailable -> "No internet connection available."
            !isConnected -> "Internet connection is present but not validated yet."
            quality == NetworkQuality.LOW -> "Connection is weak. Loading may buffer or fail temporarily."
            !originalMessage.isNullOrBlank() -> originalMessage
            else -> "Network request failed."
        }
    }

    private fun describeHttpFailure(statusCode: Int): String =
        when (statusCode) {
            401, 403 -> "Session expired or account access was denied."
            404 -> "Requested YouTube Music resource was not found."
            in 500..599 -> "YouTube Music session service is having trouble right now."
            else -> "Request failed with status $statusCode"
        }

    private fun parseErrors(json: JSONObject): List<YtmParseError> {
        val arr = json.optJSONArray("parseErrors") ?: return emptyList()
        return List(arr.length()) { index ->
            val item = arr.optJSONObject(index) ?: JSONObject()
            YtmParseError(
                where = item.optString("where").normalizedField(),
                reason = item.optString("reason").normalizedField(),
                sample = item.opt("sample")?.toString()?.takeIf { it.isNotBlank() }
            )
        }
    }

    private fun String.normalizedField(): String =
        trim().takeUnless { it.equals("null", ignoreCase = true) }.orEmpty()

    private fun String.queryValue(): String =
        URLEncoder.encode(this, Charsets.UTF_8.name())

    private val VIDEO_ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")

    private fun buildEditPlaylistBody(playlistId: String, videoIds: List<String>): JSONObject =
        JSONObject()
            .put(
                "context",
                JSONObject().put(
                    "client",
                    JSONObject()
                        .put("clientName", "WEB_REMIX")
                        .put("clientVersion", "1.20241120.01.00")
                        .put("hl", "en")
                        .put("gl", "US")
                        .put("platform", "DESKTOP")
                )
            )
            .put("playlistId", playlistId)
            .put(
                "actions",
                JSONArray().also { actions ->
                    videoIds.forEach { videoId ->
                        actions.put(
                            JSONObject()
                                .put("action", "ACTION_ADD_VIDEO")
                                .put("addedVideoId", videoId)
                        )
                    }
                }
            )

    private fun Builder.ytmEditPlaylistHeaders(
        cookies: String,
        authUser: String?,
        pageId: String?
    ): Builder {
        header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Content-Type", "application/json")
            .header("Cookie", cookies)
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
            .header("X-Origin", "https://music.youtube.com")
            .header("X-Youtube-Client-Name", "67")
            .header("X-Youtube-Client-Version", "1.20241120.01.00")
            .header("Authorization", sapisidHash(cookies))
        if (!authUser.isNullOrBlank()) header("X-Goog-Authuser", authUser)
        if (!pageId.isNullOrBlank()) header("X-Goog-PageId", pageId)
        return this
    }

    private fun sapisidHash(cookies: String): String {
        val sapisid = Regex("""(?:^|;\s*)(?:SAPISID|__Secure-3PAPISID)=([^;]+)""")
            .find(cookies)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
        require(sapisid.isNotBlank()) { "Missing SAPISID cookie for YouTube Music playlist edits" }
        val timestamp = System.currentTimeMillis() / 1000L
        val input = "$timestamp $sapisid https://music.youtube.com"
        val digest = MessageDigest.getInstance("SHA-1")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "SAPISIDHASH ${timestamp}_$digest"
    }

    private fun Builder.sessionHeaders(): Builder =
        header("Accept", "*/*")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Content-Type", "application/json")
            .header("DNT", "1")
            .header("Origin", SESSION_ORIGIN)
            .header("Referer", "$SESSION_ORIGIN/")
            .header("Sec-CH-UA", "\"Google Chrome\";v=\"147\", \"Not.A/Brand\";v=\"8\", \"Chromium\";v=\"147\"")
            .header("Sec-CH-UA-Mobile", "?0")
            .header("Sec-CH-UA-Platform", "\"Windows\"")
            .header("Sec-Fetch-Dest", "empty")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Site", "same-origin")
            .header("User-Agent", SESSION_USER_AGENT)
}
