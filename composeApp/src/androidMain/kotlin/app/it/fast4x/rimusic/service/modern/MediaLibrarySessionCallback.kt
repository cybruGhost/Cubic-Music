package app.it.fast4x.rimusic.service.modern

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import app.kreate.android.R
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.BrowseBody
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.models.bodies.ContinuationBody
import it.fast4x.innertube.requests.searchPage
import it.fast4x.innertube.requests.artistPage
import it.fast4x.innertube.requests.albumPage
import it.fast4x.innertube.requests.playlistPage
import it.fast4x.innertube.YtMusic
import it.fast4x.innertube.models.BrowseEndpoint
import it.fast4x.innertube.utils.from
import androidx.core.net.toUri
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.enums.MaxTopPlaylistItems
import app.it.fast4x.rimusic.enums.SongSortBy
import app.it.fast4x.rimusic.enums.SortOrder
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.service.MyDownloadHelper
import app.it.fast4x.rimusic.repository.QuickPicksRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import app.it.fast4x.rimusic.MONTHLY_PREFIX
import app.it.fast4x.rimusic.PINNED_PREFIX
import app.it.fast4x.rimusic.PIPED_PREFIX
import app.it.fast4x.rimusic.LOCAL_KEY_PREFIX
import app.it.fast4x.rimusic.service.modern.MediaSessionConstants.ID_ALBUMS_FAVORITES
import app.it.fast4x.rimusic.service.modern.MediaSessionConstants.ID_ALBUMS_LIBRARY
import app.it.fast4x.rimusic.service.modern.MediaSessionConstants.ID_ARTISTS_FAVORITES
import app.it.fast4x.rimusic.service.modern.MediaSessionConstants.ID_ARTISTS_LIBRARY
import app.it.fast4x.rimusic.service.modern.MediaSessionConstants.ID_PLAYLISTS_LOCAL
import app.it.fast4x.rimusic.service.modern.MediaSessionConstants.ID_PLAYLISTS_MONTHLY
import app.it.fast4x.rimusic.service.modern.MediaSessionConstants.ID_PLAYLISTS_PINNED
import app.it.fast4x.rimusic.service.modern.MediaSessionConstants.ID_PLAYLISTS_PIPED
import app.it.fast4x.rimusic.service.modern.MediaSessionConstants.ID_PLAYLISTS_YT
import app.it.fast4x.rimusic.service.modern.MediaSessionConstants.ID_QUICK_PICKS
import app.it.fast4x.rimusic.utils.MaxTopPlaylistItemsKey
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.showMonthlyPlaylistsKey
import app.it.fast4x.rimusic.utils.showPipedPlaylistsKey
import app.it.fast4x.rimusic.utils.showPinnedPlaylistsKey
import app.it.fast4x.rimusic.utils.showFavoritesPlaylistKey
import app.it.fast4x.rimusic.utils.showCachedPlaylistKey
import app.it.fast4x.rimusic.utils.showDownloadedPlaylistKey
import app.it.fast4x.rimusic.utils.showOnDevicePlaylistKey
import app.it.fast4x.rimusic.utils.getEnum
import app.it.fast4x.rimusic.utils.persistentQueueKey
import app.it.fast4x.rimusic.utils.preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.future
import app.kreate.android.me.knighthat.database.ext.FormatWithSong
import it.fast4x.innertube.models.NavigationEndpoint
import kotlinx.coroutines.flow.Flow
import it.fast4x.innertube.models.MusicShelfRenderer

