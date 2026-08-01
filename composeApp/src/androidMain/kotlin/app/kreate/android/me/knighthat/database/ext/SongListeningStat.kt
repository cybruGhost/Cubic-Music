package app.kreate.android.me.knighthat.database.ext

import androidx.room.Embedded
import app.it.fast4x.rimusic.models.Song

data class SongListeningStat(
    @Embedded val song: Song,
    val playTimeMs: Long,
    val playCount: Long,
    val lastPlayedAt: Long
)
