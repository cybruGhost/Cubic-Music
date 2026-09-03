package app.kreate.android.me.knighthat.component.song

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.typography
import app.kreate.android.R
import app.kreate.android.me.knighthat.coil.ImageCacheFactory

@Composable
internal fun SongInformationDialog(
    song: Song,
    albumTitle: String?,
    onDismiss: () -> Unit,
    onChangeTitle: () -> Unit,
    onChangeAuthors: () -> Unit,
    onUploadCover: () -> Unit,
    onRefreshSong: () -> Unit
) {
    val palette = colorPalette()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(8.dp),
        containerColor = palette.background1,
        titleContentColor = palette.text,
        textContentColor = palette.textSecondary,
        icon = {
            Icon(
                painter = painterResource(R.drawable.information),
                contentDescription = null,
                tint = palette.accent
            )
        },
        title = {
            Text(
                text = stringResource(R.string.information),
                style = typography().m.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ImageCacheFactory.AsyncImage(
                        thumbnailUrl = song.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.cleanTitle(),
                            color = palette.text,
                            style = typography().s.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.cleanArtistsText(),
                            color = palette.textSecondary,
                            style = typography().xs,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = palette.background2,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        SongInfoValue(stringResource(R.string.title), song.cleanTitle())
                        SongInfoValue(stringResource(R.string.artists), song.cleanArtistsText())
                        albumTitle?.takeIf { it.isNotBlank() }?.let {
                            SongInfoValue(stringResource(R.string.song_info_album), it)
                        }
                        song.durationText?.takeIf { it.isNotBlank() }?.let {
                            SongInfoValue(stringResource(R.string.sort_duration), it)
                        }
                        SongInfoValue(stringResource(R.string.song_info_listening_time), song.formattedTotalPlayTime)
                        SongInfoValue(stringResource(R.string.song_info_youtube_id), song.id, divider = false)
                    }
                }

                Text(
                    text = stringResource(R.string.song_info_edit_section),
                    color = palette.text,
                    style = typography().xs.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 2.dp)
                )

                SongInfoAction(
                    icon = R.drawable.pencil,
                    title = stringResource(R.string.update_title),
                    detail = stringResource(R.string.song_info_change_title_detail),
                    onClick = {
                        onDismiss()
                        onChangeTitle()
                    }
                )
                SongInfoAction(
                    icon = R.drawable.people,
                    title = stringResource(R.string.update_authors),
                    detail = stringResource(R.string.song_info_change_authors_detail),
                    onClick = {
                        onDismiss()
                        onChangeAuthors()
                    }
                )
                SongInfoAction(
                    icon = R.drawable.image,
                    title = stringResource(R.string.upload_cover),
                    detail = stringResource(R.string.song_info_upload_cover_detail),
                    onClick = {
                        onDismiss()
                        onUploadCover()
                    }
                )
                SongInfoAction(
                    icon = R.drawable.refresh,
                    title = stringResource(R.string.update),
                    detail = stringResource(R.string.song_info_refresh_detail),
                    onClick = {
                        onDismiss()
                        onRefreshSong()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), color = palette.accent)
            }
        }
    )
}

@Composable
private fun SongInfoValue(
    label: String,
    value: String,
    divider: Boolean = true
) {
    val palette = colorPalette()
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(text = label, color = palette.textSecondary, style = typography().xxs)
        Text(
            text = value,
            color = palette.text,
            style = typography().xs,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (divider) HorizontalDivider(color = palette.textDisabled.copy(alpha = 0.25f))
}

@Composable
private fun SongInfoAction(
    @DrawableRes icon: Int,
    title: String,
    detail: String,
    onClick: () -> Unit
) {
    val palette = colorPalette()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = palette.text, style = typography().xs.copy(fontWeight = FontWeight.SemiBold))
            Text(text = detail, color = palette.textSecondary, style = typography().xxs)
        }
    }
}