@UnstableApi
class MediaLibrarySessionCallback(
    val context: Context,
    val database: Database,
    val downloadHelper: MyDownloadHelper
) : MediaLibrarySession.Callback {
    private val scope = CoroutineScope(Dispatchers.Main) + Job()
    private var observationJob: Job? = null
    lateinit var binder: PlayerServiceModern.Binder
    var toggleLike: () -> Unit = {}
    var toggleDownload: () -> Unit = {}
    var toggleRepeat: () -> Unit = {}
    var toggleShuffle: () -> Unit = {}
    var startRadio: () -> Unit = {}
    var callPause: () -> Unit = {}
    var actionSearch: () -> Unit = {}
    
    var searchedSongs: List<Song> = emptyList()
    var searchedArtists: List<Innertube.ArtistItem> = emptyList()
    var searchedVideos: List<Innertube.VideoItem> = emptyList()
    var searchedAlbums: List<Innertube.AlbumItem> = emptyList()

    private val searchCache = mutableMapOf<String, List<MediaItem>>()

    fun observeRepository(session: MediaLibrarySession) {
        observationJob?.cancel()
        observationJob = scope.launch {
            combine(
                QuickPicksRepository.trendingList,
                QuickPicksRepository.relatedPage,
                database.artistTable.allFollowing(),
                database.albumTable.all(),
                database.songTable.all(),
                database.eventTable.findSongsMostPlayedBetween(0L),
                database.playlistTable.allAsPreview(),
                downloadHelper.downloads,
                database.formatTable.allWithSongs()
            ) { _ -> Unit }.collect {
                session.notifyChildrenChanged(PlayerServiceModern.ROOT, 0, null)
                session.notifyChildrenChanged(ID_QUICK_PICKS, 0, null)
                session.notifyChildrenChanged(PlayerServiceModern.SONG, 0, null)
                session.notifyChildrenChanged(PlayerServiceModern.ARTIST, 0, null)
                session.notifyChildrenChanged(PlayerServiceModern.ALBUM, 0, null)
                session.notifyChildrenChanged(PlayerServiceModern.PLAYLIST, 0, null)
            }
        }
    }

    fun release() {
        observationJob?.cancel()
        scope.cancel()
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        return MediaSession.ConnectionResult.accept(
            connectionResult.availableSessionCommands.buildUpon()
                .add(MediaSessionConstants.CommandToggleDownload)
                .add(MediaSessionConstants.CommandToggleLike)
                .add(MediaSessionConstants.CommandToggleShuffle)
                .add(MediaSessionConstants.CommandToggleRepeatMode)
                .add(MediaSessionConstants.CommandStartRadio)
                .add(MediaSessionConstants.CommandSearch)
                .build(),
            connectionResult.availablePlayerCommands.buildUpon()
                .add(androidx.media3.common.Player.COMMAND_PLAY_PAUSE)
                .add(androidx.media3.common.Player.COMMAND_PREPARE)
                .add(androidx.media3.common.Player.COMMAND_STOP)
                .build()
        )
    }

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        println("PlayerServiceModern MediaLibrarySessionCallback.onSearch: $query")
        searchCache.clear()
        session.notifySearchResultChanged(browser, query, 0, params)
        return Futures.immediateFuture(LibraryResult.ofVoid(params))
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val results = listOf(
            MediaItemMapper.browsableMediaItem("${MediaSessionConstants.ID_SEARCH_SONGS}/$query", context.getString(R.string.songs), null, MediaItemMapper.drawableUri(context, R.drawable.musical_notes), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
            MediaItemMapper.browsableMediaItem("${MediaSessionConstants.ID_SEARCH_ALBUMS}/$query", context.getString(R.string.albums), null, MediaItemMapper.drawableUri(context, R.drawable.album), MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
            MediaItemMapper.browsableMediaItem("${MediaSessionConstants.ID_SEARCH_ARTISTS}/$query", context.getString(R.string.artists), null, MediaItemMapper.drawableUri(context, R.drawable.artist), MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
            MediaItemMapper.browsableMediaItem("${MediaSessionConstants.ID_SEARCH_VIDEOS}/$query", context.getString(R.string.videos), null, MediaItemMapper.drawableUri(context, R.drawable.video), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
            MediaItemMapper.browsableMediaItem("${MediaSessionConstants.ID_SEARCH_PLAYLISTS}/$query", context.getString(R.string.playlists), null, MediaItemMapper.drawableUri(context, R.drawable.library), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
            MediaItemMapper.browsableMediaItem("${MediaSessionConstants.ID_SEARCH_FEATURED}/$query", context.getString(R.string.featured), null, MediaItemMapper.drawableUri(context, R.drawable.featured_playlist), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
            MediaItemMapper.browsableMediaItem("${MediaSessionConstants.ID_SEARCH_PODCASTS}/$query", context.getString(R.string.podcasts), null, MediaItemMapper.drawableUri(context, R.drawable.podcast), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
        )
        return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(results), params))
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            MediaSessionConstants.ACTION_TOGGLE_LIKE -> toggleLike()
            MediaSessionConstants.ACTION_TOGGLE_DOWNLOAD -> toggleDownload()
            MediaSessionConstants.ACTION_TOGGLE_SHUFFLE -> toggleShuffle()
            MediaSessionConstants.ACTION_TOGGLE_REPEAT_MODE -> toggleRepeat()
            MediaSessionConstants.ACTION_START_RADIO -> startRadio()
            MediaSessionConstants.ACTION_SEARCH -> actionSearch()
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    @OptIn(UnstableApi::class)
    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(
        LibraryResult.ofItem(
            MediaItem.Builder()
                .setMediaId(PlayerServiceModern.ROOT)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsPlayable(false)
                        .setIsBrowsable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build(),
            params
        )
    )

    @OptIn(UnstableApi::class)
    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future(Dispatchers.IO) {
        val cached = searchCache[parentId]
        if (!cached.isNullOrEmpty()) {
            return@future LibraryResult.ofItemList(ImmutableList.copyOf(cached), params)
        }

        val list: List<MediaItem> = try {
            when (parentId) {
                PlayerServiceModern.ROOT -> {
                    listOf(
                        MediaItemMapper.browsableMediaItem(MediaSessionConstants.ID_QUICK_PICKS, context.getString(R.string.quick_picks), null, MediaItemMapper.drawableUri(context, R.drawable.sparkles), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
                        MediaItemMapper.browsableMediaItem(PlayerServiceModern.SONG, context.getString(R.string.songs), null, MediaItemMapper.drawableUri(context, R.drawable.musical_notes), MediaMetadata.MEDIA_TYPE_PLAYLIST),
                        MediaItemMapper.browsableMediaItem(PlayerServiceModern.ARTIST, context.getString(R.string.artists), null, MediaItemMapper.drawableUri(context, R.drawable.artist), MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
                        MediaItemMapper.browsableMediaItem(PlayerServiceModern.ALBUM, context.getString(R.string.albums), null, MediaItemMapper.drawableUri(context, R.drawable.album), MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
                        MediaItemMapper.browsableMediaItem(PlayerServiceModern.PLAYLIST, context.getString(R.string.library), null, MediaItemMapper.drawableUri(context, R.drawable.library), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                    )
                }
                MediaSessionConstants.ID_QUICK_PICKS -> {
                    val luckyItem = MediaItem.Builder().setMediaId(MediaSessionConstants.ID_LUCKY_SHUFFLE).setMediaMetadata(MediaMetadata.Builder().setTitle(context.getString(R.string.lucky_shuffle)).setArtworkUri(MediaItemMapper.drawableUri(context, R.drawable.smart_shuffle)).setIsPlayable(true).setIsBrowsable(false).setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC).build()).build()
                    val trending = QuickPicksRepository.trendingList.value.map { song -> MediaItemMapper.mapSongToMediaItem(song, parentId) }
                    val related = QuickPicksRepository.relatedPage.value?.songs?.map { item -> MediaItemMapper.mapSongToMediaItem(item.asSong, parentId) } ?: emptyList()
                    (listOf(luckyItem) + (trending + related).distinctBy { it.mediaId })
                }
                PlayerServiceModern.SONG -> {
                    val showFavoritesPlaylist = try { context.preferences.getBoolean(showFavoritesPlaylistKey, true) } catch (e: Exception) { true }
                    val showDownloadedPlaylist = try { context.preferences.getBoolean(showDownloadedPlaylistKey, true) } catch (e: Exception) { true }
                    val showCachedPlaylist = try { context.preferences.getBoolean(showCachedPlaylistKey, true) } catch (e: Exception) { true }
                    val showOnDevicePlaylist = try { context.preferences.getBoolean(showOnDevicePlaylistKey, true) } catch (e: Exception) { true }
                    val allCount = database.songTable.sortAll(SongSortBy.DateAdded, SortOrder.Descending, excludeHidden = true).first().size
                    val favoritesCount = database.songTable.allFavorites().first().size
                    val downloadedCount = (getCountDownloadedSongs() as Flow<Int>).first()
                    val onDeviceCount = database.songTable.allOnDevice().first().size
                    val cachedCount = (getCountCachedSongs() as Flow<Int>).first()
                    val topCount = database.eventTable.findSongsMostPlayedBetween(from = 0, limit = context.preferences.getEnum(MaxTopPlaylistItemsKey, MaxTopPlaylistItems.`10`).toInt()).first().size
                    val songs = mutableListOf(
                        MediaItemMapper.browsableMediaItem(MediaSessionConstants.ID_SONGS_ALL, context.getString(R.string.all), allCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.musical_notes), MediaMetadata.MEDIA_TYPE_PLAYLIST)
                    )
                    if (showFavoritesPlaylist) {
                        songs.add(MediaItemMapper.browsableMediaItem(MediaSessionConstants.ID_SONGS_FAVORITES, context.getString(R.string.favorites), favoritesCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.heart), MediaMetadata.MEDIA_TYPE_PLAYLIST))
                    }
                    if (showDownloadedPlaylist) {
                        songs.add(MediaItemMapper.browsableMediaItem(MediaSessionConstants.ID_SONGS_DOWNLOADED, context.getString(R.string.downloaded), downloadedCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.downloaded), MediaMetadata.MEDIA_TYPE_PLAYLIST))
                    }
                    if (showCachedPlaylist) {
                        songs.add(MediaItemMapper.browsableMediaItem(MediaSessionConstants.ID_SONGS_CACHED, context.getString(R.string.cached), cachedCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.download), MediaMetadata.MEDIA_TYPE_PLAYLIST))
                    }
                    if (showOnDevicePlaylist) {
                        songs.add(MediaItemMapper.browsableMediaItem(MediaSessionConstants.ID_SONGS_ONDEVICE, context.getString(R.string.on_device), onDeviceCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.devices), MediaMetadata.MEDIA_TYPE_PLAYLIST))
                    }
                    songs.add(MediaItemMapper.browsableMediaItem(MediaSessionConstants.ID_SONGS_TOP, context.getString(R.string.playlist_top), topCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.trending), MediaMetadata.MEDIA_TYPE_PLAYLIST))
                    songs
                }
                PlayerServiceModern.ARTIST -> {
                    val libraryCount = database.artistTable.allInLibrary().first().size
                    val favoritesCount = database.artistTable.allFollowing().first().size
                    listOf(
                        MediaItemMapper.browsableMediaItem(MediaSessionConstants.ID_ARTISTS_LIBRARY, context.getString(R.string.library), libraryCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.artist), MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
                        MediaItemMapper.browsableMediaItem(MediaSessionConstants.ID_ARTISTS_FAVORITES, context.getString(R.string.favorites), favoritesCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.heart), MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS)
                    )
                }
                MediaSessionConstants.ID_SONGS_TOP -> {
                    val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_SONGS_TOP_SHUFFLE)
                    val songs = database.eventTable.findSongsMostPlayedBetween(from = 0, limit = context.preferences.getEnum(MaxTopPlaylistItemsKey, MaxTopPlaylistItems.`10`).toInt()).first().map { song -> MediaItemMapper.mapSongToMediaItem(song, parentId) }
                    listOf(shuffleItem) + songs
                }
                MediaSessionConstants.ID_SONGS_ALL -> {
                    val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_SONGS_ALL_SHUFFLE)
                    val songs = database.songTable.sortAll(SongSortBy.DateAdded, SortOrder.Descending, excludeHidden = true).first().map { song -> MediaItemMapper.mapSongToMediaItem(song, parentId) }
                    listOf(shuffleItem) + songs
                }
                MediaSessionConstants.ID_SONGS_FAVORITES -> {
                    val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_SONGS_FAVORITES_SHUFFLE)
                    val songs = database.songTable.allFavorites().first().reversed().map { song -> MediaItemMapper.mapSongToMediaItem(song, parentId) }
                    listOf(shuffleItem) + songs
                }
                MediaSessionConstants.ID_SONGS_DOWNLOADED -> {
                    val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_SONGS_DOWNLOADED_SHUFFLE)
                    downloadHelper.getDownloadManager(context)
                    val downloads = downloadHelper.downloads.value
                    val songs = database.songTable.all(excludeHidden = false).first().fastFilter { song -> downloads[song.id]?.state == Download.STATE_COMPLETED }.sortedByDescending { song -> downloads[song.id]?.updateTimeMs ?: 0L }.map { song -> MediaItemMapper.mapSongToMediaItem(song, parentId) }
                    listOf(shuffleItem) + songs
                }
                MediaSessionConstants.ID_SONGS_ONDEVICE -> {
                    val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_SONGS_ONDEVICE_SHUFFLE)
                    val songs = database.songTable.allOnDevice().first().map { song -> MediaItemMapper.mapSongToMediaItem(song, parentId) }
                    listOf(shuffleItem) + songs
                }
                MediaSessionConstants.ID_SONGS_CACHED -> {
                    val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_SONGS_CACHED_SHUFFLE)
                    val songs = database.formatTable.allWithSongs().first().fastFilter { itf -> itf.song.totalPlayTimeMs > 0 && itf.format.contentLength != null && (if (::binder.isInitialized) binder.cache.isCached(itf.song.id, 0L, itf.format.contentLength ?: 0L) else false) }.reversed().fastMap { itf -> itf.song }.map { song -> MediaItemMapper.mapSongToMediaItem(song, parentId) }
                    listOf(shuffleItem) + songs
                }
                MediaSessionConstants.ID_ARTISTS_LIBRARY -> {
                    val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_ARTISTS_LIBRARY_SHUFFLE)
                    val artists = database.artistTable.allInLibrary().first().map { artist -> MediaItemMapper.browsableMediaItem("${PlayerServiceModern.ARTIST}/${artist.id}", artist.name ?: "", null, MediaItemMapper.drawableUri(context, R.drawable.artist), MediaMetadata.MEDIA_TYPE_ARTIST) }
                    listOf(shuffleItem) + artists
                }
                MediaSessionConstants.ID_ARTISTS_FAVORITES -> {
                    val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_ARTISTS_FAVORITES_SHUFFLE)
                    val artists = database.artistTable.allFollowing().first().map { artist -> MediaItemMapper.browsableMediaItem("${PlayerServiceModern.ARTIST}/${artist.id}", artist.name ?: "", null, MediaItemMapper.drawableUri(context, R.drawable.artist), MediaMetadata.MEDIA_TYPE_ARTIST) }
                    listOf(shuffleItem) + artists
                }
                PlayerServiceModern.ALBUM -> {
                    val libraryCount = database.albumTable.allInLibrary().first().size
                    val favoritesCount = database.albumTable.allBookmarked().first().size
                    listOf(
                        MediaItemMapper.browsableMediaItem(MediaSessionConstants.ID_ALBUMS_LIBRARY, context.getString(R.string.library), libraryCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.album), MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
                        MediaItemMapper.browsableMediaItem(MediaSessionConstants.ID_ALBUMS_FAVORITES, context.getString(R.string.favorites), favoritesCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.heart), MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                    )
                }
                PlayerServiceModern.PLAYLIST -> {
                    val showMonthlyPlaylists = try { context.preferences.getBoolean(showMonthlyPlaylistsKey, true) } catch (e: Exception) { true }
                    val showPipedPlaylists = try { context.preferences.getBoolean(showPipedPlaylistsKey, true) } catch (e: Exception) { true }
                    val showPinnedPlaylists = try { context.preferences.getBoolean(showPinnedPlaylistsKey, true) } catch (e: Exception) { true }
                    val pinnedCount = database.playlistTable.allAsPreview().first().filter { it.playlist.name.startsWith(PINNED_PREFIX, true) }.size
                    val localCount = database.playlistTable.allAsPreview().first().filter { !it.playlist.isYoutubePlaylist && !it.playlist.name.startsWith(PIPED_PREFIX, true) && !it.playlist.name.startsWith(PINNED_PREFIX, true) && !it.playlist.name.startsWith(MONTHLY_PREFIX, true) }.size
                    val ytCount = database.playlistTable.allAsPreview().first().filter { it.playlist.isYoutubePlaylist }.size
                    val pipedCount = database.playlistTable.allAsPreview().first().filter { it.playlist.name.startsWith(PIPED_PREFIX, true) }.size
                    val monthlyCount = database.playlistTable.allAsPreview().first().filter { it.playlist.name.startsWith(MONTHLY_PREFIX, true) }.size
                    val playlists = mutableListOf<MediaItem>()
                    if (showPinnedPlaylists) {
                        playlists.add(MediaItemMapper.browsableMediaItem(ID_PLAYLISTS_PINNED, context.getString(R.string.pinned_playlists), pinnedCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.pin_filled), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS))
                    }
                    playlists.add(MediaItemMapper.browsableMediaItem(ID_PLAYLISTS_LOCAL, context.getString(R.string.library), localCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.library), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS))
                    playlists.add(MediaItemMapper.browsableMediaItem(ID_PLAYLISTS_YT, context.getString(R.string.ytm_playlists), ytCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.ytmusic), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS))
                    if (showPipedPlaylists) {
                        playlists.add(MediaItemMapper.browsableMediaItem(ID_PLAYLISTS_PIPED, context.getString(R.string.piped_playlists), pipedCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.piped_logo), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS))
                    }
                    if (showMonthlyPlaylists) {
                        playlists.add(MediaItemMapper.browsableMediaItem(ID_PLAYLISTS_MONTHLY, context.getString(R.string.monthly_playlists), monthlyCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.calendar), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS))
                    }
                    playlists
                }
                MediaSessionConstants.ID_ALBUMS_LIBRARY -> {
                    val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_ALBUMS_LIBRARY_SHUFFLE)
                    val albums = database.albumTable.allInLibrary().first().map { album -> MediaItemMapper.mapAlbumToMediaItem(PlayerServiceModern.ALBUM, album.id, album.title ?: "", album.authorsText, album.thumbnailUrl) }
                    listOf(shuffleItem) + albums
                }
                MediaSessionConstants.ID_ALBUMS_FAVORITES -> {
                    val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_ALBUMS_FAVORITES_SHUFFLE)
                    val albums = database.albumTable.allBookmarked().first().map { album -> MediaItemMapper.mapAlbumToMediaItem(PlayerServiceModern.ALBUM, album.id, album.title ?: "", album.authorsText, album.thumbnailUrl) }
                    listOf(shuffleItem) + albums
                }
                ID_PLAYLISTS_LOCAL -> {
                    val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_PLAYLISTS_LOCAL_SHUFFLE)
                    val playlists = database.playlistTable.allAsPreview().first().filter { !it.playlist.isYoutubePlaylist && !it.playlist.name.startsWith(PIPED_PREFIX, true) && !it.playlist.name.startsWith(PINNED_PREFIX, true) && !it.playlist.name.startsWith(MONTHLY_PREFIX, true) }.map { preview -> MediaItemMapper.browsableMediaItem("${PlayerServiceModern.PLAYLIST}/${preview.playlist.id}", preview.playlist.name, preview.songCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.library), MediaMetadata.MEDIA_TYPE_PLAYLIST) }
                    listOf(shuffleItem) + playlists
                }
                ID_PLAYLISTS_YT -> {
                    val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_PLAYLISTS_YT_SHUFFLE)
                    val playlists = database.playlistTable.allAsPreview().first().filter { it.playlist.isYoutubePlaylist }.map { preview -> MediaItemMapper.browsableMediaItem("${PlayerServiceModern.PLAYLIST}/${preview.playlist.id}", preview.playlist.name, preview.songCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.library), MediaMetadata.MEDIA_TYPE_PLAYLIST) }
                    listOf(shuffleItem) + playlists
                }
                ID_PLAYLISTS_PIPED -> {
                    val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_PLAYLISTS_PIPED_SHUFFLE)
                    val playlists = database.playlistTable.allAsPreview().first().filter { it.playlist.name.startsWith(PIPED_PREFIX, true) }.map { preview -> MediaItemMapper.browsableMediaItem("${PlayerServiceModern.PLAYLIST}/${preview.playlist.id}", preview.playlist.name, preview.songCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.library), MediaMetadata.MEDIA_TYPE_PLAYLIST) }
                    listOf(shuffleItem) + playlists
                }
                ID_PLAYLISTS_PINNED -> {
                    val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_PLAYLISTS_PINNED_SHUFFLE)
                    val playlists = database.playlistTable.allAsPreview().first().filter { it.playlist.name.startsWith(PINNED_PREFIX, true) }.map { preview -> MediaItemMapper.browsableMediaItem("${PlayerServiceModern.PLAYLIST}/${preview.playlist.id}", preview.playlist.name, preview.songCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.library), MediaMetadata.MEDIA_TYPE_PLAYLIST) }
                    listOf(shuffleItem) + playlists
                }
                ID_PLAYLISTS_MONTHLY -> {
                    val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_PLAYLISTS_MONTHLY_SHUFFLE)
                    val playlists = database.playlistTable.allAsPreview().first().filter { it.playlist.name.startsWith(MONTHLY_PREFIX, true) }.map { preview -> MediaItemMapper.browsableMediaItem("${PlayerServiceModern.PLAYLIST}/${preview.playlist.id}", preview.playlist.name, preview.songCount.toString(), MediaItemMapper.drawableUri(context, R.drawable.library), MediaMetadata.MEDIA_TYPE_PLAYLIST) }
                    listOf(shuffleItem) + playlists
                }

                else -> {
                    val parts = parentId.split("/")
                    when (parts[0]) {
                        MediaSessionConstants.ID_SEARCH_SONGS -> {
                            val allMapped = mutableListOf<MediaItem>()
                            var cont: String? = null
                            do {
                                val resultPage = if (cont == null) {
                                    Innertube.searchPage<Innertube.SongItem>(SearchBody(query = parts[1], params = Innertube.SearchFilter.Song.value), { content -> Innertube.SongItem.from(content) })?.getOrNull()
                                } else {
                                    Innertube.searchPage<Innertube.SongItem>(ContinuationBody(continuation = cont), { content -> Innertube.SongItem.from(content) })?.getOrNull()
                                }
                                val songs = resultPage?.items?.map { s -> s.asSong } ?: emptyList()
                                searchedSongs = (searchedSongs + songs).distinctBy { s -> s.id }
                                allMapped.addAll(songs.map { s -> MediaItemMapper.mapSongToMediaItem(s, parentId) })
                                cont = resultPage?.continuation
                            } while (cont != null && allMapped.size < 150)
                            allMapped
                        }
                        MediaSessionConstants.ID_SEARCH_ARTISTS -> {
                            val allMapped = mutableListOf<MediaItem>()
                            var cont: String? = null
                            do {
                                val resultPage = if (cont == null) {
                                    Innertube.searchPage<Innertube.ArtistItem>(SearchBody(query = parts[1], params = Innertube.SearchFilter.Artist.value), { content -> Innertube.ArtistItem.from(content) })?.getOrNull()
                                } else {
                                    Innertube.searchPage<Innertube.ArtistItem>(ContinuationBody(continuation = cont), { content -> Innertube.ArtistItem.from(content) })?.getOrNull()
                                }
                                val items = resultPage?.items ?: emptyList()
                                allMapped.addAll(items.map { ai -> MediaItemMapper.mapArtistToMediaItem(PlayerServiceModern.ARTIST, ai.key ?: "", ai.info?.name ?: "", ai.thumbnail?.url, ai.subscribersCountText, parentId) })
                                cont = resultPage?.continuation
                            } while (cont != null && allMapped.size < 150)
                            allMapped
                        }
                        MediaSessionConstants.ID_SEARCH_ALBUMS -> {
                            val allMapped = mutableListOf<MediaItem>()
                            var cont: String? = null
                            do {
                                val resultPage = if (cont == null) {
                                    Innertube.searchPage<Innertube.AlbumItem>(SearchBody(query = parts[1], params = Innertube.SearchFilter.Album.value), { content -> Innertube.AlbumItem.from(content) })?.getOrNull()
                                } else {
                                    Innertube.searchPage<Innertube.AlbumItem>(ContinuationBody(continuation = cont), { content -> Innertube.AlbumItem.from(content) })?.getOrNull()
                                }
                                val items = resultPage?.items ?: emptyList()
                                allMapped.addAll(items.map { ali -> MediaItemMapper.mapAlbumToMediaItem(PlayerServiceModern.ALBUM, ali.key ?: "", ali.info?.name ?: "", ali.authors?.joinToString(", ") { a -> a.name ?: "" }, ali.thumbnail?.url, parentId) })
                                cont = resultPage?.continuation
                            } while (cont != null && allMapped.size < 150)
                            allMapped
                        }
                        MediaSessionConstants.ID_SEARCH_VIDEOS -> {
                            val allMapped = mutableListOf<MediaItem>()
                            var cont: String? = null
                            do {
                                val resultPage = if (cont == null) {
                                    Innertube.searchPage<Innertube.VideoItem>(SearchBody(query = parts[1], params = Innertube.SearchFilter.Video.value), { content -> Innertube.VideoItem.from(content) })?.getOrNull()
                                } else {
                                    Innertube.searchPage<Innertube.VideoItem>(ContinuationBody(continuation = cont), { content -> Innertube.VideoItem.from(content) })?.getOrNull()
                                }
                                val items = resultPage?.items ?: emptyList()
                                val songs = items.map { it.asSong }
                                searchedVideos = (searchedVideos + items).distinctBy { it.key }
                                allMapped.addAll(songs.map { s -> MediaItemMapper.mapSongToMediaItem(s, parentId) })
                                cont = resultPage?.continuation
                            } while (cont != null && allMapped.size < 150)
                            allMapped
                        }
                        MediaSessionConstants.ID_SEARCH_PLAYLISTS -> {
                            val allMapped = mutableListOf<MediaItem>()
                            var cont: String? = null
                            do {
                                val resultPage = if (cont == null) {
                                    Innertube.searchPage<Innertube.PlaylistItem>(SearchBody(query = parts[1], params = Innertube.SearchFilter.CommunityPlaylist.value), { content -> Innertube.PlaylistItem.from(content) })?.getOrNull()
                                } else {
                                    Innertube.searchPage<Innertube.PlaylistItem>(ContinuationBody(continuation = cont), { content -> Innertube.PlaylistItem.from(content) })?.getOrNull()
                                }
                                val items = resultPage?.items ?: emptyList()
                                allMapped.addAll(items.map { pi -> MediaItemMapper.browsableMediaItem("${PlayerServiceModern.PLAYLIST}/${pi.key}", pi.info?.name ?: "", null, pi.thumbnail?.url?.toUri(), MediaMetadata.MEDIA_TYPE_PLAYLIST, parentId) })
                                cont = resultPage?.continuation
                            } while (cont != null && allMapped.size < 150)
                            allMapped
                        }
                        MediaSessionConstants.ID_SEARCH_FEATURED -> {
                            val allMapped = mutableListOf<MediaItem>()
                            var cont: String? = null
                            do {
                                val resultPage = if (cont == null) {
                                    Innertube.searchPage<Innertube.PlaylistItem>(SearchBody(query = parts[1], params = Innertube.SearchFilter.FeaturedPlaylist.value), { content -> Innertube.PlaylistItem.from(content) })?.getOrNull()
                                } else {
                                    Innertube.searchPage<Innertube.PlaylistItem>(ContinuationBody(continuation = cont), { content -> Innertube.PlaylistItem.from(content) })?.getOrNull()
                                }
                                val items = resultPage?.items ?: emptyList()
                                allMapped.addAll(items.map { pi -> MediaItemMapper.browsableMediaItem("${PlayerServiceModern.PLAYLIST}/${pi.key}", pi.info?.name ?: "", null, pi.thumbnail?.url?.toUri(), MediaMetadata.MEDIA_TYPE_PLAYLIST, parentId) })
                                cont = resultPage?.continuation
                            } while (cont != null && allMapped.size < 150)
                            allMapped
                        }
                        MediaSessionConstants.ID_SEARCH_PODCASTS -> {
                            val allMapped = mutableListOf<MediaItem>()
                            var cont: String? = null
                            do {
                                val resultPage = if (cont == null) {
                                    Innertube.searchPage<Innertube.AlbumItem>(SearchBody(query = parts[1], params = Innertube.SearchFilter.Podcast.value), { content -> Innertube.AlbumItem.from(content) })?.getOrNull()
                                } else {
                                    Innertube.searchPage<Innertube.AlbumItem>(ContinuationBody(continuation = cont), { content -> Innertube.AlbumItem.from(content) })?.getOrNull()
                                }
                                val items = resultPage?.items ?: emptyList()
                                allMapped.addAll(items.map { ali -> MediaItemMapper.mapAlbumToMediaItem(PlayerServiceModern.ALBUM, ali.key ?: "", ali.info?.name ?: "", ali.authors?.joinToString(", ") { a -> a.name ?: "" }, ali.thumbnail?.url, parentId) })
                                cont = resultPage?.continuation
                            } while (cont != null && allMapped.size < 150)
                            allMapped
                        }
                        PlayerServiceModern.ARTIST -> {
                            val artistId = parts[1]
                            if (artistId.startsWith(LOCAL_KEY_PREFIX)) {
                                database.songArtistMapTable.allSongsBy(artistId).first().map { song -> MediaItemMapper.mapSongToMediaItem(song, parentId) }
                            } else {
                                if (parts.size == 2) {
                                    listOf(
                                        MediaItemMapper.browsableMediaItem("$parentId/SONGS", context.getString(R.string.songs), null, MediaItemMapper.drawableUri(context, R.drawable.musical_notes), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
                                        MediaItemMapper.browsableMediaItem("$parentId/ALBUMS", context.getString(R.string.albums), null, MediaItemMapper.drawableUri(context, R.drawable.album), MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
                                        MediaItemMapper.browsableMediaItem("$parentId/VIDEOS", context.getString(R.string.videos), null, MediaItemMapper.drawableUri(context, R.drawable.video), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                    )
                                } else {
                                        when (parts[2]) {
                                        "VIDEOS" -> {
                                            val artistName = parts.getOrNull(1) ?: artistId
                                            val allMapped = mutableListOf<MediaItem>()
                                            var cont: String? = null
                                            do {
                                                val resultPage = if (cont == null) {
                                                    Innertube.searchPage<Innertube.VideoItem>(SearchBody(query = "$artistName official music video", params = Innertube.SearchFilter.Video.value), { content -> Innertube.VideoItem.from(content) })?.getOrNull()
                                                } else {
                                                    Innertube.searchPage<Innertube.VideoItem>(ContinuationBody(continuation = cont), { content -> Innertube.VideoItem.from(content) })?.getOrNull()
                                                }
                                                val items = resultPage?.items ?: emptyList()
                                                val songs = items.map { it.asSong }
                                                searchedVideos = (searchedVideos + items).distinctBy { it.key }
                                                allMapped.addAll(songs.map { s -> MediaItemMapper.mapSongToMediaItem(s, parentId) })
                                                cont = resultPage?.continuation
                                            } while (cont != null && allMapped.size < 150)
                                            allMapped
                                        }
                                        else -> {
                                            val params = when (parts[2]) { "SONGS" -> "ggMCcgQYAxAAMAO4AgE%3D"; "ALBUMS" -> "ggMCcgQIARAAMAO4AgE%3D"; else -> null }
                                            val result = YtMusic.getArtistItemsPage(BrowseEndpoint(browseId = artistId, params = params)).getOrNull()
                                            val items = result?.items ?: emptyList()
                                            items.mapNotNull { item ->
                                                when (item) {
                                                    is Innertube.SongItem -> { val song = item.asSong; searchedSongs = (searchedSongs + song).distinctBy { s -> s.id }; MediaItemMapper.mapSongToMediaItem(song, parentId) }
                                                    is Innertube.AlbumItem -> MediaItemMapper.mapAlbumToMediaItem(PlayerServiceModern.ALBUM, item.key ?: "", item.info?.name ?: "", item.authors?.joinToString(", ") { a -> a.name ?: "" }, item.thumbnail?.url)
                                                    else -> null
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        PlayerServiceModern.ALBUM -> {
                            val albumId = parts[1]
                            val localSongs = database.songAlbumMapTable.allSongsOf(albumId).first()
                            if (localSongs.isNotEmpty()) {
                                localSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song, parentId) }
                            } else {
                                val albumPage = Innertube.albumPage(BrowseBody(browseId = albumId))?.getOrNull()
                                val songs = albumPage?.songsPage?.items?.toList()?.map { item -> item.asSong } ?: emptyList()
                                searchedSongs = (searchedSongs + songs).distinctBy { s -> s.id }
                                songs.map { song -> MediaItemMapper.mapSongToMediaItem(song, parentId) }
                            }
                        }
                        PlayerServiceModern.PLAYLIST -> {
                            val playlistId = parts[1]
                            val shuffleItem = MediaSessionConstants.shuffleItem(context, MediaSessionConstants.ID_PLAYLIST_SHUFFLE)
                            val listFlow = when (playlistId) {
                                MediaSessionConstants.ID_FAVORITES -> database.songTable.allFavorites().map { fl -> fl.reversed() }
                                MediaSessionConstants.ID_CACHED -> database.formatTable.allWithSongs().map { flist -> flist.fastFilter { itf -> val contentLength = itf.format.contentLength; contentLength != null && (if (::binder.isInitialized) binder.cache.isCached(itf.song.id, 0L, contentLength) else false) }.reversed().fastMap { itf -> itf.song } }
                                MediaSessionConstants.ID_TOP -> database.eventTable.findSongsMostPlayedBetween(from = 0, limit = context.preferences.getEnum(MaxTopPlaylistItemsKey, MaxTopPlaylistItems.`10`).toInt())
                                MediaSessionConstants.ID_ONDEVICE -> database.songTable.allOnDevice()
                                MediaSessionConstants.ID_DOWNLOADED -> { val downloads = downloadHelper.downloads.value; database.songTable.all(excludeHidden = false).map { songs -> songs.fastFilter { song -> downloads[song.id]?.state == Download.STATE_COMPLETED }.sortedByDescending { song -> downloads[song.id]?.updateTimeMs ?: 0L } } }
                                else -> {
                                    if (playlistId.toLongOrNull() != null) { database.songPlaylistMapTable.allSongsOf(playlistId.toLong()) } else {
                                        val playlistPage = Innertube.playlistPage(BrowseBody(browseId = playlistId))?.getOrNull()
                                        val songs = playlistPage?.songsPage?.items?.toList()?.map { item -> item.asSong } ?: emptyList()
                                        searchedSongs = (searchedSongs + songs).distinctBy { s -> s.id }
                                        kotlinx.coroutines.flow.flowOf(songs)
                                    }
                                }
                            }
                            listOf(shuffleItem) + listFlow.first().map { song -> MediaItemMapper.mapSongToMediaItem(song, parentId) }
                        }
                        else -> emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            println("MediaLibrarySessionCallback.onGetChildren error for $parentId: ${e.message}")
            emptyList()
        }
        if (list.isNotEmpty()) {
            searchCache[parentId] = list
        }
        LibraryResult.ofItemList(ImmutableList.copyOf(list), params)
    }

    @OptIn(UnstableApi::class)
    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> = scope.future(Dispatchers.IO) {
        val songId = mediaId.split("/").lastOrNull() ?: mediaId
        database.songTable.findById(songId).first()?.let { song -> 
            if (mediaId.contains("/")) {
                MediaItemMapper.mapSongToMediaItem(song, mediaId.substringBeforeLast("/"))
            } else {
                MediaItemMapper.mapSongToMediaItem(song)
            }
        }?.let { LibraryResult.ofItem(it, null) } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
    }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_LUCKY_SHUFFLE) {
            val allSongs = (QuickPicksRepository.trendingList.value + (QuickPicksRepository.relatedPage.value?.songs?.map { it.asSong } ?: emptyList())).distinctBy { it.id }.shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_SONG_SHUFFLE || mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_SONGS_ALL_SHUFFLE) {
            val allSongs = database.songTable.sortAll(SongSortBy.DateAdded, SortOrder.Descending, excludeHidden = true).first().shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_SONGS_FAVORITES_SHUFFLE) {
            val allSongs = database.songTable.allFavorites().first().shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_SONGS_DOWNLOADED_SHUFFLE) {
            val downloads = downloadHelper.downloads.value
            val allSongs = database.songTable.all(excludeHidden = false).first().fastFilter { song -> downloads[song.id]?.state == Download.STATE_COMPLETED }.shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_SONGS_ONDEVICE_SHUFFLE) {
            val allSongs = database.songTable.allOnDevice().first().shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_SONGS_CACHED_SHUFFLE) {
            val allSongs = database.formatTable.allWithSongs().first().fastFilter { itf -> val contentLength = itf.format.contentLength; contentLength != null && binder.cache.isCached(itf.song.id, 0L, contentLength) }.fastMap { itf -> itf.song }.shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_SONGS_TOP_SHUFFLE) {
            val allSongs = database.eventTable.findSongsMostPlayedBetween(from = 0, limit = context.preferences.getEnum(MaxTopPlaylistItemsKey, MaxTopPlaylistItems.`10`).toInt()).first().shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_ALBUM_SHUFFLE || mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_ALBUMS_LIBRARY_SHUFFLE) {
            val allSongs = database.albumTable.allInLibrary().first().flatMap { album -> database.songAlbumMapTable.allSongsOf(album.id).first() }.distinctBy { song -> song.id }.shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_ALBUMS_FAVORITES_SHUFFLE) {
            val allSongs = database.albumTable.allBookmarked().first().flatMap { album -> database.songAlbumMapTable.allSongsOf(album.id).first() }.distinctBy { song -> song.id }.shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_ARTIST_SHUFFLE || mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_ARTISTS_LIBRARY_SHUFFLE) {
            val allSongs = database.artistTable.allInLibrary().first().flatMap { artist -> database.songArtistMapTable.allSongsBy(artist.id).first() }.distinctBy { song -> song.id }.shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_ARTISTS_FAVORITES_SHUFFLE) {
            val allSongs = database.artistTable.allFollowing().first().flatMap { artist -> database.songArtistMapTable.allSongsBy(artist.id).first() }.distinctBy { song -> song.id }.shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_PLAYLISTS_LOCAL_SHUFFLE) {
            val allSongs = database.playlistTable.allAsPreview().first().filter { !it.playlist.isYoutubePlaylist && !it.playlist.name.startsWith(PIPED_PREFIX, true) && !it.playlist.name.startsWith(PINNED_PREFIX, true) && !it.playlist.name.startsWith(MONTHLY_PREFIX, true) }.flatMap { preview -> database.songPlaylistMapTable.allSongsOf(preview.playlist.id).first() }.distinctBy { song -> song.id }.shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_PLAYLISTS_YT_SHUFFLE) {
            val allSongs = database.playlistTable.allAsPreview().first().filter { it.playlist.isYoutubePlaylist }.flatMap { preview -> database.songPlaylistMapTable.allSongsOf(preview.playlist.id).first() }.distinctBy { song -> song.id }.shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_PLAYLISTS_PIPED_SHUFFLE) {
            val allSongs = database.playlistTable.allAsPreview().first().filter { it.playlist.name.startsWith(PIPED_PREFIX, true) }.flatMap { preview -> database.songPlaylistMapTable.allSongsOf(preview.playlist.id).first() }.distinctBy { song -> song.id }.shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_PLAYLISTS_PINNED_SHUFFLE) {
            val allSongs = database.playlistTable.allAsPreview().first().filter { it.playlist.name.startsWith(PINNED_PREFIX, true) }.flatMap { preview -> database.songPlaylistMapTable.allSongsOf(preview.playlist.id).first() }.distinctBy { song -> song.id }.shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_PLAYLISTS_MONTHLY_SHUFFLE) {
            val allSongs = database.playlistTable.allAsPreview().first().filter { it.playlist.name.startsWith(MONTHLY_PREFIX, true) }.flatMap { preview -> database.songPlaylistMapTable.allSongsOf(preview.playlist.id).first() }.distinctBy { song -> song.id }.shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }
        if (mediaItems.firstOrNull()?.mediaId == MediaSessionConstants.ID_PLAYLIST_SHUFFLE) {
            val paths = mediaItems.first().mediaId.split("/")
            val playlistId = paths[1]
            val allSongs = if (playlistId.toLongOrNull() != null) {
                database.songPlaylistMapTable.allSongsOf(playlistId.toLong()).first()
            } else {
                searchedSongs
            }.shuffled()
            if (allSongs.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(allSongs.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, 0, 0)
        }

        var queryList = emptyList<Song>()
        var startIdx = startIndex
        runCatching {
            var songId = ""
            val paths = mediaItems.first().mediaId.split("/")
            when (paths.first()) {
                MediaSessionConstants.ID_QUICK_PICKS -> { songId = paths[1]; queryList = (QuickPicksRepository.trendingList.value + (QuickPicksRepository.relatedPage.value?.songs?.map { it.asSong } ?: emptyList())).distinctBy { it.id } }
                MediaSessionConstants.ID_SEARCH_SONGS -> { songId = paths[2]; queryList = searchedSongs }
                MediaSessionConstants.ID_SEARCH_VIDEOS -> { songId = paths[2]; queryList = searchedVideos.map { it.asSong } }
                PlayerServiceModern.SEARCHED -> { songId = paths[1]; queryList = searchedSongs }
                PlayerServiceModern.SONG -> { songId = paths[1]; queryList = database.songTable.all().first() }
                MediaSessionConstants.ID_SONGS_ALL -> { songId = paths[1]; queryList = database.songTable.sortAll(SongSortBy.DateAdded, SortOrder.Descending, excludeHidden = true).first() }
                MediaSessionConstants.ID_SONGS_FAVORITES -> { songId = paths[1]; queryList = database.songTable.allFavorites().first().reversed() }
                MediaSessionConstants.ID_SONGS_DOWNLOADED -> { 
                    val downloads = downloadHelper.downloads.value
                    queryList = database.songTable.all(excludeHidden = false).first().fastFilter { song -> downloads[song.id]?.state == Download.STATE_COMPLETED }.sortedByDescending { song -> downloads[song.id]?.updateTimeMs ?: 0L }
                    songId = paths[1]
                }
                MediaSessionConstants.ID_SONGS_ONDEVICE -> { songId = paths[1]; queryList = database.songTable.allOnDevice().first() }
                MediaSessionConstants.ID_SONGS_CACHED -> {
                    queryList = database.formatTable.allWithSongs().first().fastFilter { itf -> itf.song.totalPlayTimeMs > 0 && itf.format.contentLength != null && (if (::binder.isInitialized) binder.cache.isCached(itf.song.id, 0L, itf.format.contentLength ?: 0L) else false) }.reversed().fastMap { itf -> itf.song }
                    songId = paths[1]
                }
                MediaSessionConstants.ID_SONGS_TOP -> {
                    queryList = database.eventTable.findSongsMostPlayedBetween(from = 0, limit = context.preferences.getEnum(MaxTopPlaylistItemsKey, MaxTopPlaylistItems.`10`).toInt()).first()
                    songId = paths[1]
                }
                PlayerServiceModern.ARTIST -> { songId = if (paths.size == 4) paths[3] else paths[2]; queryList = if (paths.size == 4) searchedSongs else database.songArtistMapTable.allSongsBy(paths[1]).first() }
                PlayerServiceModern.ALBUM -> { songId = paths[2]; queryList = database.songAlbumMapTable.allSongsOf(paths[1]).first(); if (queryList.isEmpty()) queryList = searchedSongs }
                PlayerServiceModern.PLAYLIST -> {
                    val playlistId = paths[1]; songId = paths[2]
                    queryList = when (playlistId) {
                        MediaSessionConstants.ID_FAVORITES -> database.songTable.allFavorites().map { it.reversed() }.first()
                        MediaSessionConstants.ID_CACHED -> database.formatTable.allWithSongs().map { fl -> fl.fastFilter { itf -> itf.song.totalPlayTimeMs > 0 && itf.format.contentLength != null && (if (::binder.isInitialized) binder.cache.isCached(itf.song.id, 0L, itf.format.contentLength ?: 0L) else false) }.reversed().fastMap { itf -> itf.song } }.first()
                        MediaSessionConstants.ID_TOP -> database.eventTable.findSongsMostPlayedBetween(from = 0, limit = context.preferences.getEnum(MaxTopPlaylistItemsKey, MaxTopPlaylistItems.`10`).toInt()).first()
                        MediaSessionConstants.ID_ONDEVICE -> database.songTable.allOnDevice().first()
                        MediaSessionConstants.ID_DOWNLOADED -> {
                            downloadHelper.getDownloadManager(context)
                            val downloads = downloadHelper.downloads.value
                            database.songTable.all(excludeHidden = false).map { fl -> fl.fastFilter { s -> downloads[s.id]?.state == Download.STATE_COMPLETED }.sortedByDescending { s -> downloads[s.id]?.updateTimeMs ?: 0L } }.first()
                        }
                        else -> { if (playlistId.toLongOrNull() != null) database.songPlaylistMapTable.allSongsOf(playlistId.toLong()).first() else searchedSongs }
                    }
                }
            }
            startIdx = queryList.indexOfFirst { song -> song.id == songId }.coerceAtLeast(0)
        }
        return@future MediaSession.MediaItemsWithStartPosition(queryList.map { song -> MediaItemMapper.mapSongToMediaItem(song) }, startIdx, startPositionMs)
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> = scope.future(Dispatchers.IO) {
        mediaItems.fastMap { item ->
            val songId = item.mediaId.split("/").lastOrNull() ?: item.mediaId
            database.songTable.findById(songId).first()?.asMediaItem ?: item.buildUpon().setMediaId(songId).build()
        }.toMutableList()
    }

    @Deprecated("Deprecated in Java")
    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        val settableFuture = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
        val defaultResult = MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0)
        if (!context.preferences.getBoolean(persistentQueueKey, false)) return Futures.immediateFuture(defaultResult)
        scope.launch {
            try {
                database.queueTable.all().first().run {
                    val idx = indexOfFirst { it.position != null }.coerceAtLeast(0)
                    val startPos = getOrNull(idx)?.position ?: 0L
                    val mediaItems = map { itm -> MediaItemMapper.mapSongToMediaItem(itm.mediaItem.asSong, true) }
                    settableFuture.set(MediaSession.MediaItemsWithStartPosition(mediaItems, idx, startPos))
                }
            } catch (e: Exception) {
                settableFuture.set(defaultResult)
            }
        }
        return settableFuture
    }

    private fun getCountCachedSongs(): Flow<Int> = database.formatTable.allWithSongs().map { flist ->
        if (!::binder.isInitialized) return@map 0
        flist.filter { itf ->
            val contentLength = itf.format.contentLength
            contentLength != null && binder.cache.isCached(itf.song.id, 0L, contentLength)
        }.size
    }
    private fun getCountDownloadedSongs(): Flow<Int> {
        downloadHelper.getDownloadManager(context)
        return downloadHelper.downloads.map { dm -> dm.filter { ite -> ite.value.state == Download.STATE_COMPLETED }.size }
    }
}