package app.it.fast4x.rimusic.extensions.youtubelogin

import android.content.Context
import androidx.core.content.edit
import app.it.fast4x.rimusic.appContext
import app.it.fast4x.rimusic.utils.ytAccountChannelHandleKey
import app.it.fast4x.rimusic.utils.ytAccountEmailKey
import app.it.fast4x.rimusic.utils.ytAccountNameKey
import app.it.fast4x.rimusic.utils.ytAccountThumbnailKey
import app.it.fast4x.rimusic.utils.ytCookieKey
import app.it.fast4x.rimusic.utils.ytDataSyncIdKey
import app.it.fast4x.rimusic.utils.ytVisitorDataKey
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.utils.YoutubePreferenceItem
import it.fast4x.innertube.utils.YoutubePreferences
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

object YouTubeSessionStore {

    private const val PREFS_NAME = "youtube_account_sessions"
    private const val KEY_CURRENT_SESSION_ID = "current_session_id"
    private const val KEY_SESSIONS = "sessions"
    private const val ACCOUNT_REFRESH_INTERVAL_MS = 10 * 60 * 60 * 1000L

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCurrentSession(context: Context = appContext()): YoutubeSession? {
        val currentId = prefs(context).getString(KEY_CURRENT_SESSION_ID, null)
        return getSessions(context).firstOrNull { it.sessionId == currentId }
    }

