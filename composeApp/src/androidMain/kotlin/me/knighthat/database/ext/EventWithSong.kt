package me.knighthat.database.ext

import androidx.room.Embedded
import androidx.room.Relation
import it.fast4x.rimusic.models.Event
import it.fast4x.rimusic.models.Song

data class EventWithSong(
    @Embedded val event: Event,
    @Relation(
        entity = Song::class,
        parentColumn = "songId",
        entityColumn = "id"
    )
    val song: Song
)