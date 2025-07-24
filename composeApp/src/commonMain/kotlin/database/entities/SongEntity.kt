package database.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded

@Immutable
data class SongEntity(
    @Embedded val song: Song,
    val contentLength: Long? = null,
    val albumTitle: String? = null,
)