    fun getSessions(context: Context = appContext()): List<YoutubeSession> {
        val raw = prefs(context).getString(KEY_SESSIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        YoutubeSession(
                            sessionId = item.optString("sessionId"),
                            cookie = item.optString("cookie"),
                            visitorData = item.optString("visitorData"),
                            dataSyncId = item.optString("dataSyncId"),
                            authUser = item.optString("authUser"),
                            pageId = item.optString("pageId"),
                            accountName = item.optString("accountName"),
                            accountEmail = item.optString("accountEmail"),
                            accountChannelHandle = item.optString("accountChannelHandle"),
                            accountThumbnail = item.optString("accountThumbnail"),
                            isPreferred = item.optBoolean("isPreferred"),
                            lastUsedAt = item.optLong("lastUsedAt"),
                            lastAccountRefreshAt = item.optLong("lastAccountRefreshAt")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
            .dedupeSessions()
            .sortedByDescending { it.lastUsedAt }
    }

    fun saveSession(
        context: Context = appContext(),
        session: YoutubeSession,
        makePreferred: Boolean = true
    ): YoutubeSession {
        val normalized = session.normalize(makePreferred = makePreferred)
        val updatedSessions = getSessions(context)
            .filterNot { it.isSameStoredSession(normalized) }
            .map { it.copy(isPreferred = false) }
            .toMutableList()
            .apply { add(0, normalized) }

        writeSessions(context, updatedSessions, normalized.sessionId)
        syncLegacyPrefs(context, normalized)
        applyToInnertube(normalized)
        return normalized
    }

    fun saveCloudSession(
        context: Context = appContext(),
        session: YtmCloudSession,
        makePreferred: Boolean = true
    ): YoutubeSession {
        require(session.hasSession) { "Cloud session is not connected" }
        return saveSession(
            context = context,
            session = YoutubeSession(
                cookie = session.cookie,
                visitorData = session.visitorData,
                dataSyncId = session.dataSyncId,
                authUser = session.authUser,
                pageId = session.pageId,
                accountName = session.accountName,
                accountEmail = session.accountEmail,
                accountChannelHandle = session.accountChannelHandle,
                accountThumbnail = session.accountThumbnail,
                lastAccountRefreshAt = if (
                    session.accountName.isNotBlank() ||
                    session.accountEmail.isNotBlank() ||
                    session.accountChannelHandle.isNotBlank() ||
                    session.accountThumbnail.isNotBlank()
                ) {
                    System.currentTimeMillis()
                } else {
                    0L
                }
            ),
            makePreferred = makePreferred
        )
    }

    fun switchToSession(
        context: Context = appContext(),
        sessionId: String
    ): YoutubeSession? {
        val target = getSessions(context).firstOrNull { it.sessionId == sessionId } ?: return null
        return saveSession(context, target, makePreferred = true)
    }

    fun clearCurrentSession(context: Context = appContext(), removeStored: Boolean = false) {
        val remaining = if (removeStored) {
            getSessions(context).filterNot { it.sessionId == getCurrentSession(context)?.sessionId }
        } else {
            getSessions(context).map { it.copy(isPreferred = false) }
        }

        if (remaining.isEmpty()) {
            prefs(context).edit {
                remove(KEY_CURRENT_SESSION_ID)
                remove(KEY_SESSIONS)
            }
        } else {
            writeSessions(context, remaining, null)
        }

        clearLegacyPrefs(context)
        applyToInnertube(null)
    }

    fun clearAllSessions(context: Context = appContext()) {
        prefs(context).edit {
            remove(KEY_CURRENT_SESSION_ID)
            remove(KEY_SESSIONS)
        }
        clearLegacyPrefs(context)
        applyToInnertube(null)
    }

    fun refreshCurrentSession(
        context: Context = appContext(),
        update: YoutubeSession.() -> YoutubeSession
    ): YoutubeSession? {
        val current = getCurrentSession(context) ?: return null
        return saveSession(context, current.update(), makePreferred = true)
    }

    fun applyCurrentSession(context: Context = appContext()): YoutubeSession? =
        getCurrentSession(context)?.also {
            syncLegacyPrefs(context, it)
            applyToInnertube(it)
        }

    fun hasAuthCookies(cookie: String?): Boolean {
        val normalized = normalizeCookieString(cookie)
        if (normalized.isBlank()) return false
        return normalized.contains("SAPISID=") ||
            normalized.contains("__Secure-3PAPISID=") ||
            normalized.contains("SID=") ||
            normalized.contains("__Secure-3PSID=")
    }

    fun normalizeCookieString(cookie: String?): String {
        if (cookie.isNullOrBlank()) return ""

        val ordered = LinkedHashMap<String, String>()
        cookie.split(";")
            .map { it.trim() }
            .filter { it.isNotBlank() && it.contains("=") }
            .forEach { part ->
                val index = part.indexOf('=')
                if (index <= 0) return@forEach
                val key = part.substring(0, index).trim()
                val value = part.substring(index + 1).trim()
                if (key.isBlank() || value.isBlank()) return@forEach
                ordered[key] = value
            }

        return ordered.entries.joinToString("; ") { (key, value) -> "$key=$value" }
    }

    fun mergeCookieStrings(primary: String?, secondary: String?): String {
        val merged = LinkedHashMap<String, String>()

        fun add(source: String?) {
            normalizeCookieString(source)
                .split(";")
                .map { it.trim() }
                .filter { it.isNotBlank() && it.contains("=") }
                .forEach { part ->
                    val index = part.indexOf('=')
                    if (index <= 0) return@forEach
                    val key = part.substring(0, index).trim()
                    val value = part.substring(index + 1).trim()
                    if (key.isBlank() || value.isBlank()) return@forEach
                    merged[key] = value
                }
        }

        add(primary)
        add(secondary)

        return merged.entries.joinToString("; ") { (key, value) -> "$key=$value" }
    }

    fun shouldRefreshAccount(session: YoutubeSession?): Boolean {
        if (session == null || !hasAuthCookies(session.cookie)) return false
        if (session.accountName.isBlank()) return true
        return System.currentTimeMillis() - session.lastAccountRefreshAt >= ACCOUNT_REFRESH_INTERVAL_MS
    }

    private fun writeSessions(
        context: Context,
        sessions: List<YoutubeSession>,
        currentSessionId: String?
    ) {
        val normalizedSessions = sessions.dedupeSessions().mapIndexed { index, session ->
            session.copy(isPreferred = currentSessionId != null && index == 0 && session.sessionId == currentSessionId)
        }

        val payload = JSONArray().apply {
            normalizedSessions.forEach { session ->
                put(
                    JSONObject().apply {
                        put("sessionId", session.sessionId)
                        put("cookie", session.cookie)
                        put("visitorData", session.visitorData)
                        put("dataSyncId", session.dataSyncId)
                        put("authUser", session.authUser)
                        put("pageId", session.pageId)
                        put("accountName", session.accountName)
                        put("accountEmail", session.accountEmail)
                        put("accountChannelHandle", session.accountChannelHandle)
                        put("accountThumbnail", session.accountThumbnail)
                        put("isPreferred", session.isPreferred)
                        put("lastUsedAt", session.lastUsedAt)
                        put("lastAccountRefreshAt", session.lastAccountRefreshAt)
                    }
                )
            }
        }.toString()

        prefs(context).edit {
            putString(KEY_SESSIONS, payload)
            if (currentSessionId.isNullOrBlank()) {
                remove(KEY_CURRENT_SESSION_ID)
            } else {
                putString(KEY_CURRENT_SESSION_ID, currentSessionId)
            }
        }
    }

    private fun syncLegacyPrefs(context: Context, session: YoutubeSession) {
        val sharedPrefs = context.getSharedPreferences("youtube_account", Context.MODE_PRIVATE)
        sharedPrefs.edit {
            putString("account_name", session.accountName)
            putString("account_email", session.accountEmail)
            putString("account_channel_handle", session.accountChannelHandle)
            putString("account_thumbnail", session.accountThumbnail)
        }

        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE).edit {
            putString("yt_cookie", session.cookie)
            putString("yt_visitor_data", session.visitorData)
            putString("yt_data_sync_id", session.dataSyncId)
            putString(ytCookieKey, session.cookie)
            putString(ytVisitorDataKey, session.visitorData)
            putString(ytDataSyncIdKey, session.dataSyncId)
            putString(ytAccountNameKey, session.accountName)
            putString(ytAccountEmailKey, session.accountEmail)
            putString(ytAccountChannelHandleKey, session.accountChannelHandle)
            putString(ytAccountThumbnailKey, session.accountThumbnail)
        }
    }

    private fun clearLegacyPrefs(context: Context) {
        context.getSharedPreferences("youtube_account", Context.MODE_PRIVATE).edit {
            remove("account_name")
            remove("account_email")
            remove("account_channel_handle")
            remove("account_thumbnail")
        }

        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE).edit {
            remove("yt_cookie")
            remove("yt_visitor_data")
            remove("yt_data_sync_id")
            remove(ytCookieKey)
            remove(ytVisitorDataKey)
            remove(ytDataSyncIdKey)
            remove(ytAccountNameKey)
            remove(ytAccountEmailKey)
            remove(ytAccountChannelHandleKey)
            remove(ytAccountThumbnailKey)
        }
    }

