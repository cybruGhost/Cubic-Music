package me.knighthat.database.ext

import androidx.room.Embedded
import it.fast4x.rimusic.models.Format
import it.fast4x.rimusic.models.Song

data class FormatWithSong(
    @Embedded val format: Format,
    @Embedded val song: Song
)