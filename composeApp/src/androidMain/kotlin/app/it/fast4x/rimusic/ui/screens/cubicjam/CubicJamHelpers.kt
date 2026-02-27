package app.it.fast4x.rimusic.ui.screens.cubicjam

import android.content.Context
import it.fast4x.innertube.Innertube
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber

// Helper functions for API calls - UPDATED VERSION
suspend fun refreshFriendsActivity(
    preferences: android.content.SharedPreferences,
    json: Json
): FriendsActivityAPIResponse? {  // Changed return type
    return try {
        val token = ensureValidToken(preferences) ?: return null
        
        Timber.tag("CubicJam").d("Fetching friends activity from functions API...")
        
        val response = Innertube.client.get("https://dkbvirgavjojuyaazzun.supabase.co/functions/v1/friends-activity") {
            header("Authorization", "Bearer $token")
        }
        
        Timber.tag("CubicJam").d("Friends activity response status: ${response.status}")
        
        if (response.status.value in 200..299) {
            val responseBody = response.body<String>()
            Timber.tag("CubicJam").d("Friends activity raw response: $responseBody")
            
            try {
                // Use the NEW data class that matches your API response
                val result = json.decodeFromString<FriendsActivityAPIResponse>(responseBody)
                Timber.tag("CubicJam").d("Successfully parsed ${result.activity.size} activities")
                return result
            } catch (e: Exception) {
                Timber.tag("CubicJam").e(e, "Failed to parse friends activity response")
                // Try to parse as generic response to see what we got
                try {
                    val errorResponse = json.decodeFromString<Map<String, Any>>(responseBody)
                    Timber.tag("CubicJam").e("Error response: $errorResponse")
                } catch (e2: Exception) {
                    Timber.tag("CubicJam").e(e2, "Failed to parse error response")
                }
            }
        } else {
            val error = try {
                response.body<String>()
            } catch (e: Exception) {
                "Failed to parse error"
            }
            Timber.tag("CubicJam").e("❌ Friends activity failed: ${response.status}, $error")
        }
        null
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch friends activity")
        null
    }
}

// Keep all other functions as they are (unchanged)
suspend fun getUserProfile(
    preferences: android.content.SharedPreferences,
    username: String,
    json: Json
): UserProfileResponse {
    val token = ensureValidToken(preferences) ?: throw IllegalStateException("Not authenticated")
    
    val response = Innertube.client.get("https://dkbvirgavjojuyaazzun.supabase.co/functions/v1/user-profile") {
        header("Authorization", "Bearer $token")
        parameter("username", username)
    }
    
    if (response.status.value !in 200..299) {
        val error = try {
            response.body<String>()
        } catch (e: Exception) {
            "Failed to parse error"
        }
        throw Exception("Failed to get user profile: ${response.status}, $error")
    }
    
    return json.decodeFromString<UserProfileResponse>(response.body())
}

suspend fun addFriend(
    preferences: android.content.SharedPreferences,
    friendCode: String,
    json: Json
): FriendResponse {
    val token = ensureValidToken(preferences) ?: throw IllegalStateException("Not authenticated")
    
    Timber.tag("CubicJam").d("Sending friend request with code: $friendCode")
    
    val response = Innertube.client.post("https://dkbvirgavjojuyaazzun.supabase.co/functions/v1/friends") {
        header("Authorization", "Bearer $token")
        contentType(ContentType.Application.Json)
        setBody(FriendRequest(action = "add", friend_code = friendCode))
    }
    
    if (response.status.value !in 200..299) {
        val error = try {
            response.body<String>()
        } catch (e: Exception) {
            "Failed to parse error"
        }
        throw Exception("Failed to add friend: ${response.status}, $error")
    }
    
    val result = json.decodeFromString<FriendResponse>(response.body())
    if (!result.success) {
        throw Exception(result.message ?: "Failed to add friend")
    }
    
    Timber.tag("CubicJam").d("✅ Friend request sent successfully")
    return result
}

