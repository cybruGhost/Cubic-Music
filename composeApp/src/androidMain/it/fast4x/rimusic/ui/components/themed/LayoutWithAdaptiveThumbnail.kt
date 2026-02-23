package it.fast4x.rimusic.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import app.kreate.android.R
import com.valentinilk.shimmer.shimmer
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.PlayerThumbnailSize
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.ui.styling.shimmer
import it.fast4x.rimusic.utils.isLandscape
import it.fast4x.rimusic.utils.playerThumbnailSizeKey
import it.fast4x.rimusic.utils.rememberPreference
import me.knighthat.coil.ImageCacheFactory


@Composable
inline fun LayoutWithAdaptiveThumbnail(
    thumbnailContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val isLandscape = isLandscape

    if (isLandscape) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            thumbnailContent()
            content()
        }
    } else {
        content()
    }
}

@UnstableApi
fun adaptiveThumbnailContent(
    isLoading: Boolean,
    url: String?,
    shape: Shape? = null,
    showIcon: Boolean = false,
    onOtherVersionAvailable: (() -> Unit)? = {},
    onClick: (() -> Unit)? = {}
): @Composable () -> Unit = {
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val thumbnailSizeDp = if (isLandscape) (maxHeight - 128.dp) else (maxWidth - 64.dp)
        val playerThumbnailSize by rememberPreference(playerThumbnailSizeKey, PlayerThumbnailSize.Medium)

        val modifier = Modifier
            //.padding(all = 16.dp)
            .padding(horizontal = playerThumbnailSize.size.dp)
            .padding(top = 16.dp)
            .clip(shape ?: thumbnailShape())
            .clickable {
                if (onClick != null) {
                    onClick()
                }
            }
            //.size(thumbnailSizeDp)

        if (isLoading) {
            Spacer(
                modifier = modifier
                    .shimmer()
                    .background(colorPalette().shimmer)
            )
        } else {
            ImageCacheFactory.Thumbnail(
                thumbnailUrl = url,
                modifier = modifier
            )
            if(showIcon)
                onOtherVersionAvailable?.let {
                    Box(
                        modifier = modifier
                            .align(Alignment.BottomEnd)
                            .fillMaxWidth(0.2f)
                    ) {
                        HeaderIconButton(
                            icon = R.drawable.alternative_version,
                            color = colorPalette().text,
                            onClick = {
                                onOtherVersionAvailable()
                            },
                            modifier = Modifier.size(35.dp)
                        )
                    }
                }
        }
    }
}
