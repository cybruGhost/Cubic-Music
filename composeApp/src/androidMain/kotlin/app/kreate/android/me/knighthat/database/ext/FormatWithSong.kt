package app.kreate.android.me.knighthat.database.ext

import androidx.room.Embedded
import app.it.fast4x.rimusic.models.Format
import app.it.fast4x.rimusic.models.Song

data class FormatWithSong(
    @Embedded val format: Format,
    @Embedded val song: Song
)