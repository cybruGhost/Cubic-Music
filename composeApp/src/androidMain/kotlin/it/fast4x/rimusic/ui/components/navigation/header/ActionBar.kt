package it.fast4x.rimusic.ui.components.navigation.header

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.kreate.android.R
import me.knighthat.coil.ImageCacheFactory
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.FontType
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.extensions.pip.isPipSupported
import it.fast4x.rimusic.extensions.pip.rememberPipHandler
import it.fast4x.rimusic.ui.components.navigation.header.HeaderIcon
import it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import it.fast4x.rimusic.utils.enablePictureInPictureKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.ytAccountThumbnail
import it.fast4x.rimusic.ui.styling.Typography
import it.fast4x.rimusic.ui.styling.typographyOf
import it.fast4x.rimusic.ui.components.themed.DropdownMenu as ThemedDropdownMenu
import androidx.compose.ui.window.PopupProperties

@Composable
private fun HamburgerMenu(
    expanded: Boolean,
    onItemClick: (NavRoutes) -> Unit,
    onDismissRequest: () -> Unit
) {
    val enablePictureInPicture by rememberPreference(enablePictureInPictureKey, false)
    val pipHandler = rememberPipHandler()
    
    // Get typography instance
    val typography = typographyOf(
        color = colorPalette().text,
        useSystemFont = false,
        applyFontPadding = true,
        fontType = FontType.Rubik
    )

    // Menu items data
    val menuItems = remember {
        buildList {
            add(MenuItem(R.drawable.history, R.string.history, NavRoutes.history))
            add(MenuItem(R.drawable.stats_chart, R.string.statistics, NavRoutes.statistics))
            add(MenuItem(R.drawable.trophy, R.string.rewind, NavRoutes.rewind))
            add(MenuItem(R.drawable.heart_gift, R.string.donate, NavRoutes.donate))
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
    val isLast: Boolean = false
)

@Composable
fun ActionBar(
    navController: NavController,
) {
    var expanded by remember { mutableStateOf(false) }

    // Search Icon
    HeaderIcon(R.drawable.search) { 
        navController.navigate(NavRoutes.search.name) 
    }

    if (isYouTubeLoggedIn()) {
        if (ytAccountThumbnail() != "") {
            ImageCacheFactory.AsyncImage(
                thumbnailUrl = ytAccountThumbnail(),
                contentDescription = null,
                modifier = Modifier
                    .height(40.dp)
                    .padding(end = 10.dp)
                    .clickable { expanded = !expanded }
            )
        } else {
            HeaderIcon(R.drawable.ytmusic, size = 30.dp) { 
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
    val onDismissRequest: () -> Unit = { expanded = false }

    // Hamburger menu
    HamburgerMenu(
        expanded = expanded,
        onItemClick = onItemClick,
        onDismissRequest = onDismissRequest
    )
}