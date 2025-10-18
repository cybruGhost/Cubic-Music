package it.fast4x.rimusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.themed.HeaderWithIcon

@UnstableApi
@Composable
fun DjVeda(
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette().background0)
    ) {
        // Header with back button
        HeaderWithIcon(
            title = "DJ Veda",
            iconId = android.R.drawable.ic_media_previous,
            enabled = true,
            showIcon = true,
            modifier = Modifier,
            onClick = { navController.popBackStack() }
        )

        // Blank content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "DJ Veda - Coming Soon",
                style = typography().m,
                color = colorPalette().text
            )
        }
    }
}