package it.fast4x.rimusic.extensions.games.pacman.models

import androidx.compose.runtime.MutableState

data class DialogState (
    val shouldShow: MutableState<Boolean>,
    val message: MutableState<String>,
)