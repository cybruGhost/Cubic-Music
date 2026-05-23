package app.it.fast4x.rimusic.utils

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import app.it.fast4x.rimusic.appRunningInBackground
import androidx.compose.ui.platform.LocalContext
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmSessionApi
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmSong
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeRequestThrottler
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeSessionStore
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.YtMusic
import it.fast4x.innertube.utils.completed
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.models.Album
import app.it.fast4x.rimusic.models.Artist
import app.it.fast4x.rimusic.models.Playlist
import app.it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import app.it.fast4x.rimusic.ui.components.tab.toolbar.DynamicColor
import app.it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import app.it.fast4x.rimusic.ui.screens.settings.isYouTubeSyncEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.widget.Toast
import app.it.fast4x.rimusic.cleanPrefix
import timber.log.Timber

// ─────────────────────────────────────────────────────────────────────────────
// Module-level cache & timestamp
// ─────────────────────────────────────────────────────────────────────────────

private val ytmAlbumThumbnailCache = mutableMapOf<String, String?>()
private var lastFullAccountSyncAtMs = 0L
private var lastFullAccountSyncKey = ""
private val ytmAccountSyncMutex = Mutex()

// ─────────────────────────────────────────────────────────────────────────────
// Internal data types
// ─────────────────────────────────────────────────────────────────────────────

private data class SessionLibraryScope(
    val cookie: String,
    val authUser: String?,
    val pageId: String?
)

private val SessionLibraryScope.isBrandAccount: Boolean
    get() = !pageId.isNullOrBlank()

private val SessionLibraryScope.requiresScopedSessionResults: Boolean
    get() = !pageId.isNullOrBlank()

private data class PlaylistSyncSong(
    val song: app.it.fast4x.rimusic.models.Song,
    val setVideoId: String? = null,
    val artists: List<Artist> = emptyList(),
    val album: Album? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// Utility helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun playlistIdCandidates(vararg ids: String): List<String> =
    ids.asSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .flatMap { id -> sequenceOf(id, id.removePrefix("VL"), "VL${id.removePrefix("VL")}") }
        .distinct()
        .toList()

private fun playlistIdentityKey(playlistId: String, title: String): String =
    playlistId.removePrefix("VL").trim().ifBlank { title.trim().lowercase() }

private fun normalizedPlaylistName(name: String): String =
    cleanPrefix(name).trim().lowercase()

private fun parseRemoteSongCount(songCount: String, subtitle: String): Int =
    songCount.trim().toIntOrNull()
        ?: Regex("(\\d+)").find(subtitle)?.groupValues?.getOrNull(1)?.toIntOrNull()
        ?: 0

private fun YtmSong.asArtists(): List<Artist> {
    val names = artistsText.ifBlank { artist }
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
    val refs = artists
        .mapNotNull { ref ->
            ref.id.takeIf { it.isNotBlank() }?.let { id ->
                Artist(id = id, name = ref.name.ifBlank { names.firstOrNull() }, isYoutubeArtist = true)
            }
        }
    val idRefs = artistIds
        .mapIndexedNotNull { index, id ->
            id.takeIf { it.isNotBlank() }?.let {
                Artist(id = it, name = names.getOrNull(index) ?: names.firstOrNull(), isYoutubeArtist = true)
            }
        }
    val singleRef = artistId
        .takeIf { it.isNotBlank() }
        ?.let { Artist(id = it, name = names.firstOrNull(), isYoutubeArtist = true) }
        ?.let(::listOf)
        .orEmpty()

    return (refs + idRefs + singleRef).distinctBy { it.id }
}

private fun YtmSong.asAlbum(): Album? {
    val id = albumId.takeIf { it.isNotBlank() } ?: return null
    return Album(
        id = id,
        title = album.takeIf { it.isNotBlank() },
        authorsText = artistsText.takeIf { it.isNotBlank() } ?: artist.takeIf { it.isNotBlank() },
        thumbnailUrl = thumbnailUrl.takeIf { it.isNotBlank() } ?: thumbnail.takeIf { it.isNotBlank() },
        isYoutubeAlbum = true
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Generic safe YTM call wrapper — always off the main thread
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Wraps a suspending YTM/Innertube call with:
 *  - an explicit [Dispatchers.IO] context (fixes "Cannot access database on the main thread")
 *  - a configurable timeout
 *  - automatic retries with back-off
 */
private suspend fun <T> safeYtmCall(
    label: String,
    timeoutMs: Long = 10_000L,
    maxRetries: Int = 1,
    block: suspend () -> Result<T>,
): T? = withContext(Dispatchers.IO) {
    if (appRunningInBackground) {
        Timber.d("YTM sync call skipped in background label=%s", label)
        return@withContext null
    }
    repeat(maxRetries) { attempt ->
        try {
            return@withContext withTimeout(timeoutMs) {
                if (appRunningInBackground) {
                    Timber.d("YTM sync call aborted in background label=%s", label)
                    null
                } else {
                    block().getOrNull()
                }
            }
        } catch (error: Exception) {
            Timber.w(error, "YTM sync call failed label=%s attempt=%s/%s", label, attempt + 1, maxRetries)
            if (attempt == maxRetries - 1) return@withContext null
            if (appRunningInBackground) return@withContext null
            delay(750L * (attempt + 1))
        }
    }
    null
}

// ─────────────────────────────────────────────────────────────────────────────
// Session scope helper
// ─────────────────────────────────────────────────────────────────────────────

private fun currentSessionLibraryScope(): SessionLibraryScope? {
    val session = YouTubeSessionStore.applyCurrentSession() ?: return null
    val cookie = session.cookie.takeIf { it.isNotBlank() } ?: return null
    return SessionLibraryScope(
        cookie = cookie,
        authUser = session.authUser.trim().takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) },
        pageId = session.pageId.trim().takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) },
    )
}

