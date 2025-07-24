package it.fast4x.rimusic.enums

import android.media.audiofx.PresetReverb
import androidx.compose.runtime.Composable
import me.knighthat.enums.TextView

enum class PresetsReverb: TextView {
    NONE,
    SMALLROOM,
    MEDIUMROOM,
    LARGEROOM,
    MEDIUMHALL,
    LARGEHALL,
    PLATE;

    val preset: Short
        get() = when (this) {
            NONE -> PresetReverb.PRESET_NONE
            SMALLROOM -> PresetReverb.PRESET_SMALLROOM
            MEDIUMROOM -> PresetReverb.PRESET_MEDIUMROOM
            LARGEROOM -> PresetReverb.PRESET_LARGEROOM
            MEDIUMHALL -> PresetReverb.PRESET_MEDIUMHALL
            LARGEHALL -> PresetReverb.PRESET_LARGEHALL
            PLATE -> PresetReverb.PRESET_PLATE
        }

    override val text: String
        @Composable
        get() = when (this) {
            NONE -> "None"
            SMALLROOM -> "Small Room"
            MEDIUMROOM -> "Medium Room"
            LARGEROOM -> "Large Room"
            MEDIUMHALL -> "Medium Hall"
            LARGEHALL -> "Large Hall"
            PLATE -> "Plate"
        }
}