package it.fast4x.rimusic.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WeatherForecastPopup(
    weatherData: WeatherData,
    username: String,
    onDismiss: () -> Unit,
    onCityChange: () -> Unit
) {
    // Calculate local time based on city timezone
    val localHour = getLocalHour(weatherData.timezoneOffset)
    val isNight = localHour < 6 || localHour >= 20
    val isEvening = localHour >= 17 && localHour < 20
    val isMorning = localHour >= 6 && localHour <= 9
    val isAfternoon = localHour >= 12 && localHour < 17
    val isLateNight = localHour >= 22 || localHour < 4
    
    val normalizedCondition = normalizeCondition(weatherData.condition)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Hey $username! 👋",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = getTextColorForBackground(getConditionGradient(normalizedCondition, isNight)),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onCityChange,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Change city",
                        tint = getTextColorForBackground(getConditionGradient(normalizedCondition, isNight))
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current Weather Card
                item {
                    WeatherHeaderCard(
                        weatherData = weatherData,
                        localHour = localHour,
                        isNight = isNight,
                        normalizedCondition = normalizedCondition
                    )
                }
                
                // Personal Care Tips
                item {
                    PersonalCareCard(
                        weatherData = weatherData,
                        localHour = localHour,
                        username = username,
                        isNight = isNight
                    )
                }
                
                // Clothing Recommendation
                item {
                    ClothingCard(
                        temp = weatherData.temp,
                        condition = normalizedCondition,
                        isNight = isNight,
                        localHour = localHour
                    )
                }
                
                // Activity Suggestions
                item {
                    ActivityCard(
                        weatherData = weatherData,
                        localHour = localHour,
                        isNight = isNight,
                        username = username
                    )
                }
                
                // Weather Details
                item {
                    WeatherDetailsCard(weatherData, localHour)
                }
                
                // Emotional Forecast
                item {
                    EmotionalForecastCard(
                        weatherData = weatherData,
                        username = username,
                        localHour = localHour,
                        isNight = isNight
                    )
                }
                
                // Hydration Reminder (only if appropriate)
                if (shouldShowHydrationReminder(weatherData.temp, localHour)) {
                    item {
                        HydrationCard(username, weatherData.temp)
                    }
                }
                
                // Special Opportunity
                item {
                    SpecialTipCard(weatherData, normalizedCondition, localHour, username)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Got it! Stay amazing ✨")
            }
        }
    )
}

@Composable
private fun WeatherHeaderCard(
    weatherData: WeatherData,
    localHour: Int,
    isNight: Boolean,
    normalizedCondition: String
) {
    val gradient = getConditionGradient(normalizedCondition, isNight)
    val textColor = getTextColorForBackground(gradient)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = getWeatherEmoji(normalizedCondition),
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = "${weatherData.temp.roundToInt()}°C",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "${normalizedCondition.replaceFirstChar { it.uppercase() }} • ${getTimeOfDayGreeting(localHour)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "${weatherData.city} • Feels like ${weatherData.feelsLike.roundToInt()}°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Local time display
            Text(
                text = "Local time: ${formatLocalTime(localHour)}",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun PersonalCareCard(
    weatherData: WeatherData,
    localHour: Int,
    username: String,
    isNight: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "💖 Your Personal Care Guide",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1565C0)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = getPersonalCareMessage(weatherData, localHour, username, isNight),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF0D47A1)
            )
        }
    }
}

@Composable
private fun ClothingCard(
    temp: Double,
    condition: String,
    isNight: Boolean,
    localHour: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "👗 Today's Outfit Advice",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = getSmartClothingRecommendation(temp, condition, isNight, localHour),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1B5E20)
            )
        }
    }
}

@Composable
private fun ActivityCard(
    weatherData: WeatherData,
    localHour: Int,
    isNight: Boolean,
    username: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "🌟 Perfect Activities Right Now",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE65100)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = getActivitySuggestions(weatherData, localHour, isNight, username),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFBF360C)
            )
        }
    }
}

