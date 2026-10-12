package app.it.fast4x.rimusic.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.extensions.youtubelogin.YtmHomeSectionItem
import app.it.fast4x.rimusic.models.Song

import app.it.fast4x.rimusic.repository.QuickPicksRepository
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.forcePlay
import app.it.fast4x.rimusic.utils.isYouTubeVideoId
import app.it.fast4x.rimusic.utils.rememberPreference
import app.kreate.android.R
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.relatedPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import androidx.compose.ui.platform.LocalHapticFeedback

private fun mergeShortSources(
    sources: List<List<app.it.fast4x.rimusic.extensions.youtubelogin.YtmHomeSectionItem>>,
    seed: Int,
): List<app.it.fast4x.rimusic.extensions.youtubelogin.YtmHomeSectionItem> {
    val pools = sources.mapIndexed { index, source ->
        source.shuffled(Random(seed + index * 31))
    }
    val result = ArrayList<app.it.fast4x.rimusic.extensions.youtubelogin.YtmHomeSectionItem>()
    var index = 0
    while (pools.any { index < it.size }) {
        pools.forEach { pool ->
            pool.getOrNull(index)?.let(result::add)
        }
        index++
    }
    return result.distinctBy { it.videoId.ifBlank { it.id } }
}

internal fun Song.asMusicShortCandidate(): YtmHomeSectionItem? {
    if (!id.isYouTubeVideoId() || title.isBlank()) return null
    return YtmHomeSectionItem(
        id = id,
        videoId = id,
        title = title,
        subtitle = artistsText.orEmpty(),
        artistsText = artistsText.orEmpty(),
        thumbnail = thumbnailUrl.orEmpty(),
        thumbnailUrl = thumbnailUrl.orEmpty(),
        type = "song",
    )
}

internal object MusicShortRecommendationCache {
    @Volatile
    var items: List<YtmHomeSectionItem> = emptyList()
        private set

    fun store(newItems: List<YtmHomeSectionItem>) {
        items = (newItems + items)
            .distinctBy { item -> item.videoId.ifBlank { item.id } }
            .take(180)
    }
}

private fun Innertube.SongItem.asMusicShortCandidate(): YtmHomeSectionItem? {
    val mediaId = key.trim()
    val itemTitle = info?.name?.trim().orEmpty()
    if (!mediaId.isYouTubeVideoId() || itemTitle.isBlank()) return null

    val artistText = authors
        ?.mapNotNull { author -> author.name?.trim()?.takeIf(String::isNotBlank) }
        ?.distinct()
        ?.joinToString(", ")
        .orEmpty()
    val artwork = thumbnail?.url.orEmpty()
    return YtmHomeSectionItem(
        id = mediaId,
        videoId = mediaId,
        title = itemTitle,
        subtitle = artistText,
        artistsText = artistText,
        thumbnail = artwork,
        thumbnailUrl = artwork,
        type = "song",
    )
}

