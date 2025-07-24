package app.kreate.android.themed.rimusic.component.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kreate.android.themed.common.component.AbstractSearch
import app.kreate.android.themed.common.component.settings.SettingEntrySearch

@Composable
private fun SettingEntrySearch.AnimatedHeader() {
    AnimatedContent(
        targetState = isVisible,
        transitionSpec = {
            if ( targetState ) {
                slideInVertically { height -> height } + expandHorizontally() + fadeIn() togetherWith
                        slideOutVertically { height -> -height } + shrinkHorizontally() + fadeOut()
            } else {
                slideInVertically { height -> -height } + expandHorizontally() + fadeIn() togetherWith
                        slideOutVertically { height -> height } + shrinkHorizontally() + fadeOut()
            }.using(
                // Disable clipping since the faded slide-in/out should
                // be displayed out of bounds.
                SizeTransform(clip = false)
            )
        },
        label = "animated header and search bar"
    ) { target ->
        if (target)
            SearchBar()
        else
            HeaderText( TextAlign.End )
    }
}

@Composable
fun SettingEntrySearch.RiMusicAnimatedHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
                           .height( 40.dp )
                           .padding( horizontal = 10.dp )
    ) {
        HeaderIcon(
            Modifier.padding( end = AbstractSearch.DECO_BOX_ITEM_SPACING.dp )
                    .align( Alignment.CenterVertically )
        )
        AnimatedHeader()
    }
}