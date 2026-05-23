package app.it.fast4x.rimusic.extensions.youtubelogin

import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.AccountInfo
import it.fast4x.innertube.utils.parseCookieString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class AccountInfoFetcher {

    companion object {
        suspend fun fetchAccountInfo(
            cookie: String? = null,
            visitorData: String? = null,
            dataSyncId: String? = null
        ): AccountInfo? = withContext(Dispatchers.IO) {
            val currentSession = YouTubeSessionStore.getCurrentSession()
            if (cookie.isNullOrBlank() && currentSession != null && !YouTubeSessionStore.shouldRefreshAccount(currentSession)) {
                return@withContext AccountInfo(
                    name = currentSession.accountName.ifBlank { null },
                    email = currentSession.accountEmail.ifBlank { null },
                    channelHandle = currentSession.accountChannelHandle.ifBlank { null },
                    thumbnailUrl = currentSession.accountThumbnail.ifBlank { null }
                )
            }

            val providedCookie = cookie?.takeIf { it.isNotBlank() }
            val parsedCookie = providedCookie?.let(::parseCookieString).orEmpty()

            if (providedCookie != null && !parsedCookie.hasAuthCookies()) {
                Timber.w("AccountInfoFetcher: Missing YouTube auth cookies")
                return@withContext null
            }

            val originalCookie = Innertube.cookie
            val originalVisitorData = Innertube.visitorData
            val originalDataSyncId = Innertube.dataSyncId

            try {
                if (providedCookie != null) {
                    Innertube.cookie = providedCookie
                }
                if (!visitorData.isNullOrBlank()) {
                    Innertube.visitorData = visitorData
                }
                if (!dataSyncId.isNullOrBlank()) {
                    Innertube.dataSyncId = dataSyncId
                }

                mergeWithFallback(
                    YouTubeRequestThrottler.run {
                    Innertube.accountInfo().getOrNull()
                    },
                    currentSession
                ) ?: fallbackAccountInfo(currentSession)
            } catch (throwable: Throwable) {
                Timber.e(throwable, "AccountInfoFetcher: Failed to fetch account info from InnerTube")
                fallbackAccountInfo(currentSession)
            } finally {
                if (providedCookie != null) {
                    Innertube.cookie = originalCookie
                    Innertube.visitorData = originalVisitorData
                    Innertube.dataSyncId = originalDataSyncId
                }
            }
        }

        suspend fun fetchAllLinkedAccounts(
            cookie: String? = null,
            visitorData: String? = null,
            dataSyncId: String? = null
        ): List<AccountInfo> = withContext(Dispatchers.IO) {
            val currentSession = YouTubeSessionStore.getCurrentSession()
            val providedCookie = cookie?.takeIf { it.isNotBlank() }
            val parsedCookie = providedCookie?.let(::parseCookieString).orEmpty()

            if (providedCookie != null && !parsedCookie.hasAuthCookies()) {
                Timber.w("AccountInfoFetcher: Missing YouTube auth cookies for linked account fetch")
                return@withContext emptyList()
            }

            val originalCookie = Innertube.cookie
            val originalVisitorData = Innertube.visitorData
            val originalDataSyncId = Innertube.dataSyncId

            try {
                if (providedCookie != null) {
                    Innertube.cookie = providedCookie
                }
                if (!visitorData.isNullOrBlank()) {
                    Innertube.visitorData = visitorData
                }
                if (!dataSyncId.isNullOrBlank()) {
                    Innertube.dataSyncId = dataSyncId
                }

                (YouTubeRequestThrottler.run {
                    Innertube.allAccounts().getOrNull()
                } ?: emptyList())
                    .map { mergeWithFallback(it, currentSession) ?: it }
                    .filter {
                        !it.name.isNullOrBlank() || !it.email.isNullOrBlank() || !it.channelHandle.isNullOrBlank()
                    }
                    .distinctBy {
                        listOf(
                            it.channelHandle?.trim()?.lowercase(),
                            it.name?.trim()?.lowercase(),
                            it.thumbnailUrl?.trim()
                        ).joinToString("|")
                    }
                    .ifEmpty { listOfNotNull(fallbackAccountInfo(currentSession)) }
            } catch (throwable: Throwable) {
                Timber.e(throwable, "AccountInfoFetcher: Failed to fetch linked accounts from InnerTube")
                listOfNotNull(fallbackAccountInfo(currentSession))
            } finally {
                if (providedCookie != null) {
                    Innertube.cookie = originalCookie
                    Innertube.visitorData = originalVisitorData
                    Innertube.dataSyncId = originalDataSyncId
                }
            }
        }

        private fun fallbackAccountInfo(session: YoutubeSession?): AccountInfo? =
            session?.takeIf {
                it.accountName.isNotBlank() ||
                    it.accountEmail.isNotBlank() ||
                    it.accountChannelHandle.isNotBlank() ||
                    it.accountThumbnail.isNotBlank()
            }?.let {
                AccountInfo(
                    name = it.accountName.ifBlank { null },
                    email = it.accountEmail.ifBlank { null },
                    channelHandle = it.accountChannelHandle.ifBlank { null },
                    thumbnailUrl = it.accountThumbnail.ifBlank { null }
                )
            }

        private fun mergeWithFallback(
            accountInfo: AccountInfo?,
            session: YoutubeSession?
        ): AccountInfo? {
            if (accountInfo == null) return fallbackAccountInfo(session)

            val normalizedName = accountInfo.name?.trim().takeUnless { it.isNullOrBlank() }
                ?: session?.accountName?.trim().takeUnless { it.isNullOrBlank() }
            val normalizedEmail = accountInfo.email?.trim().takeUnless { it.isNullOrBlank() }
                ?: session?.accountEmail?.trim().takeUnless { it.isNullOrBlank() }
            val normalizedHandle = accountInfo.channelHandle?.trim().takeUnless { it.isNullOrBlank() }
                ?: session?.accountChannelHandle?.trim().takeUnless { it.isNullOrBlank() }
            val normalizedThumbnail = accountInfo.thumbnailUrl
                ?.substringBefore("?")
                ?.substringBefore("=")
                ?.trim()
                .takeUnless { it.isNullOrBlank() }
                ?: session?.accountThumbnail?.trim().takeUnless { it.isNullOrBlank() }

            return AccountInfo(
                name = normalizedName,
                email = normalizedEmail,
                channelHandle = normalizedHandle,
                thumbnailUrl = normalizedThumbnail
            )
        }

        private fun Map<String, String>.hasAuthCookies(): Boolean =
            YouTubeSessionStore.hasAuthCookies(
                entries.joinToString("; ") { "${it.key}=${it.value}" }
            )
    }
}
