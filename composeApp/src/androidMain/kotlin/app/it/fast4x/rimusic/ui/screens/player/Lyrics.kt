package app.it.fast4x.rimusic.ui.screens.player

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import app.kreate.android.R
import com.valentinilk.shimmer.shimmer
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.lyrics
import it.fast4x.kugou.KuGou
import it.fast4x.lrclib.LrcLib
import it.fast4x.lrclib.models.Track
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.ColorPaletteMode
import app.it.fast4x.rimusic.enums.ColorPaletteName
import app.it.fast4x.rimusic.enums.Languages
import app.it.fast4x.rimusic.enums.LyricsAlignment
import app.it.fast4x.rimusic.enums.LyricsBackground
import app.it.fast4x.rimusic.enums.LyricsColor
import app.it.fast4x.rimusic.enums.LyricsFontSize
import app.it.fast4x.rimusic.enums.LyricsHighlight
import app.it.fast4x.rimusic.enums.LyricsOutline
import app.it.fast4x.rimusic.enums.PlayerBackgroundColors
import app.it.fast4x.rimusic.enums.Romanization
import app.it.fast4x.rimusic.models.Lyrics
import app.it.fast4x.rimusic.models.Song
import app.it.fast4x.rimusic.thumbnailShape
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.themed.DefaultDialog
import app.it.fast4x.rimusic.ui.components.themed.IconButton
import app.it.fast4x.rimusic.ui.components.themed.InputTextDialog
import app.it.fast4x.rimusic.ui.components.themed.LyricsSizeDialog
import app.it.fast4x.rimusic.ui.components.themed.Menu
import app.it.fast4x.rimusic.ui.components.themed.MenuEntry
import app.it.fast4x.rimusic.ui.components.themed.TextPlaceholder
import app.it.fast4x.rimusic.ui.components.themed.TitleSection
import app.it.fast4x.rimusic.ui.styling.DefaultDarkColorPalette
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.ui.styling.PureBlackColorPalette
import app.it.fast4x.rimusic.ui.styling.onOverlayShimmer
import app.it.fast4x.rimusic.utils.SynchronizedLyrics
import app.it.fast4x.rimusic.utils.center
import app.it.fast4x.rimusic.utils.color
import app.it.fast4x.rimusic.utils.buildLyricsShareLink
import app.it.fast4x.rimusic.utils.buildLyricsShareLines
import app.it.fast4x.rimusic.utils.buildLyricsShareSlice
import app.it.fast4x.rimusic.utils.colorPaletteModeKey
import app.it.fast4x.rimusic.utils.colorPaletteNameKey
import app.it.fast4x.rimusic.utils.conditional
import app.it.fast4x.rimusic.utils.defaultLyricsSourceKey
import app.it.fast4x.rimusic.utils.effectRotationKey
import app.it.fast4x.rimusic.utils.expandedplayerKey
import app.it.fast4x.rimusic.utils.fetchSimpMusicLyrics
import app.cubic.android.core.network.NetworkClientFactory
import app.it.fast4x.rimusic.utils.isShowingSynchronizedLyricsKey
import app.it.fast4x.rimusic.utils.jumpPreviousKey
import app.it.fast4x.rimusic.utils.landscapeControlsKey
import app.it.fast4x.rimusic.utils.languageDestination
import app.it.fast4x.rimusic.utils.languageDestinationName
import app.it.fast4x.rimusic.utils.lyricsAlignmentKey
import app.it.fast4x.rimusic.utils.lyricsBackgroundKey
import app.it.fast4x.rimusic.utils.lyricsColorKey
import app.it.fast4x.rimusic.utils.lyricsFontSizeKey
import app.it.fast4x.rimusic.utils.lyricsHighlightKey
import app.it.fast4x.rimusic.utils.lyricsOutlineKey
import app.it.fast4x.rimusic.utils.lyricsKaraokeEnabledKey
import app.it.fast4x.rimusic.utils.lyricsSizeAnimateKey
import app.it.fast4x.rimusic.utils.lyricsSizeKey
import app.it.fast4x.rimusic.utils.lyricsSizeLKey
import app.it.fast4x.rimusic.utils.medium
import app.it.fast4x.rimusic.utils.otherLanguageAppKey
import app.it.fast4x.rimusic.utils.playNext
import app.it.fast4x.rimusic.utils.playPrevious
import app.it.fast4x.rimusic.utils.pickBestLrcLibTrack
import app.it.fast4x.rimusic.utils.plainLyricsFromTimedText
import app.it.fast4x.rimusic.utils.playerBackgroundColorsKey
import app.it.fast4x.rimusic.utils.playerEnableLyricsPopupMessageKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.romanizationKey
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.shareLyricsCard
import app.it.fast4x.rimusic.utils.showBackgroundLyricsKey
import app.it.fast4x.rimusic.utils.showSecondLineKey
import app.it.fast4x.rimusic.utils.showLyricsSourceSwitcherKey
import app.it.fast4x.rimusic.utils.showlyricsthumbnailKey
import app.it.fast4x.rimusic.utils.simpMusicTranslationEnabledKey
import app.it.fast4x.rimusic.utils.textCopyToClipboard
import app.it.fast4x.rimusic.utils.verticalFadingEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bush.translator.Language
import me.bush.translator.Translator
import app.kreate.android.me.knighthat.utils.Toaster
import timber.log.Timber
import kotlin.Float.Companion.POSITIVE_INFINITY
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val textFieldColors: TextFieldColors
    @Composable
    get() = TextFieldDefaults.colors(
        unfocusedTextColor =  colorPalette().text,
        focusedTextColor = colorPalette().text,
        unfocusedIndicatorColor = colorPalette().text,
        focusedIndicatorColor = colorPalette().text
    )


