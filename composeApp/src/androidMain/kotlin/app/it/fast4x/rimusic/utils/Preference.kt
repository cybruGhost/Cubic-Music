package app.it.fast4x.rimusic.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.input.key.Key
import app.it.fast4x.rimusic.enums.AlbumSortBy
import app.it.fast4x.rimusic.enums.ArtistSortBy
import app.it.fast4x.rimusic.enums.HomeItemSize
import app.it.fast4x.rimusic.enums.OnDeviceSongSortBy
import app.it.fast4x.rimusic.enums.PlaylistSongSortBy
import app.it.fast4x.rimusic.enums.PlaylistSortBy
import app.it.fast4x.rimusic.enums.SongSortBy
import app.it.fast4x.rimusic.enums.SortOrder
import app.it.fast4x.rimusic.enums.StatisticsType

object Preference {

    /****  ENUMS  ****/
    val HOME_ARTIST_ITEM_SIZE = Key( "AristItemSizeEnum", HomeItemSize.SMALL )
    val HOME_ALBUM_ITEM_SIZE = Key( "AlbumItemSizeEnum", HomeItemSize.SMALL )
    val HOME_LIBRARY_ITEM_SIZE = Key( "LibraryItemSizeEnum", HomeItemSize.SMALL )
    val HOME_SONGS_TOP_PLAYLIST_PERIOD = Key( "HomeSongsTopPlaylistPeriod", StatisticsType.All )
    val enableVoiceInputKey = Preference.Key("enableVoiceInput", true)
    //<editor-fold defaultstate="collapsed" desc="Sort by">
    val HOME_SONGS_SORT_BY = Key( "HomeSongsSortBy", SongSortBy.Title )
    val HOME_ON_DEVICE_SONGS_SORT_BY = Key( "HomeOnDeviceSongsSortBy", OnDeviceSongSortBy.Title )
    val HOME_ARTISTS_SORT_BY = Key( "HomeArtistsSortBy", ArtistSortBy.Name )
    val HOME_ALBUMS_SORT_BY = Key( "HomeAlbumsSortBy", AlbumSortBy.Title )
    val HOME_LIBRARY_SORT_BY = Key( "HomeLibrarySortBy", PlaylistSortBy.SongCount )
    val PLAYLIST_SONGS_SORT_BY = Key( "PlaylistSongsSortBy", PlaylistSongSortBy.Title )
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Sort order">
    val HOME_SONGS_SORT_ORDER = Key( "HomeSongsSortOrder", SortOrder.Ascending )
    val HOME_ARTISTS_SORT_ORDER = Key( "PlaylistSongsSortOrder", SortOrder.Ascending )
    val HOME_ALBUM_SORT_ORDER = Key( "PlaylistSongsSortOrder", SortOrder.Ascending )
    val HOME_LIBRARY_SORT_ORDER = Key( "HomeLibrarySortOrder", SortOrder.Ascending )
    val PLAYLIST_SONGS_SORT_ORDER = Key( "PlaylistSongsSortOrder", SortOrder.Ascending )
    //</editor-fold>
    
    val SEARCH_RESULT_GRID_STATES = Key( "searchResultGridStates", "1111111" )

    @Composable
    inline fun <reified T: Enum<T>> remember( key: Key<T>): MutableState<T> =
        rememberPreference( key.key, key.default )

    /**
     * In order to ensure consistent between input key and output value.
     * The provided key must bear a potential return value.
     */
    data class Key<T>( val key: String, val default: T )
}