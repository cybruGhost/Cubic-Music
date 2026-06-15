package app.kreate.android.me.knighthat.utils

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.core.net.toUri
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.enums.OnDeviceSongSortBy
import app.it.fast4x.rimusic.enums.SortOrder
import app.it.fast4x.rimusic.models.Format
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.service.modern.LOCAL_KEY_PREFIX
import app.it.fast4x.rimusic.utils.isAtLeastAndroid10
import app.it.fast4x.rimusic.utils.isAtLeastAndroid11
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

val PROJECTION by lazy {
    var projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM_ID,
        if (isAtLeastAndroid10) {
            MediaStore.Audio.Media.RELATIVE_PATH
        } else {
            MediaStore.Audio.Media.DATA
        },
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.IS_MUSIC,
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.DATE_MODIFIED,
        MediaStore.Audio.Media.SIZE,
    )
    if ( isAtLeastAndroid11 )
        projection += MediaStore.Audio.Media.BITRATE

    return@lazy projection
}
val PRO = buildList {
    add( MediaStore.Audio.Media._ID )
    add( MediaStore.Audio.Media.DISPLAY_NAME )
    add( MediaStore.Audio.Media.DURATION )
    add( MediaStore.Audio.Media.ARTIST )
    add( MediaStore.Audio.Media.ALBUM_ID )
    if (isAtLeastAndroid10) {
        add( MediaStore.Audio.Media.RELATIVE_PATH )
    } else {
        add( MediaStore.Audio.Media.DATA )
    }
    add( MediaStore.Audio.Media.TITLE )
    add( MediaStore.Audio.Media.IS_MUSIC )
    add( MediaStore.Audio.Media.MIME_TYPE )
    add( MediaStore.Audio.Media.DATE_MODIFIED )
    add( MediaStore.Audio.Media.SIZE )
    if ( isAtLeastAndroid11 )
        add( MediaStore.Audio.Media.BITRATE )
}.toTypedArray()

val ALBUM_URI = "content://media/external/audio/albumart".toUri()

private fun blacklistedPaths( context: Context ): Set<String> {
    val file = File(context.filesDir, "Blacklisted_paths.txt")
    return  if( file.exists() )
        file.readLines().toSet()
    else
        emptySet()
}

private fun normalizeDeviceDirectory(path: String?): String {
    val sanitizedPath = path
        ?.replace('\\', '/')
        ?.trim()
        ?.trim('/')
        .orEmpty()

    if (sanitizedPath.isBlank()) return PathUtils.INTERNAL_STORAGE_ROOT

    return if (isAtLeastAndroid10) {
        listOf(PathUtils.INTERNAL_STORAGE_ROOT, sanitizedPath)
            .filter { it.isNotBlank() }
            .joinToString("/")
    } else {
        val parentDirectory = File(sanitizedPath).parent
            ?.replace('\\', '/')
            ?.substringAfter("/storage/emulated/0/", "")
            ?.trim('/')
            .orEmpty()

        if (parentDirectory.isBlank()) PathUtils.INTERNAL_STORAGE_ROOT
        else "${PathUtils.INTERNAL_STORAGE_ROOT}/$parentDirectory"
    }
}

