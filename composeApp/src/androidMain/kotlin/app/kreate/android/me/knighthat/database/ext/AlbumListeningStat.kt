package app.kreate.android.me.knighthat.database.ext

import androidx.room.Embedded
import app.it.fast4x.rimusic.models.Album

data class AlbumListeningStat(
    @Embedded val album: Album,
    val playTimeMs: Long
)
