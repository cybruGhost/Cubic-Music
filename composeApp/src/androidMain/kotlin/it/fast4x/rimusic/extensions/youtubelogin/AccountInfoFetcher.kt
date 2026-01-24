package it.fast4x.rimusic.extensions.youtubelogin

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import it.fast4x.innertube.models.AccountInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class AccountInfoFetcher {
    
    companion object {
        suspend fun fetchAccountInfo(cookie: String? = null): AccountInfo? {
            return withContext(Dispatchers.IO) {
                try {
                    Timber.d("AccountInfoFetcher: Starting account info fetch via Invidious API")
                    
                    // We'll try multiple Invidious instances in case one is down
                    val instances = listOf(
                        "https://yt.omada.cafe",
                        "https://inv.riverside.rocks",
                        "https://invidious.nerdvpn.de",
                        "https://yt.funami.tech",
                        "https://inv.nadeko.net"
                    )
                    
                    var accountInfo: AccountInfo? = null
                    
                    for (instance in instances) {
                        try {
                            Timber.d("AccountInfoFetcher: Trying instance: $instance")
                            accountInfo = fetchFromInstance(instance, cookie)
                            if (accountInfo != null) {
                                Timber.d("AccountInfoFetcher: Successfully fetched from $instance")
                                break
                            }
                        } catch (e: Exception) {
                            Timber.e("AccountInfoFetcher: Failed with $instance: ${e.message}")
                            continue
                        }
                    }
                    
                    if (accountInfo == null) {
                        Timber.d("AccountInfoFetcher: All instances failed, trying YouTube Music directly")
                        accountInfo = tryYouTubeMusicDirect(cookie)
                    }
                    
                    accountInfo
                    
                } catch (e: Exception) {
                    Timber.e("AccountInfoFetcher: Error fetching account info: ${e.message}")
                    null
                }
            }
        }
        
        private suspend fun fetchFromInstance(instanceUrl: String, cookie: String?): AccountInfo? {
            return try {
                val client = HttpClient()
                
                // Try to get subscription feed (requires auth)
                val response: HttpResponse = client.get("$instanceUrl/api/v1/auth/feed") {
                    cookie?.let { header("Cookie", it) }
                    header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                }
                
                if (response.status.value == 200) {
                    // If we get a successful response, we're logged in
                    // Now try to get subscriptions to get account info
                    val subsResponse: HttpResponse = client.get("$instanceUrl/api/v1/auth/subscriptions") {
                        cookie?.let { header("Cookie", it) }
                    }
                    
                    if (subsResponse.status.value == 200) {
                        // We're authenticated, create account info
                        // Note: Invidious doesn't directly expose account name/email
                        // We'll need to parse from cookie or use a different endpoint
                        return try {
                            // Try to get preferences which might have some account info
                            val prefsResponse: HttpResponse = client.get("$instanceUrl/api/v1/auth/preferences") {
                                cookie?.let { header("Cookie", it) }
                            }
                            
                            if (prefsResponse.status.value == 200) {
                                // Parse account name from cookie if possible
                                val accountName = parseAccountNameFromCookie(cookie)
                                
                                AccountInfo(
                                    name = accountName ?: "YouTube Account",
                                    email = "", // Invidious doesn't expose email
                                    channelHandle = "", // Would need to parse from cookie
                                    thumbnailUrl = getThumbnailFromName(accountName)
                                )
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            Timber.e("AccountInfoFetcher: Error getting preferences: ${e.message}")
                            null
                        }
                    }
                }
                
                null
            } catch (e: Exception) {
                Timber.e("AccountInfoFetcher: Error fetching from $instanceUrl: ${e.message}")
                null
            }
        }
        
        private suspend fun tryYouTubeMusicDirect(cookie: String?): AccountInfo? {
            return try {
                if (cookie.isNullOrEmpty()) return null
                
                val client = HttpClient()
                
                // Try to get YouTube Music homepage which shows account info when logged in
                val response: HttpResponse = client.get("https://music.youtube.com") {
                    header("Cookie", cookie)
                    header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                }
                
                val html = response.body<String>()
                
                // Parse account name from HTML response
                val accountName = parseAccountNameFromHTML(html)
                
                if (accountName != null) {
                    AccountInfo(
                        name = accountName,
                        email = "", // Can't get email from HTML
                        channelHandle = "", // Can't get channel handle from HTML
                        thumbnailUrl = getThumbnailFromName(accountName)
                    )
                } else {
                    null
                }
                
            } catch (e: Exception) {
                Timber.e("AccountInfoFetcher: Error trying YouTube Music direct: ${e.message}")
                null
            }
        }
        
        private fun parseAccountNameFromCookie(cookie: String?): String? {
            if (cookie.isNullOrEmpty()) return null
            
            // Try to extract account name from SAPISID cookie
            // SAPISID cookie format: SAPISID=xxx/yyy_zzz
            val sapisidMatch = Regex("SAPISID=[^/]+/([^_]+)").find(cookie)
            sapisidMatch?.let {
                return it.groupValues.getOrNull(1)?.replace("%20", " ")?.trim()
            }
            
            // Try to extract from other cookies
            val googleAccountMatch = Regex("GoogleAccounts=([^;]+)").find(cookie)
            googleAccountMatch?.let {
                return it.groupValues.getOrNull(1)?.replace("%20", " ")?.trim()
            }
            
            return null
        }
        
        private fun parseAccountNameFromHTML(html: String): String? {
            // Try to find account name in YouTube Music HTML
            val patterns = listOf(
                Regex("\"accountName\":\"([^\"]+)\""),
                Regex("ytmAccountName[^>]*>([^<]+)<"),
                Regex("account-name[^>]*>([^<]+)<"),
                Regex("ytmusic-user-email-renderer[^>]*title=\"([^\"]+)\"")
            )
            
            for (pattern in patterns) {
                val match = pattern.find(html)
                match?.let {
                    val name = it.groupValues.getOrNull(1)?.trim()
                    if (!name.isNullOrEmpty() && name.length < 100) {
                        return name
                    }
                }
            }
            
            return null
        }
        
        private fun getThumbnailFromName(name: String?): String {
            if (name.isNullOrEmpty()) return ""
            
            // Generate a simple avatar based on name initials
            // You could also try to fetch actual thumbnail from YouTube
            val initials = name.take(2).uppercase()
            return "" // Return empty for now, or use a generated avatar URL
        }
        
        // Alternative: Get account info from YouTube Data API if you have API key
        suspend fun fetchAccountInfoWithAPI(apiKey: String, cookie: String): AccountInfo? {
            return try {
                val client = HttpClient()
                
                // Get channel info using YouTube Data API
                val response: HttpResponse = client.get("https://www.googleapis.com/youtube/v3/channels") {
                    header("Authorization", "Bearer $cookie") // Use cookie as token if valid
                    parameter("part", "snippet")
                    parameter("mine", "true")
                    parameter("key", apiKey)
                }
                
                if (response.status.value == 200) {
                    val json = response.body<String>()
                    
                    // Parse JSON response
                    val titleMatch = Regex("\"title\":\"([^\"]+)\"").find(json)
                    val thumbnailMatch = Regex("\"url\":\"([^\"]+)\"").find(json)
                    
                    val name = titleMatch?.groupValues?.getOrNull(1)
                    val thumbnail = thumbnailMatch?.groupValues?.getOrNull(1)
                    
                    if (name != null) {
                        AccountInfo(
                            name = name,
                            email = "", // YouTube Data API doesn't expose email
                            channelHandle = "", // Would need additional call
                            thumbnailUrl = thumbnail ?: ""
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
                
            } catch (e: Exception) {
                Timber.e("AccountInfoFetcher: Error with YouTube Data API: ${e.message}")
                null
            }
        }
    }
}