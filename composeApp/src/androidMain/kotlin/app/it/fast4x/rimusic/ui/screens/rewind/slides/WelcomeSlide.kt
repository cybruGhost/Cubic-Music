package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.PurpleLiquid
import app.it.fast4x.rimusic.ui.styling.LocalAppearance
import kotlinx.coroutines.delay

@Composable
fun WelcomeSlide(
    username: String,
    year: Int,
    onNext: () -> Unit
) {
    val (colorPalette, typography, thumbnailShape) = LocalAppearance.current
    val configuration = LocalConfiguration.current
    
    // Canvas height - exactly 50%
    val canvasHeight = configuration.screenHeightDp.dp * 0.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette.background0)
    ) {
        // Top half - Canvas with gradient fade at 50%
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight)
                .align(Alignment.TopCenter)
                .shaderBackground(PurpleLiquid)
        ) {
            // Gradient overlay that fades at the 50% mark
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                colorPalette.background0.copy(alpha = 0.3f),
                                colorPalette.background0.copy(alpha = 0.7f),
                                colorPalette.background0
                            )
                        )
                    )
            )
        }
        
 // Content - now properly spaced
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Space after canvas - reduced
          Spacer(modifier = Modifier.height(canvasHeight - 80.dp))
            // Main greeting section - more compact
            Text(
                text = "We've got your year",
                style = typography.xxl.copy(
                    fontWeight = FontWeight.Bold,
                    color = colorPalette.text
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "wrapped up.",
                style = typography.xl.copy(
                    fontWeight = FontWeight.Medium,
                    color = colorPalette.textSecondary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // REWIND text
            Text(
                text = "REWIND",
                style = typography.xlxl.copy(
                    fontWeight = FontWeight.Black,
                    color = colorPalette.accent
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Year display
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colorPalette.background2
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .wrapContentWidth()
                    .border(
                        width = 2.dp,
                        color = colorPalette.accent,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Text(
                    text = year.toString(),
                    style = typography.xxl.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = colorPalette.accent
                    ),
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Personal greeting
            Surface(
                color = colorPalette.background1,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp,
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = colorPalette.textSecondary,
                                fontSize = 18.sp
                            )
                        ) {
                            append("Hello, ")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = colorPalette.accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        ) {
                            append(username)
                        }
                        withStyle(
                            style = SpanStyle(
                                color = colorPalette.textSecondary,
                                fontSize = 18.sp
                            )
                        ) {
                            append("! 👋")
                        }
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom section - now with better spacing
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                // Privacy note
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = colorPalette.background2.copy(alpha = 0.9f)
                    ),
                    shape = thumbnailShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Privacy icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colorPalette.accent.copy(alpha = 0.15f))
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔒",
                                fontSize = 18.sp
                            )
                        }

                        Column {
                            Text(
                                text = "Your privacy is respected!",
                                style = typography.s.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorPalette.text
                                )
                            )
                            Text(
                                text = "All data stays on your device",
                                style = typography.xxs.copy(
                                    color = colorPalette.textSecondary
                                ),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Swipe hint
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var arrowOffset by remember { mutableStateOf(0f) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            arrowOffset = 8f
                            delay(500)
                            arrowOffset = 0f
                            delay(500)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colorPalette.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "→",
                            fontSize = 24.sp,
                            color = colorPalette.accent,
                            modifier = Modifier.absoluteOffset(x = arrowOffset.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Swipe to continue",
                        style = typography.xxs.copy(
                            fontWeight = FontWeight.Medium,
                            color = colorPalette.textSecondary
                        )
                    )
                }
            }
        }
    }
}