@UnstableApi
@Composable
fun Lyrics(
    mediaId: String,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    size: Dp,
    mediaMetadataProvider: () -> MediaMetadata,
    durationProvider: () -> Long,
    ensureSongInserted: Database.() -> Unit,
    modifier: Modifier = Modifier,
    clickLyricsText: Boolean,
    trailingContent: (@Composable () -> Unit)? = null,
    isLandscape: Boolean,
) {
    AnimatedVisibility(
        visible = isDisplayed,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        val menuState = LocalMenuState.current
        val currentView = LocalView.current
        val binder = LocalPlayerServiceBinder.current

        var showlyricsthumbnail by rememberPreference(showlyricsthumbnailKey, false)
        var isShowingSynchronizedLyrics by rememberPreference(isShowingSynchronizedLyricsKey, true)
        var invalidLrc by remember(mediaId, isShowingSynchronizedLyrics) { mutableStateOf(false) }
        var isPicking by remember(mediaId, isShowingSynchronizedLyrics) { mutableStateOf(false) }
        var lyricsColor by rememberPreference(
            lyricsColorKey,
            LyricsColor.Thememode
        )
        var lyricsOutline by rememberPreference(
            lyricsOutlineKey,
            LyricsOutline.Glow
        )
        val playerBackgroundColors by rememberPreference(
            playerBackgroundColorsKey,
            PlayerBackgroundColors.BlurredCoverColor
        )
        var lyricsFontSize by rememberPreference(lyricsFontSizeKey, LyricsFontSize.Medium)

        val thumbnailSize = Dimensions.thumbnails.player.song
        val colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.Dark)

        var isEditing by remember(mediaId, isShowingSynchronizedLyrics) {
            mutableStateOf(false)
        }

        var showPlaceholder by remember {
            mutableStateOf(false)
        }

        var lyrics by remember(mediaId) {
            mutableStateOf<Lyrics?>(null)
        }

        val text = if (isShowingSynchronizedLyrics) lyrics?.synced else lyrics?.fixed

        var isError by remember(mediaId, isShowingSynchronizedLyrics) {
            mutableStateOf(false)
        }
        var isErrorSync by remember(mediaId, isShowingSynchronizedLyrics) {
            mutableStateOf(false)
        }

        var showLanguagesList by remember {
            mutableStateOf(false)
        }
        var changingSimpMusicLanguage by remember {
            mutableStateOf(false)
        }

        var translateEnabled by remember {
            mutableStateOf(false)
        }

        var romanization by rememberPreference(romanizationKey, Romanization.Off)
        var showSecondLine by rememberPreference(showSecondLineKey, false)

        var otherLanguageApp by rememberPreference(otherLanguageAppKey, Languages.English)
        var lyricsBackground by rememberPreference(lyricsBackgroundKey, LyricsBackground.Black)

        if (showLanguagesList) {
            if (!changingSimpMusicLanguage) {
                translateEnabled = false
            }
            menuState.display {
                Menu {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TitleSection(title = stringResource(R.string.languages))
                    }

                    MenuEntry(
                        icon = R.drawable.translate,
                        text = stringResource(R.string.do_not_translate),
                        secondaryText = "",
                        onClick = {
                            menuState.hide()
                            showLanguagesList = false
                            if (changingSimpMusicLanguage) {
                                changingSimpMusicLanguage = false
                            } else {
                                translateEnabled = false
                            }

                        }
                    )
                    MenuEntry(
                        icon = R.drawable.translate,
                        text = stringResource(R.string._default),
                        secondaryText = languageDestinationName(otherLanguageApp),
                        onClick = {
                            menuState.hide()
                            showLanguagesList = false
                            if (changingSimpMusicLanguage) {
                                changingSimpMusicLanguage = false
                            } else {
                                translateEnabled = true
                            }

                        }
                    )

                    Languages.entries.forEach {
                        if (it != Languages.System)
                            MenuEntry(
                                icon = R.drawable.translate,
                                text = languageDestinationName(it),
                                secondaryText = "",
                                onClick = {
                                    menuState.hide()
                                    otherLanguageApp = it
                                    showLanguagesList = false
                                    if (changingSimpMusicLanguage) {
                                        changingSimpMusicLanguage = false
                                    } else {
                                        translateEnabled = true
                                    }

                                }
                            )
                    }
                }
            }
        }

        var languageDestination = languageDestination(otherLanguageApp)

        val translator = Translator(NetworkClientFactory.getKtorClient())

        var copyToClipboard by remember {
            mutableStateOf(false)
        }

        LaunchedEffect(copyToClipboard, text) {
            if (copyToClipboard && !text.isNullOrBlank()) {
                textCopyToClipboard(text!!, context)
                copyToClipboard = false
            }
        }

        var fontSize by rememberPreference(lyricsFontSizeKey, LyricsFontSize.Medium)
        val showBackgroundLyrics by rememberPreference(showBackgroundLyricsKey, false)
        val showLyricsSourceSwitcher by rememberPreference(showLyricsSourceSwitcherKey, true)
        val playerEnableLyricsPopupMessage by rememberPreference(
            playerEnableLyricsPopupMessageKey,
            true
        )
        var defaultLyricsSource by rememberPreference(defaultLyricsSourceKey, "lrclib")
        var simpMusicTranslationEnabled by rememberPreference(simpMusicTranslationEnabledKey, false)
        var selectedLyricsSource by rememberSaveable(mediaId) { mutableStateOf(defaultLyricsSource) }
        var showSimpMusicOptions by rememberSaveable(mediaId) { mutableStateOf(false) }
        var showShareCardPreview by rememberSaveable(mediaId) { mutableStateOf(false) }
        var shareSliceStartLine by rememberSaveable(mediaId) { mutableStateOf(0f) }
        var expandedplayer by rememberPreference(expandedplayerKey, false)

        var checkedLyricsLrc by remember(mediaId) {
            mutableStateOf(false)
        }
        var checkedLyricsKugou by remember(mediaId) {
            mutableStateOf(false)
        }
        var checkedLyricsInnertube by remember(mediaId) {
            mutableStateOf(false)
        }
        var checkLyrics by remember(mediaId) {
            mutableStateOf(false)
        }
        var lyricsHighlight by rememberPreference(lyricsHighlightKey, LyricsHighlight.None)
        var lyricsAlignment by rememberPreference(lyricsAlignmentKey, LyricsAlignment.Center)
        var lyricsSizeAnimate by rememberPreference(lyricsSizeAnimateKey, true)
        var lyricsKaraokeEnabled by rememberPreference(lyricsKaraokeEnabledKey, false)
        val mediaMetadata = mediaMetadataProvider()
        var artistName by rememberSaveable(mediaId) { mutableStateOf(cleanPrefix(mediaMetadata.artist?.toString().orEmpty()))}
        var title by rememberSaveable(mediaId) { mutableStateOf(cleanPrefix(mediaMetadata.title?.toString().orEmpty()))}
        val sharePreviewLines = remember(text) { buildLyricsShareLines(text.orEmpty()) }
        val shareMaxStartIndex = (sharePreviewLines.size - 1).coerceAtLeast(0)
        val shareSliceStartIndex = shareSliceStartLine.toInt().coerceIn(0, shareMaxStartIndex)
        val sharePreviewLyrics = remember(text, shareSliceStartIndex) {
            buildLyricsShareSlice(
                lines = sharePreviewLines,
                startIndex = shareSliceStartIndex
            )
        }
        val shareDeeplink = remember(mediaId, mediaMetadata.title, mediaMetadata.artist) {
            buildLyricsShareLink(
                mediaId = mediaId,
                title = cleanPrefix(mediaMetadata.title?.toString().orEmpty()),
                artist = cleanPrefix(mediaMetadata.artist?.toString().orEmpty())
            )
        }
        var lyricsSize by rememberPreference(lyricsSizeKey, 20f)
        var lyricsSizeL by rememberPreference(lyricsSizeLKey, 20f)
        var customSize = if (isLandscape) lyricsSizeL else lyricsSize
        var showLyricsSizeDialog by rememberSaveable {
            mutableStateOf(false)
        }
        val lightTheme = colorPaletteMode == ColorPaletteMode.Light || (colorPaletteMode == ColorPaletteMode.System && (!isSystemInDarkTheme()))
        val effectRotationEnabled by rememberPreference(effectRotationKey, true)
        var landscapeControls by rememberPreference(landscapeControlsKey, true)
        var jumpPrevious by rememberPreference(jumpPreviousKey,"3")
        var isRotated by rememberSaveable { mutableStateOf(false) }
        val rotationAngle by animateFloatAsState(
            targetValue = if (isRotated) 360F else 0f,
            animationSpec = tween(durationMillis = 200), label = ""
        )
        val colorPaletteName by rememberPreference(colorPaletteNameKey, ColorPaletteName.Dynamic)

        if (showLyricsSizeDialog) {
            LyricsSizeDialog(
                onDismiss = { showLyricsSizeDialog = false},
                sizeValue = { lyricsSize = it },
                sizeValueL = { lyricsSizeL = it}
            )
        }

        LaunchedEffect(mediaMetadata.title, mediaMetadata.artist) {
            artistName = mediaMetadata.artist?.toString().orEmpty()
            title = cleanPrefix(mediaMetadata.title?.toString().orEmpty())
        }

        LaunchedEffect(mediaId, mediaMetadata.title, mediaMetadata.artist) {
            lyrics = null
            checkedLyricsLrc = false
            checkedLyricsKugou = false
            checkedLyricsInnertube = false
            checkLyrics = false
            invalidLrc = false
            isError = false
            isErrorSync = false
            selectedLyricsSource = defaultLyricsSource
            showSimpMusicOptions = false
            showShareCardPreview = false
            shareSliceStartLine = 0f
        }

        suspend fun fetchLyricsFromSource(source: String, allowSimpFallback: Boolean = true) {
            selectedLyricsSource = source
            isError = false
            showPlaceholder = true

            val existingLyrics = withContext(Dispatchers.IO) {
                Database.lyricsTable.findBySongId(mediaId).first()
            }
            val metadataArtist = mediaMetadata.artist?.toString().orEmpty()
            val metadataTitle = cleanPrefix(mediaMetadata.title?.toString().orEmpty())

            suspend fun awaitPlaybackDurationMs(): Long {
                var duration = withContext(Dispatchers.Main) { durationProvider() }
                while (duration == C.TIME_UNSET) {
                    delay(100)
                    duration = withContext(Dispatchers.Main) { durationProvider() }
                }
                return duration
            }

            fun applyLyrics(updatedLyrics: Lyrics) {
                lyrics = updatedLyrics
                if (isShowingSynchronizedLyrics && updatedLyrics.synced.isNullOrBlank() && !updatedLyrics.fixed.isNullOrBlank()) {
                    isShowingSynchronizedLyrics = false
                }
                showPlaceholder = false
                isError = false
            }

            fun markFailure() {
                showPlaceholder = false
                isError = true
            }

            suspend fun persistLyricsSafely(updatedLyrics: Lyrics) {
                val existingSong = withContext(Dispatchers.IO) {
                    Database.songTable.findById(mediaId).first()
                }
                Database.asyncTransaction {
                    if (existingSong == null) {
                        songTable.insertIgnore(
                            Song(
                                id = mediaId,
                                title = metadataTitle.ifBlank { mediaId },
                                artistsText = metadataArtist.ifBlank { null },
                                durationText = null,
                                thumbnailUrl = mediaMetadata.artworkUri?.toString()
                            )
                        )
                    }
                    lyricsTable.upsert(updatedLyrics)
                }
            }

            suspend fun fallbackFromSimpMusic() {
                val firstFallback = defaultLyricsSource.takeIf { it != "simpmusic" }.orEmpty().ifBlank { "lrclib" }
                fetchLyricsFromSource(firstFallback, allowSimpFallback = false)
                if ((lyrics?.synced.isNullOrBlank() && lyrics?.fixed.isNullOrBlank()) || isError) {
                    fetchLyricsFromSource("kugou", allowSimpFallback = false)
                }
            }

            when (source) {
                "lrclib" -> {
                    val duration = awaitPlaybackDurationMs()

                    val result = LrcLib.lyrics(
                        artist = artistName,
                        title = title,
                        duration = duration.milliseconds,
                        album = mediaMetadata.albumTitle?.toString()
                    )?.getOrNull()

                    if (result?.text?.isNotBlank() == true) {
                        val updatedLyrics = Lyrics(
                            songId = mediaId,
                            fixed = existingLyrics?.fixed ?: plainLyricsFromTimedText(result.text),
                            synced = result.text
                        )
                        applyLyrics(updatedLyrics)
                        persistLyricsSafely(updatedLyrics)
                        checkedLyricsLrc = true
                    } else {
                        checkedLyricsLrc = true
                        markFailure()
                    }
                }

                "kugou" -> {
                    val duration = awaitPlaybackDurationMs()

                    val result = KuGou.lyrics(
                        artist = metadataArtist,
                        title = metadataTitle,
                        duration = duration / 1000
                    )?.getOrNull()

                    if (result?.value?.isNotBlank() == true) {
                        val updatedLyrics = Lyrics(
                            songId = mediaId,
                            fixed = existingLyrics?.fixed,
                            synced = result.value
                        )
                        applyLyrics(updatedLyrics)
                        persistLyricsSafely(updatedLyrics)
                        checkedLyricsKugou = true
                    } else {
                        checkedLyricsKugou = true
                        markFailure()
                    }
                }

                "simpmusic" -> {
                    val simpLanguageCode = otherLanguageApp.code
                        .substringBefore('-')
                        .substringBefore('_')
                        .ifBlank { "en" }
                        .take(2)

                    val simpLyrics = fetchSimpMusicLyrics(
                        videoId = mediaId,
                        translatedLanguage = simpLanguageCode,
                        useTranslatedLyrics = simpMusicTranslationEnabled
                    )

                    val resolvedSyncedLyrics = when {
                        simpMusicTranslationEnabled && !simpLyrics?.translatedLyrics.isNullOrBlank() -> simpLyrics?.translatedLyrics
                        !simpLyrics?.syncedLyrics.isNullOrBlank() -> simpLyrics?.syncedLyrics
                        else -> existingLyrics?.synced
                    }
                    val shouldFallbackToSyncedSources =
                        isShowingSynchronizedLyrics &&
                            resolvedSyncedLyrics.isNullOrBlank() &&
                            allowSimpFallback

                    if (shouldFallbackToSyncedSources) {
                        fallbackFromSimpMusic()
                    } else if (!resolvedSyncedLyrics.isNullOrBlank() || !simpLyrics?.plainLyrics.isNullOrBlank()) {
                        val updatedLyrics = Lyrics(
                            songId = mediaId,
                            fixed = simpLyrics?.plainLyrics ?: existingLyrics?.fixed,
                            synced = resolvedSyncedLyrics
                        )
                        applyLyrics(updatedLyrics)
                        persistLyricsSafely(updatedLyrics)
                    } else {
                        if (allowSimpFallback) {
                            fallbackFromSimpMusic()
                        } else {
                            markFailure()
                        }
                    }
                }
            }
        }

        fun translateLyricsWithRomanization(output: MutableState<String>, textToTranslate: String, isSync: Boolean, destinationLanguage: Language = Language.AUTO) = @Composable{
            LaunchedEffect(showSecondLine, romanization, textToTranslate, destinationLanguage){
                var destLanguage = destinationLanguage
                val result = withContext(Dispatchers.IO) {
                    try {

                        /** used to find the source language of the text and detect CHINESE_TRADITIONAL*/
                        val helperTranslation = translator.translate(
                            textToTranslate,
                            Language.CHINESE_TRADITIONAL,
                            Language.AUTO
                        )
                        if(destinationLanguage == Language.AUTO){
                            destLanguage = if(helperTranslation.translatedText == textToTranslate){
                                Language.CHINESE_TRADITIONAL
                            } else {
                                helperTranslation.sourceLanguage
                            }
                        }
                        val mainTranslation = translator.translate(
                            textToTranslate,
                            destLanguage,
                            Language.AUTO
                        )
                        val outputText = if (textToTranslate == "") {
                            ""
                       }
                        else if (!showSecondLine || (mainTranslation.sourceText == mainTranslation.translatedText)){
                            if (romanization == Romanization.Off) {
                                if (translateEnabled) mainTranslation.translatedText else textToTranslate
                            }
                            else if (romanization == Romanization.Original) if (helperTranslation.sourceText == helperTranslation.translatedText) helperTranslation.sourcePronunciation else mainTranslation.sourcePronunciation ?: mainTranslation.sourceText
                            else if (romanization == Romanization.Translated) mainTranslation.translatedPronunciation ?: mainTranslation.translatedText
                            else if (helperTranslation.sourceText == helperTranslation.translatedText) helperTranslation.sourcePronunciation else mainTranslation.sourcePronunciation ?: mainTranslation.sourceText
                        } else {
                            if (romanization == Romanization.Off) {
                                textToTranslate + "\\n[${mainTranslation.translatedText}]"
                            } else if (romanization == Romanization.Original) {
                                if (helperTranslation.sourceText == helperTranslation.translatedText){
                                    helperTranslation.sourcePronunciation
                                } else {mainTranslation.sourcePronunciation ?: mainTranslation.sourceText} + "\\n[${mainTranslation.translatedText}]"
                            } else if (romanization == Romanization.Translated) {
                                textToTranslate + "\\n[${mainTranslation.translatedPronunciation ?: mainTranslation.translatedText}]"
                            } else
                                if (helperTranslation.sourceText == helperTranslation.translatedText){
                                    helperTranslation.sourcePronunciation
                                } else {mainTranslation.sourcePronunciation ?: mainTranslation.sourceText} + "\\n[${mainTranslation.translatedPronunciation ?: mainTranslation.translatedText}]"
                        }
                        outputText?.replace("\\r","\r")?.replace("\\n","\n")
                    } catch (e: Exception) {
                        if(isSync){
                            Timber.e("Lyrics sync translation ${e.stackTraceToString()}")
                        } else {
                            Timber.e("Lyrics not sync translation ${e.stackTraceToString()}")
                        }
                    }
                }
                val translatedText =
                    if (result.toString() == "kotlin.Unit") "" else result.toString()
                showPlaceholder = false
                output.value = translatedText
            }
        }


        LaunchedEffect(mediaId, mediaMetadata.title, mediaMetadata.artist, isShowingSynchronizedLyrics, checkLyrics) {
            Database.lyricsTable
                    .findBySongId( mediaId )
                    .collect { currentLyrics ->
                        if (!showLyricsSourceSwitcher) {
                            if (isShowingSynchronizedLyrics && currentLyrics?.synced.isNullOrBlank()) {
                                fetchLyricsFromSource("lrclib", allowSimpFallback = false)
                                return@collect
                            }

                            if (!isShowingSynchronizedLyrics && currentLyrics?.fixed.isNullOrBlank()) {
                                var duration = withContext(Dispatchers.Main) { durationProvider() }
                                while (duration == C.TIME_UNSET) {
                                    delay(100)
                                    duration = withContext(Dispatchers.Main) { durationProvider() }
                                }
                                val lrcLibResult = runCatching {
                                    LrcLib.lyrics(
                                        artist = artistName ?: "",
                                        title = title ?: "",
                                        duration = duration.milliseconds,
                                        album = mediaMetadata.albumTitle?.toString()
                                    )?.getOrNull()
                                }.getOrNull()

                                val lrcLibPlainLyrics = lrcLibResult?.text
                                    ?.lineSequence()
                                    ?.map { line -> line.replace(Regex("""\[[^\]]*]"""), "").trim() }
                                    ?.filter { it.isNotBlank() }
                                    ?.joinToString("\n")
                                    ?.ifBlank { null }

                                if (!lrcLibPlainLyrics.isNullOrBlank()) {
                                    val updatedLyrics = Lyrics(
                                        songId = mediaId,
                                        fixed = lrcLibPlainLyrics,
                                        synced = currentLyrics?.synced ?: lrcLibResult.text
                                    )
                                    lyrics = updatedLyrics
                                    Database.asyncTransaction {
                                        lyricsTable.upsert(updatedLyrics)
                                    }
                                    isError = false
                                } else {
                                    kotlin.runCatching {
                                        Innertube.lyrics(NextBody(videoId = mediaId))
                                            ?.onSuccess { fixedLyrics ->
                                                val updatedLyrics = Lyrics(
                                                    songId = mediaId,
                                                    fixed = fixedLyrics ?: "",
                                                    synced = currentLyrics?.synced
                                                )
                                                lyrics = updatedLyrics
                                                Database.asyncTransaction {
                                                    lyricsTable.upsert(updatedLyrics)
                                                }
                                                isError = false
                                            }?.onFailure {
                                                isError = true
                                            }
                                    }.onFailure {
                                        Timber.e("Lyrics hidden-source fixed fallback error ${it.stackTraceToString()}")
                                        isError = true
                                    }
                                }
                                checkedLyricsInnertube = true
                                return@collect
                            }

                            lyrics = currentLyrics
                            return@collect
                        }

                        if (isShowingSynchronizedLyrics && currentLyrics?.synced == null) {
                            lyrics = null
                            var duration = withContext(Dispatchers.Main) {
                                durationProvider()
                            }

                            while (duration == C.TIME_UNSET) {
                                delay(100)
                                duration = withContext(Dispatchers.Main) {
                                    durationProvider()
                                }
                            }

                            kotlin.runCatching {
                                LrcLib.lyrics(
                                    artist = artistName ?: "",
                                    title = title ?: "",
                                    duration = duration.milliseconds,
                                    album = mediaMetadata.albumTitle?.toString()
                                )?.onSuccess {
                                    if ((it?.text?.isNotEmpty() == true || it?.sentences?.isNotEmpty() == true)
                                        && playerEnableLyricsPopupMessage
                                    )
                                        coroutineScope.launch {
                                            Toaster.s(
                                                R.string.info_lyrics_found_on_s,
                                                "LrcLib.net"
                                            )
                                        }
                                    else
                                        if (playerEnableLyricsPopupMessage)
                                            coroutineScope.launch {

                                                Toaster.e(
                                                    R.string.info_lyrics_not_found_on_s,
                                                    "LrcLib.net",
                                                    duration = Toast.LENGTH_LONG
                                                )
                                            }

                                    isError = false
                                    checkedLyricsLrc = true

                                    Database.asyncTransaction {
                                        lyricsTable.upsert(
                                            Lyrics(
                                                songId = mediaId,
                                                fixed = currentLyrics?.fixed ?: plainLyricsFromTimedText(it?.text),
                                                synced = it?.text.orEmpty()
                                            )
                                        )
                                    }
                                }?.onFailure {
                                    if (playerEnableLyricsPopupMessage)
                                        coroutineScope.launch {
                                            Toaster.e(
                                                R.string.info_lyrics_not_found_on_s_try_on_s,
                                                "LrcLib.net", "KuGou.com",
                                                duration = Toast.LENGTH_LONG
                                            )
                                        }

                                    checkedLyricsLrc = true

                                    kotlin.runCatching {
                                        KuGou.lyrics(
                                            artist = mediaMetadata.artist?.toString() ?: "",
                                            title = cleanPrefix(mediaMetadata.title?.toString() ?: ""),
                                            duration = duration / 1000
                                        )?.onSuccess {
                                            if ((it?.value?.isNotEmpty() == true || it?.sentences?.isNotEmpty() == true)
                                                && playerEnableLyricsPopupMessage
                                            )
                                                coroutineScope.launch {
                                                    Toaster.s(
                                                        R.string.info_lyrics_found_on_s,
                                                        "KuGou.com"
                                                    )
                                                }
                                            else
                                                if (playerEnableLyricsPopupMessage)
                                                    coroutineScope.launch {
                                                        Toaster.e(
                                                            R.string.info_lyrics_not_found_on_s,
                                                            "KuGou.com",
                                                            duration = Toast.LENGTH_LONG
                                                        )
                                                    }

                                            isError = false
                                            checkedLyricsKugou = true
                                            Database.asyncTransaction {
                                                lyricsTable.upsert(
                                                    Lyrics(
                                                        songId = mediaId,
                                                        fixed = currentLyrics?.fixed,
                                                        synced = it?.value.orEmpty()
                                                    )
                                                )
                                            }
                                        }?.onFailure {
                                            checkedLyricsKugou = true
                                            val simpLyrics = fetchSimpMusicLyrics(mediaId)
                                            if (!simpLyrics?.syncedLyrics.isNullOrBlank() || !simpLyrics?.plainLyrics.isNullOrBlank()) {
                                                if (playerEnableLyricsPopupMessage) {
                                                    coroutineScope.launch {
                                                        Toaster.s(
                                                            R.string.info_lyrics_found_on_s,
                                                            "SimpMusic"
                                                        )
                                                    }
                                                }

                                                Database.asyncTransaction {
                                                    lyricsTable.upsert(
                                                        Lyrics(
                                                            songId = mediaId,
                                                            fixed = simpLyrics?.plainLyrics ?: currentLyrics?.fixed,
                                                            synced = simpLyrics?.syncedLyrics ?: currentLyrics?.synced
                                                        )
                                                    )
                                                }
                                                isError = false
                                            } else {
                                                if (playerEnableLyricsPopupMessage)
                                                    coroutineScope.launch {
                                                        Toaster.e(
                                                            R.string.info_lyrics_not_found_on_s_try_on_s,
                                                            "KuGou.com",
                                                            "SimpMusic",
                                                            duration = Toast.LENGTH_LONG
                                                        )
                                                    }

                                                isError = true
                                            }
                                        }
                                    }.onFailure {
                                        Timber.e("Lyrics Kugou get error ${it.stackTraceToString()}")
                                    }
                                }
                            }.onFailure {
                                Timber.e("Lyrics get error ${it.stackTraceToString()}")
                            }

                        } else if (!isShowingSynchronizedLyrics && currentLyrics?.fixed == null) {
                            isError = false
                            lyrics = null
                            kotlin.runCatching {
                                Innertube.lyrics(NextBody(videoId = mediaId))
                                    ?.onSuccess { fixedLyrics ->
                                        Database.asyncTransaction {
                                            lyricsTable.upsert(
                                                Lyrics(
                                                    songId = mediaId,
                                                    fixed = fixedLyrics ?: "",
                                                    synced = currentLyrics?.synced
                                                )
                                            )
                                        }
                                    }?.onFailure {
                                    isError = true
                                }
                            }.onFailure {
                                Timber.e("Lyrics Innertube get error ${it.stackTraceToString()}")
                            }
                            checkedLyricsInnertube = true
                        } else {
                            lyrics = currentLyrics
                        }
                    }
        }


        if (isEditing) {
            InputTextDialog(
                onDismiss = { isEditing = false },
                setValueRequireNotNull = false,
                title = stringResource(R.string.enter_the_lyrics),
                value = text ?: "",
                placeholder = stringResource(R.string.enter_the_lyrics),
                setValue = {
                    Database.asyncTransaction {
                        ensureSongInserted()
                        lyricsTable.upsert(
                            Lyrics(
                                songId = mediaId,
                                fixed = if (isShowingSynchronizedLyrics) lyrics?.fixed else it,
                                synced = if (isShowingSynchronizedLyrics) it else lyrics?.synced,
                            )
                        )
                    }

                }
            )
        }

        if (showShareCardPreview) {
            val configuration = LocalConfiguration.current
            val previewMaxHeight = (configuration.screenHeightDp.dp * if (isLandscape) 0.80f else 0.86f)
            val previewCardScale = if (isLandscape) 0.9f else 0.96f
            DefaultDialog(
                onDismiss = { showShareCardPreview = false },
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.86f else 0.95f)
                    .heightIn(max = previewMaxHeight)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                BasicText(
                    text = "Share lyrics card",
                    style = typography().m.semiBold.color(colorPalette().text)
                )
                BasicText(
                    text = "Preview the card, tap the lyric line you want to start from, then send that exact slice.",
                    style = typography().xs.color(colorPalette().textSecondary),
                    modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LyricsSharePreviewCard(
                        title = cleanPrefix(mediaMetadata.title?.toString().orEmpty()),
                        artist = cleanPrefix(mediaMetadata.artist?.toString().orEmpty()),
                        lyricsSnippet = sharePreviewLyrics,
                        artworkUrl = mediaMetadata.artworkUri?.toString(),
                        deeplinkUrl = shareDeeplink,
                        modifier = Modifier
                            .fillMaxWidth(if (isLandscape) 0.92f else 0.97f)
                            .heightIn(max = if (isLandscape) 286.dp else 356.dp)
                            .graphicsLayer {
                                scaleX = previewCardScale
                                scaleY = previewCardScale
                                transformOrigin = TransformOrigin(0.5f, 0f)
                            }
                    )
                }

                BasicText(
                    text = "Start from line ${shareSliceStartIndex + 1}",
                    style = typography().xs.semiBold.color(colorPalette().text),
                    modifier = Modifier.padding(top = 16.dp)
                )

                Slider(
                    value = shareSliceStartLine.coerceIn(0f, shareMaxStartIndex.toFloat()),
                    onValueChange = { shareSliceStartLine = it },
                    valueRange = 0f..shareMaxStartIndex.toFloat().coerceAtLeast(0f)
                )

                BasicText(
                    text = "Selected ${sharePreviewLyrics.lines().size.coerceAtMost(6)} preview lines",
                    style = typography().xxs.color(colorPalette().textSecondary),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colorPalette().background2.copy(alpha = 0.55f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    itemsIndexed(sharePreviewLines) { index, line ->
                        val isSelected = index == shareSliceStartIndex
                        BasicText(
                            text = line,
                            style = typography().xs.color(
                                if (isSelected) colorPalette().text else colorPalette().textSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) colorPalette().accent.copy(alpha = 0.20f)
                                    else Color.Transparent
                                )
                                .clickable { shareSliceStartLine = index.toFloat() }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colorPalette().background2)
                            .clickable { showShareCardPreview = false }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = stringResource(R.string.cancel),
                            style = typography().xs.semiBold.color(colorPalette().text)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colorPalette().accent)
                            .clickable {
                                coroutineScope.launch {
                                    shareLyricsCard(
                                        context = context,
                                        mediaId = mediaId,
                                        title = cleanPrefix(mediaMetadata.title?.toString().orEmpty()),
                                        artist = cleanPrefix(mediaMetadata.artist?.toString().orEmpty()),
                                        lyricsText = sharePreviewLyrics,
                                        artworkUrl = mediaMetadata.artworkUri?.toString(),
                                        deeplinkUrl = shareDeeplink
                                    ).onSuccess {
                                        showShareCardPreview = false
                                    }.onFailure {
                                        Toaster.e("Failed to share lyrics card")
                                    }
                                }
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = "Share now",
                            style = typography().xs.semiBold.color(colorPalette().onAccent),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }

@Composable
fun SelectLyricFromTrack(
    tracks: List<Track>,
    mediaId: String,
    lyrics: Lyrics?
) {
    menuState.display {
        Menu {
            MenuEntry(
                icon = R.drawable.chevron_back,
                text = stringResource(R.string.cancel),
                onClick = { menuState.hide() }
            )
            MenuEntry(
                icon = R.drawable.text,
                text = stringResource(R.string.fetch_lyrics_from_simpmusic),
                onClick = {
                    menuState.hide()
                    coroutineScope.launch {
                        val simpLyrics = fetchSimpMusicLyrics(mediaId)
                        if (!simpLyrics?.syncedLyrics.isNullOrBlank() || !simpLyrics?.plainLyrics.isNullOrBlank()) {
                            Database.asyncTransaction {
                                lyricsTable.upsert(
                                    Lyrics(
                                        songId = mediaId,
                                        fixed = simpLyrics?.plainLyrics ?: lyrics?.fixed,
                                        synced = simpLyrics?.syncedLyrics ?: lyrics?.synced
                                    )
                                )
                            }
                            if (playerEnableLyricsPopupMessage) {
                                Toaster.s(
                                    R.string.info_lyrics_found_on_s,
                                    context.getString(R.string.lyrics_source_simpmusic)
                                )
                            }
                        } else if (playerEnableLyricsPopupMessage) {
                            Toaster.e(
                                R.string.info_lyrics_not_found_on_s,
                                context.getString(R.string.lyrics_source_simpmusic),
                                duration = Toast.LENGTH_LONG
                            )
                        }
                    }
                }
            )
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ){
                TextField(
                    value = title,
                    onValueChange = {
                        title = it
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        unfocusedTextColor = Color(0xFF008080), // Teal
                        focusedTextColor = Color(0xFF008080), // Teal
                        unfocusedIndicatorColor = colorPalette().text,
                        focusedIndicatorColor = colorPalette().text,
                        unfocusedContainerColor = colorPalette().background2,
                        focusedContainerColor = colorPalette().background2,
                        cursorColor = Color(0xFF008080)
                    ),
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .weight(1f)
                )
                TextField(
                    value = artistName,
                    onValueChange = {
                        artistName = it
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        unfocusedTextColor = Color(0xFF008080), // Teal
                        focusedTextColor = Color(0xFF008080), // Teal
                        unfocusedIndicatorColor = colorPalette().text,
                        focusedIndicatorColor = colorPalette().text,
                        unfocusedContainerColor = colorPalette().background2,
                        focusedContainerColor = colorPalette().background2,
                        cursorColor = Color(0xFF008080)
                    ),
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .weight(1f)
                )
                IconButton(
                    icon = R.drawable.search,
                    color = Color.White,
                    onClick = {
                        isPicking = false
                        menuState.hide()
                        isPicking = true
                    },
                    modifier = Modifier
                        .background(shape = RoundedCornerShape(4.dp), color = Color(0xFF008080))
                        .padding(all = 4.dp)
                        .size(28.dp)
                        .align(Alignment.CenterVertically)
                )
            }
            tracks.forEach {
                MenuEntry(
                    icon = R.drawable.text,
                    text = "${it.artistName} - ${it.trackName}",
                    secondaryText = "(${stringResource(R.string.sort_duration)} ${
                        it.duration.seconds.toComponents { minutes, seconds, _ ->
                            "$minutes:${seconds.toString().padStart(2, '0')}"
                        }
                    } ${stringResource(R.string.id)} ${it.id}) ",
                    onClick = {
                        menuState.hide()
                        val syncedLyrics = it.syncedLyrics?.takeIf(String::isNotBlank)
                        val fixedLyrics = it.plainLyrics?.takeIf(String::isNotBlank)
                            ?: plainLyricsFromTimedText(syncedLyrics)
                        Database.asyncTransaction {
                            lyricsTable.upsert(
                                Lyrics(
                                    songId = mediaId,
                                    fixed = fixedLyrics ?: lyrics?.fixed,
                                    synced = syncedLyrics
                                )
                            )
                        }
                    }
                )
            }
            MenuEntry(
                icon = R.drawable.chevron_back,
                text = stringResource(R.string.cancel),
                onClick = { menuState.hide() }
            )
        }
    }
    isPicking = false
}

     if (isPicking && isShowingSynchronizedLyrics) {
    var loading by remember { mutableStateOf(true) }
    val tracks = remember { mutableStateListOf<Track>() }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlin.runCatching {
            LrcLib.lyrics(
                artist = artistName,
                title = title
            )?.onSuccess {
                if (it.isNotEmpty() && playerEnableLyricsPopupMessage)
                    coroutineScope.launch {
                        Toaster.e(
                            R.string.info_lyrics_tracks_found_on_s,
                            "LrcLib.net",
                            duration = Toast.LENGTH_LONG
                        )
                    }
                else
                    if (playerEnableLyricsPopupMessage)
                        coroutineScope.launch {
                            Toaster.e(
                                R.string.info_lyrics_not_found_on_s,
                                "LrcLib.net",
                                duration = Toast.LENGTH_LONG
                            )
                        }
                if (it.isEmpty()){
                    menuState.display {
                        Menu {
                            MenuEntry(
                                icon = R.drawable.chevron_back,
                                text = stringResource(R.string.cancel),
                                onClick = { menuState.hide() }
                            )
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = title,
                                    onValueChange = { it ->
                                        title = it
                                    },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        unfocusedTextColor = Color(0xFF008080), // Teal
                                        focusedTextColor = Color(0xFF008080), // Teal
                                        unfocusedIndicatorColor = colorPalette().text,
                                        focusedIndicatorColor = colorPalette().text,
                                        unfocusedContainerColor = colorPalette().background2,
                                        focusedContainerColor = colorPalette().background2,
                                        cursorColor = Color(0xFF008080)
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .weight(1f)
                                )
                                TextField(
                                    value = artistName,
                                    onValueChange = { it ->
                                        artistName = it
                                    },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        unfocusedTextColor = Color(0xFF008080), // Teal
                                        focusedTextColor = Color(0xFF008080), // Teal
                                        unfocusedIndicatorColor = colorPalette().text,
                                        focusedIndicatorColor = colorPalette().text,
                                        unfocusedContainerColor = colorPalette().background2,
                                        focusedContainerColor = colorPalette().background2,
                                        cursorColor = Color(0xFF008080)
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .weight(1f)
                                )
                                IconButton(
                                    icon = R.drawable.search,
                                    color = Color.White,
                                    onClick = {
                                        isPicking = false
                                        menuState.hide()
                                        isPicking = true
                                    },
                                    modifier = Modifier
                                        .background(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF008080)
                                        )
                                        .padding(all = 4.dp)
                                        .size(28.dp)
                                        .align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                    isPicking = false
                }

                        tracks.clear()
                        val durationMs = durationProvider().takeIf { value -> value != C.TIME_UNSET } ?: 0L
                        val bestTrack = pickBestLrcLibTrack(it, title, durationMs)
                        bestTrack?.let { match -> tracks.add(match) }
                        tracks.addAll(it.filterNot { track -> track.id == bestTrack?.id })
                        loading = false
                        error = false
                    }?.onFailure {
                        if (playerEnableLyricsPopupMessage)
                            coroutineScope.launch {
                                Toaster.e(
                                    R.string.an_error_has_occurred_while_fetching_the_lyrics,
                                    "KuGou.com",
                                    duration = Toast.LENGTH_LONG
                                )
                            }

                        loading = false
                        error = true
                    } ?: run { loading = false }
                }.onFailure {
                    Timber.e("Lyrics get error 1 ${it.stackTraceToString()}")
                }
            }

            if (loading)
                DefaultDialog(
                    onDismiss = {
                        isPicking = false
                    }
                ) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

            if (tracks.isNotEmpty()) {
                SelectLyricFromTrack(tracks = tracks,mediaId = mediaId,lyrics = lyrics)
            }
        }




        if (isShowingSynchronizedLyrics) {
            DisposableEffect(Unit) {
                currentView.keepScreenOn = true
                onDispose {
                    currentView.keepScreenOn = false
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onDismiss() }
                    )
                }
                .fillMaxSize()
                .background(if (!showlyricsthumbnail) Color.Transparent else Color.Black.copy(0.8f))
                .clip(thumbnailShape())

        ) {
            AnimatedVisibility(
                visible = (isError && text == null) || (invalidLrc && isShowingSynchronizedLyrics),
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
            ) {
                BasicText(
                    text = stringResource(R.string.an_error_has_occurred_while_fetching_the_lyrics),
                    style = typography().xs.center.medium.color(PureBlackColorPalette.text),
                    modifier = Modifier
                        .background(
                            if (!showlyricsthumbnail) Color.Transparent else Color.Black.copy(
                                0.4f
                            )
                        )
                        .padding(all = 8.dp)
                        .fillMaxWidth()
                )
            }

            if (showLyricsSourceSwitcher) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .zIndex(10f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 14.dp, end = 14.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.28f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        if (isShowingSynchronizedLyrics) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colorPalette().background2.copy(alpha = 0.9f))
                                    .clickable {
                                        showSimpMusicOptions = false
                                        isPicking = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.search),
                                    contentDescription = "Search LrcLib",
                                    colorFilter = ColorFilter.tint(colorPalette().accent),
                                    modifier = Modifier.size(12.dp)
                                )
                                BasicText(
                                    text = "Lrc",
                                    style = typography().xxs.semiBold.copy(color = colorPalette().accent)
                                )
                            }
                        }

                        listOf(
                            "lrclib" to "Lrc",
                            "kugou" to "Kg",
                            "simpmusic" to "Simp"
                        ).forEach { (sourceKey, label) ->
                            BasicText(
                                text = label,
                                style = typography().xxs.semiBold.copy(
                                    color = if (selectedLyricsSource == sourceKey) colorPalette().accent else Color.White.copy(alpha = 0.82f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selectedLyricsSource == sourceKey) colorPalette().background2.copy(alpha = 0.9f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        if (sourceKey == "simpmusic") {
                                            selectedLyricsSource = sourceKey
                                            showSimpMusicOptions = !showSimpMusicOptions
                                        } else {
                                            showSimpMusicOptions = false
                                            selectedLyricsSource = sourceKey
                                            coroutineScope.launch {
                                                fetchLyricsFromSource(sourceKey)
                                            }
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (showSimpMusicOptions) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .padding(top = 8.dp, end = 14.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.38f))
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                BasicText(
                                    text = if (simpMusicTranslationEnabled) "Translated" else "Original",
                                    style = typography().xxs.semiBold.copy(color = Color.White),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorPalette().background2.copy(alpha = 0.9f))
                                        .clickable { simpMusicTranslationEnabled = !simpMusicTranslationEnabled }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                                BasicText(
                                    text = languageDestinationName(otherLanguageApp),
                                    style = typography().xxs.semiBold.copy(color = Color.White),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorPalette().background2.copy(alpha = 0.9f))
                                        .clickable {
                                            changingSimpMusicLanguage = true
                                            showLanguagesList = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                BasicText(
                                    text = if (defaultLyricsSource == "simpmusic") "Default: Simp" else "Make Default",
                                    style = typography().xxs.semiBold.copy(
                                        color = if (defaultLyricsSource == "simpmusic") colorPalette().accent else Color.White
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorPalette().background2.copy(alpha = 0.9f))
                                        .clickable {
                                            defaultLyricsSource =
                                                if (defaultLyricsSource == "simpmusic") "lrclib" else "simpmusic"
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                                BasicText(
                                    text = "Fetch",
                                    style = typography().xxs.semiBold.copy(color = colorPalette().accent),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorPalette().background2.copy(alpha = 0.9f))
                                        .clickable {
                                            coroutineScope.launch {
                                                fetchLyricsFromSource("simpmusic")
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    BasicText(
                        text = "Share Card",
                        style = typography().xxs.semiBold.copy(
                            color = if (text.isNullOrBlank()) Color.White.copy(alpha = 0.45f) else colorPalette().accent
                        ),
                        modifier = Modifier
                            .padding(top = 8.dp, end = 14.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.32f))
                            .clickable(enabled = !text.isNullOrBlank()) {
                                shareSliceStartLine = 0f
                                showShareCardPreview = true
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (text?.isEmpty() == true && !checkedLyricsLrc && !checkedLyricsKugou && !checkedLyricsInnertube)
                checkLyrics = !checkLyrics

            if (text?.isNotEmpty() == true) {
                if (isShowingSynchronizedLyrics) {
                    val density = LocalDensity.current
                    val player = LocalPlayerServiceBinder.current?.player
                        ?: return@AnimatedVisibility

                    val synchronizedLyrics = remember(text) {
                        val sentences = LrcLib.Lyrics(text).sentences
                        invalidLrc = sentences.size <= 1
                        SynchronizedLyrics(sentences) {
                            player.currentPosition + 50L
                        }
                    }

                    val lazyListState = rememberLazyListState()

                    LaunchedEffect(mediaId, text) {
                        lazyListState.scrollToItem(0)
                    }

                    LaunchedEffect(synchronizedLyrics, density) {
                        //val centerOffset = with(density) { (-thumbnailSize / 3).roundToPx() }
                        val centerOffset = with(density) {
                            (-thumbnailSize.div(if (!showlyricsthumbnail && !isLandscape) if (trailingContent == null) 2 else 1
                                                else if (trailingContent == null) 3 else 2))
                                .roundToPx()
                        }

                        lazyListState.animateScrollToItem(
                            index = synchronizedLyrics.index + 1,
                            scrollOffset = centerOffset
                        )

                        while (isActive) {
                            delay(50)
                            if (!synchronizedLyrics.update()) continue

                            lazyListState.animateScrollToItem(
                                index = synchronizedLyrics.index + 1,
                                scrollOffset = centerOffset
                            )
                        }
                    }

                    var modifierBG = Modifier.verticalFadingEdge()
                    if (showBackgroundLyrics && showlyricsthumbnail) modifierBG =
                        modifierBG.background(colorPalette().accent)

                    LazyColumn(
                        state = lazyListState,
                        userScrollEnabled = true,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = modifierBG
                            .background(
                                if (isDisplayed && !showlyricsthumbnail) if (lyricsBackground == LyricsBackground.Black) Color.Black.copy(0.6f)
                                else if (lyricsBackground == LyricsBackground.White) Color.White.copy(0.4f)
                                else Color.Transparent else Color.Transparent
                            )
                    ) {

                        item(key = "header", contentType = 0) {
                            Spacer(modifier = Modifier.height(thumbnailSize))
                        }
                        itemsIndexed(
                            items = synchronizedLyrics.sentences
                        ) { index, sentence ->
                            var translatedText by remember { mutableStateOf("") }
                            val trimmedSentence = sentence.second.trim()
                            if (showSecondLine || translateEnabled || romanization != Romanization.Off) {
                                val mutState = remember { mutableStateOf("") }
                                translateLyricsWithRomanization(mutState, trimmedSentence, true, languageDestination)()
                                translatedText = mutState.value
                            } else {
                                    translatedText = trimmedSentence
                            }

                            //Rainbow Shimmer
                            val infiniteTransition = rememberInfiniteTransition()

                            val offset by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        durationMillis = 10000,
                                        easing = LinearEasing
                                    ),
                                    repeatMode = RepeatMode.Reverse
                                ), label = ""
                            )

                            val RainbowColors = listOf(
                                Color.Red,
                                Color.Magenta,
                                Color.Blue,
                                Color.Cyan,
                                Color.Green,
                                Color.Yellow,
                                Color.Red
                            )
                            val RainbowColorsdark = listOf(
                                Color.Black.copy(0.35f).compositeOver(Color.Red),
                                Color.Black.copy(0.35f).compositeOver(Color.Magenta),
                                Color.Black.copy(0.35f).compositeOver(Color.Blue),
                                Color.Black.copy(0.35f).compositeOver(Color.Cyan),
                                Color.Black.copy(0.35f).compositeOver(Color.Green),
                                Color.Black.copy(0.35f).compositeOver(Color.Yellow),
                                Color.Black.copy(0.35f).compositeOver(Color.Red)
                            )
                            val RainbowColors2 = listOf(
                                Color.Red.copy(0.3f),
                                Color.Magenta.copy(0.3f),
                                Color.Blue.copy(0.3f),
                                Color.Cyan.copy(0.3f),
                                Color.Green.copy(0.3f),
                                Color.Yellow.copy(0.3f),
                                Color.Red.copy(0.3f)
                            )
                            val Themegradient =
                                listOf(colorPalette().background2, colorPalette().accent)
                            val Themegradient2 = listOf(
                                colorPalette().background2.copy(0.5f),
                                colorPalette().accent.copy(0.5f)
                            )
                            val oldlyrics =
                                listOf(PureBlackColorPalette.text, PureBlackColorPalette.text)
                            val oldlyrics2 = listOf(
                                PureBlackColorPalette.textDisabled,
                                PureBlackColorPalette.textDisabled
                            )

                            val brushrainbow = remember(offset) {
                                object : ShaderBrush() {
                                    override fun createShader(size: Size): Shader {
                                        val widthOffset = size.width * offset
                                        val heightOffset = size.height * offset
                                        return LinearGradientShader(
                                            colors = if (index == synchronizedLyrics.index)
                                                if (showlyricsthumbnail) oldlyrics else RainbowColors
                                            else if (showlyricsthumbnail) oldlyrics2 else RainbowColors2,
                                            from = Offset(widthOffset, heightOffset),
                                            to = Offset(
                                                widthOffset + size.width,
                                                heightOffset + size.height
                                            ),
                                            tileMode = TileMode.Mirror
                                        )
                                    }
                                }
                            }
                            val brushrainbowdark = remember(offset) {
                                object : ShaderBrush() {
                                    override fun createShader(size: Size): Shader {
                                        val widthOffset = size.width * offset
                                        val heightOffset = size.height * offset
                                        return LinearGradientShader(
                                            colors = if (index == synchronizedLyrics.index) RainbowColorsdark else RainbowColors2,
                                            from = Offset(widthOffset, heightOffset),
                                            to = Offset(
                                                widthOffset + size.width,
                                                heightOffset + size.height
                                            ),
                                            tileMode = TileMode.Mirror
                                        )
                                    }
                                }
                            }
                            val brushtheme = remember(offset) {
                                object : ShaderBrush() {
                                    override fun createShader(size: Size): Shader {
                                        val widthOffset = size.width * offset
                                        val heightOffset = size.height * offset
                                        return LinearGradientShader(
                                            colors = if (index == synchronizedLyrics.index)
                                                if (showlyricsthumbnail) oldlyrics else Themegradient
                                            else if (showlyricsthumbnail) oldlyrics2 else Themegradient2,
                                            from = Offset(widthOffset, heightOffset),
                                            to = Offset(
                                                widthOffset + size.width,
                                                heightOffset + size.height
                                            ),
                                            tileMode = TileMode.Mirror
                                        )
                                    }
                                }
                            }
                            val animateSizeText by animateFloatAsState(
                                targetValue = when {
                                    lyricsKaraokeEnabled && index == synchronizedLyrics.index -> 1.02f
                                    lyricsKaraokeEnabled -> 0.92f
                                    index == synchronizedLyrics.index -> 1.05f
                                    else -> 0.85f
                                },
                                animationSpec = tween(
                                    durationMillis = if (lyricsKaraokeEnabled) 220 else 500,
                                    easing = LinearOutSlowInEasing
                                ),
                                label = ""
                            )
                            val animateOpacity by animateFloatAsState(
                                targetValue = when {
                                    lyricsKaraokeEnabled && index == synchronizedLyrics.index -> 1f
                                    lyricsKaraokeEnabled -> 0.38f
                                    index == synchronizedLyrics.index -> 1f
                                    else -> 0.6f
                                },
                                animationSpec = tween(
                                    durationMillis = if (lyricsKaraokeEnabled) 220 else 500,
                                    easing = LinearOutSlowInEasing
                                ),
                                label = ""
                            )
                            //Rainbow Shimmer
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                ////Lyrics Outline Synced
                                if (!showlyricsthumbnail) {
                                    if (lyricsOutline == LyricsOutline.None) {

                                    } else if ((lyricsOutline == LyricsOutline.White) || (lyricsOutline == LyricsOutline.Black) || (lyricsOutline == LyricsOutline.Thememode))
                                        BasicText(
                                            text = translatedText,
                                            style = TextStyle(
                                                fontWeight = FontWeight.Medium,
                                                color = if (lyricsOutline == LyricsOutline.White) Color.White
                                                else if (lyricsOutline == LyricsOutline.Black) Color.Black
                                                else if (lyricsOutline == LyricsOutline.Thememode)
                                                    if (colorPaletteMode == ColorPaletteMode.Light) Color.White
                                                    else Color.Black
                                                else Color.Transparent,
                                                fontSize = if (fontSize == LyricsFontSize.Light) typography().m.fontSize
                                                else if (fontSize == LyricsFontSize.Medium) typography().l.fontSize
                                                else if (fontSize == LyricsFontSize.Heavy) typography().xl.fontSize
                                                else if (fontSize == LyricsFontSize.Large) typography().xlxl.fontSize
                                                else customSize.sp,
                                                textAlign = lyricsAlignment.selected,
                                                drawStyle = Stroke(
                                                    width = if (fontSize == LyricsFontSize.Large)
                                                        if (lyricsOutline == LyricsOutline.White) 6.0f
                                                        else if (lyricsOutline == LyricsOutline.Black) 10.0f
                                                        else if (lyricsOutline == LyricsOutline.Thememode)
                                                            if (colorPaletteMode == ColorPaletteMode.Light) 6.0f
                                                            else 10.0f
                                                        else 0f
                                                    else if (fontSize == LyricsFontSize.Heavy)
                                                        if (lyricsOutline == LyricsOutline.White) 3f
                                                        else if (lyricsOutline == LyricsOutline.Black) 7f
                                                        else if (lyricsOutline == LyricsOutline.Thememode)
                                                            if (colorPaletteMode == ColorPaletteMode.Light) 3f
                                                            else 7f
                                                        else 0f
                                                    else if (fontSize == LyricsFontSize.Medium)
                                                        if (lyricsOutline == LyricsOutline.White) 2f
                                                        else if (lyricsOutline == LyricsOutline.Black) 6f
                                                        else if (lyricsOutline == LyricsOutline.Thememode)
                                                            if (colorPaletteMode == ColorPaletteMode.Light) 2f
                                                            else 6f
                                                        else 0f
                                                    else if (fontSize == LyricsFontSize.Light)
                                                        if (lyricsOutline == LyricsOutline.White) 1.3f
                                                        else if (lyricsOutline == LyricsOutline.Black) 5.3f
                                                        else if (lyricsOutline == LyricsOutline.Thememode)
                                                            if (colorPaletteMode == ColorPaletteMode.Light) 1.3f
                                                            else 5.3f
                                                        else 0f
                                                    else
                                                        if (lyricsOutline == LyricsOutline.White) (customSize/5.6f)
                                                        else if (lyricsOutline == LyricsOutline.Black) (customSize/3.4f)
                                                        else if (lyricsOutline == LyricsOutline.Thememode)
                                                            if (colorPaletteMode == ColorPaletteMode.Light) (customSize/5.6f)
                                                            else (customSize/3.4f)
                                                        else 0f,
                                                    join = StrokeJoin.Round
                                                )
                                            ),
                                            modifier = Modifier
                                                .padding(vertical = 4.dp, horizontal = 32.dp)
                                                .conditional(lyricsSizeAnimate) { padding(vertical = 4.dp) }
                                                .align(if (lyricsAlignment == LyricsAlignment.Left) Alignment.CenterStart else if (lyricsAlignment == LyricsAlignment.Right) Alignment.CenterEnd else Alignment.Center)
                                                .conditional(lyricsSizeAnimate) {
                                                    graphicsLayer {
                                                        transformOrigin =
                                                            if (lyricsAlignment == LyricsAlignment.Center) TransformOrigin(
                                                                0.5f,
                                                                0.5f
                                                            )
                                                            else if (lyricsAlignment == LyricsAlignment.Left) TransformOrigin(
                                                                0f,
                                                                0.5f
                                                            )
                                                            else TransformOrigin(1f, 0.5f)
                                                        scaleY = animateSizeText
                                                        scaleX = animateSizeText
                                                    }
                                                }
                                                .graphicsLayer{
                                                    alpha = animateOpacity
                                                }
                                        )
                                    else if (lyricsOutline == LyricsOutline.Rainbow)
                                        BasicText(
                                            text = translatedText,
                                            style = TextStyle(
                                                textAlign = lyricsAlignment.selected,
                                                brush = if (lightTheme) brushrainbow else brushrainbowdark,
                                                fontSize = if (fontSize == LyricsFontSize.Light) typography().m.fontSize
                                                else if (fontSize == LyricsFontSize.Medium) typography().l.fontSize
                                                else if (fontSize == LyricsFontSize.Heavy) typography().xl.fontSize
                                                else if (fontSize == LyricsFontSize.Large) typography().xlxl.fontSize
                                                else customSize.sp,
                                                fontWeight = FontWeight.Medium,
                                                drawStyle = Stroke(
                                                    width = if (fontSize == LyricsFontSize.Large) if (index == synchronizedLyrics.index) 10.0f else 6f
                                                    else if (fontSize == LyricsFontSize.Heavy) if (index == synchronizedLyrics.index) 7f else 5f
                                                    else if (fontSize == LyricsFontSize.Medium) if (index == synchronizedLyrics.index) 6f else 4f
                                                    else if (fontSize == LyricsFontSize.Light) if (index == synchronizedLyrics.index) 5.3f else 3.3f
                                                    else if (index == synchronizedLyrics.index) (customSize/3.4f) else (customSize/5.6f),
                                                    join = StrokeJoin.Round
                                                )
                                            ),
                                            modifier = Modifier
                                                .padding(vertical = 4.dp, horizontal = 32.dp)
                                                .conditional(lyricsSizeAnimate) { padding(vertical = 4.dp) }
                                                .align(if (lyricsAlignment == LyricsAlignment.Left) Alignment.CenterStart else if (lyricsAlignment == LyricsAlignment.Right) Alignment.CenterEnd else Alignment.Center)
                                                .conditional(lyricsSizeAnimate) {
                                                    graphicsLayer {
                                                        transformOrigin =
                                                            if (lyricsAlignment == LyricsAlignment.Center) TransformOrigin(
                                                                0.5f,
                                                                0.5f
                                                            )
                                                            else if (lyricsAlignment == LyricsAlignment.Left) TransformOrigin(
                                                                0f,
                                                                0.5f
                                                            )
                                                            else TransformOrigin(1f, 0.5f)
                                                        scaleY = animateSizeText
                                                        scaleX = animateSizeText
                                                    }
                                                }
                                                .graphicsLayer{
                                                    alpha = animateOpacity
                                                }
                                        )
                                    else //For Glow Outline//
                                        BasicText(
                                            text = translatedText,
                                            style = TextStyle(
                                                fontSize = if (fontSize == LyricsFontSize.Light) typography().m.fontSize
                                                else if (fontSize == LyricsFontSize.Medium) typography().l.fontSize
                                                else if (fontSize == LyricsFontSize.Heavy) typography().xl.fontSize
                                                else if (fontSize == LyricsFontSize.Large) typography().xlxl.fontSize
                                                else customSize.sp,
                                                fontWeight = FontWeight.Medium,
                                                textAlign = lyricsAlignment.selected,
                                                color = if ((lyricsColor == LyricsColor.Thememode || lyricsColor == LyricsColor.White || lyricsColor == LyricsColor.Black) || lyricsColor == LyricsColor.Accent)
                                                    Color.White.copy(0.3f) else Color.Transparent,
                                                shadow = Shadow(
                                                    color = if (index == synchronizedLyrics.index)
                                                        if (lyricsColor == LyricsColor.Thememode) Color.White.copy(
                                                            0.3f
                                                        ).compositeOver(colorPalette().text)
                                                        else if (lyricsColor == LyricsColor.White) Color.White.copy(
                                                            0.3f
                                                        ).compositeOver(Color.White)
                                                        else if (lyricsColor == LyricsColor.Black) Color.White.copy(
                                                            0.3f
                                                        ).compositeOver(Color.Black)
                                                        else if (lyricsColor == LyricsColor.Accent) Color.White.copy(
                                                            0.3f
                                                        ).compositeOver(colorPalette().accent)
                                                        else Color.Transparent
                                                    else Color.Transparent,
                                                    offset = Offset(0f, 0f), blurRadius = 25f
                                                ),
                                            ),
                                            modifier = Modifier
                                                .padding(vertical = 4.dp, horizontal = 32.dp)
                                                .conditional(lyricsSizeAnimate) { padding(vertical = 4.dp) }
                                                .align(if (lyricsAlignment == LyricsAlignment.Left) Alignment.CenterStart else if (lyricsAlignment == LyricsAlignment.Right) Alignment.CenterEnd else Alignment.Center)
                                                .conditional(lyricsSizeAnimate) {
                                                    graphicsLayer {
                                                        transformOrigin =
                                                            if (lyricsAlignment == LyricsAlignment.Center) TransformOrigin(
                                                                0.5f,
                                                                0.5f
                                                            )
                                                            else if (lyricsAlignment == LyricsAlignment.Left) TransformOrigin(
                                                                0f,
                                                                0.5f
                                                            )
                                                            else TransformOrigin(1f, 0.5f)
                                                        scaleY = animateSizeText
                                                        scaleX = animateSizeText
                                                    }
                                                }

                                        )
                                }
                                if (showlyricsthumbnail) {
                                    BasicText(
                                        text = translatedText,
                                        style = TextStyle(
                                            fontWeight = FontWeight.Medium,
                                            color = if (index == synchronizedLyrics.index) PureBlackColorPalette.text else PureBlackColorPalette.textDisabled,
                                            fontSize = if (fontSize == LyricsFontSize.Light) typography().m.fontSize
                                                       else if (fontSize == LyricsFontSize.Medium) typography().l.fontSize
                                                       else if (fontSize == LyricsFontSize.Heavy) typography().xl.fontSize
                                                       else if (fontSize == LyricsFontSize.Large) typography().xlxl.fontSize
                                                       else customSize.sp,
                                            textAlign = lyricsAlignment.selected,
                                        ),
                                        modifier = Modifier
                                            .padding(vertical = 4.dp, horizontal = 32.dp)
                                            .align(if (lyricsAlignment == LyricsAlignment.Left) Alignment.CenterStart else if (lyricsAlignment == LyricsAlignment.Right) Alignment.CenterEnd else Alignment.Center)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = if (clickLyricsText) ripple(true) else null,
                                                onClick = {
                                                    if (clickLyricsText)
                                                        binder?.player?.seekTo(sentence.first)
                                                    else onDismiss()
                                                }
                                            )
                                    )
                                }
                                else if ((lyricsColor == LyricsColor.White) || (lyricsColor == LyricsColor.Black) || (lyricsColor == LyricsColor.Accent) || (lyricsColor == LyricsColor.Thememode)) {
                                    BasicText(
                                        text = translatedText,
                                        style = TextStyle(
                                            fontWeight = FontWeight.Medium,
                                            color = if (lyricsColor == LyricsColor.White) Color.White
                                            else if (lyricsColor == LyricsColor.Black) Color.Black
                                            else if (lyricsColor == LyricsColor.Thememode) colorPalette().text
                                            else colorPalette().accent,
                                            fontSize = if (fontSize == LyricsFontSize.Light) typography().m.fontSize
                                            else if (fontSize == LyricsFontSize.Medium) typography().l.fontSize
                                            else if (fontSize == LyricsFontSize.Heavy) typography().xl.fontSize
                                            else if (fontSize == LyricsFontSize.Large) typography().xlxl.fontSize
                                            else customSize.sp,
                                            textAlign = lyricsAlignment.selected,
                                        ),
                                        modifier = Modifier
                                            .padding(vertical = 4.dp, horizontal = 32.dp)
                                            .conditional(lyricsSizeAnimate) { padding(vertical = 4.dp) }
                                            .align(if (lyricsAlignment == LyricsAlignment.Left) Alignment.CenterStart else if (lyricsAlignment == LyricsAlignment.Right) Alignment.CenterEnd else Alignment.Center)
                                            .conditional(lyricsSizeAnimate) {
                                                graphicsLayer {
                                                    transformOrigin =
                                                        if (lyricsAlignment == LyricsAlignment.Center) TransformOrigin(
                                                            0.5f,
                                                            0.5f
                                                        )
                                                        else if (lyricsAlignment == LyricsAlignment.Left) TransformOrigin(
                                                            0f,
                                                            0.5f
                                                        )
                                                        else TransformOrigin(1f, 0.5f)
                                                    scaleY = animateSizeText
                                                    scaleX = animateSizeText
                                                }
                                            }
                                            .graphicsLayer{
                                                alpha = animateOpacity
                                            }
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = if (clickLyricsText) ripple(true) else null,
                                                onClick = {
                                                    if (clickLyricsText)
                                                        binder?.player?.seekTo(sentence.first)
                                                    else onDismiss()
                                                }
                                            )
                                            .background(
                                                if (index == synchronizedLyrics.index) if (lyricsHighlight == LyricsHighlight.White) Color.White.copy(
                                                    0.5f
                                                ) else if (lyricsHighlight == LyricsHighlight.Black) Color.Black.copy(
                                                    0.5f
                                                ) else Color.Transparent else Color.Transparent,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .conditional(lyricsHighlight != LyricsHighlight.None) { fillMaxWidth() }
                                    )
                                }
                                else
                                    BasicText(
                                        text = translatedText,
                                        style = TextStyle(
                                            brush = if (lightTheme) brushrainbow else brushrainbowdark,
                                            fontSize = if (fontSize == LyricsFontSize.Light) typography().m.fontSize
                                                       else if (fontSize == LyricsFontSize.Medium) typography().l.fontSize
                                                       else if (fontSize == LyricsFontSize.Heavy) typography().xl.fontSize
                                                       else if (fontSize == LyricsFontSize.Large) typography().xlxl.fontSize
                                                       else customSize.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = lyricsAlignment.selected
                                        ),
                                        modifier = Modifier
                                            .padding(vertical = 4.dp, horizontal = 32.dp)
                                            .conditional(lyricsSizeAnimate){padding(vertical = 4.dp)}
                                            .align(if (lyricsAlignment == LyricsAlignment.Left) Alignment.CenterStart else if (lyricsAlignment == LyricsAlignment.Right) Alignment.CenterEnd else Alignment.Center)
                                            .conditional(lyricsSizeAnimate){
                                                graphicsLayer {
                                                    transformOrigin = if (lyricsAlignment == LyricsAlignment.Center) TransformOrigin(0.5f,0.5f)
                                                    else if (lyricsAlignment == LyricsAlignment.Left) TransformOrigin(0f,0.5f)
                                                    else TransformOrigin(1f,0.5f)
                                                    scaleY = if (index == synchronizedLyrics.index) 1.1f else 0.9f
                                                    scaleX = if (index == synchronizedLyrics.index) 1.1f else 0.9f
                                                }
                                            }
                                            .graphicsLayer{
                                                alpha = animateOpacity
                                            }
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = if (clickLyricsText) ripple(true) else null,
                                                onClick = {
                                                    if (clickLyricsText)
                                                        binder?.player?.seekTo(sentence.first)
                                                    else onDismiss()
                                                }
                                            )
                                    )
                                /*else
                                  BasicText(
                                     text = translatedText,
                                     style = TextStyle(
                                         brush = brushtheme
                                     ).merge(when (fontSize) {
                                         LyricsFontSize.Light ->
                                             typography().m.center.medium.color(
                                                 if (index == synchronizedLyrics.index) PureBlackColorPalette.text
                                                 else colorPalette().text.copy(0.6f)
                                             )
                                         LyricsFontSize.Medium ->
                                             typography().l.center.medium.color(
                                                 if (index == synchronizedLyrics.index) PureBlackColorPalette.text
                                                 else colorPalette().text.copy(0.6f)
                                             )
                                         LyricsFontSize.Heavy ->
                                             typography().xl.center.medium.color(
                                                 if (index == synchronizedLyrics.index) PureBlackColorPalette.text
                                                 else colorPalette().text.copy(0.6f)
                                             )
                                         LyricsFontSize.Large ->
                                             typography().xlxl.center.medium.color(
                                                 if (index == synchronizedLyrics.index) PureBlackColorPalette.text
                                                 else colorPalette().text.copy(0.6f)
                                             )
                                     },
                                     ),
                                     modifier = Modifier
                                         .padding(vertical = 4.dp, horizontal = 32.dp)
                                         .clickable {
                                             if (enableClick)
                                                 binder?.player?.seekTo(sentence.first)
                                         }
                                 )*/

                            }
                        }
                        item(key = "footer", contentType = 0) {
                            Spacer(modifier = Modifier.height(thumbnailSize))
                        }
                    }
                } else {
                    var translatedText by remember { mutableStateOf("") }
                    if (showSecondLine || translateEnabled || romanization != Romanization.Off) {
                        val mutState = remember { mutableStateOf("") }
                        translateLyricsWithRomanization(mutState, text, false, languageDestination)()
                        translatedText = mutState.value
                    } else {
                        translatedText = text
                    }

                    Column(
                        modifier = Modifier
                            .verticalFadingEdge()
                            .background(
                                if (isDisplayed && !showlyricsthumbnail) if (lyricsBackground == LyricsBackground.Black) Color.Black.copy(
                                    0.4f
                                ) else if (lyricsBackground == LyricsBackground.White) Color.White.copy(
                                    0.4f
                                ) else Color.Transparent else Color.Transparent
                            ),
                    ) {
                        Box(
                            modifier = Modifier
                                .verticalFadingEdge()
                                .verticalScroll(rememberScrollState())
                                .fillMaxWidth()
                                .padding(vertical = size / 4, horizontal = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            //Rainbow Shimmer
                            val infiniteTransition = rememberInfiniteTransition()

                            val offset by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        durationMillis = 10000,
                                        easing = LinearEasing
                                    ),
                                    repeatMode = RepeatMode.Reverse
                                ), label = ""
                            )

                            val RainbowColors = listOf(
                                Color.Red,
                                Color.Magenta,
                                Color.Blue,
                                Color.Cyan,
                                Color.Green,
                                Color.Yellow,
                                Color.Red
                            )
                            val RainbowColorsdark = listOf(
                                Color.Black.copy(0.35f).compositeOver(Color.Red),
                                Color.Black.copy(0.35f).compositeOver(Color.Magenta),
                                Color.Black.copy(0.35f).compositeOver(Color.Blue),
                                Color.Black.copy(0.35f).compositeOver(Color.Cyan),
                                Color.Black.copy(0.35f).compositeOver(Color.Green),
                                Color.Black.copy(0.35f).compositeOver(Color.Yellow),
                                Color.Black.copy(0.35f).compositeOver(Color.Red)
                            )

                            val brushrainbow = remember(offset) {
                                object : ShaderBrush() {
                                    override fun createShader(size: Size): Shader {
                                        val widthOffset = size.width * offset
                                        val heightOffset = size.height * offset
                                        return LinearGradientShader(
                                            colors = RainbowColors,
                                            from = Offset(widthOffset, heightOffset),
                                            to = Offset(
                                                widthOffset + size.width,
                                                heightOffset + size.height
                                            ),
                                            tileMode = TileMode.Mirror
                                        )
                                    }
                                }
                            }
                            val brushrainbowdark = remember(offset) {
                                object : ShaderBrush() {
                                    override fun createShader(size: Size): Shader {
                                        val widthOffset = size.width * offset
                                        val heightOffset = size.height * offset
                                        return LinearGradientShader(
                                            colors = RainbowColorsdark,
                                            from = Offset(widthOffset, heightOffset),
                                            to = Offset(
                                                widthOffset + size.width,
                                                heightOffset + size.height
                                            ),
                                            tileMode = TileMode.Mirror
                                        )
                                    }
                                }
                            }

                            if (!showlyricsthumbnail) {
                                if ((lyricsOutline == LyricsOutline.None) || (lyricsOutline == LyricsOutline.Glow)) {

                                } else if (lyricsOutline == LyricsOutline.Thememode || (lyricsOutline == LyricsOutline.White) || (lyricsOutline == LyricsOutline.Black))
                                    BasicText(
                                        text = translatedText,
                                        style = TextStyle(
                                            textAlign = lyricsAlignment.selected,
                                            fontWeight = FontWeight.Medium,
                                            color = if (lyricsOutline == LyricsOutline.White) Color.White
                                            else if (lyricsOutline == LyricsOutline.Black) Color.Black
                                            else if (lyricsOutline == LyricsOutline.Thememode)
                                                if (colorPaletteMode == ColorPaletteMode.Light) Color.White
                                                else Color.Black
                                            else Color.Transparent,
                                            fontSize = if (fontSize == LyricsFontSize.Light) typography().m.fontSize
                                            else if (fontSize == LyricsFontSize.Medium) typography().l.fontSize
                                            else if (fontSize == LyricsFontSize.Heavy) typography().xl.fontSize
                                            else if (fontSize == LyricsFontSize.Large) typography().xlxl.fontSize
                                            else customSize.sp,
                                            drawStyle = Stroke(
                                                width = if (fontSize == LyricsFontSize.Large)
                                                    if (lyricsOutline == LyricsOutline.White) 6.0f
                                                    else if (lyricsOutline == LyricsOutline.Black) 10.0f
                                                    else if (lyricsOutline == LyricsOutline.Thememode)
                                                        if (colorPaletteMode == ColorPaletteMode.Light) 6.0f
                                                        else 10.0f
                                                    else 0f
                                                else if (fontSize == LyricsFontSize.Heavy)
                                                    if (lyricsOutline == LyricsOutline.White) 3f
                                                    else if (lyricsOutline == LyricsOutline.Black) 7f
                                                    else if (lyricsOutline == LyricsOutline.Thememode)
                                                        if (colorPaletteMode == ColorPaletteMode.Light) 3f
                                                        else 7f
                                                    else 0f
                                                else if (fontSize == LyricsFontSize.Medium)
                                                    if (lyricsOutline == LyricsOutline.White) 2f
                                                    else if (lyricsOutline == LyricsOutline.Black) 6f
                                                    else if (lyricsOutline == LyricsOutline.Thememode)
                                                        if (colorPaletteMode == ColorPaletteMode.Light) 2f
                                                        else 6f
                                                    else 0f
                                                else if (fontSize == LyricsFontSize.Light)
                                                    if (lyricsOutline == LyricsOutline.White) 1.3f
                                                    else if (lyricsOutline == LyricsOutline.Black) 5.3f
                                                    else if (lyricsOutline == LyricsOutline.Thememode)
                                                        if (colorPaletteMode == ColorPaletteMode.Light) 1.3f
                                                        else 5.3f
                                                    else 0f
                                                else
                                                    if (lyricsOutline == LyricsOutline.White) (customSize/5.6f)
                                                    else if (lyricsOutline == LyricsOutline.Black) (customSize/3.4f)
                                                    else if (lyricsOutline == LyricsOutline.Thememode)
                                                        if (colorPaletteMode == ColorPaletteMode.Light) (customSize/5.6f)
                                                        else (customSize/3.4f)
                                                    else 0f,
                                                join = StrokeJoin.Round
                                            )
                                        ),
                                        modifier = Modifier
                                            .align(if (lyricsAlignment == LyricsAlignment.Left) Alignment.CenterStart else if (lyricsAlignment == LyricsAlignment.Right) Alignment.CenterEnd else Alignment.Center)
                                    )
                                else
                                    BasicText(
                                        text = translatedText,
                                        style = TextStyle(
                                            textAlign = lyricsAlignment.selected,
                                            brush = if (lightTheme) brushrainbow else brushrainbowdark,
                                            fontSize = if (fontSize == LyricsFontSize.Light) typography().m.fontSize
                                            else if (fontSize == LyricsFontSize.Medium) typography().l.fontSize
                                            else if (fontSize == LyricsFontSize.Heavy) typography().xl.fontSize
                                            else if (fontSize == LyricsFontSize.Large) typography().xlxl.fontSize
                                            else customSize.sp,
                                            fontWeight = FontWeight.Medium,
                                            drawStyle = Stroke(
                                                width = if (fontSize == LyricsFontSize.Large) 10f
                                                else if (fontSize == LyricsFontSize.Heavy) 7f
                                                else if (fontSize == LyricsFontSize.Medium) 6f
                                                else if (fontSize == LyricsFontSize.Light) 5.3f
                                                else (customSize/3.4f),
                                                join = StrokeJoin.Round
                                            )
                                        ),
                                        modifier = Modifier
                                            .align(if (lyricsAlignment == LyricsAlignment.Left) Alignment.CenterStart else if (lyricsAlignment == LyricsAlignment.Right) Alignment.CenterEnd else Alignment.Center)
                                    )
                            }

                            if (showlyricsthumbnail) {
                                BasicText(
                                    text = translatedText,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Medium,
                                        color = PureBlackColorPalette.text,
                                        fontSize = if (fontSize == LyricsFontSize.Light) typography().m.fontSize
                                        else if (fontSize == LyricsFontSize.Medium) typography().l.fontSize
                                        else if (fontSize == LyricsFontSize.Heavy) typography().xl.fontSize
                                        else if (fontSize == LyricsFontSize.Large) typography().xlxl.fontSize
                                        else customSize.sp,
                                        textAlign = lyricsAlignment.selected,
                                    ),
                                    modifier = Modifier
                                        .align(if (lyricsAlignment == LyricsAlignment.Left) Alignment.CenterStart else if (lyricsAlignment == LyricsAlignment.Right) Alignment.CenterEnd else Alignment.Center)
                                )
                            }
                            else if ((lyricsColor == LyricsColor.Thememode) || (lyricsColor == LyricsColor.White) || (lyricsColor == LyricsColor.Black) || (lyricsColor == LyricsColor.Accent))
                                BasicText(
                                    text = translatedText,
                                    style = TextStyle (
                                        fontWeight = FontWeight.Medium,
                                        color = if (lyricsColor == LyricsColor.White) Color.White
                                                else if (lyricsColor == LyricsColor.Black) Color.Black
                                                else if (lyricsColor == LyricsColor.Thememode) colorPalette().text
                                                else colorPalette().accent,
                                        fontSize = if (fontSize == LyricsFontSize.Light) typography().m.fontSize
                                                   else if (fontSize == LyricsFontSize.Medium) typography().l.fontSize
                                                   else if (fontSize == LyricsFontSize.Heavy) typography().xl.fontSize
                                                   else if (fontSize == LyricsFontSize.Large) typography().xlxl.fontSize
                                                   else customSize.sp,
                                        textAlign = lyricsAlignment.selected,
                                    ),
                                    modifier = Modifier
                                        .align(if (lyricsAlignment == LyricsAlignment.Left) Alignment.CenterStart else if (lyricsAlignment == LyricsAlignment.Right) Alignment.CenterEnd else Alignment.Center)
                                )
                            else
                                BasicText(
                                    text = translatedText,
                                    style = TextStyle(
                                        brush = if (lightTheme) brushrainbow else brushrainbowdark,
                                        fontSize = if (fontSize == LyricsFontSize.Light) typography().m.fontSize
                                                   else if (fontSize == LyricsFontSize.Medium) typography().l.fontSize
                                                   else if (fontSize == LyricsFontSize.Heavy) typography().xl.fontSize
                                                   else if (fontSize == LyricsFontSize.Large) typography().xlxl.fontSize
                                                   else customSize.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = lyricsAlignment.selected
                                    ),
                                    modifier = Modifier
                                        .align(if (lyricsAlignment == LyricsAlignment.Left) Alignment.CenterStart else if (lyricsAlignment == LyricsAlignment.Right) Alignment.CenterEnd else Alignment.Center)
                                )
                            //Lyrics Outline Non Synced
                        }
                    }
                }
            }

            if ((text == null && !isError) || showPlaceholder) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .shimmer()
                ) {
                    repeat(4) {
                        TextPlaceholder(
                            color = colorPalette().onOverlayShimmer,
                            modifier = Modifier
                                .alpha(1f - it * 0.1f)
                        )
                    }
                }
            }

            /**********/
            if (trailingContent != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.4f)
                ) {
                    trailingContent()
                }
            }
            /*********/

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(if (trailingContent == null) 0.30f else 0.22f)
            ) {
                if (isLandscape && !showlyricsthumbnail)
                    IconButton(
                        icon = R.drawable.chevron_back,
                        color = colorPalette().accent,
                        enabled = true,
                        onClick = onDismiss,
                        modifier = Modifier
                            .padding(all = 8.dp)
                            .align(Alignment.BottomStart)
                            .size(30.dp)
                    )

                if (showlyricsthumbnail)
                    IconButton(
                        icon = R.drawable.text,
                        color = DefaultDarkColorPalette.text,
                        enabled = true,
                        onClick = {
                            menuState.display {
                                Menu {
                                    MenuEntry(
                                        icon = R.drawable.text,
                                        text = stringResource(R.string.light),
                                        secondaryText = "",
                                        onClick = {
                                            menuState.hide()
                                            fontSize = LyricsFontSize.Light
                                        }
                                    )
                                    MenuEntry(
                                        icon = R.drawable.text,
                                        text = stringResource(R.string.medium),
                                        secondaryText = "",
                                        onClick = {
                                            menuState.hide()
                                            fontSize = LyricsFontSize.Medium
                                        }
                                    )
                                    MenuEntry(
                                        icon = R.drawable.text,
                                        text = stringResource(R.string.heavy),
                                        secondaryText = "",
                                        onClick = {
                                            menuState.hide()
                                            fontSize = LyricsFontSize.Heavy
                                        }
                                    )
                                    MenuEntry(
                                        icon = R.drawable.text,
                                        text = stringResource(R.string.large),
                                        secondaryText = "",
                                        onClick = {
                                            menuState.hide()
                                            fontSize = LyricsFontSize.Large
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(all = 8.dp)
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                    )
            }
            if (!showlyricsthumbnail && isDisplayed && isLandscape && landscapeControls) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent,if (lightTheme) Color.White.copy(0.5f) else Color.Black.copy(0.5f)),
                                startY = 0f,
                                endY = POSITIVE_INFINITY
                            ),
                        )
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                ){
                    Image(
                        painter = painterResource(R.drawable.play_skip_back),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette().text),
                        modifier = Modifier
                            .clickable(
                                indication = ripple(bounded = false),
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    if (jumpPrevious == "") jumpPrevious = "0"
                                    if(binder?.player?.hasPreviousMediaItem() == false || (jumpPrevious != "0" && (binder?.player?.currentPosition ?: 0) > jumpPrevious.toInt() * 1000)
                                    ){
                                        binder?.player?.seekTo(0)
                                    }
                                    else binder?.player?.playPrevious()
                                    if (effectRotationEnabled) isRotated = !isRotated
                                }
                            )
                            .rotate(rotationAngle)
                            .padding(horizontal = 15.dp)
                            .size(30.dp)

                    )
                    Box {
                        Box(modifier = Modifier
                            .align(Alignment.Center)
                            .size(45.dp)
                            .background(colorPalette().accent, RoundedCornerShape(15.dp))
                        ){}
                        Image(
                            painter = painterResource(if (binder?.player?.isPlaying == true) R.drawable.pause else R.drawable.play),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(if (colorPaletteName == ColorPaletteName.PureBlack) Color.Black else colorPalette().text),
                            modifier = Modifier
                                .clickable(
                                    indication = ripple(bounded = false),
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        if (binder?.player?.isPlaying == true) {
                                            binder.gracefulPause()
                                        } else {
                                            binder?.player?.play()
                                        }
                                    },
                                )
                                .align(Alignment.Center)
                                .rotate(rotationAngle)
                                .padding(horizontal = 15.dp)
                                .size(36.dp)

                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.play_skip_forward),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette().text),
                        modifier = Modifier
                            .clickable(
                                indication = ripple(bounded = false),
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    binder?.player?.playNext()
                                    if (effectRotationEnabled) isRotated = !isRotated
                                }
                            )
                            .rotate(rotationAngle)
                            .padding(horizontal = 15.dp)
                            .size(30.dp)

                    )
                }

            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth(0.2f)
            ) {


                if (showlyricsthumbnail)
                    IconButton(
                        icon = R.drawable.translate,
                        color = if (translateEnabled == true) colorPalette().text else colorPalette().textDisabled,
                        enabled = true,
                        onClick = {
                            translateEnabled = !translateEnabled
                            if (translateEnabled) showLanguagesList = true
                        },
                        modifier = Modifier
                            //.padding(horizontal = 8.dp)
                            .padding(bottom = 10.dp)
                            .align(Alignment.BottomStart)
                            .size(24.dp)
                    )


                Image(
                    painter = painterResource(R.drawable.ellipsis_vertical),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(DefaultDarkColorPalette.text),
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .clickable(
                            indication = ripple(bounded = false),
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                menuState.display {
                                    Menu {
                                        if (isLandscape && !showlyricsthumbnail){
                                            MenuEntry(
                                                icon = if (landscapeControls) R.drawable.checkmark else R.drawable.play,
                                                text = stringResource(R.string.toggle_controls_landscape),
                                                enabled = true,
                                                onClick = {
                                                    menuState.hide()
                                                    landscapeControls = !landscapeControls
                                                }
                                            )
                                        }
                                        MenuEntry(
                                            icon = R.drawable.text,
                                            enabled = true,
                                            text = stringResource(R.string.lyricsalignment),
                                            onClick = {
                                                menuState.display {
                                                    Menu {
                                                        MenuEntry(
                                                            icon = R.drawable.arrow_left,
                                                            text = stringResource(R.string.direction_left),
                                                            secondaryText = "",
                                                            onClick = {
                                                                menuState.hide()
                                                                lyricsAlignment = LyricsAlignment.Left
                                                            }
                                                        )
                                                        MenuEntry(
                                                            icon = R.drawable.arrow_down,
                                                            text = stringResource(R.string.center),
                                                            secondaryText = "",
                                                            onClick = {
                                                                menuState.hide()
                                                                lyricsAlignment = LyricsAlignment.Center
                                                            }
                                                        )
                                                        MenuEntry(
                                                            icon = R.drawable.arrow_right,
                                                            text = stringResource(R.string.direction_right),
                                                            secondaryText = "",
                                                            onClick = {
                                                                menuState.hide()
                                                                lyricsAlignment = LyricsAlignment.Right
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        )

                                        if (!showlyricsthumbnail)
                                            MenuEntry(
                                                icon = R.drawable.text,
                                                enabled = true,
                                                text = stringResource(R.string.lyrics_size),
                                                onClick = {
                                                    menuState.display {
                                                        Menu {
                                                            MenuEntry(
                                                                icon = R.drawable.text,
                                                                text = stringResource(R.string.light),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    fontSize = LyricsFontSize.Light
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.text,
                                                                text = stringResource(R.string.medium),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    fontSize = LyricsFontSize.Medium
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.text,
                                                                text = stringResource(R.string.heavy),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    fontSize = LyricsFontSize.Heavy
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.text,
                                                                text = stringResource(R.string.large),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    fontSize = LyricsFontSize.Large
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.text,
                                                                text = stringResource(R.string.custom),
                                                                secondaryText = stringResource(R.string.lyricsSizeSecondary),
                                                                onClick = {
                                                                    menuState.hide()
                                                                    fontSize = LyricsFontSize.Custom
                                                                },
                                                                onLongClick = {showLyricsSizeDialog = !showLyricsSizeDialog},
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        if (!showlyricsthumbnail)
                                            MenuEntry(
                                                icon = R.drawable.droplet,
                                                enabled = true,
                                                text = stringResource(R.string.lyricscolor),
                                                onClick = {
                                                    menuState.display {
                                                        Menu {
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.theme),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsColor =
                                                                        LyricsColor.Thememode
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.white),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsColor =
                                                                        LyricsColor.White
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.black),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsColor =
                                                                        LyricsColor.Black
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.accent),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsColor = LyricsColor.Accent
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.fluidrainbow),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsColor =
                                                                        LyricsColor.FluidRainbow
                                                                }
                                                            )
                                                            /*MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.fluidtheme),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsColor = LyricsColor.FluidTheme
                                                                }
                                                            )*/
                                                        }
                                                    }
                                                }
                                            )
                                        if (!showlyricsthumbnail)
                                            MenuEntry(
                                                icon = R.drawable.horizontal_bold_line,
                                                enabled = true,
                                                text = stringResource(R.string.lyricsoutline),
                                                onClick = {
                                                    menuState.display {
                                                        Menu {
                                                            MenuEntry(
                                                                icon = R.drawable.close,
                                                                text = stringResource(R.string.none),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsOutline =
                                                                        LyricsOutline.None
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.horizontal_bold_line,
                                                                text = stringResource(R.string.theme),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsOutline =
                                                                        LyricsOutline.Thememode
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.horizontal_bold_line,
                                                                text = stringResource(R.string.white),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsOutline =
                                                                        LyricsOutline.White
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.horizontal_bold_line,
                                                                text = stringResource(R.string.black),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsOutline =
                                                                        LyricsOutline.Black
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.fluidrainbow),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsOutline =
                                                                        LyricsOutline.Rainbow
                                                                }
                                                            )
                                                            if (isShowingSynchronizedLyrics) {
                                                                MenuEntry(
                                                                    icon = R.drawable.droplet,
                                                                    text = stringResource(R.string.glow),
                                                                    secondaryText = "",
                                                                    onClick = {
                                                                        menuState.hide()
                                                                        lyricsOutline =
                                                                            LyricsOutline.Glow
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            )

                                        //if (!showlyricsthumbnail)
                                            MenuEntry(
                                                icon = R.drawable.translate,
                                                text = stringResource(R.string.translate_to, otherLanguageApp),
                                                enabled = true,
                                                onClick = {
                                                    menuState.hide()
                                                    translateEnabled = true
                                                }
                                            )
                                        MenuEntry(
                                            icon = R.drawable.translate,
                                            text = stringResource(R.string.translate_to_other_language),
                                            enabled = true,
                                            onClick = {
                                                menuState.hide()
                                                showLanguagesList = true
                                            }
                                        )

                                        MenuEntry(
                                            icon = if (romanization == Romanization.Original || romanization == Romanization.Translated || romanization == Romanization.Both) R.drawable.checkmark else R.drawable.text,
                                            enabled = true,
                                            text = stringResource(R.string.toggle_romanization),
                                            onClick = {
                                                menuState.display {
                                                    Menu {
                                                        MenuEntry(
                                                            icon = if (romanization == Romanization.Off) R.drawable.checkmark else R.drawable.text,
                                                            text = stringResource(R.string.turn_off),
                                                            secondaryText = "",
                                                            onClick = {
                                                                menuState.hide()
                                                                romanization =
                                                                    Romanization.Off
                                                            }
                                                        )
                                                        MenuEntry(
                                                            icon = if (romanization == Romanization.Original || (romanization == Romanization.Both && !showSecondLine)) R.drawable.checkmark else R.drawable.text,
                                                            text = stringResource(R.string.original_lyrics),
                                                            secondaryText = "",
                                                            onClick = {
                                                                menuState.hide()
                                                                romanization =
                                                                    Romanization.Original
                                                            }
                                                        )
                                                        MenuEntry(
                                                            icon = if (romanization == Romanization.Translated) R.drawable.checkmark else R.drawable.text,
                                                            text = stringResource(R.string.translated_lyrics),
                                                            secondaryText = "",
                                                            onClick = {
                                                                menuState.hide()
                                                                romanization =
                                                                    Romanization.Translated
                                                            }
                                                        )
                                                        if (showSecondLine) {
                                                            MenuEntry(
                                                                icon = if (romanization == Romanization.Both) R.drawable.checkmark else R.drawable.text,
                                                                text = stringResource(R.string.both),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    romanization =
                                                                        Romanization.Both
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                        MenuEntry(
                                            icon = if (showSecondLine) R.drawable.checkmark else R.drawable.close,
                                            text = stringResource(R.string.showsecondline),
                                            enabled = true,
                                            onClick = {
                                                menuState.hide()
                                                showSecondLine = !showSecondLine
                                            }
                                        )

                                        if (!showlyricsthumbnail && isShowingSynchronizedLyrics) {
                                            MenuEntry(
                                                icon = if (lyricsKaraokeEnabled) R.drawable.checkmark else R.drawable.text,
                                                text = stringResource(R.string.karaoke_animation),
                                                secondaryText = stringResource(R.string.karaoke_animation_description),
                                                enabled = true,
                                                onClick = {
                                                    menuState.hide()
                                                    lyricsKaraokeEnabled = !lyricsKaraokeEnabled
                                                }
                                            )
                                            MenuEntry(
                                                icon = if (lyricsSizeAnimate) R.drawable.checkmark else R.drawable.close,
                                                text = stringResource(R.string.lyricsanimate),
                                                enabled = true,
                                                onClick = {
                                                    menuState.hide()
                                                    lyricsSizeAnimate = !lyricsSizeAnimate
                                                }
                                            )
                                        }

                                        if (!showlyricsthumbnail)
                                            MenuEntry(
                                                icon = R.drawable.horizontal_bold_line_rounded,
                                                enabled = true,
                                                text = stringResource(R.string.highlight),
                                                onClick = {
                                                    menuState.display {
                                                        Menu {
                                                            MenuEntry(
                                                                icon = R.drawable.horizontal_straight_line,
                                                                text = stringResource(R.string.none),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsHighlight =
                                                                        LyricsHighlight.None
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.horizontal_straight_line,
                                                                text = stringResource(R.string.white),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsHighlight =
                                                                        LyricsHighlight.White
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.horizontal_straight_line,
                                                                text = stringResource(R.string.black),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsHighlight =
                                                                        LyricsHighlight.Black
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            )

                                        if (!showlyricsthumbnail)
                                            MenuEntry(
                                                icon = R.drawable.droplet,
                                                enabled = true,
                                                text = stringResource(R.string.lyricsbackground),
                                                onClick = {
                                                    menuState.display {
                                                        Menu {
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.none),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsBackground =
                                                                        LyricsBackground.None
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.white),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsBackground =
                                                                        LyricsBackground.White
                                                                }
                                                            )
                                                            MenuEntry(
                                                                icon = R.drawable.droplet,
                                                                text = stringResource(R.string.black),
                                                                secondaryText = "",
                                                                onClick = {
                                                                    menuState.hide()
                                                                    lyricsBackground =
                                                                        LyricsBackground.Black
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            )

                                        MenuEntry(
                                            icon = R.drawable.time,
                                            text = stringResource(R.string.show) + " ${
                                                if (isShowingSynchronizedLyrics) stringResource(
                                                    R.string.unsynchronized_lyrics
                                                ) else stringResource(R.string.synchronized_lyrics)
                                            }",
                                            secondaryText = if (isShowingSynchronizedLyrics) null else stringResource(
                                                R.string.provided_by
                                            ) + " kugou.com and LrcLib.net",
                                            onClick = {
                                                menuState.hide()
                                                isShowingSynchronizedLyrics =
                                                    !isShowingSynchronizedLyrics
                                            }
                                        )

                                        MenuEntry(
                                            icon = R.drawable.title_edit,
                                            text = stringResource(R.string.edit_lyrics),
                                            onClick = {
                                                menuState.hide()
                                                isEditing = true
                                            }
                                        )

                                        MenuEntry(
                                            icon = R.drawable.copy,
                                            text = stringResource(R.string.copy_lyrics),
                                            onClick = {
                                                menuState.hide()
                                                copyToClipboard = true
                                            }
                                        )

                                        MenuEntry(
                                            icon = R.drawable.share_social,
                                            text = "Share lyrics card",
                                            enabled = !text.isNullOrBlank(),
                                            onClick = {
                                                menuState.hide()
                                                shareSliceStartLine = 0f
                                                showShareCardPreview = true
                                            }
                                        )

                                        MenuEntry(
                                            icon = R.drawable.search,
                                            text = stringResource(R.string.search_lyrics_online),
                                            onClick = {
                                                menuState.hide()
                                                val mediaMetadata = mediaMetadataProvider()

                                                try {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_WEB_SEARCH).apply {
                                                            putExtra(
                                                                SearchManager.QUERY,
                                                                "${cleanPrefix(mediaMetadata.title.toString())} ${mediaMetadata.artist} lyrics"
                                                            )
                                                        }
                                                    )
                                                } catch (e: ActivityNotFoundException) {
                                                    Toaster.e( R.string.info_not_find_app_browse_internet )
                                                }
                                            }
                                        )

                                        MenuEntry(
                                            icon = R.drawable.sync,
                                            text = stringResource(R.string.fetch_lyrics_again),
                                            enabled = lyrics != null,
                                            onClick = {
                                                menuState.hide()
                                                Database.asyncTransaction {
                                                    lyricsTable.upsert(
                                                        Lyrics(
                                                            songId = mediaId,
                                                            fixed = if (isShowingSynchronizedLyrics) lyrics?.fixed else null,
                                                            synced = if (isShowingSynchronizedLyrics) null else lyrics?.synced,
                                                        )
                                                    )
                                                }
                                            }
                                        )

                                        if (isShowingSynchronizedLyrics) {
                                            MenuEntry(
                                                icon = R.drawable.sync,
                                                text = stringResource(R.string.pick_from) + " LrcLib.net",
                                                onClick = {
                                                    menuState.hide()
                                                    isPicking = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        .padding(all = 8.dp)
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                )
            }
        }
    }
}


/*@Composable
fun SelectLyricFromTrack(
    tracks: List<Track>,
    mediaId: String,
    lyrics: Lyrics?
) {
    val menuState = LocalMenuState.current

    menuState.display {
        Menu {
            MenuEntry(
                icon = R.drawable.chevron_back,
                text = stringResource(R.string.cancel),
                onClick = { menuState.hide() }
            )
            tracks.forEach {
                MenuEntry(
                    icon = R.drawable.text,
                    text = "${it.artistName} - ${it.trackName}",
                    secondaryText = "(${stringResource(R.string.sort_duration)} ${
                        it.duration.seconds.toComponents { minutes, seconds, _ ->
                            "$minutes:${seconds.toString().padStart(2, '0')}"
                        }
                    } ${stringResource(R.string.id)} ${it.id}) ",
                    onClick = {
                        menuState.hide()
                        Database.asyncTransaction {
                            upsert(
                                Lyrics(
                                    songId = mediaId,
                                    fixed = lyrics?.fixed,
                                    synced = it.syncedLyrics.orEmpty()
                                )
                            )
                        }
                    }
                )
            }
            MenuEntry(
                icon = R.drawable.chevron_back,
                text = stringResource(R.string.cancel),
                onClick = { menuState.hide() }
            )
        }
    }
}*/

private val LyricsShareHeadlineFont = FontFamily(
    Font(R.font.poppins_w600, FontWeight.SemiBold),
    Font(R.font.poppins_w700, FontWeight.Bold)
)

private val LyricsShareBodyFont = FontFamily(
    Font(R.font.poppins_w500, FontWeight.Medium),
    Font(R.font.poppins_w600, FontWeight.SemiBold)
)

@Composable
private fun LyricsSharePreviewCard(
    title: String,
    artist: String,
    lyricsSnippet: String,
    artworkUrl: String?,
    deeplinkUrl: String,
    modifier: Modifier = Modifier
) {
    val previewBackground = Brush.verticalGradient(
        colors = listOf(
            colorPalette().accent.copy(alpha = 0.38f),
            colorPalette().background0.copy(alpha = 0.96f),
            colorPalette().background1
        )
    )

    val displayedLyrics = lyricsSnippet
        .lines()
        .take(6)
        .joinToString("\n")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(previewBackground)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(colorPalette().background1.copy(alpha = 0.72f))
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 6.dp)
                ) {
                    BasicText(
                        text = title.ifBlank { "Unknown title" },
                        style = TextStyle(
                            color = colorPalette().text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = LyricsShareHeadlineFont
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        text = artist.ifBlank { "Unknown artist" },
                        style = TextStyle(
                            color = colorPalette().textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = LyricsShareBodyFont
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            BasicText(
                text = displayedLyrics,
                style = TextStyle(
                    color = colorPalette().text,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LyricsShareHeadlineFont,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.15f),
                        offset = Offset(0f, 2f),
                        blurRadius = 10f
                    )
                ),
                modifier = Modifier.padding(top = 18.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    BasicText(
                        text = "Cubic Music",
                        style = TextStyle(
                            color = colorPalette().text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = LyricsShareBodyFont
                        )
                    )
                }
            }
        }
    }
}
