package it.fast4x.rimusic.ui.screens.cubicjam

import android.content.Context
import it.fast4x.innertube.Innertube
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber

// Helper functions for API calls
suspend fun refreshFriendsActivity(
    preferences: android.content.SharedPreferences,
    json: Json
): FriendsActivityResponse? {
    val token = preferences.getString("bearer_token", null)
    if (token == null) return null
    
    return try {
        val response = Innertube.client.get("https://dkbvirgavjojuyaazzun.supabase.co/functions/v1/friends-activity") {
            header("Authorization", "Bearer $token")
        }
        json.decodeFromString<FriendsActivityResponse>(response.body())
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch friends activity")
        null
    }
}

suspend fun getUserProfile(
    token: String,
    username: String,
    json: Json
): UserProfileResponse {
    val response = Innertube.client.get("https://dkbvirgavjojuyaazzun.supabase.co/functions/v1/user-profile") {
        header("Authorization", "Bearer $token")
        parameter("username", username)
    }
    return json.decodeFromString<UserProfileResponse>(response.body())
}

suspend fun addFriend(
    token: String,
    friendCode: String,
    json: Json
) {
    val response = Innertube.client.post("https://dkbvirgavjojuyaazzun.supabase.co/functions/v1/friends") {
        header("Authorization", "Bearer $token")
        contentType(ContentType.Application.Json)
        setBody(FriendRequest(action = "add", friend_code = friendCode))
    }
    val result = json.decodeFromString<FriendResponse>(response.body())
    if (!result.success) {
        throw Exception(result.message ?: "Failed to add friend")
    }
}