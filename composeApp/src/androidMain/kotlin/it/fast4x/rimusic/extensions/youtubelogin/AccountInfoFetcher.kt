package it.fast4x.rimusic.extensions.youtubelogin

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.AccountInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration.Companion.seconds

class AccountInfoFetcher {
    
    companion object {
        suspend fun fetchAccountInfo(cookie: String? = null): AccountInfo? {
            return withContext(Dispatchers.IO) {
                try {
                    Timber.d("AccountInfoFetcher: Starting account info fetch with cookie length: ${cookie?.length ?: 0}")
                    
                    if (cookie.isNullOrEmpty()) {
                        Timber.e("AccountInfoFetcher: No cookie provided")
                        return@withContext null
                    }
                    
                    // Method 1: Try Invidious API (Omada Cafe) - Most reliable
                    var accountInfo = tryGetAccountInfoViaInvidious(cookie)
                    
                    // Method 2: Try YouTube Music directly
                    if (accountInfo == null) {
                        accountInfo = tryGetAccountInfoViaYTM(cookie)
                    }
                    
                    // Method 3: Extract from cookie as fallback
                    if (accountInfo == null) {
                        accountInfo = createAccountInfoFromCookie(cookie)
                    }
                    
                    return@withContext accountInfo
                    
                } catch (e: Exception) {
                    Timber.e("AccountInfoFetcher: Error fetching account info: ${e.message}")
                    null
                }
            }
        }
        
        private suspend fun tryGetAccountInfoViaInvidious(cookie: String): AccountInfo? {
            return try {
                Timber.d("AccountInfoFetcher: Trying Invidious API")
                
                // List of Invidious instances to try
                val instances = listOf(
                    "https://yt.omada.cafe",
                    "https://inv.riverside.rocks", 
                    "https://invidious.nerdvpn.de",
                    "https://yt.funami.tech",
                    "https://inv.nadeko.net"
                )
                
                for (instance in instances) {
                    try {
                        Timber.d("AccountInfoFetcher: Trying instance: $instance")
                        
                        val client = HttpClient()
                        
                        // Extract SID from cookie for Invidious auth
                        val cookies = parseCookieString(cookie)
                        val sid = cookies["SID"]
                        
                        if (sid == null) {
                            Timber.d("AccountInfoFetcher: No SID found in cookies")
                            continue
                        }
                        
                        // Try to get authenticated preferences
                        val response: HttpResponse = client.get("$instance/api/v1/auth/preferences") {
                            header("Cookie", "SID=$sid")
                            header("User-Agent", "Mozilla/5.0")
                        }
                        
                        if (response.status.value == 200) {
                            Timber.d("AccountInfoFetcher: Authenticated with Invidious")
                            
                            // Try to get subscriptions
                            val subsResponse: HttpResponse = client.get("$instance/api/v1/auth/subscriptions") {
                                header("Cookie", "SID=$sid")
                            }
                            
                            if (subsResponse.status.value == 200) {
                                val subsJson = subsResponse.body<String>()
                                val jsonArray = JSONArray(subsJson)
                                
                                if (jsonArray.length() > 0) {
                                    // Get first subscription as reference
                                    val firstSub = jsonArray.getJSONObject(0)
                                    val channelName = firstSub.optString("author", "")
                                    val channelId = firstSub.optString("authorId", "")
                                    
                                    // Try to get more details about this channel
                                    try {
                                        val channelResponse: HttpResponse = client.get("$instance/api/v1/channels/$channelId") {
                                            header("Cookie", "SID=$sid")
                                        }
                                        
                                        if (channelResponse.status.value == 200) {
                                            val channelJson = channelResponse.body<String>()
                                            val channelObj = JSONObject(channelJson)
                                            
                                            val name = channelObj.optString("author", channelName)
                                            val thumbnail = if (channelObj.has("authorThumbnails") && 
                                                channelObj.getJSONArray("authorThumbnails").length() > 0) {
                                                channelObj.getJSONArray("authorThumbnails")
                                                    .getJSONObject(0)
                                                    .optString("url", "")
                                            } else ""
                                            
                                            return AccountInfo(
                                                name = name,
                                                email = "",
                                                channelHandle = "@$channelId",
                                                thumbnailUrl = thumbnail
                                            )
                                        }
                                    } catch (e: Exception) {
                                        Timber.e("AccountInfoFetcher: Error getting channel details: ${e.message}")
                                    }
                                    
                                    // If channel details fail, return basic info
                                    return AccountInfo(
                                        name = channelName,
                                        email = "",
                                        channelHandle = "@$channelId",
                                        thumbnailUrl = ""
                                    )
                                }
                            }
                            
                            // Even if we can't get subscriptions, being authenticated is enough
                            // Try to extract account name from SAPISID
                            val sapisid = cookies["SAPISID"]
                            if (sapisid != null) {
                                val name = extractNameFromSAPISID(sapisid)
                                return AccountInfo(
                                    name = name,
                                    email = "",
                                    channelHandle = "",
                                    thumbnailUrl = ""
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e("AccountInfoFetcher: Failed with $instance: ${e.message}")
                        continue
                    }
                }
                
                null
            } catch (e: Exception) {
                Timber.e("AccountInfoFetcher: Invidious method failed: ${e.message}")
                null
            }
        }
        
        private suspend fun tryGetAccountInfoViaYTM(cookie: String): AccountInfo? {
            return try {
                Timber.d("AccountInfoFetcher: Trying YouTube Music directly")
                
                val client = HttpClient()
                
                val response: HttpResponse = client.get("https://music.youtube.com") {
                    header("Cookie", cookie)
                    header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                    header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    header("Accept-Language", "en-US,en;q=0.9")
                }
                
                val html = response.body<String>()
                
                // Parse account name
                val accountName = parseAccountNameFromHTML(html)
                val thumbnailUrl = parseThumbnailUrlFromHTML(html)
                val channelId = parseChannelIdFromHTML(html)
                
                if (accountName != null) {
                    return AccountInfo(
                        name = accountName,
                        email = "",
                        channelHandle = if (channelId != null) "@$channelId" else "",
                        thumbnailUrl = thumbnailUrl
                    )
                }
                
                null
            } catch (e: Exception) {
                Timber.e("AccountInfoFetcher: YTM method failed: ${e.message}")
                null
            }
        }
        
        private fun createAccountInfoFromCookie(cookie: String): AccountInfo? {
            return try {
                val cookies = parseCookieString(cookie)
                val sapisid = cookies["SAPISID"] ?: return null
                
                val name = extractNameFromSAPISID(sapisid)
                
                // Try to get channel ID from other cookies
                val channelId = cookies["PREF"]?.let { pref ->
                    pref.split("&").find { it.contains("channel") }?.split("=")?.getOrNull(1)
                } ?: ""
                
                AccountInfo(
                    name = name,
                    email = "",
                    channelHandle = if (channelId.isNotEmpty()) "@$channelId" else "",
                    thumbnailUrl = ""
                )
            } catch (e: Exception) {
                Timber.e("AccountInfoFetcher: Error creating from cookie: ${e.message}")
                null
            }
        }
        
        private fun extractNameFromSAPISID(sapisid: String): String {
            try {
                // SAPISID format: hash/name@timestamp
                val parts = sapisid.split("/")
                if (parts.size >= 2) {
                    val namePart = parts[1].split("@").firstOrNull() ?: "YouTube User"
                    return namePart
                        .replace("%20", " ")
                        .replace("%40", "@")
                        .replace("_", " ")
                        .replace("-", " ")
                        .trim()
                }
            } catch (e: Exception) {
                Timber.e("AccountInfoFetcher: Error extracting name from SAPISID: ${e.message}")
            }
            
            return "YouTube Account"
        }
        
        private fun parseCookieString(cookie: String): Map<String, String> {
            return cookie.split(";")
                .map { it.trim() }
                .filter { it.contains("=") }
                .associate {
                    val parts = it.split("=", limit = 2)
                    parts[0] to (parts.getOrNull(1) ?: "")
                }
        }
        
        private fun parseAccountNameFromHTML(html: String): String? {
            // Try multiple patterns
            val patterns = listOf(
                Regex("\"accountName\":\"([^\"]+)\""),
                Regex("\"displayName\":\"([^\"]+)\""),
                Regex("\"authorName\":\"([^\"]+)\""),
                Regex("\"title\":\"([^\"]+)\""),
                Regex("ytmAccountName[^>]*>([^<]+)<"),
                Regex("account-name[^>]*>([^<]+)<"),
                Regex("ytmusic-user-email-renderer[^>]*title=\"([^\"]+)\""),
                Regex("\"name\":\"([^\"]+)\"")
            )
            
            for (pattern in patterns) {
                val match = pattern.find(html)
                match?.let {
                    val name = it.groupValues.getOrNull(1)?.trim()
                    if (!name.isNullOrEmpty() && name.length < 100 && name != "null") {
                        Timber.d("AccountInfoFetcher: Found account name: $name")
                        return name
                    }
                }
            }
            
            // Try ytInitialData
            val ytInitialDataMatch = Regex("ytInitialData\\s*=\\s*({[^;]+});").find(html)
            ytInitialDataMatch?.let {
                try {
                    val jsonStr = it.groupValues.getOrNull(1)
                    if (jsonStr != null) {
                        val json = JSONObject(jsonStr)
                        
                        // Navigate to possible account info
                        val accountButton = json
                            .optJSONObject("topbar")
                            ?.optJSONObject("desktopTopbarRenderer")
                            ?.optJSONObject("accountButton")
                            ?.optJSONObject("accountButtonRenderer")
                        
                        accountButton?.let { button ->
                            val name = button.optString("displayName", "")
                            if (name.isNotEmpty()) return name
                        }
                        
                        // Try other paths
                        val responseContext = json.optJSONObject("responseContext")
                        val serviceTrackingParams = responseContext?.optJSONArray("serviceTrackingParams")
                        
                        serviceTrackingParams?.let { array ->
                            for (i in 0 until array.length()) {
                                val param = array.optJSONObject(i)
                                val service = param.optString("service", "")
                                if (service == "GFEEDBACK") {
                                    val params = param.optJSONArray("params")
                                    for (j in 0 until params.length()) {
                                        val paramObj = params.optJSONObject(j)
                                        if (paramObj.optString("key", "") == "logged_in") {
                                            val value = paramObj.optString("value", "")
                                            if (value.contains("@")) {
                                                return value.split("@").firstOrNull()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e("AccountInfoFetcher: Error parsing ytInitialData: ${e.message}")
                }
            }
            
            return null
        }
        
        private fun parseChannelIdFromHTML(html: String): String? {
            val patterns = listOf(
                Regex("\"channelId\":\"([^\"]+)\""),
                Regex("\"ucid\":\"([^\"]+)\""),
                Regex("\"authorId\":\"([^\"]+)\""),
                Regex("channel/([a-zA-Z0-9_-]{24})"),
                Regex("/c/([a-zA-Z0-9_-]+)")
            )
            
            for (pattern in patterns) {
                val match = pattern.find(html)
                match?.let {
                    val id = it.groupValues.getOrNull(1)?.trim()
                    if (!id.isNullOrEmpty()) {
                        return id
                    }
                }
            }
            
            return null
        }
        
        private fun parseThumbnailUrlFromHTML(html: String): String {
            val patterns = listOf(
                Regex("\"thumbnailUrl\":\"([^\"]+)\""),
                Regex("\"avatar\":\\{[^}]*\"thumbnails\":\\[\\{[^}]*\"url\":\"([^\"]+)\""),
                Regex("avatar-img[^>]*src=\"([^\"]+)\""),
                Regex("yt-img-shadow[^>]*src=\"([^\"]+)\""),
                Regex("yt-image[^>]*src=\"([^\"]+)\"")
            )
            
            for (pattern in patterns) {
                val match = pattern.find(html)
                match?.let {
                    val url = it.groupValues.getOrNull(1)?.trim()
                    if (!url.isNullOrEmpty() && (url.startsWith("http") || url.startsWith("//"))) {
                        return if (url.startsWith("//")) "https:$url" else url
                    }
                }
            }
            
            return ""
        }
    }
}