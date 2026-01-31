package it.fast4x.rimusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.models.UserPresence
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.semiBold

@Composable
fun FriendActivityCard(
    presence: UserPresence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorPalette().background1)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Online indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (presence.isOnline) Color.Green else Color.Gray)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = presence.userId,
                style = typography().s.semiBold,
                color = colorPalette().text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (presence.isPlaying && presence.currentSongTitle != null) {
                Text(
                    text = "🎵 ${presence.currentSongTitle}",
                    style = typography().xs,
                    color = colorPalette().textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                presence.currentArtist?.let { artist ->
                    Text(
                        text = artist,
                        style = typography().xxs,
                        color = colorPalette().textDisabled,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = if (presence.isOnline) "Online" else "Offline",
                    style = typography().xs,
                    color = colorPalette().textSecondary
                )
            }
        }
    }
}