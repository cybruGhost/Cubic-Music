package it.fast4x.rimusic.ui.screens.rewind.slides

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.hypnoticcanvas.shaderBackground
import kotlinx.coroutines.delay

@Composable
fun WelcomeSlide(
    username: String, 
    year: Int, 
    onNext: () -> Unit
) {
    // Using PurpleLiquid from your shader library
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shaderBackground(com.mikepenz.hypnoticcanvas.shaders.PurpleLiquid)
    ) {
        // Simple dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Welcome message
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Spacer(modifier = Modifier.height(80.dp))
                
                // Main greeting
                Text(
                    text = "We've got your year",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "wrapped up.",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // REWIND text
                Text(
                    text = "REWIND",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = 4.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Year display
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            width = 2.dp,
                            color = Color(0xFF9C27B0),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .background(
                            color = Color(0x209C27B0),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Text(
                        text = year.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 40.dp, vertical = 16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Personal greeting
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 18.sp
                            )
                        ) {
                            append("Hello, ")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        ) {
                            append(username)
                        }
                        withStyle(
                            style = SpanStyle(
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 18.sp
                            )
                        ) {
                            append("! 👋")
                        }
                    },
                    textAlign = TextAlign.Center
                )
            }
            
            // Privacy note
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔒",
                            fontSize = 16.sp
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Your privacy is respected!",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "All data stays on your device and is managed only by you.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            
            // Swipe hint
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp, bottom = 32.dp)
            ) {
                Text(
                    text = "➤",
                    fontSize = 32.sp,
                    color = Color(0xFFFFD700),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Swipe to get started",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}