    private fun applyToInnertube(session: YoutubeSession?) {
        Innertube.cookie = session?.cookie?.ifBlank { null }
        Innertube.visitorData = session?.visitorData?.ifBlank { Innertube.DEFAULT_VISITOR_DATA }
            ?: Innertube.DEFAULT_VISITOR_DATA
        Innertube.dataSyncId = session?.dataSyncId?.ifBlank { null }
        YoutubePreferences.preference = YoutubePreferenceItem(
            cookie = session?.cookie?.ifBlank { null },
            visitordata = session?.visitorData?.ifBlank { null },
            dataSyncId = session?.dataSyncId?.ifBlank { null }
        )
    }

    private fun YoutubeSession.normalize(makePreferred: Boolean): YoutubeSession {
        val now = System.currentTimeMillis()
        val identitySeed = cookie.ifBlank {
            listOf(
                accountEmail.ifBlank { null },
                accountChannelHandle.ifBlank { null },
                accountName.ifBlank { "ytmusic" }
            ).filterNotNull().joinToString("|")
        }
        return copy(
            cookie = normalizeCookieString(cookie),
            visitorData = visitorData.trim(),
            dataSyncId = dataSyncId.trim(),
            authUser = authUser.trim().takeUnless { it.equals("null", ignoreCase = true) }.orEmpty(),
            pageId = pageId.trim().takeUnless { it.equals("null", ignoreCase = true) }.orEmpty(),
            sessionId = sessionId.ifBlank {
                "ytmusic-${listOf(identitySeed, authUser.trim(), pageId.trim()).joinToString("|").sha1().take(12)}"
            },
            isPreferred = makePreferred,
            lastUsedAt = now
        )
    }

    private fun List<YoutubeSession>.dedupeSessions(): List<YoutubeSession> =
        sortedByDescending { it.lastUsedAt }
            .distinctBy { it.identityKey() }

    private fun YoutubeSession.identityKey(): String =
        listOf(
            normalizeCookieString(cookie).ifBlank {
                listOf(
                    accountEmail.trim().lowercase().ifBlank { null },
                    accountChannelHandle.trim().lowercase().ifBlank { null },
                    accountName.trim().lowercase().ifBlank { null }
                ).filterNotNull().joinToString("|")
            },
            authUser.trim(),
            pageId.trim()
        ).joinToString("|").sha1()

    private fun YoutubeSession.isSameStoredSession(other: YoutubeSession): Boolean =
        identityKey() == other.identityKey()

    private fun String.sha1(): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(toByteArray())
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
