package app.it.fast4x.rimusic.ui.components.navigation.header

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import app.kreate.android.R
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.extensions.games.pacman.Pacman
import app.it.fast4x.rimusic.ui.components.themed.Button
import app.it.fast4x.rimusic.ui.styling.favoritesIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppHeader(
    val navController: NavController
) {

    companion object {

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun colors(): TopAppBarColors = TopAppBarColors(
            containerColor = colorPalette().background0,
            scrolledContainerColor = colorPalette().background0,
            navigationIconContentColor = colorPalette().background0,
            titleContentColor = colorPalette().text,
            subtitleContentColor = colorPalette().text.copy(alpha = 0.7f), // added
            actionIconContentColor = colorPalette().text
        )
    }

    @Composable
    private fun BackButton() {
        if ( NavRoutes.home.isNotHere( navController ) )
            androidx.compose.material3.IconButton(
                onClick = {
                    if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED)
                        navController.popBackStack()
                }
            ) {
                Button(
                    R.drawable.chevron_back,
                    colorPalette().favoritesIcon,
                    0.dp,
                    24.dp
                ).Draw()
            }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Draw(
        enableBounceAnimation: Boolean = true,
        autoBounce: Boolean = false,
        bounceTrigger: Boolean = false
    ) {
        val showGames by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        
        // Animation states
        var isVisible by remember { mutableStateOf(false) }
        
        // Scale animation for bounce effect
        val scale = remember { Animatable(1f) }
        
        // Entrance animation
        val offsetY by animateIntAsState(
            targetValue = if (isVisible) 0 else -300,
            animationSpec = spring(
                dampingRatio = 0.5f,
                stiffness = 50f
            ),
            label = "offsetY"
        )

        val alpha by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 800),
            label = "alpha"
        )

        // Trigger entrance animation
        LaunchedEffect(Unit) {
            isVisible = true
        }

        // Bounce animation
        LaunchedEffect(autoBounce, bounceTrigger) {
            if (autoBounce || bounceTrigger) {
                while (true) {
                    // Bounce in
                    launch {
                        scale.animateTo(
                            targetValue = 1.2f,
                            animationSpec = tween(150, easing = FastOutSlowInEasing)
                        )
                    }
                    
                    delay(150)
                    
                    // Bounce out
                    launch {
                        scale.animateTo(
                            targetValue = 0.9f,
                            animationSpec = tween(120, easing = FastOutSlowInEasing)
                        )
                    }
                    
                    delay(120)
                    
                    // Return to normal
                    launch {
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(200, easing = FastOutSlowInEasing)
                        )
                    }
                    
                    if (!autoBounce) break
                    delay(3000)
                }
            }
        }

        if (showGames) {
            Pacman()
        }

        // Animated Surface container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, offsetY) }
                .alpha(alpha)
                .scale(
                    scaleX = if (enableBounceAnimation) scale.value else 1f,
                    scaleY = if (enableBounceAnimation) scale.value else 1f
                )
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
            color = Color.Transparent,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colorPalette().background1.copy(alpha = 0.85f),
                                colorPalette().background1.copy(alpha = 0.95f)
                            )
                        )
                    )
            ) {
                // Your original TopAppBar
                TopAppBar(
                    title = { AppTitle(navController, context) },
                    actions = { ActionBar(navController) },
                    navigationIcon = { BackButton() },
                    scrollBehavior = scrollBehavior,
                    colors = colors()
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Draw() {
        Draw(enableBounceAnimation = false)
    }
}