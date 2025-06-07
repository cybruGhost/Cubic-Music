package it.fast4x.rimusic.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.fast4x.rimusic.colorPalette
import androidx.compose.foundation.basicMarquee

val GridMenuItemHeight = 96.dp

@Composable
fun GridMenu(
    modifier: Modifier = Modifier,
    topContent: @Composable (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyGridScope.() -> Unit
) {

    Column(
        modifier = modifier
            .padding(top = 48.dp)
            .fillMaxWidth()
            .background(colorPalette().background1)
            .padding(top = 2.dp)
            .padding(vertical = 8.dp),
    ) {

        if (topContent != null) {
            topContent()
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = modifier,
            contentPadding = contentPadding,
            content = content,
        )
    }

}


fun LazyGridScope.GridMenuItem(
    modifier: Modifier = Modifier,
    colorIcon: Color,
    colorText: Color,
    @DrawableRes icon: Int,
    @StringRes title: Int,
    titleString: String = "",
    enabled: Boolean = true,
    onClick: () -> Unit,
    disableScrollingText: Boolean = false
) = GridMenuItem(
    modifier = modifier,
    icon = {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colorIcon,
            modifier = Modifier.size(20.dp)
        )
    },
    title = title,
    titleString = titleString,
    enabled = enabled,
    onClick = onClick,
    colorText = colorText,
    disableScrollingText = disableScrollingText
)


fun LazyGridScope.GridMenuItem(
    modifier: Modifier = Modifier,
    colorText: Color,
    icon: @Composable BoxScope.() -> Unit,
    @StringRes title: Int,
    titleString: String = "",
    enabled: Boolean = true,
    onClick: () -> Unit,
    disableScrollingText: Boolean = false
) {
    item {
        Column(
            modifier = modifier
                .clip(ShapeDefaults.Large)
                .height(GridMenuItemHeight)
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
                .alpha(if (enabled) 1f else 0.5f)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
                content = icon
            )
            Text(
                text = titleString.ifEmpty { stringResource(title) },
                overflow = TextOverflow.Ellipsis,
                color = colorText,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (!disableScrollingText) it.basicMarquee(iterations = Int.MAX_VALUE) else it }
            )
        }
    }
}