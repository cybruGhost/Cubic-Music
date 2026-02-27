package app.kreate.android.me.knighthat.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class From26To27Migration : Migration(26, 27) {

    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE Album ADD COLUMN isYoutubeAlbum INTEGER NOT NULL DEFAULT 0;")
        } catch (e: Exception) {
            println("Database From26To27Migration error ${e.stackTraceToString()}")
        }
        try {
            db.execSQL("ALTER TABLE Artist ADD COLUMN isYoutubeArtist INTEGER NOT NULL DEFAULT 0;")
        } catch (e: Exception) {
            println("Database From26To27Migration error ${e.stackTraceToString()}")
        }
        try {
            db.execSQL("ALTER TABLE SongPlaylistMap ADD COLUMN dateAdded INTEGER NULL;")
        } catch (e: Exception) {
            println("Database From26To27Migration error ${e.stackTraceToString()}")
        }

    }
}