@Composable
private fun WeatherDetailsCard(weatherData: WeatherData, localHour: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "📊 Weather Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val details = listOf(
                WeatherDetail("💧 Humidity", "${weatherData.humidity}%", getHumidityTip(weatherData.humidity)),
                WeatherDetail("💨 Wind", "${weatherData.windSpeed} m/s", getWindTip(weatherData.windSpeed)),
                WeatherDetail("🌡 Pressure", "${weatherData.pressure} hPa", getPressureTip(weatherData.pressure)),
                WeatherDetail("👁 Visibility", "${weatherData.visibility / 1000} km", getVisibilityTip(weatherData.visibility)),
                WeatherDetail("📊 Min/Max", "${weatherData.minTemp.roundToInt()}°C / ${weatherData.maxTemp.roundToInt()}°C", getTempRangeTip(weatherData))
            )
            
            details.forEach { detail ->
                WeatherDetailRow(detail)
            }
        }
    }
}

@Composable
private fun EmotionalForecastCard(
    weatherData: WeatherData,
    username: String,
    localHour: Int,
    isNight: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "💫 Today's Emotional Vibe",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF7B1FA2)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = getEmotionalForecast(weatherData, username, localHour, isNight),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6A1B9A)
            )
        }
    }
}

@Composable
private fun HydrationCard(username: String, temp: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💧",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = getHydrationMessage(username, temp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF01579B),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SpecialTipCard(
    weatherData: WeatherData,
    condition: String,
    localHour: Int,
    username: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "💡 Special Opportunity Today",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = getSpecialTip(weatherData, condition, localHour, username),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1B5E20)
            )
        }
    }
}

@Composable
private fun WeatherDetailRow(detail: WeatherDetail) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = detail.emojiLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = detail.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = detail.tip,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// ============ ALL MESSAGE GENERATION FUNCTIONS ============

private fun getPersonalCareMessage(weatherData: WeatherData, localHour: Int, username: String, isNight: Boolean): String {
    val temp = weatherData.temp
    val condition = normalizeCondition(weatherData.condition)
    
    val messages = mutableListOf<String>()
    
    // Time-based care messages
    when {
        localHour < 6 -> messages.add("It's late night! Make sure you're getting the rest you deserve. Your body will thank you tomorrow. 🌙")
        localHour < 12 -> messages.add("Good morning! Start your day with a deep breath and positive intention. You've got this! ☀️")
        localHour < 17 -> messages.add("Hope your day is going well! Remember to take short breaks if you've been busy. 🕒")
        localHour < 21 -> messages.add("Evening time - perfect moment to unwind and reflect on your day. 🌆")
        else -> messages.add("Night owl! Don't forget to wind down properly before bed for quality sleep. 🌃")
    }
    
    // Temperature-based care
    when {
        temp >= 30 -> {
            messages.add("It's quite hot! Stay in shaded areas when outside and avoid strenuous activities during peak heat. 🔥")
            messages.add("Your skin will appreciate some extra protection from the strong sun today. ☂️")
        }
        temp >= 25 -> messages.add("Warm day ahead - perfect weather but still stay mindful of sun exposure. 😎")
        temp <= 5 -> {
            messages.add("Brrr, it's properly cold! Keep your extremities warm and limit time outside. ❄️")
            messages.add("This is perfect weather for warm, nourishing foods and drinks. 🍵")
        }
        temp <= 0 -> messages.add("Freezing temperatures! Layer up properly and be extra careful on any icy surfaces. ⚠️")
    }
    
    // Weather condition care
    when (condition) {
        "rain", "drizzle" -> messages.add("Rainy weather can be cozy! Perfect for staying indoors with your favorite music or a good book. 📚")
        "thunderstorm" -> messages.add("Stormy conditions! Stay safe indoors and unplug electronics if the storm gets close. ⚡")
        "snow" -> messages.add("Snowy day! If you need to go out, wear proper footwear and take small steps. 👢")
        "clear" -> messages.add("Beautiful clear skies! Great opportunity for some vitamin D if it's daytime. 🌞")
        "clouds" -> messages.add("Cloudy days have their own gentle charm. Perfect for activities that don't depend on bright sun. ☁️")
    }
    
    // Night-specific care
    if (isNight) {
        messages.add("Since it's nighttime, consider reducing screen brightness to help your eyes relax. 📱")
    }
    
    // Always include these caring reminders
    messages.add("Remember to listen to your body today - it knows what you need. 💝")
    messages.add("Take a moment to appreciate yourself and how far you've come. You're doing great, $username! 🌟")
    
    return messages.shuffled().take(2).joinToString(" ") + " Sending you positive energy! ✨"
}

