package app.kreate.android.me.knighthat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import app.kreate.android.R
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.styling.Dimensions

@Composable
fun FolderItem(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = colorPalette().background1.copy(alpha = 0.72f),
    onClick: () -> Unit = {}
) {
    val folderShape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy( 12.dp ),
        modifier = modifier.fillMaxWidth()
                           .clip( folderShape )
                           .background(
                               Brush.horizontalGradient(
                                   listOf(
                                       colorPalette().accent.copy(alpha = 0.14f),
                                       backgroundColor,
                                       colorPalette().background0.copy(alpha = 0.82f)
                                   )
                               )
                           )
                           .border(
                               width = 1.dp,
                               brush = Brush.horizontalGradient(
                                   listOf(
                                       colorPalette().accent.copy(alpha = 0.35f),
                                       colorPalette().textDisabled.copy(alpha = 0.16f)
                                   )
                               ),
                               shape = folderShape
                           )
                           .clickable( onClick = onClick )
                           .padding(
                               vertical = Dimensions.itemsVerticalPadding + 2.dp,
                               horizontal = 16.dp
                           )
    )
 {
        Box(
            Modifier
                .size( Dimensions.thumbnails.song )
                .clip(RoundedCornerShape(14.dp))
                .background(colorPalette().background0.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource( R.drawable.folder ),
                tint = colorPalette().accent,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.72f)
            )
        }

        BasicText(
            text = text,
            style = typography().m.copy( color = colorPalette().text )
        )
    }
}
