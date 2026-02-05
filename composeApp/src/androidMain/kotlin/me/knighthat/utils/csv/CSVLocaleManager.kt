package me.knighthat.utils.csv

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.fast4x.rimusic.appContext
import org.json.JSONObject

/**
 * 🔹 CSV Locale Manager - Handles multilingual CSV headers
 */
object CSVLocaleManager {
    
    private var localeMappings: Map<String, Map<String, String>> = emptyMap()
    private var isInitialized = false
    
    /** 🔹 Initialize with locales from assets */
    fun initialize(context: Context = appContext()) {
        if (isInitialized) return
        
        try {
            // List of supported locales from your i18n setup
            val locales = listOf("de", "en", "es", "fr", "it", "nl", "pt", "sv", "ar", "ja", "tr")
            val mappings = mutableMapOf<String, Map<String, String>>()
            
            locales.forEach { locale ->
                try {
                    // Load locale file from assets
                    val inputStream = context.assets.open("csv_locales/$locale.json")
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonString)
                    val headers = mutableMapOf<String, String>()
                    
                    json.keys().forEach { key ->
                        headers[key] = json.getString(key)
                    }
                    
                    mappings[locale] = headers
                    println("✅ Loaded CSV locale: $locale")
                } catch (e: Exception) {
                    // If locale file doesn't exist, skip it
                    println("⚠️ CSV locale $locale not found: ${e.message}")
                }
            }
            
            localeMappings = mappings
            isInitialized = true
            println("✅ CSVLocaleManager initialized with ${mappings.size} locales")
        } catch (e: Exception) {
            println("❌ Error loading CSV locales: ${e.message}")
        }
    }
    
    /** 🔹 Normalize headers to English using locale detection */
    fun normalizeRow(row: Map<String, String>): Map<String, String> {
        if (!isInitialized) {
            initialize()
        }
        
        val normalized = mutableMapOf<String, String>()
        
        // First, copy all original headers and values
        row.forEach { (key, value) ->
            normalized[key] = value
        }
        
        // Skip if no locales loaded
        if (localeMappings.isEmpty()) {
            return normalized
        }
        
        // Detect language by checking if any header matches any locale
        var detectedLocale: String? = null
        
        for ((locale, mapping) in localeMappings) {
            // Check if any header matches this locale's values
            val matches = mapping.values.any { localizedHeader ->
                row.keys.any { rowHeader ->
                    rowHeader.equals(localizedHeader, ignoreCase = true) ||
                    rowHeader.contains(localizedHeader, ignoreCase = true)
                }
            }
            
            if (matches) {
                detectedLocale = locale
                println("📝 Detected CSV locale: $locale")
                break
            }
        }
        
        // If we detected a locale, map localized headers to English
        if (detectedLocale != null) {
            val mapping = localeMappings[detectedLocale] ?: return normalized
            
            mapping.forEach { (englishHeader, localizedHeader) ->
                // Find the row header that matches the localized header
                row.keys.find { rowHeader ->
                    rowHeader.equals(localizedHeader, ignoreCase = true) ||
                    rowHeader.contains(localizedHeader, ignoreCase = true)
                }?.let { matchingHeader ->
                    // Add English header with the value from localized header
                    normalized[englishHeader] = row[matchingHeader] ?: ""
                    println("🔀 Mapped '$matchingHeader' -> '$englishHeader'")
                }
            }
        }
        
        return normalized
    }
    /** 🔹 Detect locale from CSV headers */
fun detectLocale(headers: Set<String>): String? {
    for ((locale, mapping) in localeMappings) {
        // Check if any header matches this locale's mapping
        val matched = mapping.values.any { localizedHeader ->
            headers.any { header ->
                header.equals(localizedHeader, ignoreCase = true) ||
                header.contains(localizedHeader, ignoreCase = true)
            }
        }
        
        if (matched) {
            return locale
        }
    }
    return null
}
    
    /** 🔹 Check if CSV has Spotify/Exportify format headers (in any language) */
    fun isSpotifyFormat(headers: Set<String>): Boolean {
        if (!isInitialized) {
            initialize()
        }
        
        // Check for standard English headers first
        val englishHeaders = setOf("Track Name", "Artist Name(s)", "Track URI")
        val hasEnglishHeaders = englishHeaders.any { header -> headers.contains(header) }
        if (hasEnglishHeaders) return true
        
        // Check if any header matches localized versions
        for ((_, mapping) in localeMappings) {
            val trackNameLocalized = mapping["Track Name"] ?: continue
            val artistNameLocalized = mapping["Artist Name(s)"]
            
            val hasLocalizedHeaders = headers.any { header ->
                header.equals(trackNameLocalized, ignoreCase = true)
            } && (artistNameLocalized == null || headers.any { header ->
                header.equals(artistNameLocalized, ignoreCase = true)
            })
            
            if (hasLocalizedHeaders) {
                return true
            }
        }
        
        return false
    }
    
    /** 🔹 Check if CSV has custom format headers */
    fun isCustomFormat(headers: Set<String>): Boolean {
        val customHeaders = setOf(
            "PlaylistBrowseId", "PlaylistName", "MediaId", "Title", 
            "Artists", "Duration", "ThumbnailUrl", "AlbumId", 
            "AlbumTitle", "ArtistIds"
        )
        return customHeaders.any { header -> headers.contains(header) }
    }
    
    /** 🔹 Get supported locales */
    fun getSupportedLocales(): List<String> = localeMappings.keys.toList()
    
    /** 🔹 Get locale name for display */
    @Composable
    fun getLocaleDisplayName(locale: String): String {
        return when (locale) {
            "de" -> "Deutsch"
            "en" -> "English"
            "es" -> "Español"
            "fr" -> "Français"
            "it" -> "Italiano"
            "nl" -> "Nederlands"
            "pt" -> "Português"
            "sv" -> "Svenska"
            "ar" -> "العربية"
            "ja" -> "日本語"
            "tr" -> "Türkçe"
            else -> locale
        }
    }
}