private fun getSmartClothingRecommendation(temp: Double, condition: String, isNight: Boolean, localHour: Int): String {
    val baseRecommendation = when {
        temp >= 30 -> "Light, breathable fabrics are essential! Think cotton t-shirts, linen pants, shorts, and open shoes. Avoid dark colors that absorb heat. "
        temp >= 25 -> "Light and comfortable is the way to go! T-shirts with light pants or skirts. Natural fabrics will keep you feeling fresh. "
        temp >= 20 -> "Perfect for light layers! A t-shirt with a light jacket or cardigan gives you flexibility as temperatures change. "
        temp >= 15 -> "Time for warmer layers! A sweater or hoodie would be cozy. Consider a light scarf for extra warmth. "
        temp >= 10 -> "Definitely jacket weather! A warm coat, long pants, and closed shoes will keep you comfortable. "
        temp >= 0 -> "Bundle up properly! Thermal layers under your clothes, heavy winter coat, gloves, warm hat, and scarf. "
        else -> "Extreme cold conditions! Multiple thermal layers, heavy winter gear, and make sure no skin is exposed. "
    }
    
    val conditionAddition = when (condition) {
        "rain", "drizzle" -> " Waterproof or water-resistant jacket is a must! Consider waterproof shoes or boots too. "
        "snow" -> " Waterproof boots with good grip are essential. Don't forget warm, waterproof gloves. "
        "thunderstorm" -> " If you must go out, full waterproof gear is necessary. Otherwise, stay cozy indoors! "
        "clear" -> if (!isNight && temp > 20) " Sun hat or cap would be smart for sun protection. " else ""
        else -> ""
    }
    
    val timeAddition = when {
        isNight -> " Since it's nighttime, you might want an extra layer as temperatures often drop after dark. "
        localHour < 10 -> " Morning temperatures can be cooler - dress in layers you can remove as it warms up. "
        localHour > 18 -> " Evening is approaching - consider bringing an extra layer for when it gets cooler. "
        else -> ""
    }
    
    val comfortAddition = "Most importantly, wear what makes you feel confident and comfortable! Your comfort affects your whole day. 😊"
    
    return baseRecommendation + conditionAddition + timeAddition + comfortAddition
}

