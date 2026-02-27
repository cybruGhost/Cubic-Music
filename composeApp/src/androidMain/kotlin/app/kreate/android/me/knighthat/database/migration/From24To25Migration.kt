package app.kreate.android.me.knighthat.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class From24To25Migration : Migration(24, 25) {

    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE Playlist ADD COLUMN isEditable INTEGER NOT NULL DEFAULT 0;")
        } catch (e: Exception) {
            println("Database From24To25Migration error ${e.stackTraceToString()}")
        }

    }
}