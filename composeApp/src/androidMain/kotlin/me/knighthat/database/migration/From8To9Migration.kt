package me.knighthat.database.migration

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase

class From8To9Migration : Migration(8, 9) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.query(SimpleSQLiteQuery("SELECT DISTINCT browseId, text, Info.id FROM Info JOIN Song ON Info.id = Song.albumId;"))
            .use { cursor ->
                val albumValues = ContentValues(2)
                while (cursor.moveToNext()) {
                    albumValues.put("id", cursor.getString(0))
                    albumValues.put("title", cursor.getString(1))
                    db.insert("Album", CONFLICT_IGNORE, albumValues)

                    db.execSQL(
                        "UPDATE Song SET albumId = '${cursor.getString(0)}' WHERE albumId = ${
                            cursor.getLong(
                                2
                            )
                        }"
                    )
                }
            }

        db.query(SimpleSQLiteQuery("SELECT GROUP_CONCAT(text, ''), SongWithAuthors.songId FROM Info JOIN SongWithAuthors ON Info.id = SongWithAuthors.authorInfoId GROUP BY songId;"))
            .use { cursor ->
                val songValues = ContentValues(1)
                while (cursor.moveToNext()) {
                    songValues.put("artistsText", cursor.getString(0))
                    db.update(
                        "Song",
                        CONFLICT_IGNORE,
                        songValues,
                        "id = ?",
                        arrayOf(cursor.getString(1))
                    )
                }
            }

        db.query(SimpleSQLiteQuery("SELECT browseId, text, Info.id FROM Info JOIN SongWithAuthors ON Info.id = SongWithAuthors.authorInfoId WHERE browseId NOT NULL;"))
            .use { cursor ->
                val artistValues = ContentValues(2)
                while (cursor.moveToNext()) {
                    artistValues.put("id", cursor.getString(0))
                    artistValues.put("name", cursor.getString(1))
                    db.insert("Artist", CONFLICT_IGNORE, artistValues)

                    db.execSQL(
                        "UPDATE SongWithAuthors SET authorInfoId = '${cursor.getString(0)}' WHERE authorInfoId = ${
                            cursor.getLong(
                                2
                            )
                        }"
                    )
                }
            }

        db.execSQL("INSERT INTO SongArtistMap(songId, artistId) SELECT songId, authorInfoId FROM SongWithAuthors")

        db.execSQL("DROP TABLE Info;")
        db.execSQL("DROP TABLE SongWithAuthors;")
    }
}