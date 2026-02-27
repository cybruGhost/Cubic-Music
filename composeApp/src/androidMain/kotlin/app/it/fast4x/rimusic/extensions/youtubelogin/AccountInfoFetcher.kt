package app.it.fast4x.rimusic.extensions.youtubelogin

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import it.fast4x.innertube.models.AccountInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import org.json.JSONArray
import org.json.JSONObject

class AccountInfoFetcher {
    
    companion object {
        // Only use Omada Cafe - it works reliably
        private const val OMADA_INSTANCE = "https://yt.omada.cafe"
        private const val COOKIE_WAIT_TIME = 2000L // Wait 2 seconds for cookies
        
        suspend fun fetchAccountInfo(cookie: String? = null): AccountInfo? {
            return withContext(Dispatchers.IO) {
                try {
                    Timber.d("AccountInfoFetcher: Starting fetch with Omada Cafe")
                    
                    if (cookie.isNullOrEmpty()) {
                        Timber.e("AccountInfoFetcher: No cookie provided")
                        return@withContext null
                    }
                    
                    // Give cookies time to be fully restored/set
                    delay(COOKIE_WAIT_TIME)
                    
                    val parsedCookies = parseCookieString(cookie)
                    Timber.d("AccountInfoFetcher: Parsed ${parsedCookies.size} cookies")
                    
                    // Check for essential auth cookies
                    val hasEssentialCookies = parsedCookies.containsKey("SAPISID") || 
                                              parsedCookies.containsKey("__Secure-3PAPISID") ||
                                              parsedCookies.containsKey("SID") ||
                                              parsedCookies.containsKey("__Secure-3PSID")
                    
                    if (!hasEssentialCookies) {
                        Timber.e("AccountInfoFetcher: No essential auth cookies found")
                        return@withContext null
                    }
                    
                    // Try to get account info via Omada
                    val accountInfo = tryGetAccountInfoViaOmada(cookie, parsedCookies)
                    
                    return@withContext accountInfo
                    
                } catch (e: Exception) {
                    Timber.e("AccountInfoFetcher: Error: ${e.message}")
                    null
                }
            }
        }
        
        private suspend fun tryGetAccountInfoViaOmada(cookie: String, parsedCookies: Map<String, String>): AccountInfo? {
            return try {
                Timber.d("AccountInfoFetcher: Connecting to Omada Cafe...")
                
                val client = HttpClient()
                
                // First, try to get authenticated preferences to check if we're logged in
                val authResult = tryAuthenticateWithOmada(client, cookie, parsedCookies)
                
                if (authResult == null) {
                    Timber.d("AccountInfoFetcher: Could not authenticate with Omada")
                    return null
                }
                
                Timber.d("AccountInfoFetcher: Successfully authenticated with Omada")
                
                // Method 1: Try to get subscriptions (best for account info)
                val accountInfo = try {
                    getAccountInfoFromSubscriptions(client, authResult.sid)
                } catch (e: Exception) {
                    Timber.d("AccountInfoFetcher: Subscriptions method failed: ${e.message}")
                    null
                }
                
                // Method 2: If subscriptions fail, try to extract from SAPISID
                accountInfo ?: extractAccountInfoFromSAPISID(parsedCookies)
                
            } catch (e: Exception) {
                Timber.e("AccountInfoFetcher: Omada method failed: ${e.message}")
                null
            }
        }
        
        private data class AuthResult(val sid: String, val isAuthenticated: Boolean)
        
        private suspend fun tryAuthenticateWithOmada(
            client: HttpClient, 
            cookie: String, 
            parsedCookies: Map<String, String>
        ): AuthResult? {
            return try {
                // First try with SID
                val sid = parsedCookies["SID"] ?: parsedCookies["__Secure-3PSID"]
                
                if (sid != null) {
                    Timber.d("AccountInfoFetcher: Trying authentication with SID")
                    val response: HttpResponse = client.get("$OMADA_INSTANCE/api/v1/auth/preferences") {
                        header("Cookie", "SID=$sid")
                        header("User-Agent", "Mozilla/5.0")
                    }
                    
                    if (response.status.value == 200) {
                        Timber.d("AccountInfoFetcher: SID authentication successful")
                        return AuthResult(sid, true)
                    }
                }
                
                // If SID fails, try with full cookie
                Timber.d("AccountInfoFetcher: Trying authentication with full cookie")
                val response: HttpResponse = client.get("$OMADA_INSTANCE/api/v1/auth/preferences") {
                    header("Cookie", cookie)
                    header("User-Agent", "Mozilla/5.0")
                }
                
                if (response.status.value == 200) {
                    Timber.d("AccountInfoFetcher: Full cookie authentication successful")
                    return AuthResult(sid ?: "", true)
                }
                
                null
            } catch (e: Exception) {
                Timber.e("AccountInfoFetcher: Authentication error: ${e.message}")
                null
            }
        }
        
        private suspend fun getAccountInfoFromSubscriptions(client: HttpClient, sid: String): AccountInfo? {
            return try {
                Timber.d("AccountInfoFetcher: Fetching subscriptions...")
                
                val response: HttpResponse = client.get("$OMADA_INSTANCE/api/v1/auth/subscriptions") {
                    header("Cookie", "SID=$sid")
                    header("User-Agent", "Mozilla/5.0")
                }
                
                if (response.status.value != 200) {
                    Timber.d("AccountInfoFetcher: Subscriptions endpoint returned ${response.status.value}")
                    return null
                }
                
                val subsJson = response.body<String>()
                val jsonArray = JSONArray(subsJson)
                
                if (jsonArray.length() == 0) {
                    Timber.d("AccountInfoFetcher: No subscriptions found")
                    return null
                }
                
                // Get first subscription to extract account info
                val firstSub = jsonArray.getJSONObject(0)
                val channelName = firstSub.optString("author", "YouTube User")
                val channelId = firstSub.optString("authorId", "")
                
                Timber.d("AccountInfoFetcher: Found subscription to channel: $channelName")
                
                // Try to get channel thumbnail
                val thumbnail = if (channelId.isNotEmpty()) {
                    getChannelThumbnail(client, channelId, sid)
                } else ""
                
                AccountInfo(
                    name = channelName,
                    email = "", // Omada doesn't provide email
                    channelHandle = if (channelId.isNotEmpty()) "@$channelId" else "",
                    thumbnailUrl = thumbnail
                )
                
            } catch (e: Exception) {
                Timber.e("AccountInfoFetcher: Error getting subscriptions: ${e.message}")
                null
            }
        }
        
        private suspend fun getChannelThumbnail(client: HttpClient, channelId: String, sid: String): String {
            return try {
                val response: HttpResponse = client.get("$OMADA_INSTANCE/api/v1/channels/$channelId") {
                    header("Cookie", "SID=$sid")
                    header("User-Agent", "Mozilla/5.0")
                }
                
                if (response.status.value == 200) {
                    val channelJson = response.body<String>()
                    val channelObj = JSONObject(channelJson)
                    
                    if (channelObj.has("authorThumbnails") && 
                        channelObj.getJSONArray("authorThumbnails").length() > 0) {
                        return channelObj.getJSONArray("authorThumbnails")
                            .getJSONObject(0)
                            .optString("url", "")
                            .replace("\\/", "/") // Fix escaped slashes
                    }
                }
                ""
            } catch (e: Exception) {
                Timber.e("AccountInfoFetcher: Error getting thumbnail: ${e.message}")
                ""
            }
        }
        
        private fun extractAccountInfoFromSAPISID(parsedCookies: Map<String, String>): AccountInfo? {
            val sapisid = parsedCookies["SAPISID"] ?: parsedCookies["__Secure-3PAPISID"]
            
            if (sapisid != null) {
                val name = extractNameFromSAPISID(sapisid)
                
                // Try to get channel ID from PREF cookie
                val channelId = parsedCookies["PREF"]?.let { pref ->
                    pref.split("&").find { it.contains("channel") }?.split("=")?.getOrNull(1)
                } ?: ""
                
                return AccountInfo(
                    name = name,
                    email = "", // Can't get email from SAPISID
                    channelHandle = if (channelId.isNotEmpty()) "@$channelId" else "",
                    thumbnailUrl = ""
                )
            }
            
            return null
        }
        
        private fun extractNameFromSAPISID(sapisid: String): String {
            try {
                // SAPISID format examples: 
                // - hash/name@timestamp
                // - name@timestamp/hash
                // - hash/encoded_name@timestamp
                
                val parts = sapisid.split("/", "@")
                
                // Look for the part that looks like a name (not a hash, not a timestamp)
                for (part in parts) {
                    if (part.length in 2..50 && 
                        !part.matches(Regex("[0-9]+")) && // Not just numbers (timestamp)
                        !part.matches(Regex("[a-fA-F0-9]{20,}"))) { // Not a hex hash
                        
                        // Try to decode URL-encoded characters
                        val decoded = try {
                            java.net.URLDecoder.decode(part, "UTF-8")
                        } catch (e: Exception) {
                            part
                        }
                        
                        // Clean up the name
                        val cleanedName = decoded
                            .replace("_", " ")
                            .replace("-", " ")
                            .replace("%20", " ")
                            .replace("%40", "@")
                            .trim()
                        
                        if (cleanedName.isNotBlank() && cleanedName.length < 50) {
                            return cleanedName.split(" ").joinToString(" ") { word ->
                                word.replaceFirstChar { it.uppercaseChar() }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e("AccountInfoFetcher: Error parsing SAPISID: ${e.message}")
            }
            
            return "YouTube Account"
        }
        
        private fun parseCookieString(cookie: String): Map<String, String> {
            return try {
                cookie.split(";")
                    .map { it.trim() }
                    .filter { it.contains("=") }
                    .associate {
                        val parts = it.split("=", limit = 2)
                        parts[0] to (parts.getOrNull(1) ?: "")
                    }
            } catch (e: Exception) {
                Timber.e("AccountInfoFetcher: Error parsing cookie string: ${e.message}")
                emptyMap()
            }
        }
    }
}