private fun getActivitySuggestions(weatherData: WeatherData, localHour: Int, isNight: Boolean, username: String): String {
    val temp = weatherData.temp
    val condition = normalizeCondition(weatherData.condition)
    
    val activities = mutableListOf<String>()
    
    // Time-appropriate activities (NO night strolls!)
    if (isNight) {
        activities.addAll(listOf(
            "Cozy indoor music session 🎵",
            "Reading with warm lighting 📖",
            "Journaling or planning tomorrow 📝",
            "Gentle stretching or yoga 🧘",
            "Movie night with favorite snacks 🍿",
            "Digital detox hour 📴",
            "Meditation or deep breathing exercises 🪷"
        ))
    } else {
        // Daytime activities
        when {
            localHour < 12 -> activities.addAll(listOf(
                "Morning walk in fresh air 🚶",
                "Outdoor breakfast or coffee ☕",
                "Gardening or plant care 🌱",
                "Sunrise photography 📸"
            ))
            localHour < 17 -> activities.addAll(listOf(
                "Lunch break outside 🍽️",
                "Park visit or nature walk 🌳",
                "Outdoor exercise or sports ⚽",
                "Reading in a sunny spot 📚"
            ))
            else -> activities.addAll(listOf(
                "Evening stroll before dark 🌆",
                "Sunset watching 🌅",
                "Outdoor social gathering 👥",
                "Gentle outdoor exercise 🏃"
            ))
        }
    }
    
    // Weather-appropriate activities
    when (condition) {
        "rain", "drizzle" -> activities.addAll(listOf(
            "Indoor cooking or baking 🍳",
            "Board games or puzzles 🎲",
            "Creative projects or crafts 🎨",
            "Home organization session 🏠"
        ))
        "clear" -> if (!isNight) activities.addAll(listOf(
            "Beach or water activities 🏖️",
            "Picnic in the park 🧺",
            "Outdoor photography 📷",
            "Bike riding or skating 🚴"
        ))
        "snow" -> activities.addAll(listOf(
            "Building a snowman ⛄",
            "Hot chocolate making ☕",
            "Winter photography ❄️",
            "Cozy blanket fort 🏰"
        ))
        "clouds" -> activities.addAll(listOf(
            "Perfect for outdoor activities without strong sun 🌤️",
            "Ideal for long walks or hikes 🥾",
            "Great for outdoor sports 🎾"
        ))
    }
    
    // Temperature considerations
    when {
        temp > 28 -> activities.add("Swimming or water-based activities 🏊")
        temp < 10 -> activities.add("Warm beverage tasting session ☕")
    }
    
    val selectedActivities = activities.shuffled().take(3)
    
    val timeContext = when {
        isNight -> "Since it's nighttime in ${weatherData.city}, here are some perfect activities:"
        localHour < 12 -> "Good morning! Here are great ways to start your day:"
        localHour < 17 -> "For this afternoon, consider these activities:"
        else -> "As evening approaches, these would be lovely:"
    }
    
    return "$timeContext ${selectedActivities.joinToString(", ")}. Whatever you choose, make it enjoyable, $username! 😊"
}

private fun getEmotionalForecast(weatherData: WeatherData, username: String, localHour: Int, isNight: Boolean): String {
    val temp = weatherData.temp
    val condition = normalizeCondition(weatherData.condition)
    
    val emotionalMessage = when (condition) {
        "clear" -> {
            if (isNight) {
                "The clear night sky is so peaceful and expansive. 🌌 It's a perfect time for dreaming big dreams and feeling connected to the universe. The stars remind us that even in darkness, there's always light somewhere."
            } else {
                "Sunny days have a way of lifting spirits! ☀️ The brightness can help boost your mood and energy. It's nature's way of saying 'anything is possible today.' Perfect for taking on challenges with optimism."
            }
        }
        "clouds" -> "Cloudy days create a soft, gentle atmosphere that's perfect for reflection. ☁️ There's a calmness in the air that encourages slowing down and being present. Sometimes the most beautiful moments happen under cloud cover."
        "rain", "drizzle" -> {
            if (isNight) {
                "There's something magical about rain at night. 🌧️ The sound is so soothing, perfect for letting go of the day's stresses. It's like nature is washing everything clean for a fresh start tomorrow."
            } else {
                "Rainy days have a special cozy energy. 🌧️ They remind us that it's okay to slow down and be gentle with ourselves. Perfect for introspection or enjoying simple comforts."
            }
        }
        "thunderstorm" -> "Storms bring powerful energy and dramatic beauty! ⚡ They remind us of nature's strength and our own resilience. There's something exhilarating about witnessing such raw power from a safe space."
        "snow" -> "Snow creates a magical, quiet world that feels both peaceful and exciting. ❄️ It's nature's way of hitting the pause button, inviting us to appreciate stillness and simple pleasures."
        "mist", "fog" -> "Misty conditions feel mysterious and romantic. 🌫️ Like you're in your own private world where anything could happen. Perfect for imagination and creative thinking."
        else -> "Every weather pattern has its own unique beauty. 🌈 Today's conditions are inviting you to find the special moments hidden in ordinary experiences."
    }
    
    val temperatureEmotion = when {
        temp > 28 -> " The warmth can feel invigorating - like a comforting embrace from nature! 🔥"
        temp < 5 -> " The crisp air can feel refreshing and clarifying - perfect for feeling alert and focused! ❄️"
        else -> " The comfortable temperature makes it easy to feel at peace with your surroundings. 🌡️"
    }
    
    val personalTouch = when {
        isNight -> " As night wraps around ${weatherData.city}, remember that rest is productive too. You deserve this quiet time, $username. 💫"
        localHour < 12 -> " However your day unfolds, remember that you have the strength to handle whatever comes your way. You're capable and resilient, $username! 💪"
        else -> " However the rest of your day goes, know that you're doing better than you think. Be proud of your progress, $username! 🌟"
    }
    
    return emotionalMessage + temperatureEmotion + personalTouch
}