private fun SessionLibraryScope.syncIdentityKey(): String =
    listOf(cookie.take(48), authUser.orEmpty(), pageId.orEmpty()).joinToString("|")

// ─────────────────────────────────────────────────────────────────────────────
// Song normalisation
// ─────────────────────────────────────────────────────────────────────────────

/**
 * All DB reads here happen inside [safeYtmCall] which runs on [Dispatchers.IO],
 * so there is no risk of a "DB on main thread" error.
 */
private suspend fun Innertube.SongItem.asNormalizedSong(): app.it.fast4x.rimusic.models.Song {
    val cleanedFallbackThumbnail = asSong.thumbnailUrl?.trim()?.takeIf { it.isNotBlank() }
    val albumId = album?.endpoint?.browseId?.trim().orEmpty()

    val albumThumbnail = if (albumId.isBlank()) {
        null
    } else {
        // All three lines below are IO-bound; caller is already on Dispatchers.IO via safeYtmCall.
        val resolvedThumbnail = ytmAlbumThumbnailCache[albumId]
            ?: withContext(Dispatchers.IO) {
                Database.albumTable.findById(albumId).first()?.thumbnailUrl?.trim()?.takeIf { it.isNotBlank() }
            }
            ?: safeYtmCall(label = "ytmAlbumThumbnail:$albumId", timeoutMs = 12_000L, maxRetries = 1) {
                YtMusic.getAlbum(albumId, withSongs = false)
            }?.album?.thumbnail?.url?.trim()?.takeIf { it.isNotBlank() }?.also { thumbnail ->
                withContext(Dispatchers.IO) {
                    Database.albumTable.upsert(
                        Album(id = albumId, title = album?.name, thumbnailUrl = thumbnail, isYoutubeAlbum = true)
                    )
                }
            }
        ytmAlbumThumbnailCache[albumId] = resolvedThumbnail
        resolvedThumbnail
    }

    return asSong.copy(
        id = key.trim(),
        title = cleanPrefix(asSong.title).trim(),
        artistsText = asSong.artistsText?.trim(),
        thumbnailUrl = albumThumbnail ?: cleanedFallbackThumbnail,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Duplicate playlist cleanup
// ─────────────────────────────────────────────────────────────────────────────

private suspend fun cleanupDuplicateYtmPlaylists() = withContext(Dispatchers.IO) {
    Database.playlistTable
        .allAsPreview()
        .first()
        .filter { it.playlist.isYoutubePlaylist }
        .groupBy { preview ->
            preview.playlist.browseId?.takeIf { it.isNotBlank() }
                ?: normalizedPlaylistName(preview.playlist.name)
        }
        .values
        .forEach { duplicates ->
            duplicates
                .sortedWith(
                    compareByDescending<app.it.fast4x.rimusic.models.PlaylistPreview> { it.songCount > 0 }
                        .thenByDescending { it.playlist.browseId?.isNotBlank() == true }
                        .thenBy { it.playlist.id }
                )
                .drop(1)
                .forEach { duplicate ->
                    Database.songPlaylistMapTable.clear(duplicate.playlist.id)
                    Database.playlistTable.delete(duplicate.playlist)
                }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// Find existing playlist — must be on IO
// ─────────────────────────────────────────────────────────────────────────────

private suspend fun findExistingYtmPlaylist(browseId: String, title: String): Playlist? =
    withContext(Dispatchers.IO) {
        val allPlaylists = Database.playlistTable.allAsPreview().first().map { it.playlist }
        val candidates = playlistIdCandidates(browseId)
        allPlaylists.firstOrNull { it.isYoutubePlaylist && !it.browseId.isNullOrBlank() && candidates.contains(it.browseId.trim()) }
            ?: allPlaylists.firstOrNull { it.isYoutubePlaylist && normalizedPlaylistName(it.name) == normalizedPlaylistName(title) }
    }

// ─────────────────────────────────────────────────────────────────────────────
// Fetch remote playlist songs
// ─────────────────────────────────────────────────────────────────────────────

private suspend fun fetchRemotePlaylistSongs(
    sessionScope: SessionLibraryScope?,
    playlistId: String,
    browseId: String,
    timeoutMs: Long = 12_000L,
): List<PlaylistSyncSong> = withContext(Dispatchers.IO) {
    val isLikedMusicPlaylist = playlistId.equals("LM", ignoreCase = true) || browseId.equals("LM", ignoreCase = true)

    val sessionSongs = sessionScope?.let { scope ->
        val directSongs = if (isLikedMusicPlaylist) {
            safeYtmCall(label = "sessionLikedSongs:LM", timeoutMs = timeoutMs) {
                YtmSessionApi.fetchLikedSongs(scope.cookie, scope.authUser, scope.pageId)
            }?.takeIf { it.isNotEmpty() }
        } else {
            playlistIdCandidates(playlistId, browseId).firstNotNullOfOrNull { candidateId ->
                safeYtmCall(label = "sessionPlaylistSongs:$candidateId", timeoutMs = timeoutMs) {
                    YtmSessionApi.fetchPlaylistSongs(scope.cookie, candidateId, scope.authUser, scope.pageId)
                }?.takeIf { it.isNotEmpty() }
            }
        }
        directSongs?.map { remoteSong ->
            PlaylistSyncSong(
                song = app.it.fast4x.rimusic.models.Song(
                    id = remoteSong.id.ifBlank { remoteSong.videoId },
                    title = cleanPrefix(remoteSong.title),
                    artistsText = remoteSong.artistsText.ifBlank { remoteSong.artist.ifBlank { null } },
                    durationText = remoteSong.durationText.ifBlank { remoteSong.duration.ifBlank { null } },
                    thumbnailUrl = remoteSong.thumbnailUrl.ifBlank { remoteSong.thumbnail.ifBlank { null } },
                    totalPlayTimeMs = 1L,
                ),
                setVideoId = remoteSong.setVideoId.ifBlank { null },
                artists = remoteSong.asArtists(),
                album = remoteSong.asAlbum()
            )
        }
    }

    if (!sessionSongs.isNullOrEmpty()) {
        return@withContext sessionSongs
            .filter { it.song.id.isNotBlank() && it.song.title.isNotBlank() }
            .distinctBy { it.song.id }
    }

    if (sessionScope?.requiresScopedSessionResults == true) {
        Timber.w(
            "Skipping unscoped playlist fallback for scoped session authUser=%s pageId=%s playlistId=%s browseId=%s",
            sessionScope.authUser,
            sessionScope.pageId,
            playlistId,
            browseId
        )
        return@withContext emptyList()
    }

    // Fallback: use YtMusic API
    safeYtmCall(label = "ytmPlaylistSongs:$browseId", timeoutMs = timeoutMs) {
        YtMusic.getPlaylist(playlistId = browseId).completed()
    }?.songs
        ?.map { PlaylistSyncSong(song = it.asNormalizedSong()) }
        ?.filter { it.song.id.isNotBlank() && it.song.title.isNotBlank() }
        ?.distinctBy { it.song.id }
        ?.takeIf { it.isNotEmpty() }
        ?: playlistIdCandidates(browseId).firstNotNullOfOrNull { candidateBrowseId ->
            safeYtmCall(label = "ytmPlaylistSongsCandidate:$candidateBrowseId", timeoutMs = timeoutMs, maxRetries = 1) {
                YtMusic.getPlaylist(playlistId = candidateBrowseId).completed()
            }?.songs
                ?.map { PlaylistSyncSong(song = it.asNormalizedSong()) }
                ?.filter { it.song.id.isNotBlank() && it.song.title.isNotBlank() }
                ?.distinctBy { it.song.id }
                ?.takeIf { it.isNotEmpty() }
        }
        .orEmpty()
}

// ─────────────────────────────────────────────────────────────────────────────
// Session playlist sync
// ─────────────────────────────────────────────────────────────────────────────

private suspend fun syncSessionPlaylists(scope: SessionLibraryScope): Boolean =
    withContext(Dispatchers.IO) {
        withTimeoutOrNull(60_000L) {
            val remotePlaylists = safeYtmCall("sessionPlaylists") {
                YtmSessionApi.fetchPlaylists(scope.cookie, scope.authUser, scope.pageId)
            }?.distinctBy { playlistIdentityKey(it.playlistId, it.title) } ?: return@withTimeoutOrNull false

            if (remotePlaylists.isEmpty()) return@withTimeoutOrNull false

            val syncedBrowseIds = mutableSetOf<String>()
            remotePlaylists.forEachIndexed { index, remotePlaylist ->
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                if (remotePlaylist.playlistId.isBlank() || remotePlaylist.title.isBlank()) return@forEachIndexed

                val remoteBrowseId = remotePlaylist.playlistId.trim()
                val localPlaylist = findExistingYtmPlaylist(browseId = remoteBrowseId, title = remotePlaylist.title)
                syncedBrowseIds += remoteBrowseId

                val playlist = (localPlaylist ?: Playlist(
                    name = remotePlaylist.title, browseId = remoteBrowseId, isYoutubePlaylist = true
                )).copy(name = remotePlaylist.title, browseId = remoteBrowseId, isYoutubePlaylist = true)

                val localSongCount = if ((localPlaylist?.id ?: 0) > 0) {
                    Database.songPlaylistMapTable.allSongsOf(localPlaylist!!.id).first().size
                } else 0

                val remoteSongCount = parseRemoteSongCount(remotePlaylist.songCount, remotePlaylist.subtitle)
                val shouldFetchSongs = localSongCount <= 0 || (remoteSongCount > 0 && localSongCount != remoteSongCount)
                if (index > 0 && shouldFetchSongs) delay(650L)

                val remoteSongs = if (!shouldFetchSongs) emptyList()
                else fetchRemotePlaylistSongs(sessionScope = scope, playlistId = remotePlaylist.playlistId, browseId = remoteBrowseId)

                Database.asyncTransaction {
                    val playlistId = if (playlist.id > 0) { playlistTable.update(playlist); playlist.id } else playlistTable.insert(playlist)
                    if (remoteSongs.isNotEmpty()) {
                        remoteSongs.forEach { remoteSong ->
                            songTable.insertIgnore(remoteSong.song)
                            remoteSong.artists.forEach { artist ->
                                artistTable.upsert(artist)
                                songArtistMapTable.insertIgnore(
                                    app.it.fast4x.rimusic.models.SongArtistMap(remoteSong.song.id, artist.id)
                                )
                            }
                            remoteSong.album?.let { album ->
                                albumTable.insertIgnore(album)
                                songAlbumMapTable.map(remoteSong.song.id, album.id)
                            }
                        }
                        songPlaylistMapTable.clear(playlistId)
                        songPlaylistMapTable.updateReplace(remoteSongs.mapIndexed { i, remoteSong ->
                            app.it.fast4x.rimusic.models.SongPlaylistMap(
                                songId = remoteSong.song.id,
                                playlistId = playlistId,
                                position = i,
                                setVideoId = remoteSong.setVideoId
                            )
                        })
                    }
                }
            }

            // Unmark local playlists that are no longer remote
            Database.playlistTable.allAsPreview().first().map { it.playlist }
                .filter { it.isYoutubePlaylist && it.browseId !in syncedBrowseIds }
                .forEach { Database.playlistTable.update(it.copy(isYoutubePlaylist = false, browseId = null)) }

            cleanupDuplicateYtmPlaylists()
            true
        } ?: false
    }

// ─────────────────────────────────────────────────────────────────────────────
// Public API
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sync a single private YTM playlist. All DB access is on [Dispatchers.IO].
 */
@OptIn(UnstableApi::class)
fun ytmPrivatePlaylistSync(playlist: Playlist, playlistId: Long) {
    if (!playlist.isYoutubePlaylist || playlist.browseId.isNullOrBlank()) {
        Timber.d("ytmPrivatePlaylistSync skipped: playlist %s is not a YT playlist", playlist.name)
        return
    }

    CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            val browseId = playlist.browseId ?: return@launch
            val remotePlaylist = safeYtmCall(label = "ytmPrivatePlaylistSync:$browseId", timeoutMs = 30_000L) {
                YtMusic.getPlaylist(playlistId = browseId).completed()
            } ?: return@launch

            val normalizedSongs = remotePlaylist.songs
                .map { it.asNormalizedSong() }
                .filter { it.id.isNotBlank() && it.title.isNotBlank() }
                .distinctBy { it.id }

            Database.asyncTransaction {
                val currentPlaylist = playlist.copy(id = playlistId, isEditable = remotePlaylist.isEditable == true || playlist.isEditable)
                playlistTable.insertIgnore(currentPlaylist)
                playlistTable.update(currentPlaylist)
                normalizedSongs.forEach(songTable::insertIgnore)
                if (normalizedSongs.isNotEmpty()) {
                    songPlaylistMapTable.clear(currentPlaylist.id)
                    songPlaylistMapTable.updateReplace(normalizedSongs.mapIndexed { index, song ->
                        app.it.fast4x.rimusic.models.SongPlaylistMap(songId = song.id, playlistId = currentPlaylist.id, position = index)
                    })
                }
            }
        }.onFailure { Timber.e(it, "ytmPrivatePlaylistSync failed playlistId=%s browseId=%s", playlistId, playlist.browseId) }
    }
}

/**
 * Import subscribed YTM artists. Guaranteed to run on [Dispatchers.IO].
 */
suspend fun importYTMSubscribedChannels(): Boolean = withContext(Dispatchers.IO) {
    if (!isYouTubeSyncEnabled()) return@withContext false

    val sessionScope = currentSessionLibraryScope() ?: return@withContext false
    val sessionArtists = sessionScope.let { scope ->
        safeYtmCall("sessionArtists") {
            YtmSessionApi.fetchArtists(scope.cookie, scope.authUser, scope.pageId)
        }
    }

    runCatching {
        val ytmArtists = sessionArtists.orEmpty()
            .filter { it.browseId.isNotBlank() && it.name.isNotBlank() }

        if (ytmArtists.isEmpty()) return@withContext false

        ytmArtists.forEach { remoteArtist ->
            val localArtist = Database.artistTable.findById(remoteArtist.browseId).first()
            if (localArtist == null) {
                Database.artistTable.upsert(
                    Artist(
                        id = remoteArtist.browseId,
                        name = remoteArtist.name,
                        thumbnailUrl = remoteArtist.thumbnail,
                        bookmarkedAt = System.currentTimeMillis(),
                        isYoutubeArtist = true
                    )
                )
            } else {
                Database.artistTable.update(
                    localArtist.copy(
                        name = remoteArtist.name,
                        thumbnailUrl = remoteArtist.thumbnail,
                        bookmarkedAt = localArtist.bookmarkedAt ?: System.currentTimeMillis(),
                        isYoutubeArtist = true
                    )
                )
            }
        }
        Database.artistTable.allFollowing().first()
            .filter { it.isYoutubeArtist && it.id !in ytmArtists.map { a -> a.browseId } }
            .map { it.copy(isYoutubeArtist = false, bookmarkedAt = null) }
            .forEach(Database.artistTable::update)
    }.onFailure {
        Timber.e(it, "importYTMSubscribedChannels failed")
        return@withContext false
    }
    true
}

/**
 * Import liked YTM albums. Guaranteed to run on [Dispatchers.IO].
 */
suspend fun importYTMLikedAlbums(): Boolean = withContext(Dispatchers.IO) {
    if (!isYouTubeSyncEnabled()) return@withContext false

    val sessionScope = currentSessionLibraryScope() ?: return@withContext false
    val sessionAlbums = sessionScope.let { scope ->
        safeYtmCall("sessionAlbums") { YtmSessionApi.fetchAlbums(scope.cookie, scope.authUser, scope.pageId) }
    }

    runCatching {
        val ytmAlbums = sessionAlbums.orEmpty()
            .filter { it.browseId.isNotBlank() && it.title.isNotBlank() }

        if (ytmAlbums.isEmpty()) return@withContext false

        ytmAlbums.forEach { remoteAlbum ->
            val localAlbum = Database.albumTable.findById(remoteAlbum.browseId).first()
            val album = (localAlbum ?: Album(
                id = remoteAlbum.browseId,
                title = remoteAlbum.title,
                authorsText = remoteAlbum.artist,
                thumbnailUrl = remoteAlbum.thumbnail,
                year = remoteAlbum.year,
                bookmarkedAt = System.currentTimeMillis(),
                isYoutubeAlbum = true,
            )).copy(
                title = remoteAlbum.title,
                authorsText = remoteAlbum.artist,
                thumbnailUrl = remoteAlbum.thumbnail,
                year = remoteAlbum.year,
                bookmarkedAt = localAlbum?.bookmarkedAt ?: System.currentTimeMillis(),
                isYoutubeAlbum = true,
            )
            if (localAlbum == null) Database.albumTable.upsert(album) else Database.albumTable.updateReplace(album)
        }
        Database.albumTable.all().first()
            .filter { it.isYoutubeAlbum && it.id !in ytmAlbums.map { a -> a.browseId } }
            .map { it.copy(isYoutubeAlbum = false, bookmarkedAt = null) }
            .also(Database.albumTable::updateReplace)
    }.onFailure {
        Timber.e(it, "importYTMLikedAlbums failed")
        return@withContext false
    }
    true
}

/**
 * Import liked YTM playlists. Guaranteed to run on [Dispatchers.IO].
 */
suspend fun importYTMLikedPlaylists(): Boolean = withContext(Dispatchers.IO) {
    if (!isYouTubeSyncEnabled()) return@withContext false

    val sessionScope = currentSessionLibraryScope() ?: return@withContext false
    syncSessionPlaylists(sessionScope)
}

/**
 * Import liked YTM songs. Guaranteed to run on [Dispatchers.IO].
 */
suspend fun importYTMLikedSongs(): Boolean = withContext(Dispatchers.IO) {
    if (!isYouTubeSyncEnabled()) return@withContext false

    val sessionScope = currentSessionLibraryScope() ?: return@withContext false
    val sessionSongs = safeYtmCall("sessionLikedSongs") {
        YtmSessionApi.fetchLikedSongs(sessionScope.cookie, sessionScope.authUser, sessionScope.pageId)
    }
    val fallbackSongs = if (sessionSongs.isNullOrEmpty() && !sessionScope.requiresScopedSessionResults) {
        fetchRemotePlaylistSongs(sessionScope = sessionScope, playlistId = "LM", browseId = "LM")
    } else emptyList()

    if (sessionSongs.isNullOrEmpty() && fallbackSongs.isEmpty()) return@withContext false

    runCatching {
        if (!sessionSongs.isNullOrEmpty()) {
            sessionSongs.forEach { remoteSong ->
                if (remoteSong.videoId.isBlank() || remoteSong.title.isBlank()) return@forEach
                val localSong = Database.songTable.findById(remoteSong.videoId).first()
                val normalizedSong = (localSong ?: app.it.fast4x.rimusic.models.Song(
                    id = remoteSong.videoId, title = cleanPrefix(remoteSong.title),
                    artistsText = remoteSong.artistsText.ifBlank { remoteSong.artist.ifBlank { null } },
                    durationText = remoteSong.durationText.ifBlank { remoteSong.duration.ifBlank { null } },
                    thumbnailUrl = remoteSong.thumbnailUrl.ifBlank { remoteSong.thumbnail.ifBlank { null } },
                )).copy(
                    title = cleanPrefix(remoteSong.title),
                    artistsText = remoteSong.artistsText.ifBlank { remoteSong.artist.ifBlank { localSong?.artistsText } },
                    durationText = remoteSong.durationText.ifBlank { remoteSong.duration.ifBlank { localSong?.durationText } },
                    thumbnailUrl = remoteSong.thumbnailUrl.ifBlank { remoteSong.thumbnail.ifBlank { localSong?.thumbnailUrl } },
                    likedAt = localSong?.likedAt?.takeIf { it > 0 } ?: System.currentTimeMillis(),
                )
                Database.songTable.upsert(normalizedSong)
                remoteSong.asArtists().forEach { artist ->
                    Database.artistTable.upsert(artist)
                    Database.songArtistMapTable.insertIgnore(
                        app.it.fast4x.rimusic.models.SongArtistMap(normalizedSong.id, artist.id)
                    )
                }
                remoteSong.asAlbum()?.let { album ->
                    Database.albumTable.insertIgnore(album)
                    Database.songAlbumMapTable.map(normalizedSong.id, album.id)
                }
            }
        } else {
            fallbackSongs.forEach { remoteSong ->
                val song = remoteSong.song
                val localSong = Database.songTable.findById(song.id).first()
                Database.songTable.upsert(
                    (localSong ?: song).copy(
                        title = cleanPrefix((localSong ?: song).title),
                        likedAt = localSong?.likedAt?.takeIf { it > 0 } ?: System.currentTimeMillis(),
                    )
                )
            }
        }
    }.onFailure {
        Timber.e(it, "importYTMLikedSongs failed")
        return@withContext false
    }
    true
}

/**
 * Full account sync. Guaranteed to run on [Dispatchers.IO].
 * Rate-limited to once per minute.
 */
suspend fun syncSelectedYtmAccountData(): Boolean = withContext(Dispatchers.IO) {
    if (!isYouTubeSyncEnabled()) return@withContext false
    if (appRunningInBackground) return@withContext false
    val sessionScope = currentSessionLibraryScope() ?: return@withContext false
    ytmAccountSyncMutex.withLock {
        if (appRunningInBackground) return@withLock false

        val now = System.currentTimeMillis()
        val syncKey = sessionScope.syncIdentityKey()
        if (syncKey == lastFullAccountSyncKey && now - lastFullAccountSyncAtMs < 60_000L) {
            return@withLock false
        }

        val syncSteps = listOf(
            "liked songs" to ::importYTMLikedSongs,
            "playlists" to ::importYTMLikedPlaylists,
            "artists" to ::importYTMSubscribedChannels,
            "albums" to ::importYTMLikedAlbums,
        )

        var syncedAny = false
        syncSteps.forEach { (label, syncStep) ->
            if (appRunningInBackground) return@withLock syncedAny
            runCatching { syncStep() }
                .onSuccess { syncedAny = syncedAny || it }
                .onFailure { Timber.e(it, "syncSelectedYtmAccountData failed while syncing %s", label) }
        }

        if (syncedAny) {
            lastFullAccountSyncAtMs = now
            lastFullAccountSyncKey = syncKey
        }
        syncedAny
    }
}

/**
 * Remove a song from a YTM playlist. Guaranteed to run on [Dispatchers.IO].
 */
suspend fun removeYTSongFromPlaylist(songId: String, playlistBrowseId: String, playlistId: Long): Boolean =
    withContext(Dispatchers.IO) {
        if (!isYouTubeSyncEnabled()) return@withContext false
        val setVideoId = Database.songPlaylistMapTable.findById(songId, playlistId).first()?.setVideoId
            ?: return@withContext false
        YtMusic.removeFromPlaylist(playlistBrowseId, songId, setVideoId)
        true
    }

// ─────────────────────────────────────────────────────────────────────────────
// Toolbar button composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun autoSyncToolbutton(
    messageId: Int,
    onSyncNow: (() -> Unit)? = null
): MenuIcon {

    val context = LocalContext.current  // ✅ get it here

    return object : MenuIcon, DynamicColor, Descriptive {
        override var isFirstColor: Boolean by rememberPreference(autosyncKey, false)
        override val iconId: Int = R.drawable.sync
        override val messageId: Int = messageId
        override val menuIconTitle: String
            @Composable get() = stringResource(messageId)

        override fun onShortClick() {
            if (onSyncNow != null) onSyncNow() else isFirstColor = !isFirstColor
        }

        override fun onLongClick() {
            isFirstColor = !isFirstColor

            Toast.makeText(
                context,
                context.getString(messageId),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
