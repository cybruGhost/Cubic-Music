package it.fast4x.rimusic.enums

enum class MaxTopPlaylistItems {
    `10`,
    `20`,
    `30`,
    `40`,
    `50`,
    `70`,
    `90`,
    `100`,
    `150`,
    `200`;

    fun toInt(): Int = this.name.toInt()
}