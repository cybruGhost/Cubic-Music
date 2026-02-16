package it.fast4x.rimusic.ui.screens.search

import androidx.annotation.DrawableRes
import app.kreate.android.R
import android.util.Log

object MoodImages {

    private const val TAG = "MoodImages"

    @DrawableRes
    fun getImageResource(moodTitle: String): Int {
        Log.d(TAG, "Getting image for mood: $moodTitle")

        val resourceId = when {
            moodTitle.contains("Rock", ignoreCase = true) -> R.drawable.mood_rock
            moodTitle.contains("Pop", ignoreCase = true) -> R.drawable.mood_pop
            moodTitle.contains("Jazz", ignoreCase = true) -> R.drawable.mood_jazz
            moodTitle.contains("Hip Hop", ignoreCase = true) ||
                    moodTitle.contains("Rap", ignoreCase = true) -> R.drawable.mood_hiphop
            moodTitle.contains("Electronic", ignoreCase = true) ||
                    moodTitle.contains("EDM", ignoreCase = true) -> R.drawable.mood_electric

            moodTitle.contains("Metal", ignoreCase = true) -> getDrawableIfExists("mood_metal")
            moodTitle.contains("Country", ignoreCase = true) -> getDrawableIfExists("mood_country")
            moodTitle.contains("Blues", ignoreCase = true) -> getDrawableIfExists("mood_blues")
            moodTitle.contains("Chill", ignoreCase = true) ||
                    moodTitle.contains("Relax", ignoreCase = true) -> getDrawableIfExists("mood_chill")
            moodTitle.contains("Workout", ignoreCase = true) ||
                    moodTitle.contains("Gym", ignoreCase = true) -> getDrawableIfExists("mood_workout")
            moodTitle.contains("Party", ignoreCase = true) -> getDrawableIfExists("mood_party")
            moodTitle.contains("Classical", ignoreCase = true) -> getDrawableIfExists("mood_classical")
            moodTitle.contains("Reggae", ignoreCase = true) -> getDrawableIfExists("mood_reggae")
            moodTitle.contains("Latin", ignoreCase = true) -> getDrawableIfExists("mood_latin")
            moodTitle.contains("Indie", ignoreCase = true) -> getDrawableIfExists("mood_indie")
            moodTitle.contains("Folk", ignoreCase = true) -> getDrawableIfExists("mood_folk")
            moodTitle.contains("R&B", ignoreCase = true) ||
                    moodTitle.contains("Soul", ignoreCase = true) -> getDrawableIfExists("mood_rnb")
            moodTitle.contains("K-Pop", ignoreCase = true) -> getDrawableIfExists("mood_kpop")
            moodTitle.contains("Afrobeats", ignoreCase = true) ||
                    moodTitle.contains("Afro", ignoreCase = true) -> getDrawableIfExists("mood_afrobeats")

            // Default fallback
            else -> R.drawable.musical_notes
        }

        Log.d(TAG, "Returning resource ID: $resourceId for mood: $moodTitle")
        return resourceId
    }

    private fun getDrawableIfExists(name: String): Int {
        return try {
            val field = R.drawable::class.java.getField(name)
            val id = field.getInt(null)
            Log.d(TAG, "Found drawable: $name with ID: $id")
            id
        } catch (e: NoSuchFieldException) {
            Log.d(TAG, "Drawable not found: $name, using fallback")
            R.drawable.musical_notes
        } catch (e: Exception) {
            Log.e(TAG, "Error getting drawable: $name", e)
            R.drawable.musical_notes
        }
    }
}