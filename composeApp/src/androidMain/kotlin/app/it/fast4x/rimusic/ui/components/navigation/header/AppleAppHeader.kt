package app.it.fast4x.rimusic.ui.components.navigation.header

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.utils.bold
import app.it.fast4x.rimusic.utils.secondary
import app.kreate.android.R

@Composable
fun AppleAppHeader(navController: NavController) {
    val isHome = NavRoutes.home.isHere(navController)

    Surface(
        color = colorPalette().background0,
        contentColor = colorPalette().text,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .heightIn(min = 54.dp)
                    .padding(start = 10.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isHome) {
                    IconButton(
                        onClick = {
                            if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chevron_back),
                            contentDescription = null,
                            tint = colorPalette().accent,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }

                Text(
                    text = if (isHome) stringResource(R.string.apple_listen_now) else "Cubic Music",
                    style = typography().xxl.bold,
                    color = colorPalette().text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !isHome) {
                            if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                                val returnedHome = navController.popBackStack(NavRoutes.home.name, false)
                                if (!returnedHome) {
                                    navController.navigate(NavRoutes.home.name) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                        .padding(start = if (isHome) 10.dp else 2.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ActionBar(navController)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colorPalette().text.copy(alpha = 0.08f))
            )
        }
    }
}
