package app.kreate.android.me.knighthat.database.migration

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase

class From10To11Migration : Migration(10, 11) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.query(SimpleSQLiteQuery("SELECT id, albumId FROM Song;")).use { cursor ->
            val songAlbumMapValues = ContentValues(2)
            while (cursor.moveToNext()) {
                songAlbumMapValues.put("songId", cursor.getString(0))
                songAlbumMapValues.put("albumId", cursor.getString(1))
                db.insert("SongAlbumMap", CONFLICT_IGNORE, songAlbumMapValues)
            }
        }

        db.execSQL("CREATE TABLE IF NOT EXISTS `Song_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artistsText` TEXT, `durationText` TEXT NOT NULL, `thumbnailUrl` TEXT, `lyrics` TEXT, `likedAt` INTEGER, `totalPlayTimeMs` INTEGER NOT NULL, `loudnessDb` REAL, `contentLength` INTEGER, PRIMARY KEY(`id`))")

        db.execSQL("INSERT INTO Song_new(id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs, loudnessDb, contentLength) SELECT id, title, artistsText, durationText, thumbnailUrl, lyrics, likedAt, totalPlayTimeMs, loudnessDb, contentLength FROM Song;")
        db.execSQL("DROP TABLE Song;")
        db.execSQL("ALTER TABLE Song_new RENAME TO Song;")
    }
}