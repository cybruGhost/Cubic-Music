package app.it.fast4x.rimusic.ui.desktop

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.prefs.Preferences

internal object CubicTasteStore {
    private val node = Preferences.userRoot().node("CubicMusic/Desktop")

    fun ids(): List<String> = node.get("tasteHistory", "")
        .lineSequence().map(String::trim).filter(String::isNotBlank).distinct().take(100).toList()

    fun record(songId: String) {
        val updated = listOf(songId) + ids().filterNot { it == songId }
        node.put("tasteHistory", updated.take(100).joinToString("\n"))
    }

    fun clear() = node.remove("tasteHistory")
}

internal object CubicPlaylistStore {
    private val node = Preferences.userRoot().node("CubicMusic/Desktop/Playlists")

    fun names(): List<String> = node.get("names", "")
        .lineSequence().map(String::trim).filter(String::isNotBlank).distinct().toList()

    fun create(rawName: String): List<String> {
        val name = rawName.trim().replace(Regex("\\s+"), " ")
        if (name.isBlank()) return names()
        val updated = (names() + name).distinctBy(String::lowercase)
        node.put("names", updated.joinToString("\n"))
        return updated
    }

    fun addSong(playlist: String, songId: String) {
        if (playlist !in names()) return
        val key = songKey(playlist)
        val updated = listOf(songId) + songIds(playlist).filterNot { it == songId }
        node.put(key, updated.joinToString("\n"))
    }

    fun songIds(playlist: String): List<String> = node.get(songKey(playlist), "")
        .lineSequence().map(String::trim).filter(String::isNotBlank).distinct().toList()

    fun clear() {
        names().forEach { node.remove(songKey(it)) }
        node.remove("names")
    }

    private fun songKey(name: String): String = "songs." + Base64.getUrlEncoder().withoutPadding()
        .encodeToString(name.toByteArray(StandardCharsets.UTF_8))
}
