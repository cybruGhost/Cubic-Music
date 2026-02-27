package app.kreate.android.me.knighthat.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class From25To26Migration : Migration(25, 26) {

    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE Playlist ADD COLUMN isYoutubePlaylist INTEGER NOT NULL DEFAULT 0;")
        } catch (e: Exception) {
            println("Database From25To26Migration error ${e.stackTraceToString()}")
        }

    }
}