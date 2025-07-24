package it.fast4x.rimusic.enums

import androidx.compose.runtime.Composable
import me.knighthat.enums.TextView

enum class MaxStatisticsItems: TextView {
    `10`,
    `20`,
    `30`,
    `40`,
    `50`;

    override val text: String
        @Composable
        get() = this.name

    fun toInt(): Int = this.name.toInt()

    fun toLong(): Long = this.name.toLong()
}