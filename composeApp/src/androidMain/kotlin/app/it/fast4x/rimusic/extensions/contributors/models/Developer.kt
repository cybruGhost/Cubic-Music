package app.it.fast4x.rimusic.extensions.contributors.models

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import app.kreate.android.me.knighthat.coil.*
import com.google.gson.annotations.SerializedName
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.styling.favoritesIcon

data class Developer(
    val id: Int,
    @SerializedName("login") val username: String,
    @SerializedName("name") val displayName: String?,
    @SerializedName("html_url") val url: String,
    @SerializedName("avatar_url") val avatar: String,
    val contributions: Int?,
    // Add missing fields from your JSON
    val about: String? = null,
    val why_choose_us: String? = null
) {
    private val handle: String
        get() = url.split("/").last()

    @Composable
    fun Draw() {
        val uriHandler = LocalUriHandler.current
        val avatarPainter = ImageCacheFactory.Painter(this.avatar)
        val backgroundColor = if (id == 1484476) colorPalette().background1 else Color.Transparent

        Card(
            modifier = Modifier
                .padding(start = 12.dp, end = 20.dp, bottom = 10.dp)
                .fillMaxWidth(),
            colors = CardColors(
                containerColor = Color.Transparent,
                contentColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = Color.Transparent
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp, horizontal = 15.dp)
                    .background(backgroundColor, RoundedCornerShape(12.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = avatarPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .border(1.dp, Color.White, RoundedCornerShape(25.dp)),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(Modifier.fillMaxWidth().padding(end = 10.dp)) {
                    Text(
                        text = displayName ?: username,
                        style = TextStyle(
                            color = colorPalette().text,
                            fontSize = typography().xs.fontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Start
                    )

                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            text = "@$handle",
                            style = TextStyle(
                                color = colorPalette().textSecondary,
                                fontSize = typography().xs.fontSize,
                                fontStyle = FontStyle.Italic,
                            ),
                            modifier = Modifier
                                .wrapContentSize()
                                .clickable { uriHandler.openUri(url) },
                        )

                        if (contributions == null)
                            return@Column

                        val color = colorPalette().favoritesIcon.copy(alpha = .8f)

                        Text(
                            text = contributions.toString(),
                            style = TextStyle(
                                color = color,
                                fontSize = typography().xs.fontSize,
                            ),
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(5.dp))

                        Icon(
                            painter = painterResource(R.drawable.git_pull_request_outline),
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(typography().xs.fontSize.value.dp)
                        )
                    }

                    // Display about text if available
                    about?.let { aboutText ->
                        Spacer(modifier = Modifier.padding(top = 4.dp))
                        Text(
                            text = aboutText,
                            style = TextStyle(
                                color = colorPalette().textSecondary,
                                fontSize = typography().xxs.fontSize,
                                fontStyle = FontStyle.Normal,
                            ),
                            textAlign = TextAlign.Start
                        )
                    }

                    // Display why_choose_us text if available
                    why_choose_us?.let { whyText ->
                        Spacer(modifier = Modifier.padding(top = 2.dp))
                        Text(
                            text = whyText,
                            style = TextStyle(
                                color = colorPalette().textSecondary,
                                fontSize = typography().xxs.fontSize,
                                fontStyle = FontStyle.Italic,
                            ),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}