internal suspend fun relatedMusicShortCandidates(
    seedItems: List<YtmHomeSectionItem>,
): List<YtmHomeSectionItem> {
    val seedIds = seedItems
        .map { item -> item.videoId.trim().ifBlank { item.id.trim() } }
        .filter(String::isYouTubeVideoId)
        .distinct()
        .shuffled()
        .take(3)

    return seedIds.flatMap { seedId ->
        runCatching { Innertube.relatedPage(NextBody(videoId = seedId)) }
            .getOrNull()
            ?.getOrNull()
            ?.songs
            .orEmpty()
            .mapNotNull(Innertube.SongItem::asMusicShortCandidate)
    }.distinctBy { item -> item.videoId }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicShortsScreen(
    navController: NavController,
    initialVideoId: String,
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val hapticFeedback = LocalHapticFeedback.current
    var items by remember { mutableStateOf(emptyList<YtmHomeSectionItem>()) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var loadGeneration by remember { mutableStateOf(0L) }
    var seenIds by remember { mutableStateOf(emptySet<String>()) }
    var wasPlayingBeforeShorts by remember { mutableStateOf(false) }
    var handedOffToFullSong by remember { mutableStateOf(false) }
    var recentShortIdsCsv by rememberPreference("musicShortsRecentIds", "")

    LaunchedEffect(Unit) {
        wasPlayingBeforeShorts = binder?.player?.isPlaying == true
        binder?.player?.pause()

        val recentIds = recentShortIdsCsv
            .split(',')
            .filter(String::isNotBlank)
            .toSet()
        val casualCandidates = withContext(Dispatchers.IO) {
            withTimeoutOrNull(25_000L) {
                QuickPicksRepository.loadCasualPlayedRecommendations(
                    limit = 48,
                    excludedIds = recentIds,
                )
            }.orEmpty()
        }.mapNotNull(Song::asMusicShortCandidate)
        val cachedCandidates = MusicShortRecommendationCache.items
        val radioCandidates = withContext(Dispatchers.IO) {
            withTimeoutOrNull(25_000L) {
                relatedMusicShortCandidates(casualCandidates + cachedCandidates)
            }.orEmpty()
        }
        val candidates = mergeShortSources(
            sources = listOf(cachedCandidates, casualCandidates, radioCandidates),
            seed = (System.nanoTime() xor System.currentTimeMillis()).toInt(),
        )
        val requestedItem = candidates.firstOrNull { candidate ->
            candidate.videoId.ifBlank { candidate.id } == initialVideoId
        }
        val freshCandidates = candidates.filterNot { candidate ->
            candidate.videoId.ifBlank { candidate.id } in recentIds
        }
        items = (listOfNotNull(requestedItem) + freshCandidates + candidates)
            .distinctBy { candidate -> candidate.videoId.ifBlank { candidate.id } }
            .take(48)
        seenIds = items.map { item -> item.videoId.ifBlank { item.id } }.toSet()
        recentShortIdsCsv = (items.map { item -> item.videoId.ifBlank { item.id } } + recentIds)
            .filter(String::isNotBlank)
            .distinct()
            .take(120)
            .joinToString(",")
        MusicShortRecommendationCache.store(items)
        loading = false
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!handedOffToFullSong && wasPlayingBeforeShorts) {
                runCatching { binder?.player?.play() }
            }
        }
    }

    LaunchedEffect(loadGeneration) {
        if (loadGeneration == 0L || loadingMore || items.isEmpty()) return@LaunchedEffect
        loadingMore = true

        val rotatingStart = ((loadGeneration * 7L) % items.size).toInt()
        val rotatingSeeds = buildList {
            repeat(minOf(8, items.size)) { offset ->
                add(items[(rotatingStart + offset) % items.size])
            }
            addAll(items.takeLast(minOf(8, items.size)))
        }.distinctBy { item -> item.videoId.ifBlank { item.id } }

        val radioAdditions = withContext(Dispatchers.IO) {
            withTimeoutOrNull(25_000L) {
                relatedMusicShortCandidates(rotatingSeeds)
            }.orEmpty()
        }
        val casualAdditions = if (radioAdditions.count { item ->
                item.videoId.ifBlank { item.id } !in seenIds
            } < 18
        ) {
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(25_000L) {
                    QuickPicksRepository.loadCasualPlayedRecommendations(
                        limit = 48,
                        excludedIds = seenIds,
                        forceRefresh = true,
                    )
                }.orEmpty()
            }.mapNotNull(Song::asMusicShortCandidate)
        } else {
            emptyList()
        }
        val expandedCasual = if (casualAdditions.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(25_000L) {
                    relatedMusicShortCandidates(casualAdditions)
                }.orEmpty()
            }
        } else {
            emptyList()
        }
        val additions = mergeShortSources(
            sources = listOf(radioAdditions, casualAdditions, expandedCasual),
            seed = (System.nanoTime() xor loadGeneration).toInt(),
        ).filter { item ->
            val id = item.videoId.ifBlank { item.id }
            id.isNotBlank() && id !in seenIds
        }

        if (additions.isNotEmpty()) {
            items = (items + additions)
                .distinctBy { item -> item.videoId.ifBlank { item.id } }
            seenIds = items.map { item -> item.videoId.ifBlank { item.id } }.toSet()
            recentShortIdsCsv = (additions.map { item -> item.videoId.ifBlank { item.id } } +
                recentShortIdsCsv.split(','))
                .filter(String::isNotBlank)
                .distinct()
                .take(120)
                .joinToString(",")
            MusicShortRecommendationCache.store(additions)
        }
        loadingMore = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when {
            items.isNotEmpty() -> MusicShortsFeed(
                items = items,
                initialIndex = items.indexOfFirst {
                    it.videoId.ifBlank { it.id } == initialVideoId
                }.takeIf { it >= 0 } ?: 0,
                isLoadingMore = loadingMore,
                onLoadMore = {
                    if (!loadingMore) loadGeneration += 1L
                },
                onClose = { navController.popBackStack() },
                onPlayFullSong = { item ->
                    handedOffToFullSong = true
                    item.asMusicShortMediaItem()?.let { mediaItem ->
                        binder?.stopRadio()
                        binder?.player?.forcePlay(mediaItem)
                        navController.popBackStack()
                    }
                },
                onAddToPlaylist = { item ->
                    item.asMusicShortMediaItem()?.let { mediaItem ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.display {
                            app.kreate.android.me.knighthat.component.menu.search.SearchItemMenu(
                                navController = navController,
                                song = mediaItem.asSong,
                            ).MenuComponent()
                        }
                    }
                },
            )

            loading -> CircularProgressIndicator(
                color = colorPalette().accent,
                strokeWidth = 2.dp,
            )
        }
    }
}