private fun getHydrationMessage(username: String, temp: Double): String {
    return when {
        temp > 28 -> "Hey $username! With this heat, your body needs extra hydration. 💧 Drink water regularly to stay cool and energized! Your body will thank you. 🌡️"
        temp > 22 -> "Friendly reminder, $username! Even in comfortable weather, hydration is key. 💧 Keep that water bottle nearby and sip throughout the day! 💪"
        else -> "Quick hydration check, $username! 💧 Don't forget to drink water - it helps with energy and focus, no matter the weather! 🌈"
    }
}

private fun getSpecialTip(weatherData: WeatherData, condition: String, localHour: Int, username: String): String {
    val temp = weatherData.temp
    val isNight = localHour < 6 || localHour >= 20
    
    return when (condition) {
        "rain" -> {
            if (!isNight) {
                "Special opportunity: The rain makes everything look fresh and clean! 🌧️ Perfect time for creative photography or simply enjoying the unique atmosphere. The world looks different in the rain - see what beauty you can discover today!"
            } else {
                "Cozy opportunity: Rainy nights are perfect for audio experiences! 🎧 Try listening to ambient sounds, podcasts, or music that matches the rain's rhythm. It can be incredibly relaxing and immersive."
            }
        }
        "clear" -> {
            if (!isNight) {
                "Golden hour alert! 🌅 If you're available around ${if (localHour < 12) "sunrise" else "sunset"}, the light will be absolutely magical. Perfect for photos, mindfulness, or simply appreciating nature's beauty."
            } else {
                "Stargazing opportunity! 🌠 With clear skies tonight, you might see some beautiful stars. Even from your window, take a moment to appreciate the vastness above. It's great for perspective and wonder."
            }
        }
        "snow" -> "Winter magic opportunity! ❄️ If it's safe to go outside, notice how snow muffles sound and creates peace. Or stay cozy inside with warm drinks - either way, embrace the unique atmosphere snow creates."
        "clouds" -> "Perfect lighting opportunity! 🌤️ Cloudy days provide soft, even light that's ideal for photography, video calls, or any activity where harsh shadows would be problematic. Make the most of this natural diffuser!"
        else -> "Today's weather creates a unique atmosphere! 🌈 Whether it's for productivity, creativity, or relaxation, there's something special about these conditions. Notice what feels different today and embrace it, $username!"
    }
}

// ============ HELPER FUNCTIONS ============

private fun getLocalHour(timezoneOffset: Int): Int {
    val currentTimeUTC = System.currentTimeMillis() / 1000
    val localTimestamp = currentTimeUTC + timezoneOffset
    return ((localTimestamp % 86400) / 3600).toInt()
}

private fun normalizeCondition(apiCondition: String): String {
    val condition = apiCondition.lowercase(Locale.ROOT)
    return when {
        condition.contains("thunderstorm") -> "thunderstorm"
        condition.contains("drizzle") -> "drizzle"
        condition.contains("rain") -> "rain"
        condition.contains("snow") -> "snow"
        condition.contains("clear") -> "clear"
        condition.contains("cloud") -> "clouds"
        condition.contains("mist") || condition.contains("fog") || condition.contains("haze") -> "mist"
        else -> "clear"
    }
}

