package app.it.fast4x.rimusic.ui.screens.donate

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.styling.*
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(
    onBackClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    val typography = appearance.typography

    val koFiUrl = "https://ko-fi.com/anonghost40418/"
    val githubUrl = "https://github.com/cybruGhost/Cubic-Music"
    val profileImageUrl = "https://avatars.githubusercontent.com/u/113809799?v=4&size=200"
    val koFiImageUrl = "https://storage.ko-fi.com/cdn/useruploads/351b9585-ab4f-4264-82bf-bd4f6a21619f_89c804f1-994c-412d-b21a-a110c577b372.jpeg"
    
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette.background0)
    ) {
        // Animated Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension * 0.8f
            
            // Rotating gradient circles
            for (i in 0..2) {
                val offset = i * 120f
                rotate(offset) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colorPalette.accent.copy(alpha = glowAlpha * 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(centerX - radius * 0.2f, centerY - radius * 0.2f)
                        ),
                        radius = radius * 0.4f,
                        center = Offset(centerX, centerY)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Back Button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorPalette.background2.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = colorPalette.text
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                // Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Profile Image with glow
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .drawBehind {
                                drawCircle(
                                    color = colorPalette.accent.copy(alpha = glowAlpha * 0.3f),
                                    radius = size.minDimension / 2 + 15f
                                )
                            }
                    ) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Developer Profile",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 3.dp,
                                    color = colorPalette.accent,
                                    shape = CircleShape
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Anon Ghost",
                        style = typography.xxxl.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = colorPalette.text
                    )

                    Text(
                        text = "Mobile App Developer",
                        style = typography.m.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = colorPalette.accent,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // GitHub Link
                    Card(
                        onClick = { uriHandler.openUri(githubUrl) },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorPalette.background2
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Code,
                                contentDescription = null,
                                tint = colorPalette.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "github.com/cybruGhost",
                                style = typography.s,
                                color = colorPalette.textSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = colorPalette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))

                // Support Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    colorPalette.accent,
                                    colorPalette.accent.copy(alpha = 0.7f),
                                    colorPalette.background2
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❤️ Support Development ❤️",
                            style = typography.xl.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = colorPalette.onAccent
                        )
                        Text(
                            text = "Keep the music playing",
                            style = typography.s,
                            color = colorPalette.onAccent.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Ko-fi Banner
                Card(
                    onClick = { uriHandler.openUri(koFiUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = koFiImageUrl,
                            contentDescription = "Ko-fi Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            colorPalette.background0.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )
                        
                        // Text Overlay
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = "Support on Ko-fi",
                                style = typography.xl.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "Buy me a coffee ☕",
                                style = typography.m,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Support Info
                Text(
                    text = "Support via Ko-fi:",
                    style = typography.l.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colorPalette.text,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Payment Methods Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorPalette.background2
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Ko-fi accepts:",
                            style = typography.s.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = colorPalette.textSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            PaymentMethod(
                                icon = "💳",
                                name = "Credit Card",
                                colorPalette = colorPalette
                            )
                            PaymentMethod(
                                icon = "💰",
                                name = "PayPal",
                                colorPalette = colorPalette
                            )
                            PaymentMethod(
                                icon = "🏦",
                                name = "Bank Transfer",
                                colorPalette = colorPalette
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Main Support Button
                Button(
                    onClick = { uriHandler.openUri(koFiUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF29ABE0)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Support on Ko-fi",
                        style = typography.l.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Thank You Message
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorPalette.background2
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Thank You! ",
                            style = typography.xl.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = colorPalette.accent
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Your support motivates me. Every contribution, no matter how small, is greatly appreciated!",
                            style = typography.s,
                            color = colorPalette.textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(3) {
                                Text(
                                    "❤️",
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PaymentMethod(
    icon: String,
    name: String,
    colorPalette: ColorPalette
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colorPalette.background3)
                .border(
                    width = 1.dp,
                    color = colorPalette.accent.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            color = colorPalette.background3,
            shape = CircleShape
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = icon,
                    fontSize = 20.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = LocalAppearance.current.typography.xxs,
            color = colorPalette.textSecondary
        )
    }
}