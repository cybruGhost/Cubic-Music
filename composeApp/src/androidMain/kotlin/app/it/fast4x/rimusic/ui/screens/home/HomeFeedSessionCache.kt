package app.it.fast4x.rimusic.ui.screens.home

import app.it.fast4x.rimusic.extensions.youtubelogin.YtmHomeSection
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmHomeSectionItem
import app.it.fast4x.rimusic.utils.isYouTubeVideoId
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.requests.HomePage

/** Process-lifetime cache for live Home feeds. No credentials are stored here. */
internal object HomeFeedSessionCache {
    @Volatile
    var publicGuestSections: List<YtmHomeSection>? = null
        private set

    @Volatile
    var directHomePage: HomePage? = null
        private set

    @Volatile
    var discoverPage: Innertube.DiscoverPage? = null
        private set

    @Volatile
    private var signedAccountKey: String = ""

    @Volatile
    private var signedHomeSections: List<YtmHomeSection>? = null

    fun signedSections(accountKey: String): List<YtmHomeSection>? = synchronized(this) {
        signedHomeSections.takeIf { accountKey.isNotBlank() && signedAccountKey == accountKey }
    }

    fun latestSignedSections(): List<YtmHomeSection>? = synchronized(this) {
        signedHomeSections
    }

    fun storePublicGuest(sections: List<YtmHomeSection>) = synchronized(this) {
        publicGuestSections = sections
    }

    fun storeDirectHome(page: HomePage?) = synchronized(this) {
        if (page != null) directHomePage = page
    }

    fun storeDiscover(page: Innertube.DiscoverPage?) = synchronized(this) {
        if (page != null) discoverPage = page
    }

    fun storeSigned(accountKey: String, sections: List<YtmHomeSection>) = synchronized(this) {
        if (accountKey.isNotBlank()) {
            signedAccountKey = accountKey
            signedHomeSections = sections
        }
    }

    fun clearSigned() = synchronized(this) {
        signedAccountKey = ""
        signedHomeSections = null
    }
}

private fun musicShortSectionPriority(title: String): Int {
    val normalized = title.trim().lowercase()
    return when {
        normalized == "for you" || "made for you" in normalized -> 0
        "mixed for you" in normalized || "create a mix" in normalized -> 1
        "quick pick" in normalized -> 2
        "daily discover" in normalized || "fresh find" in normalized -> 3
        "trending song" in normalized || ("today" in normalized && "hit" in normalized) -> 4
        "new release" in normalized -> 5
        "heard in shorts" in normalized || ("shorts" in normalized && "music" in normalized) -> 6
        "music video" in normalized -> 7
        else -> 30
    }
}

private fun <T> interleaveMusicShortPools(
    pools: List<List<T>>,
    limit: Int,
    stableId: (T) -> String,
): List<T> {
    if (limit <= 0) return emptyList()
    val result = ArrayList<T>(limit)
    val seen = hashSetOf<String>()
    var itemIndex = 0
    while (result.size < limit && pools.any { pool -> itemIndex < pool.size }) {
        pools.forEach { pool ->
            val item = pool.getOrNull(itemIndex) ?: return@forEach
            val id = stableId(item)
            if (id.isNotBlank() && seen.add(id)) result.add(item)
            if (result.size >= limit) return result
        }
        itemIndex++
    }
    return result
}

internal fun List<YtmHomeSection>.musicShortCandidates(
    limit: Int = 18,
): List<YtmHomeSectionItem> {
    val pools = sortedBy { section -> musicShortSectionPriority(section.title) }
        .map { section ->
            section.items
                .filter { item ->
                    item.type.equals("song", ignoreCase = true) ||
                        item.type.equals("video", ignoreCase = true)
                }
                .filter { item ->
                    item.videoId.trim().ifBlank { item.id.trim() }.isYouTubeVideoId() &&
                        item.title.isNotBlank() &&
                        item.artistsText.ifBlank { item.subtitle }.isNotBlank()
                }
        }
        .filter(List<YtmHomeSectionItem>::isNotEmpty)

    return interleaveMusicShortPools(
        pools = pools,
        limit = limit,
        stableId = { item -> item.videoId.trim().ifBlank { item.id.trim() } },
    )
}

internal fun HomePage.musicShortCandidates(
    limit: Int = 18,
): List<YtmHomeSectionItem> {
    val pools = sections
        .sortedBy { section -> musicShortSectionPriority(section.title) }
        .map { section ->
            section.items.mapNotNull { item ->
                when (item) {
                    is Innertube.SongItem -> YtmHomeSectionItem(
                        id = item.key.orEmpty(),
                        videoId = item.key.orEmpty(),
                        title = item.info?.name.orEmpty(),
                        subtitle = item.authors.orEmpty().joinToString(", ") { author -> author.name.orEmpty() },
                        artistsText = item.authors.orEmpty().joinToString(", ") { author -> author.name.orEmpty() },
                        thumbnail = item.thumbnail?.url.orEmpty(),
                        thumbnailUrl = item.thumbnail?.url.orEmpty(),
                        type = "song",
                    )

                    is Innertube.VideoItem -> YtmHomeSectionItem(
                        id = item.key.orEmpty(),
                        videoId = item.key.orEmpty(),
                        title = item.info?.name.orEmpty(),
                        subtitle = item.authors.orEmpty().joinToString(", ") { author -> author.name.orEmpty() },
                        artistsText = item.authors.orEmpty().joinToString(", ") { author -> author.name.orEmpty() },
                        thumbnail = item.thumbnail?.url.orEmpty(),
                        thumbnailUrl = item.thumbnail?.url.orEmpty(),
                        type = "video",
                    )

                    else -> null
                }
            }.filter { item ->
                item.videoId.isYouTubeVideoId() &&
                    item.title.isNotBlank() &&
                    item.artistsText.isNotBlank()
            }
        }
        .filter(List<YtmHomeSectionItem>::isNotEmpty)

    return interleaveMusicShortPools(
        pools = pools,
        limit = limit,
        stableId = YtmHomeSectionItem::videoId,
    )
}