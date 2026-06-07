package app.kreate.android.themed.rimusic.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.utils.bold

@Composable
fun AlphabetIndexBar(
    alphabetIndex: Map<Char, Int>,
    modifier: Modifier = Modifier,
    onLetterClick: (Char) -> Unit
) {
    val letters = rememberOrderedLetters(alphabetIndex)
    if (letters.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .width(32.dp)
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colorPalette().background1.copy(alpha = 0.72f))
            .padding(vertical = 6.dp)
    ) {
        letters.forEach { letter ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 24.dp, height = 16.dp)
                    .clickable { onLetterClick(letter) }
            ) {
                BasicText(
                    text = letter.toString(),
                    style = typography().xxs.bold.copy(
                        color = colorPalette().accent,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

fun buildSongAlphabetIndex(songs: List<Song>): Map<Char, Int> {
    if (songs.isEmpty()) return emptyMap()

    val index = linkedMapOf<Char, Int>()
    songs.forEachIndexed { position, song ->
        index.putIfAbsent(song.alphabetIndexLetter(), position)
    }
    return index
}

private fun rememberOrderedLetters(index: Map<Char, Int>): List<Char> =
    buildList {
        if ('#' in index) add('#')
        ('A'..'Z').forEach { letter ->
            if (letter in index) add(letter)
        }
    }

private fun Song.alphabetIndexLetter(): Char {
    val first = cleanTitle().trim().firstOrNull { it.isLetterOrDigit() } ?: return '#'
    val upper = first.uppercaseChar()
    return if (upper in 'A'..'Z') upper else '#'
}
