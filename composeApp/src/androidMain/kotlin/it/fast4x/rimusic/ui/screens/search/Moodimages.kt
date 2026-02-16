package it.fast4x.rimusic.ui.screens.search

import androidx.annotation.DrawableRes
import app.kreate.android.R

object MoodImages {

    @DrawableRes
    fun getImageResource(moodTitle: String): Int {
        return when {

            moodTitle.contains("Rock", ignoreCase = true) -> getDrawableOrDefault("mood_rock")
            moodTitle.contains("Metal", ignoreCase = true) -> getDrawableOrDefault("mood_metal")
            moodTitle.contains("Punk", ignoreCase = true) -> getDrawableOrDefault("mood_punk")
            moodTitle.contains("Alternative", ignoreCase = true) -> getDrawableOrDefault("mood_alternative")


            moodTitle.contains("Pop", ignoreCase = true) -> getDrawableOrDefault("mood_pop")
            moodTitle.contains("Dance", ignoreCase = true) -> getDrawableOrDefault("mood_dance")
            moodTitle.contains("Disco", ignoreCase = true) -> getDrawableOrDefault("mood_disco")


            moodTitle.contains("Hip Hop", ignoreCase = true) ||
                    moodTitle.contains("Rap", ignoreCase = true) -> getDrawableOrDefault("mood_hiphop")
            moodTitle.contains("R&B", ignoreCase = true) ||
                    moodTitle.contains("Soul", ignoreCase = true) -> getDrawableOrDefault("mood_rnb")


            moodTitle.contains("Electronic", ignoreCase = true) ||
                    moodTitle.contains("EDM", ignoreCase = true) -> getDrawableOrDefault("mood_electric")
            moodTitle.contains("House", ignoreCase = true) -> getDrawableOrDefault("mood_house")
            moodTitle.contains("Techno", ignoreCase = true) -> getDrawableOrDefault("mood_techno")


            moodTitle.contains("Jazz", ignoreCase = true) -> getDrawableOrDefault("mood_jazz")
            moodTitle.contains("Blues", ignoreCase = true) -> getDrawableOrDefault("mood_blues")


            moodTitle.contains("Classical", ignoreCase = true) -> getDrawableOrDefault("mood_classical")
            moodTitle.contains("Orchestra", ignoreCase = true) -> getDrawableOrDefault("mood_orchestra")


            moodTitle.contains("Country", ignoreCase = true) -> getDrawableOrDefault("mood_country")
            moodTitle.contains("Folk", ignoreCase = true) -> getDrawableOrDefault("mood_folk")


            moodTitle.contains("Reggae", ignoreCase = true) -> getDrawableOrDefault("mood_reggae")
            moodTitle.contains("Latin", ignoreCase = true) -> getDrawableOrDefault("mood_latin")
            moodTitle.contains("Afrobeats", ignoreCase = true) ||
                    moodTitle.contains("Afro", ignoreCase = true) -> getDrawableOrDefault("mood_afrobeats")
            moodTitle.contains("K-Pop", ignoreCase = true) ||
                    moodTitle.contains("Korean", ignoreCase = true) -> getDrawableOrDefault("mood_kpop")


            moodTitle.contains("Indie", ignoreCase = true) -> getDrawableOrDefault("mood_indie")


            moodTitle.contains("Workout", ignoreCase = true) ||
                    moodTitle.contains("Gym", ignoreCase = true) ||
                    moodTitle.contains("Fitness", ignoreCase = true) -> getDrawableOrDefault("mood_workout")

            moodTitle.contains("Chill", ignoreCase = true) ||
                    moodTitle.contains("Relax", ignoreCase = true) ||
                    moodTitle.contains("Calm", ignoreCase = true) -> getDrawableOrDefault("mood_chill")

            moodTitle.contains("Party", ignoreCase = true) ||
                    moodTitle.contains("Celebration", ignoreCase = true) -> getDrawableOrDefault("mood_party")

            moodTitle.contains("Sleep", ignoreCase = true) ||
                    moodTitle.contains("Night", ignoreCase = true) -> getDrawableOrDefault("mood_sleep")

            moodTitle.contains("Focus", ignoreCase = true) ||
                    moodTitle.contains("Study", ignoreCase = true) ||
                    moodTitle.contains("Concentration", ignoreCase = true) -> getDrawableOrDefault("mood_focus")

            moodTitle.contains("Happy", ignoreCase = true) ||
                    moodTitle.contains("Feel Good", ignoreCase = true) ||
                    moodTitle.contains("Upbeat", ignoreCase = true) -> getDrawableOrDefault("mood_happy")

            moodTitle.contains("Sad", ignoreCase = true) ||
                    moodTitle.contains("Melancholy", ignoreCase = true) -> getDrawableOrDefault("mood_sad")

            moodTitle.contains("Romance", ignoreCase = true) ||
                    moodTitle.contains("Love", ignoreCase = true) -> getDrawableOrDefault("mood_romance")

            moodTitle.contains("Travel", ignoreCase = true) ||
                    moodTitle.contains("Road Trip", ignoreCase = true) -> getDrawableOrDefault("mood_travel")

            moodTitle.contains("Morning", ignoreCase = true) -> getDrawableOrDefault("mood_morning")

            moodTitle.contains("Evening", ignoreCase = true) -> getDrawableOrDefault("mood_evening")

            // Default fallback - use a generic music icon
            else -> R.drawable.musical_notes
        }
    }

    private fun getDrawableOrDefault(resourceName: String): Int {
        return try {
            val field = R.drawable::class.java.getField(resourceName)
            field.getInt(null)
        } catch (e: Exception) {
            // If image doesn't exist, use musical_notes as fallback
            R.drawable.musical_notes
        }
    }

    fun hasCustomImage(moodTitle: String): Boolean {
        return getImageResource(moodTitle) != R.drawable.musical_notes
    }
}