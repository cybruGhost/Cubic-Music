package app.it.fast4x.rimusic.enums

import androidx.compose.runtime.Composable
import app.kreate.android.me.knighthat.enums.TextView

enum class MaxStatisticsItems: TextView {
    `10`,
    `20`,
    `30`,
    `40`,
    `50`,
    `100`,
    `250`,
    `750`;  // Added this for fun

    override val text: String
        @Composable
        get() = this.name

    fun toInt(): Int = this.name.toInt()

    fun toLong(): Long = this.name.toLong()
}