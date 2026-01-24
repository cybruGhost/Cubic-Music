package it.fast4x.rimusic.ui.screens.donate

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

@Composable
fun DonateScreen(
    onBackClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    val koFiUrl = "https://ko-fi.com/anonghost40418/"
    val githubUrl = "https://github.com/cybruGhost/Cubic-Music"
    val profileImageUrl = "https://avatars.githubusercontent.com/u/113809799?v=4&size=200"
    val koFiImageUrl =
        "https://storage.ko-fi.com/cdn/useruploads/351b9585-ab4f-4264-82bf-bd4f6a21619f_89c804f1-994c-412d-b21a-a110c577b372.jpeg"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0B1A),
                        Color(0xFF1A1625)
                    )
                )
            )
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ================= PROFILE CARD =================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1B26)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // 🔧 CHANGED: Profile Image now uses AsyncImage
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Anon Ghost Profile",
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Anon Ghost",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "Mobile App Developer",
                            fontSize = 14.sp,
                            color = Color(0xFF7C4DFF),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // GitHub Link
                        Card(
                            onClick = { uriHandler.openUri(githubUrl) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2D2A3A)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🐙", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "github.com/cybruGhost",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("↗", color = Color(0xFF7C4DFF))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 🔧 CHANGED: Ko-fi Banner uses AsyncImage
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            AsyncImage(
                                model = koFiImageUrl,
                                contentDescription = "Ko-fi Support Banner",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Support Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFFE040FB),
                                            Color(0xFF7C4DFF),
                                            Color(0xFF4527A0)
                                        )
                                    )
                                )
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "❤️ Support Development ❤️",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Keep the music playing",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                // ================= SUPPORT BUTTON =================
                Button(
                    onClick = { uriHandler.openUri(koFiUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF29ABE0)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("☕", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Support on Ko-fi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Thank you for being part of this journey! 🙏",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ================= HELPER COMPOSABLES =================

@Composable
private fun BenefitItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFFE040FB).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", color = Color(0xFFE040FB))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = Color.White)
    }
}

@Composable
private fun PaymentOption(text: String, icon: String = "💳") {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1B26))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = Color.White.copy(alpha = 0.8f))
    }
}