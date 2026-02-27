package app.kreate.android.me.knighthat.database.migration

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase

class From22To23Migration : Migration(22, 23) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS Lyrics (`songId` TEXT NOT NULL, `fixed` TEXT, `synced` TEXT, PRIMARY KEY(`songId`), FOREIGN KEY(`songId`) REFERENCES `Song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")

        db.query(SimpleSQLiteQuery("SELECT id, lyrics, synchronizedLyrics FROM Song;")).use { cursor ->
            val lyricsValues = ContentValues(3)
            while (cursor.moveToNext()) {
                lyricsValues.put("songId", cursor.getString(0))
                lyricsValues.put("fixed", cursor.getString(1))
                lyricsValues.put("synced", cursor.getString(2))
                db.insert("Lyrics", CONFLICT_IGNORE, lyricsValues)
            }
        }

        db.execSQL("CREATE TABLE IF NOT EXISTS Song_new (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artistsText` TEXT, `durationText` TEXT, `thumbnailUrl` TEXT, `likedAt` INTEGER, `totalPlayTimeMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("INSERT INTO Song_new(id, title, artistsText, durationText, thumbnailUrl, likedAt, totalPlayTimeMs) SELECT id, title, artistsText, durationText, thumbnailUrl, likedAt, totalPlayTimeMs FROM Song;")
        db.execSQL("DROP TABLE Song;")
        db.execSQL("ALTER TABLE Song_new RENAME TO Song;")
    }
}