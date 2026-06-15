package app.it.fast4x.rimusic.ui.components.navigation.header

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.kreate.android.R
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import app.it.fast4x.rimusic.RescueCenterActivity
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.FontType
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.extensions.pip.isPipSupported
import app.it.fast4x.rimusic.extensions.pip.rememberPipHandler
import app.it.fast4x.rimusic.extensions.youtubelogin.YouTubeSessionStore
import app.it.fast4x.rimusic.extensions.youtubelogin.YoutubeSession
import app.it.fast4x.rimusic.ui.components.navigation.header.HeaderIcon
import app.it.fast4x.rimusic.utils.enablePictureInPictureKey
import app.it.fast4x.rimusic.utils.logDebugEnabledKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.showRescueCenterInMenuKey
import app.it.fast4x.rimusic.utils.ytCookieKey
import app.it.fast4x.rimusic.ui.styling.Typography
import app.it.fast4x.rimusic.ui.styling.typographyOf
import app.it.fast4x.rimusic.ui.components.themed.DropdownMenu as ThemedDropdownMenu
import androidx.compose.ui.window.PopupProperties

@Composable
private fun HamburgerMenu(
    expanded: Boolean,
    onItemClick: (NavRoutes) -> Unit,
    onRescueClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val enablePictureInPicture by rememberPreference(enablePictureInPictureKey, false)
    val showRescueCenterInMenu by rememberPreference(showRescueCenterInMenuKey, true)
    val logDebugEnabled by rememberPreference(logDebugEnabledKey, false)
    val pipHandler = rememberPipHandler()
    val showRescueCenter = showRescueCenterInMenu || logDebugEnabled
    
    // Get typography instance
    val typography = typographyOf(
        color = colorPalette().text,
        useSystemFont = false,
        applyFontPadding = true,
        fontType = FontType.GothicBold
    )

    // Menu items data
    val menuItems = remember(showRescueCenter, enablePictureInPicture) {
        buildList {
            add(MenuItem(R.drawable.history, R.string.history, NavRoutes.history))
            add(MenuItem(R.drawable.stats_chart, R.string.statistics, NavRoutes.statistics))
            add(MenuItem(R.drawable.trophy, R.string.rewind, NavRoutes.rewind))
            add(MenuItem(R.drawable.heart_gift, R.string.donate, NavRoutes.donate))
            if (showRescueCenter) {
                add(MenuItem(R.drawable.rescue, R.string.rescue_center, isRescueItem = true))
            }
            if (isPipSupported && enablePictureInPicture) {
                add(MenuItem(
                    iconRes = R.drawable.images_sharp,
                    textRes = R.string.menu_go_to_picture_in_picture,
                    isPipItem = true
                ))
            }
            add(MenuItem(isDivider = true))
            add(MenuItem(R.drawable.settings, R.string.settings, NavRoutes.settings, isLast = true))
        }
    }

    // Create the custom dropdown menu - adjusted width and positioning
    val dropdownMenu = ThemedDropdownMenu(
        expanded = expanded,
        containerColor = colorPalette().background0.copy(alpha = 0.90f),
        modifier = Modifier
            .width(260.dp) // Reduced from 280.dp to 260.dp (quarter reduction)
            .padding(top = 12.dp), // Increased from 8.dp to 12.dp (micro-millimeter down)
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    )

    // Add menu items to the dropdown with adjusted internal padding
    dropdownMenu.add {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp) // Reduced horizontal padding slightly
        ) {
            menuItems.forEachIndexed { index, item ->
                when {
                    item.isDivider -> {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp), // Reduced horizontal padding
                            color = colorPalette().accent.copy(alpha = 0.7f)
                        )
                    }
                    else -> {
                        ModernMenuItem(
                            index = index,
                            iconRes = item.iconRes,
                            textRes = item.textRes,
                            onClick = {
                                if (item.isPipItem) {
                                    pipHandler.enterPictureInPictureMode()
                                } else if (item.isRescueItem) {
                                    onRescueClick()
                                } else {
                                    onItemClick(item.route!!)
                                }
                                onDismissRequest()
                            },
                            isLast = item.isLast,
                            typography = typography
                        )
                    }
                }
            }
        }
    }

    // Draw the dropdown menu
    dropdownMenu.Draw()
}

@Composable
private fun ModernMenuItem(
    index: Int,
    @androidx.annotation.DrawableRes iconRes: Int,
    @androidx.annotation.StringRes textRes: Int,
    onClick: () -> Unit,
    isLast: Boolean = false,
    typography: Typography
) {
    var isVisible by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = index * 30,
            easing = FastOutSlowInEasing
        ), label = "alpha"
    )

    val offsetX by animateIntAsState(
        targetValue = if (isVisible) 0 else -20,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = index * 30
        ), label = "offsetX"
    )

    LaunchedEffect(Unit) { isVisible = true }

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else 4.dp)
            .offset { IntOffset(offsetX, 0) }
            .alpha(alpha),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp) // Reduced horizontal padding slightly
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = colorPalette().accent.copy(alpha = 0.5f),
                modifier = Modifier.size(34.dp) // Slightly reduced from 36.dp to 34.dp for better fit
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = colorPalette().text,
                    modifier = Modifier
                        .padding(7.dp) // Slightly reduced from 8.dp to 7.dp
                        .size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp)) // Slightly reduced from 12.dp to 10.dp

            Text(
                text = stringResource(id = textRes),
                style = typography.s,
                color = colorPalette().text
            )
        }
    }
}

// Data class to hold menu item information
private data class MenuItem(
    val iconRes: Int = 0,
    val textRes: Int = 0,
    val route: NavRoutes? = null,
    val isDivider: Boolean = false,
    val isPipItem: Boolean = false,
    val isRescueItem: Boolean = false,
    val isLast: Boolean = false
)

@Composable
fun ActionBar(
    navController: NavController,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val activeCookie by rememberPreference(ytCookieKey, "")
    val currentSession: YoutubeSession? = remember(activeCookie) {
        YouTubeSessionStore.applyCurrentSession(context)
    }
    val activeThumbnail = currentSession?.accountThumbnail.orEmpty()
    val isYouTubeLoggedIn = remember(activeCookie, currentSession?.sessionId) {
        YouTubeSessionStore.hasAuthCookies(currentSession?.cookie ?: activeCookie)
    }

    // Search Icon
    HeaderIcon(R.drawable.search) { 
        navController.navigate(NavRoutes.search.name) 
    }

    if (isYouTubeLoggedIn) {
        if (activeThumbnail.isNotBlank()) {
            ImageCacheFactory.AsyncImage(
                thumbnailUrl = activeThumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { expanded = !expanded }
            )
        } else {
            HeaderIcon(R.drawable.ytmusic, size = 32.dp) {
                expanded = !expanded 
            }
        }
    } else {
        HeaderIcon(R.drawable.burger) { 
            expanded = !expanded 
        }
    }

    // Define actions for when item inside menu clicked,
    // and when user clicks on places other than the menu (dismiss)
    val onItemClick: (NavRoutes) -> Unit = {
        expanded = false
        navController.navigate(it.name)
    }
    val onRescueClick: () -> Unit = {
        expanded = false
        context.startActivity(Intent(context, RescueCenterActivity::class.java))
    }
    val onDismissRequest: () -> Unit = { expanded = false }

    // Hamburger menu
    HamburgerMenu(
        expanded = expanded,
        onItemClick = onItemClick,
        onRescueClick = onRescueClick,
        onDismissRequest = onDismissRequest
    )
}
