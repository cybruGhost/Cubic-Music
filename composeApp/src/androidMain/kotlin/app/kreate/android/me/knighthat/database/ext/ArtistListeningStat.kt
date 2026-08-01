package app.kreate.android.me.knighthat.database.ext

import androidx.room.Embedded
import app.it.fast4x.rimusic.models.Artist

data class ArtistListeningStat(
    @Embedded val artist: Artist,
    val playTimeMs: Long
)
