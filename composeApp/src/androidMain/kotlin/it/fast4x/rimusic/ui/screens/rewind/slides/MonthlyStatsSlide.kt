package it.fast4x.rimusic.ui.screens.rewind.slides

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.rimusic.ui.screens.rewind.*
import it.fast4x.rimusic.ui.screens.rewind.components.getMonthName

@Composable
fun MonthlyStatsSlide(monthlyStats: List<MonthlyStat>, onNext: () -> Unit) {
    val sortedStats = monthlyStats.sortedBy { it.month }
    val peakMonth = monthlyStats.maxByOrNull { it.minutes }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📈 MONTHLY LISTENING BREAKDOWN",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 30.dp)
        )
        
        if (peakMonth != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.2f)
                ),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🏆 PEAK MONTH",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = getMonthName(peakMonth.month),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = "${peakMonth.minutes} minutes",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "${peakMonth.plays} plays",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        // Monthly list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sortedStats.forEach { monthStat ->
                MonthlyStatItemSlide(monthStat, isPeak = monthStat == peakMonth)
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = "Swipe → for Daily Patterns",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}

@Composable
fun MonthlyStatItemSlide(monthStat: MonthlyStat, isPeak: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPeak) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = getMonthName(monthStat.month),
                fontSize = 16.sp,
                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium,
                color = Color.White
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${monthStat.minutes}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "minutes",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${monthStat.plays}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "plays",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}