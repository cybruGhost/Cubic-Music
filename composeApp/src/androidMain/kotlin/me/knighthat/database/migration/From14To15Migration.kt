package me.knighthat.database.migration

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import androidx.core.database.getFloatOrNull
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase

class From14To15Migration : Migration(14, 15) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.query(SimpleSQLiteQuery("SELECT id, loudnessDb, contentLength FROM Song;"))
            .use { cursor ->
                val formatValues = ContentValues(3)
                while (cursor.moveToNext()) {
                    formatValues.put("songId", cursor.getString(0))
                    formatValues.put("loudnessDb", cursor.getFloatOrNull(1))
                    formatValues.put("contentLength", cursor.getFloatOrNull(2))
                    db.insert("Format", CONFLICT_IGNORE, formatValues)
                }
            }

        db.execSQL("CREATE TABLE IF NOT EXISTS `Song_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artistsText` TEXT, `durationText` TEXT NOT NULL, `thumbnailUrl` TEXT, `lyrics` TEXT, `likedAt` INTEGER, `totalPlayTimeMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")

        db.execSQL("INSERT INTO Song_new(id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs) SELECT id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs FROM Song;")
        db.execSQL("DROP TABLE Song;")
        db.execSQL("ALTER TABLE Song_new RENAME TO Song;")
    }
}