fun Context.getLocalSongs(
    sortBy: OnDeviceSongSortBy,
    sortOrder: SortOrder
): Flow<Map<Song, String>> = flow {
    val results = linkedMapOf<Song, String>()
    val blacklistedPaths = blacklistedPaths( this@getLocalSongs )

    val uri =
        if (isAtLeastAndroid10)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} > 0"
    val order = "${sortBy.value} COLLATE NOCASE ${sortOrder.asSqlString}"

    contentResolver.query( uri, PROJECTION, selection, null, order )?.use { cursor ->
        val idColumn = cursor.getColumnIndex( MediaStore.Audio.Media._ID )
        val nameColumn = cursor.getColumnIndex( MediaStore.Audio.Media.DISPLAY_NAME )
        val durationColumn = cursor.getColumnIndex( MediaStore.Audio.Media.DURATION )
        val artistColumn = cursor.getColumnIndex( MediaStore.Audio.Media.ARTIST )
        val albumIdColumn = cursor.getColumnIndex( MediaStore.Audio.Media.ALBUM_ID )
        val pathColumn = if (isAtLeastAndroid10) {
            cursor.getColumnIndex( MediaStore.Audio.Media.RELATIVE_PATH )
        } else {
            cursor.getColumnIndex( MediaStore.Audio.Media.DATA )
        }
        val titleColumn = cursor.getColumnIndex( MediaStore.Audio.Media.TITLE )
        val mimeTypeColumn = cursor.getColumnIndex( MediaStore.Audio.Media.MIME_TYPE )
        val bitrateColumn = if (isAtLeastAndroid11) cursor.getColumnIndex( MediaStore.Audio.Media.BITRATE ) else -1
        val fileSizeColumn = cursor.getColumnIndex( MediaStore.Audio.Media.SIZE )
        val dateModifiedColumn = cursor.getColumnIndex( MediaStore.Audio.Media.DATE_MODIFIED )

        while( cursor.moveToNext() ) {
            val relPath = normalizeDeviceDirectory(cursor.getString(pathColumn))
            if( blacklistedPaths.contains( relPath ) ) continue

            // Nullable so SongItem can display "✨"
            // TODO apply some non-null method
            val durationText =
                cursor.getInt( durationColumn )
                    .takeIf { it > 0 }
                    ?.milliseconds
                    ?.toComponents { hrs, mins, secs, _ ->
                        if( hrs > 0 )
                            "%02d:%02d:%02d".format( hrs, mins, secs )
                        else
                            "%02d:%02d".format( mins, secs )
                    }
            val id = cursor.getLong( idColumn )
            val displayName = cursor.getString( nameColumn ).orEmpty()
            val filename = displayName.substringBeforeLast(".")
            val rawTitle = cursor.getString( titleColumn )?.takeIf { it.isNotBlank() } ?: filename
            val rawArtist = cursor.getString( artistColumn ).asKnownMetadata()
            val filenameParts = splitArtistTitle(rawTitle) ?: splitArtistTitle(filename)
            val title = if (rawArtist == null && filenameParts != null) filenameParts.second else rawTitle
            val artist = rawArtist ?: filenameParts?.first
            val albumUri = ContentUris.withAppendedId( ALBUM_URI, cursor.getLong( albumIdColumn ) )
            val song = Song( "$LOCAL_KEY_PREFIX$id", title, artist, durationText, albumUri.toString() )

            val mimeType = cursor.getString( mimeTypeColumn )
            val bitrate = if( isAtLeastAndroid11 ) cursor.getLong( bitrateColumn ) else 0
            val fileSize = cursor.getLong( fileSizeColumn )
            val dateModified = cursor.getLong( dateModifiedColumn )
            val format = Format( song.id, 0, mimeType, bitrate, fileSize, dateModified )

            Database.asyncTransaction {
                songTable.insertIgnore( song )
                formatTable.upsert( format )
            }

            results[song] = relPath
        }
    }

    emit( results )
}.stateIn( CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly, mapOf() )

private fun String?.asKnownMetadata(): String? =
    this
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.takeUnless { it.equals("unknown", ignoreCase = true) }
        ?.takeUnless { it.equals("<unknown>", ignoreCase = true) }
        ?.takeUnless { it.equals("null", ignoreCase = true) }

private fun splitArtistTitle(name: String): Pair<String, String>? {
    val parts = name.split(Regex("\\s+-\\s+"), limit = 2)
    if (parts.size != 2) return null
    val artist = parts[0].trim()
    val title = parts[1].trim()
    return if (artist.isNotBlank() && title.isNotBlank()) artist to title else null
}