private fun getConditionGradient(condition: String, isNight: Boolean): Brush {
    return when (condition) {
        "clear" -> if (isNight) {
            Brush.verticalGradient(listOf(Color(0xFF0F0F23), Color(0xFF2D2D5A)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFF64B5F6), Color(0xFF1976D2)))
        }
        "rain", "drizzle" -> Brush.verticalGradient(listOf(Color(0xFF90CAF9), Color(0xFF1565C0)))
        "thunderstorm" -> Brush.verticalGradient(listOf(Color(0xFF424242), Color(0xFF212121)))
        "snow" -> Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFF90CAF9)))
        "clouds" -> Brush.verticalGradient(listOf(Color(0xFFBDBDBD), Color(0xFF757575)))
        "mist" -> Brush.verticalGradient(listOf(Color(0xFFE0E0E0), Color(0xFF9E9E9E)))
        else -> Brush.verticalGradient(listOf(Color(0xFF64B5F6), Color(0xFF1976D2)))
    }
}

private fun getTextColorForBackground(gradient: Brush): Color {
    return Color.White
}

private fun getTimeOfDayGreeting(hour: Int): String {
    return when {
        hour < 6 -> "Late Night"
        hour < 12 -> "Morning"
        hour < 17 -> "Afternoon"
        hour < 20 -> "Evening"
        else -> "Night"
    }
}

private fun formatLocalTime(hour: Int): String {
    val minutes = ((System.currentTimeMillis() / 1000) % 3600) / 60
    return String.format("%02d:%02d", hour, minutes)
}

private fun shouldShowHydrationReminder(temp: Double, localHour: Int): Boolean {
    return when {
        temp > 26 -> true
        temp in 15.0..25.0 -> Math.random() < 0.6
        localHour < 7 || localHour > 23 -> false
        else -> Math.random() < 0.3
    }
}

private fun getHumidityTip(humidity: Int): String {
    return when {
        humidity > 80 -> "Very humid! Air might feel heavy and sticky"
        humidity > 60 -> "Comfortable humidity level"
        humidity < 30 -> "Dry air - great for breathing, hydrate more"
        else -> "Pleasant humidity conditions"
    }
}

private fun getWindTip(windSpeed: Double): String {
    return when {
        windSpeed > 8 -> "Quite windy! Secure loose items"
        windSpeed > 4 -> "Nice breeze - feels refreshing"
        else -> "Calm conditions - very peaceful"
    }
}

private fun getPressureTip(pressure: Int): String {
    return when {
        pressure > 1020 -> "High pressure - stable weather expected"
        pressure < 1000 -> "Low pressure - changes possible"
        else -> "Normal pressure - typical conditions"
    }
}

private fun getVisibilityTip(visibility: Int): String {
    return when {
        visibility > 10000 -> "Excellent visibility - crystal clear"
        visibility > 5000 -> "Good visibility - can see far"
        visibility > 2000 -> "Moderate visibility - somewhat hazy"
        else -> "Reduced visibility - be cautious if traveling"
    }
}

private fun getTempRangeTip(weatherData: WeatherData): String {
    val range = weatherData.maxTemp - weatherData.minTemp
    return when {
        range > 10 -> "Big temperature swing today - dress in layers"
        range > 5 -> "Moderate variation - be prepared for changes"
        else -> "Stable temperatures throughout the day"
    }
}

private fun getWeatherEmoji(condition: String): String {
    return when (condition) {
        "clear" -> "☀️"
        "clouds" -> "☁️"
        "rain" -> "🌧️"
        "drizzle" -> "🌦️"
        "thunderstorm" -> "⛈️"
        "snow" -> "❄️"
        "mist" -> "🌫️"
        else -> "🌈"
    }
}

data class WeatherDetail(val emojiLabel: String, val value: String, val tip: String)