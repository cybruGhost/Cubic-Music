package app.kreate.android.me.knighthat.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class From23To24Migration : Migration(23, 24) {

    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE SongPlaylistMap ADD COLUMN setVideoId TEXT;")
        } catch (e: Exception) {
            println("Database From23To24Migration error ${e.stackTraceToString()}")
        }

    }
}