suspend fun getFriendsList(
    preferences: android.content.SharedPreferences,
    json: Json
): FriendsListResponse? {
    return try {
        val token = ensureValidToken(preferences) ?: return null
        
        Timber.tag("CubicJam").d("Fetching friends list from functions API...")
        
        val response = Innertube.client.get("https://dkbvirgavjojuyaazzun.supabase.co/functions/v1/friends") {
            header("Authorization", "Bearer $token")
        }
        
        if (response.status.value in 200..299) {
            val responseBody = response.body<String>()
            Timber.tag("CubicJam").d("Friends list response: $responseBody")
            return json.decodeFromString<FriendsListResponse>(responseBody)
        } else {
            val error = try {
                response.body<String>()
            } catch (e: Exception) {
                "Failed to parse error"
            }
            Timber.tag("CubicJam").e("❌ Friends list failed: ${response.status}, $error")
            null
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch friends list")
        null
    }
}

// Token refresh helper for direct API calls
suspend fun ensureValidToken(preferences: android.content.SharedPreferences): String? {
    val currentToken = preferences.getString("bearer_token", null)
    val refreshToken = preferences.getString("refresh_token", null)
    val refreshAt = preferences.getLong("refresh_at", 0L)
    val currentTime = System.currentTimeMillis() / 1000

    // Check if token needs refresh
    if (currentToken != null && refreshToken != null && currentTime >= refreshAt) {
        try {
            Timber.tag("CubicJam").d("Token expired or near expiry, refreshing...")
            val response = Innertube.client.post("https://dkbvirgavjojuyaazzun.supabase.co/functions/v1/mobile-auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("refresh_token" to refreshToken))
            }
            
            val refreshResponse = Json.decodeFromString<RefreshResponse>(response.body())
            
            if (refreshResponse.success) {
                preferences.edit().apply {
                    putString("bearer_token", refreshResponse.session.access_token)
                    putString("refresh_token", refreshResponse.session.refresh_token)
                    putLong("expires_at", refreshResponse.session.expires_at)
                    val refreshTime = refreshResponse.session.refresh_at ?: 
                        (refreshResponse.session.expires_at - 300) // 5 minutes before
                    putLong("refresh_at", refreshTime)
                    apply()
                }
                Timber.tag("CubicJam").d("✅ Token refreshed successfully")
                return refreshResponse.session.access_token
            }
            } catch (e: Exception) {
                Timber.tag("CubicJam").e(e, "Failed to refresh token: ${e.message}")
                // ⚠️ FIXED: Don't clear tokens, just return current one
                // Let the app handle network errors gracefully
                return currentToken
            }
    }
    
    return currentToken
}

fun isTokenValid(preferences: android.content.SharedPreferences): Boolean {
    val refreshAt = preferences.getLong("refresh_at", 0L)
    val currentTime = System.currentTimeMillis() / 1000
    val hasToken = preferences.getString("bearer_token", null) != null
    
    return hasToken && currentTime < refreshAt
}

fun logout(context: Context) {
    val prefs = context.getSharedPreferences("cubic_jam_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()
    Timber.tag("CubicJam").d("Logged out")
}

// Add this test function to debug the API
suspend fun testFriendsActivityAPI(preferences: android.content.SharedPreferences): Boolean {
    return try {
        val token = ensureValidToken(preferences) ?: return false
        Timber.tag("CubicJam").d("Testing friends activity API with token: ${token.take(20)}...")
        
        val response = Innertube.client.get("https://dkbvirgavjojuyaazzun.supabase.co/functions/v1/friends-activity") {
            header("Authorization", "Bearer $token")
        }
        
        Timber.tag("CubicJam").d("Test response status: ${response.status}")
        if (response.status.value in 200..299) {
            val body = response.body<String>()
            Timber.tag("CubicJam").d("Test response body: $body")
            true
        } else {
            Timber.tag("CubicJam").e("Test failed: ${response.status}")
            false
        }
    } catch (e: Exception) {
        Timber.tag("CubicJam").e(e, "Test API failed")
        false
    }
}