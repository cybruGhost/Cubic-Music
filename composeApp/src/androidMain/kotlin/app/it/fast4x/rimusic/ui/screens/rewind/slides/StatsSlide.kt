package app.it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.ui.styling.LocalAppearance
import app.it.fast4x.rimusic.ui.styling.ColorPalette
import app.it.fast4x.rimusic.ui.styling.Typography
import app.it.fast4x.rimusic.ui.styling.favoritesIcon
import app.it.fast4x.rimusic.ui.styling.shimmer
import app.it.fast4x.rimusic.ui.screens.rewind.RewindData
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.InkFlow

@Composable
fun StatsSlide(
    username: String,
    data: RewindData,
    userRanking: String,
    userPercentile: String,
    showMinutesInHours: Boolean,
    onNext: () -> Unit
) {
    val (colorPalette, typography, _) = LocalAppearance.current
    val configuration = LocalConfiguration.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shaderBackground(InkFlow)
    ) {
        // Enhanced gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorPalette.background0.copy(alpha = 0.3f),
                            colorPalette.background2.copy(alpha = 0.2f),
                            colorPalette.background0.copy(alpha = 0.3f)
                        )
                    )
                )
        )
        
        // Decorative circles
        Box(modifier = Modifier.fillMaxSize()) {
            // Decorative circle 1
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-50).dp, y = (-50).dp)
                    .clip(CircleShape)
                    .background(colorPalette.accent.copy(alpha = 0.03f))
            )
            
            // Decorative circle 2
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 50.dp, y = 50.dp)
                    .clip(CircleShape)
                    .background(colorPalette.favoritesIcon.copy(alpha = 0.03f))
            )
            
            // Decorative circle 3
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(colorPalette.shimmer.copy(alpha = 0.02f))
            )
        }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            item {
                HeaderSection(colorPalette = colorPalette)
            }
            
            // Main Stats Grid
            item {
                MainStatsGrid(
                    data = data,
                    showMinutesInHours = showMinutesInHours,
                    colorPalette = colorPalette,
                    typography = typography
                )
            }
            
            // Ranking Card
            item {
                RankingCard(
                    userRanking = userRanking,
                    userPercentile = userPercentile,
                    colorPalette = colorPalette,
                    typography = typography
                )
            }
            
            // Additional Stats Section
            item {
                AdditionalStatsSection(
                    data = data,
                    colorPalette = colorPalette,
                    typography = typography
                )
            }
            
            // Navigation Hint
            item {
                NavigationHint(
                    colorPalette = colorPalette,
                    typography = typography
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    colorPalette: ColorPalette
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colorPalette.accent.copy(alpha = 0.2f),
                            colorPalette.background2.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            colorPalette.accent,
                            colorPalette.shimmer,
                            colorPalette.favoritesIcon,
                            colorPalette.accent
                        )
                    ),
                    shape = CircleShape
                )
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colorPalette.accent.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension / 2
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✨",
                fontSize = 42.sp,
                color = colorPalette.text
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Title
        Text(
            text = "YEAR IN MUSIC",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colorPalette.accent,
            letterSpacing = 2.sp
        )
        
        Text(
            text = "Your listening journey visualized",
            fontSize = 14.sp,
            color = colorPalette.textSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MainStatsGrid(
    data: RewindData,
    showMinutesInHours: Boolean,
    colorPalette: ColorPalette,
    typography: Typography
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Total Plays Card
            StatCard(
                modifier = Modifier.weight(1f),
                value = data.stats.totalPlays.toString(),
                label = "Total Plays",
                icon = "▶",
                gradientColors = listOf(
                    Color(0xFFFF6B6B),
                    Color(0xFFFF8E8E)
                ),
                colorPalette = colorPalette,
                typography = typography
            )
            
            // Listening Time Card
            StatCard(
                modifier = Modifier.weight(1f),
                value = if (showMinutesInHours) {
                    val hours = data.stats.totalMinutes / 60
                    val minutes = data.stats.totalMinutes % 60
                    if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
                } else {
                    "${data.stats.totalMinutes}m"
                },
                label = "Listening Time",
                icon = "⏱",
                gradientColors = listOf(
                    Color(0xFF4ECDC4),
                    Color(0xFF45B7D1)
                ),
                colorPalette = colorPalette,
                typography = typography
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Unique Songs Card
            StatCard(
                modifier = Modifier.weight(1f),
                value = data.totalUniqueSongs.toString(),
                label = "Unique Songs",
                icon = "🎵",
                gradientColors = listOf(
                    Color(0xFFFFD93D),
                    Color(0xFFFFB347)
                ),
                colorPalette = colorPalette,
                typography = typography
            )
            
            // Active Days Card
            StatCard(
                modifier = Modifier.weight(1f),
                value = data.daysWithMusic.toString(),
                label = "Active Days",
                icon = "📅",
                gradientColors = listOf(
                    Color(0xFF6C5CE7),
                    Color(0xFFA463F5)
                ),
                colorPalette = colorPalette,
                typography = typography
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    value: String,
    label: String,
    icon: String,
    gradientColors: List<Color>,
    colorPalette: ColorPalette,
    typography: Typography
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            colorPalette.background2.copy(alpha = 0.4f),
                            colorPalette.background1.copy(alpha = 0.3f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(vertical = 20.dp, horizontal = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    gradientColors[0].copy(alpha = 0.3f),
                                    gradientColors[1].copy(alpha = 0.1f)
                                )
                            )
                        )
                        .border(
                            width = 1.5f.dp,
                            brush = Brush.linearGradient(
                                colors = gradientColors.map { it.copy(alpha = 0.5f) }
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 24.sp,
                        color = colorPalette.text
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Value
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = gradientColors[0]
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Label
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = colorPalette.textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RankingCard(
    userRanking: String,
    userPercentile: String,
    colorPalette: ColorPalette,
    typography: Typography
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorPalette.background2.copy(alpha = 0.4f),
                            colorPalette.background1.copy(alpha = 0.3f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.5f),
                            Color(0xFFFFA500).copy(alpha = 0.3f),
                            Color(0xFFFFD700).copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trophy
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .drawBehind {
                            drawCircle(
                                color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                radius = size.minDimension / 1.5f
                            )
                        }
                ) {
                    Text(
                        text = "🏆",
                        fontSize = 48.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "LISTENER RANKING",
                    fontSize = 12.sp,
                    color = colorPalette.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = userRanking,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD700)
                )
                
                Text(
                    text = userPercentile,
                    fontSize = 16.sp,
                    color = colorPalette.accent,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = 0.75f,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFFFD700),
                    trackColor = colorPalette.background2.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun AdditionalStatsSection(
    data: RewindData,
    colorPalette: ColorPalette,
    typography: Typography
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorPalette.background1.copy(alpha = 0.3f),
                            colorPalette.background2.copy(alpha = 0.2f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(16.dp)
        ) {
            val additionalStats = listOf(
                Triple("👤", "Unique Artists", data.totalUniqueArtists.toString()),
                Triple("💿", "Unique Albums", data.totalUniqueAlbums.toString()),
                Triple("📋", "Unique Playlists", data.totalUniquePlaylists.toString())
            )
            
            additionalStats.forEachIndexed { index, (icon, label, value) ->
                AdditionalStatRow(
                    icon = icon,
                    label = label,
                    value = value,
                    colorPalette = colorPalette,
                    typography = typography,
                    showDivider = index < additionalStats.size - 1
                )
            }
        }
    }
}

@Composable
private fun AdditionalStatRow(
    icon: String,
    label: String,
    value: String,
    colorPalette: ColorPalette,
    typography: Typography,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colorPalette.background2.copy(alpha = 0.3f))
                        .border(
                            width = 1.dp,
                            color = colorPalette.accent.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 18.sp,
                        color = colorPalette.text
                    )
                }
                
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = colorPalette.textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = value,
                fontSize = 16.sp,
                color = colorPalette.text,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (showDivider) {
            Divider(
                modifier = Modifier.padding(start = 48.dp),
                color = colorPalette.background2.copy(alpha = 0.3f),
                thickness = 1.dp
            )
        }
    }
}

@Composable
private fun NavigationHint(
    colorPalette: ColorPalette,
    typography: Typography
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        // Arrow
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colorPalette.accent.copy(alpha = 0.1f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            colorPalette.accent.copy(alpha = 0.5f),
                            colorPalette.shimmer.copy(alpha = 0.3f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "→",
                fontSize = 24.sp,
                color = colorPalette.accent
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Swipe to continue your journey",
            fontSize = 10.sp,
            color = colorPalette.textDisabled,
            fontWeight = FontWeight.Normal
        )
    }
}