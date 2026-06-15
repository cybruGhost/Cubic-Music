package app.it.fast4x.rimusic.ui.screens.player

import android.annotation.SuppressLint
import android.graphics.RenderEffect
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import app.kreate.android.me.knighthat.coil.thumbnail
import app.kreate.android.me.knighthat.coil.ImageCacheFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.util.fastZip
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils.colorToHSL
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import app.kreate.android.R
import app.kreate.android.drawable.APP_ICON_IMAGE_BITMAP
import app.kreate.android.screens.player.background.BlurredCover
import app.kreate.android.themed.rimusic.screen.player.ActionBar
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.BlackCherryCosmos
import com.mikepenz.hypnoticcanvas.shaders.GlossyGradients
import com.mikepenz.hypnoticcanvas.shaders.GoldenMagma
import com.mikepenz.hypnoticcanvas.shaders.GradientFlow
import com.mikepenz.hypnoticcanvas.shaders.IceReflection
import com.mikepenz.hypnoticcanvas.shaders.InkFlow
import com.mikepenz.hypnoticcanvas.shaders.MeshGradient
import com.mikepenz.hypnoticcanvas.shaders.MesmerizingLens
import com.mikepenz.hypnoticcanvas.shaders.OilFlow
import com.mikepenz.hypnoticcanvas.shaders.PurpleLiquid
import com.mikepenz.hypnoticcanvas.shaders.Shader
import com.mikepenz.hypnoticcanvas.shaders.Stage
import app.it.fast4x.rimusic.Database
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.appRunningInBackground
import app.it.fast4x.rimusic.cleanPrefix
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.AnimatedGradient
import app.it.fast4x.rimusic.enums.BackgroundProgress
import app.it.fast4x.rimusic.enums.CarouselSize
import app.it.fast4x.rimusic.enums.ColorPaletteMode
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.enums.PlayerBackgroundColors
import app.it.fast4x.rimusic.enums.PlayerThumbnailSize
import app.it.fast4x.rimusic.enums.PlayerType
import app.it.fast4x.rimusic.enums.QueueLoopType
import app.it.fast4x.rimusic.enums.QueueType
import app.it.fast4x.rimusic.enums.SwipeAnimationNoThumbnail
import app.it.fast4x.rimusic.enums.ThumbnailCoverType
import app.it.fast4x.rimusic.enums.ThumbnailRoundness
import app.it.fast4x.rimusic.enums.ThumbnailType
import app.it.fast4x.rimusic.models.Info
import app.it.fast4x.rimusic.models.ui.toUiMedia
import app.it.fast4x.rimusic.service.modern.isLocal
import app.it.fast4x.rimusic.thumbnailShape
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.CustomModalBottomSheet
import app.it.fast4x.rimusic.ui.components.LocalMenuState
import app.it.fast4x.rimusic.ui.components.themed.CircularSlider
import app.it.fast4x.rimusic.ui.components.themed.ConfirmationDialog
import app.it.fast4x.rimusic.ui.components.themed.DefaultDialog
import app.it.fast4x.rimusic.ui.components.themed.IconButton
import app.it.fast4x.rimusic.ui.components.themed.NowPlayingSongIndicator
import app.it.fast4x.rimusic.ui.components.themed.PlayerMenu
import app.it.fast4x.rimusic.ui.components.themed.RotateThumbnailCoverAnimationModern
import app.it.fast4x.rimusic.ui.components.themed.SecondaryTextButton
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import app.it.fast4x.rimusic.ui.components.themed.ThumbnailOffsetDialog
import app.it.fast4x.rimusic.ui.components.themed.animateBrushRotation
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.ui.styling.collapsedPlayerProgressBar
import app.it.fast4x.rimusic.ui.styling.dynamicColorPaletteOf
import app.it.fast4x.rimusic.ui.styling.favoritesOverlay
import app.it.fast4x.rimusic.utils.DisposableListener
import app.it.fast4x.rimusic.utils.SearchYoutubeEntity
import app.it.fast4x.rimusic.utils.SecureApiConfig
import app.it.fast4x.rimusic.utils.playerVideoModeActiveKey
import app.it.fast4x.rimusic.utils.VerticalfadingEdge2
import app.it.fast4x.rimusic.utils.VinylSizeKey
import app.it.fast4x.rimusic.utils.albumCoverRotationKey
import app.it.fast4x.rimusic.utils.animatedGradientKey
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.backgroundProgressKey
import app.it.fast4x.rimusic.utils.blackgradientKey
import app.it.fast4x.rimusic.utils.bottomgradientKey
import app.it.fast4x.rimusic.utils.carouselKey
import app.it.fast4x.rimusic.utils.carouselSizeKey
import app.it.fast4x.rimusic.utils.clickOnLyricsTextKey
import app.it.fast4x.rimusic.utils.colorPaletteModeKey
import app.it.fast4x.rimusic.utils.controlsExpandedKey
import app.it.fast4x.rimusic.utils.coverThumbnailAnimationKey
import app.it.fast4x.rimusic.utils.currentWindow
import app.it.fast4x.rimusic.utils.disablePlayerHorizontalSwipeKey
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.discoverKey
import app.it.fast4x.rimusic.utils.doubleShadowDrop
import app.it.fast4x.rimusic.utils.durationTextToMillis
import app.it.fast4x.rimusic.utils.effectRotationKey
import app.it.fast4x.rimusic.utils.expandedplayerKey
import app.it.fast4x.rimusic.utils.extraspaceKey
import app.it.fast4x.rimusic.utils.fadingedgeKey
import app.it.fast4x.rimusic.utils.formatAsDuration
import app.it.fast4x.rimusic.utils.formatAsTime
import app.it.fast4x.rimusic.utils.getBitmapFromUrl
import app.it.fast4x.rimusic.utils.horizontalFadingEdge
import app.it.fast4x.rimusic.utils.isExplicit
import app.it.fast4x.rimusic.utils.isLandscape
import app.it.fast4x.rimusic.utils.mediaItems
import app.it.fast4x.rimusic.utils.noblurKey
import app.it.fast4x.rimusic.utils.playAtIndex
import app.it.fast4x.rimusic.utils.playNext
import app.it.fast4x.rimusic.utils.playPrevious
import app.it.fast4x.rimusic.utils.playerBackgroundColorsKey
import app.it.fast4x.rimusic.utils.playerThumbnailSizeKey
import app.it.fast4x.rimusic.utils.playerThumbnailSizeLKey
import app.it.fast4x.rimusic.utils.playerTypeKey
import app.it.fast4x.rimusic.utils.positionAndDurationState
import app.it.fast4x.rimusic.utils.queueDurationExpandedKey
import app.it.fast4x.rimusic.utils.queueLoopTypeKey
import app.it.fast4x.rimusic.utils.queueTypeKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.shouldBePlaying
import app.it.fast4x.rimusic.utils.showButtonPlayerMenuKey
import app.it.fast4x.rimusic.utils.showButtonPlayerVideoKey
import app.it.fast4x.rimusic.utils.showCoverThumbnailAnimationKey
import app.it.fast4x.rimusic.utils.showTopActionsBarKey
import app.it.fast4x.rimusic.utils.showTotalTimeQueueKey
import app.it.fast4x.rimusic.utils.showlyricsthumbnailKey
import app.it.fast4x.rimusic.utils.showthumbnailKey
import app.it.fast4x.rimusic.utils.showvisthumbnailKey
import app.it.fast4x.rimusic.utils.statsExpandedKey
import app.it.fast4x.rimusic.utils.statsfornerdsKey
import app.it.fast4x.rimusic.utils.swipeAnimationsNoThumbnailKey
import app.it.fast4x.rimusic.utils.textoutlineKey
import app.it.fast4x.rimusic.utils.thumbnailFadeExKey
import app.it.fast4x.rimusic.utils.thumbnailFadeKey
import app.it.fast4x.rimusic.utils.thumbnailRoundnessKey
import app.it.fast4x.rimusic.utils.thumbnailSpacingKey
import app.it.fast4x.rimusic.utils.thumbnailSpacingLKey
import app.it.fast4x.rimusic.utils.thumbnailTapEnabledKey
import app.it.fast4x.rimusic.utils.thumbnailTypeKey
import app.it.fast4x.rimusic.utils.timelineExpandedKey
import app.it.fast4x.rimusic.utils.titleExpandedKey
import app.it.fast4x.rimusic.utils.topPaddingKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import app.kreate.android.me.knighthat.component.player.BlurAdjuster
import app.kreate.android.me.knighthat.utils.Toaster
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.searchPage
import it.fast4x.innertube.utils.from
import kotlin.Float.Companion.POSITIVE_INFINITY
import kotlin.math.absoluteValue
import kotlin.math.sqrt
import app.it.fast4x.rimusic.ui.screens.spotify.SpotifyCanvasWorker
import app.it.fast4x.rimusic.ui.screens.spotify.SpotifyCanvasState
import androidx.compose.ui.viewinterop.AndroidView
import app.it.fast4x.rimusic.utils.FadeAdjuster
import app.it.fast4x.rimusic.enums.DurationInMilliseconds
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import app.it.fast4x.rimusic.ui.screens.spotify.LogEntry
import app.it.fast4x.rimusic.ui.screens.spotify.LogType
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.common.MimeTypes
import app.it.fast4x.rimusic.ui.screens.spotify.CanvasPlayerManager
import androidx.compose.material3.CircularProgressIndicator
import app.it.fast4x.rimusic.service.MyDownloadHelper
import timber.log.Timber

// ─────────────────────────────────────────────────────────────
// FIX 1 helper: network availability check.
// Called before ANY network I/O so we never throw
// UnknownHostException / IOException into the player.
// ─────────────────────────────────────────────────────────────
private fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

// ─────────────────────────────────────────────────────────────
// FIX 2 helper: safe progress fraction for drawBehind.
// Prevents divide-by-zero / NaN / near-infinite floats
// when duration is C.TIME_UNSET or 0 during buffering.
// ─────────────────────────────────────────────────────────────
private fun safeProgressFraction(positionMs: Long, durationMs: Long): Float {
    if (durationMs <= 0L || durationMs == C.TIME_UNSET) return 0f
    if (positionMs <= 0L || positionMs == C.TIME_UNSET) return 0f
    return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation", "RememberReturnType", "NewApi")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
fun Player(
    navController: NavController,
    onDismiss: () -> Unit,
) {
    PlayerContent(
        navController = navController,
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation", "RememberReturnType", "NewApi")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@UnstableApi
@Composable
private fun PlayerContent(
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val menuState = LocalMenuState.current
    val binder = LocalPlayerServiceBinder.current ?: return
    val uiConfig = rememberPlayerUiConfig()
    val disablePlayerHorizontalSwipe = uiConfig.disablePlayerHorizontalSwipe
    val showlyricsthumbnail = uiConfig.showlyricsthumbnail
    val effectRotationEnabled = uiConfig.effectRotationEnabled
    val playerThumbnailSize by uiConfig.playerThumbnailSizeState
    var playerThumbnailSizeL by uiConfig.playerThumbnailSizeLState
    val showvisthumbnail = uiConfig.showvisthumbnail
    var thumbnailSpacing by uiConfig.thumbnailSpacingState
    var thumbnailSpacingL by uiConfig.thumbnailSpacingLState
    var thumbnailFade by uiConfig.thumbnailFadeState
    var thumbnailFadeEx by uiConfig.thumbnailFadeExState
    var imageCoverSize by uiConfig.imageCoverSizeState
    val queueDurationExpanded = uiConfig.queueDurationExpanded
    val statsExpanded = uiConfig.statsExpanded
    var showthumbnail by uiConfig.showthumbnailState
    val showButtonPlayerMenu = uiConfig.showButtonPlayerMenu
    val showTotalTimeQueue = uiConfig.showTotalTimeQueue
    val backgroundProgress = uiConfig.backgroundProgress
    val queueLoopState = uiConfig.queueLoopState
    val playerType = uiConfig.playerType
    val queueType = uiConfig.queueType
    val noblur = uiConfig.noblur
    val fadingedge = uiConfig.fadingedge
    val colorPaletteMode = uiConfig.colorPaletteMode
    val playerBackgroundColors = uiConfig.playerBackgroundColors
    val animatedGradient = uiConfig.animatedGradient
    val thumbnailTapEnabled = uiConfig.thumbnailTapEnabled
    val showTopActionsBar = uiConfig.showTopActionsBar
    val blackgradient = uiConfig.blackgradient
    val bottomgradient = uiConfig.bottomgradient
    val disableScrollingText = uiConfig.disableScrollingText
    val discoverState = uiConfig.discoverState
    val titleExpanded = uiConfig.titleExpanded
    val timelineExpanded = uiConfig.timelineExpanded
    val controlsExpanded = uiConfig.controlsExpanded
    val showCoverThumbnailAnimation = uiConfig.showCoverThumbnailAnimation
    var coverThumbnailAnimation by uiConfig.coverThumbnailAnimationState
    var albumCoverRotation by uiConfig.albumCoverRotationState
    val textoutline = uiConfig.textoutline
    val carousel = uiConfig.carousel
    val carouselSize = uiConfig.carouselSize
    val clickLyricsText = uiConfig.clickLyricsText
    var extraspace by uiConfig.extraspaceState
    val thumbnailRoundness = uiConfig.thumbnailRoundness
    val thumbnailType = uiConfig.thumbnailType
    val statsfornerds = uiConfig.statsfornerds
    val topPadding = uiConfig.topPadding
    var swipeAnimationNoThumbnail by uiConfig.swipeAnimationNoThumbnailState
    val expandPlayerState = uiConfig.expandedPlayerState
    var expandedplayer by expandPlayerState
    val spotifyCanvasEnabled = uiConfig.spotifyCanvasEnabled
    var playerVideoModeActive by rememberPreference(playerVideoModeActiveKey, false)
    val showButtonPlayerVideo by rememberPreference(showButtonPlayerVideoKey, false)
    val showSpotifyCanvasLogs = uiConfig.showSpotifyCanvasLogs
    val alternateSourceRetryEnabled = uiConfig.alternateSourceRetryEnabled
    val playbackFadeAudioDuration = uiConfig.playbackFadeAudioDuration
    val blurAdjuster = BlurAdjuster()

    val fadeAdjuster = FadeAdjuster()
    fadeAdjuster.setContext(context)
    val currentSongDownloadState by binder.service.currentSongStateDownload.collectAsState()

    LaunchedEffect(playbackFadeAudioDuration) {
        fadeAdjuster.setDuration(playbackFadeAudioDuration.milliSeconds)
    }

    if (binder.player.currentTimeline.windowCount == 0) return

    val displayedPlayerState = rememberDisplayedPlayerState(binder)
    val shouldBePlaying = displayedPlayerState.shouldBePlaying

    // ── FIX 3: isBuffering comes from the single source of truth (DisplayState),
    //           not a second competing playbackStateState() flow.
    val isBuffering = displayedPlayerState.isBuffering

    val rotateState = rememberSaveable { mutableStateOf(false) }
    var isRotated by rotateState
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRotated) 360F else 0f,
        animationSpec = tween(durationMillis = 200), label = ""
    )

    val showQueueState = rememberSaveable { mutableStateOf(false) }
    var showQueue by showQueueState

    val showSearchEntityState = rememberSaveable { mutableStateOf(false) }
    var showSearchEntity by showSearchEntityState

    val showVisualizerState = rememberSaveable { mutableStateOf(false) }
    var isShowingVisualizer by showVisualizerState

    val showSleepTimerState = rememberSaveable { mutableStateOf(false) }
    var isShowingSleepTimerDialog by showSleepTimerState

    val showLyricsState = rememberSaveable { mutableStateOf(false) }
    var isShowingLyrics by showLyricsState

    var shouldRememberVisualizerState by rememberPreference("showVisualizerStateKey", false)
    var shouldRememberLyricsState by rememberPreference("showLyricsStateKey", false)
    var savedVisualizerState by rememberPreference("saveVisualizerStateKey", false)
    var savedLyricsState by rememberPreference("saveLyricsStateKey", false)

    LaunchedEffect(Unit) {
        if (shouldRememberVisualizerState) isShowingVisualizer = savedVisualizerState
        if (shouldRememberLyricsState) isShowingLyrics = savedLyricsState
    }

    LaunchedEffect(isShowingVisualizer) {
        if (shouldRememberVisualizerState) {
            savedVisualizerState = isShowingVisualizer
            if (isShowingVisualizer) isShowingLyrics = false
        }
    }

    LaunchedEffect(isShowingLyrics) {
        if (shouldRememberLyricsState) {
            savedLyricsState = isShowingLyrics
            if (isShowingLyrics) isShowingVisualizer = false
        }
    }

    var showThumbnailOffsetDialog by rememberSaveable { mutableStateOf(false) }

    if (showThumbnailOffsetDialog) {
        ThumbnailOffsetDialog(
            onDismiss = { showThumbnailOffsetDialog = false },
            spacingValue = { thumbnailSpacing = it },
            spacingValueL = { thumbnailSpacingL = it },
            fadeValue = { thumbnailFade = it },
            fadeValueEx = { thumbnailFadeEx = it },
            imageCoverSizeValue = { imageCoverSize = it }
        )
    }

    var mediaItems by remember {
        mutableStateOf(binder.player.currentTimeline.mediaItems)
    }
    var playerError by remember {
        mutableStateOf<PlaybackException?>(binder.player.playerError)
    }
    var retryWithAlternateSourcesNonce by remember { mutableIntStateOf(0) }
    var lastSearchFallbackMediaId by remember { mutableStateOf<String?>(null) }
    var playbackErrorMessage by remember { mutableStateOf<String?>(null) }
    val hasNetworkConnection = isNetworkAvailable(context)
    val currentErrorItem = binder.displayedMediaItem ?: binder.player.currentMediaItem
    val isCurrentSongDownloaded = currentSongDownloadState == Download.STATE_COMPLETED

    fun PagerState.offsetForPage(page: Int) = (currentPage - page) + currentPageOffsetFraction

    fun PagerState.startOffsetForPage(page: Int): Float {
        return offsetForPage(page).coerceAtLeast(0f)
    }

    fun PagerState.endOffsetForPage(page: Int): Float {
        return offsetForPage(page).coerceAtMost(0f)
    }

    class CirclePath(private val progress: Float, private val origin: Offset = Offset(0f, 0f)) : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val center = Offset(
                x = (size.width / 2f) - (((size.width / 2f) - origin.x) * (1f - progress)),
                y = (size.height / 2f) - (((size.height / 2f) - origin.y) * (1f - progress)),
            )
            val radius = (sqrt(size.height * size.height + size.width * size.width) * .5f) * progress
            return Outline.Generic(Path().apply {
                addOval(Rect(center = center, radius = radius))
            })
        }
    }

    // ── FIX 5 (mid-stream network loss): onPlayerError now shows the playback
    //   error UI instead of letting the exception propagate and crash the player.
    //   The player is NOT released here — ExoPlayer handles retries internally,
    //   and the user can manually retry via the error banner.
    binder.player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                playbackErrorMessage = null
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                playerError = binder.player.playerError
                if (playerError == null && playbackState == Player.STATE_READY) {
                    playbackErrorMessage = null
                }
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                mediaItems = timeline.mediaItems
            }

            override fun onPlayerError(playbackException: PlaybackException) {
                playerError = playbackException
                // Surface the error gracefully — never crash, just show the banner.
                playbackErrorMessage = playbackExceptionMessage(
                    context = context,
                    error = playbackException,
                    isLocal = binder.player.currentWindow?.mediaItem?.isLocal == true,
                    isDownloaded = isCurrentSongDownloaded,
                    isNetworkAvailable = hasNetworkConnection,
                )
                // If the error is a network error and we were mid-stream, ExoPlayer
                // will retry automatically when the network comes back. We don't
                // need to do anything here except show the error UI.
            }
        }
    }

    // ── FIX 1: Guard ALL network calls with isNetworkAvailable().
    //   Also catches the mid-stream case: if network dropped while a song was
    //   half-buffered, playerError fires. We check connectivity first — if still
    //   offline, we skip the search and let ExoPlayer retry on its own.
    LaunchedEffect(playerError, alternateSourceRetryEnabled, retryWithAlternateSourcesNonce) {
        if (false && alternateSourceRetryEnabled && playerError != null) {
            val currentItem = binder.displayedMediaItem ?: binder.player.currentMediaItem ?: return@LaunchedEffect
            if (currentItem.isLocal || isCurrentSongDownloaded) return@LaunchedEffect

            // ── FIX 1: Do NOT attempt any HTTP if we are offline.
            //   This prevents UnknownHostException / IOException from crashing
            //   the player when the network drops mid-stream.
            if (!isNetworkAvailable(context)) return@LaunchedEffect

            try {
                val currentMediaId = currentItem.mediaId

                if (currentMediaId == lastSearchFallbackMediaId) return@LaunchedEffect

                val title = cleanPrefix(currentItem.mediaMetadata.title?.toString().orEmpty()).trim()
                val artist = cleanPrefix(currentItem.mediaMetadata.artist?.toString().orEmpty()).trim()

                if (title.isBlank()) return@LaunchedEffect

                fun normalizeMatchText(value: String): String =
                    cleanPrefix(value)
                        .lowercase()
                        .replace(Regex("\\b(official|music video|video|audio|lyrics|visualizer|topic|vevo|hd|4k)\\b"), " ")
                        .replace(Regex("[^a-z0-9]+"), " ")
                        .trim()
                        .replace(Regex("\\s+"), " ")

                fun scoreCandidate(candidateTitle: String, candidateArtist: String): Int {
                    val expectedTitle = normalizeMatchText(title)
                    val expectedArtist = normalizeMatchText(artist)
                    val normalizedCandidateTitle = normalizeMatchText(candidateTitle)
                    val normalizedCandidateArtist = normalizeMatchText(candidateArtist)

                    if (normalizedCandidateTitle.isBlank()) return Int.MIN_VALUE
                    if (
                        normalizedCandidateTitle.contains("mix") ||
                        normalizedCandidateTitle.contains("playlist") ||
                        normalizedCandidateTitle.contains("full album")
                    ) return Int.MIN_VALUE

                    var score = 0
                    if (normalizedCandidateTitle == expectedTitle) score += 120
                    else if (
                        normalizedCandidateTitle.contains(expectedTitle) ||
                        expectedTitle.contains(normalizedCandidateTitle)
                    ) score += 80
                    else {
                        val expectedTokens = expectedTitle.split(" ").filter { it.length > 1 }.toSet()
                        val candidateTokens = normalizedCandidateTitle.split(" ").filter { it.length > 1 }.toSet()
                        score += expectedTokens.intersect(candidateTokens).size * 18
                    }

                    if (expectedArtist.isNotBlank()) {
                        if (normalizedCandidateArtist == expectedArtist) score += 70
                        else if (
                            normalizedCandidateArtist.contains(expectedArtist) ||
                            expectedArtist.contains(normalizedCandidateArtist)
                        ) score += 45
                        else {
                            val expectedArtistTokens = expectedArtist.split(" ").filter { it.length > 1 }.toSet()
                            val candidateArtistTokens = normalizedCandidateArtist.split(" ").filter { it.length > 1 }.toSet()
                            score += expectedArtistTokens.intersect(candidateArtistTokens).size * 14
                        }
                    }
                    return score
                }

                val queries = buildList {
                    add(title)
                    if (artist.isNotBlank()) add("$title $artist")
                    if (artist.isNotBlank()) add("$artist - $title")
                }.distinct()

                suspend fun findReplacement(): MediaItem? {
                    // Re-check network before each I/O call inside findReplacement.
                    if (!isNetworkAvailable(context)) return null

                    var bestCandidate: Pair<MediaItem, Int>? = null

                    for (query in queries) {
                        if (!isNetworkAvailable(context)) break

                        val songPage = runCatching {
                            Innertube.searchPage(
                                body = SearchBody(query = query, params = Innertube.SearchFilter.Song.value),
                                fromMusicShelfRendererContent = { content -> Innertube.SongItem.from(content) }
                            )?.getOrNull()
                        }.getOrNull()

                        songPage?.items
                            ?.filterIsInstance<Innertube.SongItem>()
                            ?.forEach { item ->
                                if (item.key.isBlank() || item.key == currentMediaId) return@forEach
                                val score = scoreCandidate(
                                    candidateTitle = item.info?.name.orEmpty(),
                                    candidateArtist = item.authors?.joinToString(", ") { it.name.orEmpty() }.orEmpty()
                                )
                                if (score > (bestCandidate?.second ?: Int.MIN_VALUE)) {
                                    bestCandidate = item.asMediaItem to score
                                }
                            }

                        if (!isNetworkAvailable(context)) break

                        val videoPage = runCatching {
                            Innertube.searchPage(
                                body = SearchBody(query = query, params = Innertube.SearchFilter.Video.value),
                                fromMusicShelfRendererContent = { content -> Innertube.VideoItem.from(content) }
                            )?.getOrNull()
                        }.getOrNull()

                        videoPage?.items
                            ?.filterIsInstance<Innertube.VideoItem>()
                            ?.forEach { item ->
                                if (item.key.isBlank() || item.key == currentMediaId) return@forEach
                                val score = scoreCandidate(
                                    candidateTitle = item.info?.name.orEmpty(),
                                    candidateArtist = item.authors?.joinToString(", ") { it.name.orEmpty() }.orEmpty()
                                )
                                if (score > (bestCandidate?.second ?: Int.MIN_VALUE)) {
                                    bestCandidate = item.asMediaItem to score
                                }
                            }
                    }

                    bestCandidate?.takeIf { it.second >= 70 }?.let { return it.first }

                    for (query in queries) {
                        if (!isNetworkAvailable(context)) break

                        val encodedQuery = URLEncoder.encode(query, "UTF-8")
                        val result = runCatching {
                            val connection = URL("${SecureApiConfig.resolveOmadaSearchApi()}?q=$encodedQuery")
                                .openConnection() as HttpURLConnection
                            connection.requestMethod = "GET"
                            connection.connectTimeout = 8000
                            connection.readTimeout = 8000

                            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null

                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            val results = JSONArray(response)

                            var found: MediaItem? = null
                            for (index in 0 until results.length()) {
                                val item = results.optJSONObject(index) ?: continue
                                if (item.optString("type") != "video") continue
                                val candidateVideoId = item.optString("videoId").trim()
                                if (candidateVideoId.isBlank() || candidateVideoId == currentMediaId) continue
                                val score = scoreCandidate(
                                    candidateTitle = item.optString("title"),
                                    candidateArtist = item.optString("author")
                                )
                                if (score < 60) continue
                                val metadata = MediaMetadata.Builder()
                                    .setTitle(currentItem.mediaMetadata.title)
                                    .setArtist(currentItem.mediaMetadata.artist)
                                    .setAlbumTitle(currentItem.mediaMetadata.albumTitle)
                                    .setArtworkUri(currentItem.mediaMetadata.artworkUri?.toString()?.toUri())
                                    .setExtras(currentItem.mediaMetadata.extras)
                                    .build()
                                found = MediaItem.Builder()
                                    .setMediaId(candidateVideoId)
                                    .setUri(candidateVideoId)
                                    .setCustomCacheKey(candidateVideoId)
                                    .setMediaMetadata(metadata)
                                    .build()
                                break
                            }
                            found
                        }.getOrNull()

                        if (result != null) return result
                    }

                    return null
                }

                val replacementItem = withContext(Dispatchers.IO) {
                    findReplacement()
                } ?: return@LaunchedEffect
                lastSearchFallbackMediaId = currentMediaId

                val wasPlaying = binder.player.isPlaying
                if (!runCatching { binder.player.setMediaItem(replacementItem) }.isSuccess) return@LaunchedEffect
                if (!runCatching { binder.player.prepare() }.isSuccess) return@LaunchedEffect
                if (wasPlaying) {
                    runCatching { binder.player.play() }
                }
                Toaster.n("Retrying with alternate YouTube source")

            } catch (e: Exception) {
                // Swallow all exceptions here — we must never crash the player.
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(retryWithAlternateSourcesNonce) {
        if (retryWithAlternateSourcesNonce <= 0 || playerError == null) return@LaunchedEffect
        val wasPlaying = binder.player.playWhenReady || binder.player.isPlaying
        runCatching { binder.player.prepare() }
            .onSuccess {
                if (wasPlaying) runCatching { binder.player.play() }
                playbackErrorMessage = null
                playerError = null
            }
            .onFailure { Timber.e(it, "Manual playback retry failed for mediaId=%s", binder.player.currentMediaItem?.mediaId) }
    }

    val pagerState = rememberPagerState(pageCount = { mediaItems.size })
    val pagerStateFS = rememberPagerState(pageCount = { mediaItems.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val isDraggedFS by pagerStateFS.interactionSource.collectIsDraggedAsState()

    var delayedSleepTimer by remember { mutableStateOf(false) }

    val sleepTimerMillisLeft by (binder.sleepTimerMillisLeft ?: flowOf(null))
        .collectAsState(initial = null)

    val displayedPositionAndDuration = displayedPlayerState.position to displayedPlayerState.duration
    val mediaItem = displayedPlayerState.mediaItem ?: return

    val displayedMediaItemIndex = remember(
        mediaItems,
        mediaItem.mediaId,
        binder.player.currentMediaItemIndex
    ) {
        mediaItems.indexOfFirst { queuedItem -> queuedItem.mediaId == mediaItem.mediaId }
            .takeIf { it >= 0 }
            ?: binder.player.currentMediaItemIndex.coerceAtLeast(0)
    }

    val displayedArtworkUrl = mediaItem.mediaMetadata.artworkUri?.toString().orEmpty()

    // ── FIX 4: Safe index guard — mediaItemCount can be 0 during song transitions.
    fun queuedMediaItemAt(index: Int): MediaItem {
        return mediaItems.getOrNull(index)
            ?: run {
                val count = binder.player.mediaItemCount
                if (count > 0 && index in 0 until count)
                    binder.player.getMediaItemAt(index)
                else
                    mediaItem
            }
    }

    var timeRemaining by remember { mutableIntStateOf(0) }
    // Safe subtraction: both values are already guarded in DisplayState
    timeRemaining = (displayedPositionAndDuration.second - displayedPositionAndDuration.first)
        .coerceAtLeast(0L).toInt()

    if (sleepTimerMillisLeft != null)
        if (sleepTimerMillisLeft!! < timeRemaining.toLong() && !delayedSleepTimer) {
            binder.cancelSleepTimer()
            binder.startSleepTimer(timeRemaining.toLong())
            delayedSleepTimer = true
            Toaster.n(R.string.info_sleep_timer_delayed_at_end_of_song)
        }

    val windowInsets = WindowInsets.systemBars

    var updateBrush by remember { mutableStateOf(false) }

    if (showlyricsthumbnail) expandedplayer = false

    LaunchedEffect(mediaItem.mediaId) {
        updateBrush = true
    }

    val artistInfos by remember(mediaItem) {
        val ids = mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds").orEmpty()
        val names = mediaItem.mediaMetadata.extras?.getStringArrayList("artistNames").orEmpty()
        if (ids.isNotEmpty())
            return@remember flowOf(ids.fastZip(names) { id, name -> Info(id, name) })
        Database.songArtistMapTable
            .findArtistsOf(mediaItem.mediaId)
            .distinctUntilChanged()
            .map { list -> list.map { Info(it.id, it.name) } }
    }.collectAsState(emptyList(), Dispatchers.IO)

    val albumId by remember(mediaItem) {
        val result = mediaItem.mediaMetadata.extras?.getString("albumId")
        if (!result.isNullOrBlank()) return@remember flowOf(result)
        Database.songAlbumMapTable.findAlbumOf(mediaItem.mediaId).map { it?.id }
    }.collectAsState(null, Dispatchers.IO)

    var showCircularSlider by remember { mutableStateOf(false) }
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    if (isShowingSleepTimerDialog) {
        if (sleepTimerMillisLeft != null) {
            ConfirmationDialog(
                text = stringResource(R.string.stop_sleep_timer),
                cancelText = stringResource(R.string.no),
                confirmText = stringResource(R.string.stop),
                onDismiss = { isShowingSleepTimerDialog = false },
                onConfirm = {
                    val fadeDuration = playbackFadeAudioDuration.milliSeconds
                    if (fadeDuration > 0) {
                        fadeAdjuster.fadeOut(binder.player) {
                            binder.cancelSleepTimer()
                            delayedSleepTimer = false
                        }
                    } else {
                        binder.cancelSleepTimer()
                        delayedSleepTimer = false
                    }
                }
            )
        } else {
            DefaultDialog(onDismiss = { isShowingSleepTimerDialog = false }) {
                var amount by remember { mutableStateOf(1) }

                BasicText(
                    text = stringResource(R.string.set_sleep_timer),
                    style = typography().s.semiBold,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 16.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    modifier = Modifier.padding(vertical = 10.dp)
                ) {
                    if (!showCircularSlider) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .alpha(if (amount <= 1) 0.5f else 1f)
                                .clip(CircleShape)
                                .clickable(enabled = amount > 1) { amount-- }
                                .size(48.dp)
                                .background(colorPalette().background0)
                        ) {
                            BasicText(text = "-", style = typography().xs.semiBold)
                        }

                        Box(contentAlignment = Alignment.Center) {
                            BasicText(
                                text = stringResource(R.string.left, formatAsDuration(amount * 5 * 60 * 1000L)),
                                style = typography().s.semiBold,
                                modifier = Modifier.clickable { showCircularSlider = !showCircularSlider }
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .alpha(if (amount >= 60) 0.5f else 1f)
                                .clip(CircleShape)
                                .clickable(enabled = amount < 60) { amount++ }
                                .size(48.dp)
                                .background(colorPalette().background0)
                        ) {
                            BasicText(text = "+", style = typography().xs.semiBold)
                        }
                    } else {
                        CircularSlider(
                            stroke = 40f,
                            thumbColor = colorPalette().accent,
                            text = formatAsDuration(amount * 5 * 60 * 1000L),
                            modifier = Modifier.size(300.dp),
                            onChange = { amount = (it * 120).toInt() }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.padding(bottom = 20.dp).fillMaxWidth()
                ) {
                    SecondaryTextButton(
                        text = stringResource(R.string.set_to) + " " +
                                formatAsDuration(timeRemaining.toLong()) + " " +
                                stringResource(R.string.end_of_song),
                        onClick = {
                            binder.startSleepTimer(timeRemaining.toLong())
                            isShowingSleepTimerDialog = false
                        }
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { showCircularSlider = !showCircularSlider },
                        icon = R.drawable.time,
                        color = colorPalette().text
                    )
                    IconButton(
                        onClick = { isShowingSleepTimerDialog = false },
                        icon = R.drawable.close,
                        color = colorPalette().text
                    )
                    IconButton(
                        enabled = amount > 0,
                        onClick = {
                            binder.startSleepTimer(amount * 5 * 60 * 1000L)
                            isShowingSleepTimerDialog = false
                        },
                        icon = R.drawable.checkmark,
                        color = colorPalette().accent
                    )
                }
            }
        }
    }

    val color = colorPalette()
    val progressOverlayBrush: Brush? = null
    var dynamicColorPalette by remember { mutableStateOf(color) }
    var dominant by remember { mutableStateOf(0) }
    var vibrant by remember { mutableStateOf(0) }
    var lightVibrant by remember { mutableStateOf(0) }
    var darkVibrant by remember { mutableStateOf(0) }
    var muted by remember { mutableStateOf(0) }
    var lightMuted by remember { mutableStateOf(0) }
    var darkMuted by remember { mutableStateOf(0) }

    val lightTheme = colorPaletteMode == ColorPaletteMode.Light ||
            (colorPaletteMode == ColorPaletteMode.System && (!isSystemInDarkTheme()))

    fun saturate(color: Int): Color {
        val colorHSL = floatArrayOf(0f, 0f, 0f)
        colorToHSL(color, colorHSL)
        colorHSL[1] = (colorHSL[1] + if (lightTheme || colorHSL[1] < 0.1f) 0f else 0.35f).coerceIn(0f, 1f)
        colorHSL[2] = if (lightTheme) colorHSL[2].coerceIn(0.5f, 1f) else colorHSL[2]
        return Color.hsl(colorHSL[0], colorHSL[1], colorHSL[2])
    }

    var ratio = if (lightTheme) 1f else 0.5f

    fun Color.darkenBy(): Color {
        return copy(red = red * ratio, green = green * ratio, blue = blue * ratio, alpha = alpha)
    }

    val isGradientBackgroundEnabled =
        playerBackgroundColors == PlayerBackgroundColors.ThemeColorGradient ||
                playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient ||
                playerBackgroundColors == PlayerBackgroundColors.AnimatedGradient

    LaunchedEffect(mediaItem.mediaId, updateBrush) {
        if (playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient ||
            playerBackgroundColors == PlayerBackgroundColors.CoverColor ||
            playerBackgroundColors == PlayerBackgroundColors.ThemeColorGradient ||
            playerBackgroundColors == PlayerBackgroundColors.AnimatedGradient || updateBrush
        ) {
            try {
                val imageUrl = mediaItem.mediaMetadata.artworkUri.thumbnail(1000).toString()
                val bitmap = getBitmapFromUrl(context, imageUrl)
                dynamicColorPalette = dynamicColorPaletteOf(bitmap, !lightTheme) ?: color
                val palette = Palette.from(bitmap).generate()
                dominant = palette.getDominantColor(dynamicColorPalette.accent.toArgb())
                vibrant = palette.getVibrantColor(dynamicColorPalette.accent.toArgb())
                lightVibrant = palette.getLightVibrantColor(dynamicColorPalette.accent.toArgb())
                darkVibrant = palette.getDarkVibrantColor(dynamicColorPalette.accent.toArgb())
                muted = palette.getMutedColor(dynamicColorPalette.accent.toArgb())
                lightMuted = palette.getLightMutedColor(dynamicColorPalette.accent.toArgb())
                darkMuted = palette.getDarkMutedColor(dynamicColorPalette.accent.toArgb())
            } catch (e: Exception) {
                dynamicColorPalette = color
            }
        }
    }

    var sizeShader by remember { mutableStateOf(Size.Zero) }

    var totalPlayTimes = 0L
    mediaItems.forEach {
        totalPlayTimes += it.mediaMetadata.extras?.getString("durationText")?.let { it1 ->
            durationTextToMillis(it1)
        }?.toLong() ?: 0
    }

    var isShowingStatsForNerds by rememberSaveable { mutableStateOf(false) }

    var containerModifier = Modifier.padding(bottom = 0.dp)
    var deltaX by remember { mutableStateOf(0f) }

    var valueGrad by remember { mutableStateOf(2) }
    val gradients = enumValues<AnimatedGradient>()
    var tempGradient by remember { mutableStateOf(AnimatedGradient.Linear) }
    var circleOffsetY by remember { mutableStateOf(0f) }

    @Composable
    fun Modifier.conditional(condition: Boolean, modifier: @Composable Modifier.() -> Modifier): Modifier {
        return if (condition) then(modifier(Modifier)) else this
    }

    if (animatedGradient == AnimatedGradient.Random) {
        LaunchedEffect(mediaItem.mediaId) { valueGrad = (2..13).random() }
        tempGradient = gradients[valueGrad]
    }

    LaunchedEffect(playbackFadeAudioDuration) {
        fadeAdjuster.setDuration(playbackFadeAudioDuration.milliSeconds)
    }

    var previousMediaItemId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(mediaItem.mediaId) {
        val fadeDuration = playbackFadeAudioDuration.milliSeconds
        if (fadeDuration > 0 && previousMediaItemId != null && previousMediaItemId != mediaItem.mediaId) {
            fadeAdjuster.fadeOut(binder.player) {
                fadeAdjuster.fadeIn(binder.player, binder.player.volume)
            }
        }
        previousMediaItemId = mediaItem.mediaId
    }

    val previousShouldBePlaying by remember { derivedStateOf { shouldBePlaying } }
    LaunchedEffect(shouldBePlaying) {
        val fadeDuration = playbackFadeAudioDuration.milliSeconds
        if (fadeDuration > 0 && previousShouldBePlaying != shouldBePlaying) {
            if (shouldBePlaying) {
                fadeAdjuster.fadeIn(binder.player, binder.player.volume)
            } else {
                fadeAdjuster.fadeOut(binder.player)
            }
        }
    }

    containerModifier = containerModifier.then(
        rememberPlayerBackgroundModifier(
            isGradientBackgroundEnabled = isGradientBackgroundEnabled,
            playerBackgroundColors = playerBackgroundColors,
            playerType = playerType,
            showthumbnail = showthumbnail,
            albumCoverRotation = albumCoverRotation,
            bottomgradient = bottomgradient,
            colorPaletteMode = colorPaletteMode,
            expandedplayer = expandedplayer,
            isLandscape = isLandscape,
            dynamicColorPalette = dynamicColorPalette,
            basePalette = color,
            blackgradient = blackgradient,
            lightTheme = lightTheme,
            animatedGradient = animatedGradient,
            tempGradient = tempGradient,
            dominant = dominant,
            vibrant = vibrant,
            lightVibrant = lightVibrant,
            darkVibrant = darkVibrant,
            muted = muted,
            lightMuted = lightMuted,
            darkMuted = darkMuted,
            isPlaying = binder.player.isPlaying,
            saturate = { input -> saturate(input) },
            darkenBy = { source -> source.darkenBy() },
        )
    )

    if (!isGradientBackgroundEnabled &&
        playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor &&
        (playerType == PlayerType.Essential || (showthumbnail && !albumCoverRotation))
    ) {
        containerModifier = containerModifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (thumbnailTapEnabled && !showthumbnail) {
                        if (isShowingVisualizer) isShowingVisualizer = false
                        isShowingLyrics = !isShowingLyrics
                    }
                },
                onDoubleClick = {
                    if (!showlyricsthumbnail && !showvisthumbnail) showthumbnail = !showthumbnail
                },
                onLongClick = {
                    blurAdjuster.isActive = showthumbnail || (isShowingLyrics && !isShowingVisualizer) || !noblur
                }
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount -> deltaX = dragAmount },
                    onDragEnd = {
                        if (!disablePlayerHorizontalSwipe && playerType == PlayerType.Essential) {
                            if (deltaX > 5) binder.player.playPrevious()
                            else if (deltaX < -5) binder.player.playNext()
                        }
                    }
                )
            }
    }

    val thumbnailContent: @Composable () -> Unit = {
        var deltaX by remember { mutableStateOf(0f) }

        val isSongLiked by remember(mediaItem.mediaId) {
            Database.songTable.isLiked(mediaItem.mediaId).distinctUntilChanged()
        }.collectAsState(false, Dispatchers.IO)

        Thumbnail(
            thumbnailTapEnabledKey = thumbnailTapEnabled,
            isShowingLyrics = isShowingLyrics,
            onShowLyrics = { isShowingLyrics = it },
            isShowingStatsForNerds = isShowingStatsForNerds,
            onShowStatsForNerds = { isShowingStatsForNerds = it },
            isShowingVisualizer = isShowingVisualizer,
            onShowEqualizer = { isShowingVisualizer = it },
            showthumbnail = showthumbnail,
            onMaximize = {},
            onDoubleTap = {
                val currentMediaItem = binder.displayedMediaItem ?: binder.player.currentMediaItem
                Database.asyncTransaction {
                    if (!isSongLiked)
                        currentMediaItem
                            ?.takeIf { it.mediaId == mediaItem.mediaId }
                            ?.let {
                                insertIgnore(currentMediaItem)
                                songTable.toggleLike(currentMediaItem.mediaId)
                            }
                }
                if (effectRotationEnabled) isRotated = !isRotated
            },
            modifier = Modifier
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount -> deltaX = dragAmount },
                        onDragStart = {},
                        onDragEnd = {
                            if (!disablePlayerHorizontalSwipe && playerType == PlayerType.Essential) {
                                if (deltaX > 5) binder.player.playPrevious()
                                else if (deltaX < -5) binder.player.playNext()
                            }
                        }
                    )
                }
                .padding(all = if (isLandscape) playerThumbnailSizeL.size.dp else playerThumbnailSize.size.dp)
                .thumbnailpause(shouldBePlaying = shouldBePlaying)
        )
    }

    @Composable
    fun Controller(mediaItem: MediaItem, modifier: Modifier, isBuffering: Boolean) {
        Controls(
            navController = navController,
            onCollapse = onDismiss,
            onBlurScaleChange = { strength -> blurAdjuster.strength = strength },
            expandedplayer = expandedplayer,
            titleExpanded = titleExpanded,
            timelineExpanded = timelineExpanded,
            controlsExpanded = controlsExpanded,
            isShowingLyrics = isShowingLyrics,
            mediaItem = mediaItem,
            artistIds = artistInfos,
            albumId = albumId,
            shouldBePlaying = shouldBePlaying,
            isBuffering = isBuffering,
            positionAndDuration = displayedPositionAndDuration,
            modifier = modifier,
        )
    }

    blurAdjuster.Render()
    fadeAdjuster.Render()
    SpotifyCanvasWorker()

    Box(modifier = Modifier.fillMaxSize()) {
        @Composable
        fun ActionsBar() = ActionBar(
            navController,
            showQueueState,
            showSearchEntityState,
            rotateState,
            showVisualizerState,
            showSleepTimerState,
            showLyricsState,
            discoverState,
            queueLoopState,
            expandPlayerState,
            onDismiss
        )

        val player = binder.player

        // ── FIX 2: All four drawBehind progress-bar calls now use safeProgressFraction()
        //   which guards against C.TIME_UNSET, 0-duration, and near-infinite floats.
        @Composable
        fun Modifier.progressBarBackground(): Modifier = this.drawBehind {
            if (backgroundProgress == BackgroundProgress.Both || backgroundProgress == BackgroundProgress.Player) {
                val fraction = safeProgressFraction(
                    displayedPositionAndDuration.first,
                    displayedPositionAndDuration.second
                )
                val progressSize = Size(width = fraction * size.width, height = size.maxDimension)
                if (progressOverlayBrush != null) {
                    drawRect(brush = progressOverlayBrush, topLeft = Offset.Zero, size = progressSize)
                } else {
                    drawRect(color = color.favoritesOverlay, topLeft = Offset.Zero, size = progressSize)
                }
            }
        }

        if (isLandscape) {
            LandscapePlayerContent {
                Box {
                    val shouldShowCanvas = rememberShouldShowPlayerCanvas(
                        spotifyCanvasEnabled = spotifyCanvasEnabled,
                        playerVideoModeActive = playerVideoModeActive,
                        mediaItem = mediaItem,
                        isShowingLyrics = isShowingLyrics,
                        isShowingVisualizer = isShowingVisualizer,
                        showthumbnail = showthumbnail,
                    )
                    PlayerCanvasLayer(
                        shouldShowCanvas = shouldShowCanvas,
                        showSpotifyCanvasLogs = showSpotifyCanvasLogs,
                        screenWidth = screenWidth,
                        showVideoButton = showButtonPlayerVideo,
                        onVideoClick = {
                            CanvasPlayerManager.pauseKeepingState()
                            playerVideoModeActive = true
                        },
                        modifier = Modifier.fillMaxSize().zIndex(0f)
                    )
                    if (!shouldShowCanvas) {
                        if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor &&
                            playerType == PlayerType.Modern && (!showthumbnail || albumCoverRotation)
                        ) {
                            val fling = PagerDefaults.flingBehavior(
                                state = pagerStateFS,
                                snapPositionalThreshold = 0.20f
                            )
                            pagerStateFS.LaunchedEffectScrollToPage(displayedMediaItemIndex)

                            LaunchedEffect(pagerStateFS) {
                                var previousPage = pagerStateFS.settledPage
                                snapshotFlow { pagerStateFS.settledPage }.distinctUntilChanged().collect {
                                    if (previousPage != it) {
                                        if (it != displayedMediaItemIndex) binder.player.playAtIndex(it)
                                    }
                                    previousPage = it
                                }
                            }

                            HorizontalPager(
                                state = pagerStateFS,
                                beyondViewportPageCount = 1,
                                flingBehavior = fling,
                                userScrollEnabled = !(albumCoverRotation && (isShowingLyrics || showthumbnail)),
                                modifier = Modifier
                            ) {
                                var currentRotation by remember { mutableFloatStateOf(0f) }
                                val rotation = remember { Animatable(currentRotation) }

                                LaunchedEffect(player.isPlaying, pagerStateFS.settledPage) {
                                    if (player.isPlaying && it == pagerStateFS.settledPage) {
                                        rotation.animateTo(
                                            targetValue = currentRotation + 360f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(30000, easing = LinearEasing),
                                                repeatMode = RepeatMode.Restart
                                            )
                                        ) { currentRotation = value }
                                    } else {
                                        if (currentRotation > 0f && it == pagerStateFS.settledPage) {
                                            rotation.animateTo(
                                                targetValue = currentRotation + 10,
                                                animationSpec = tween(1250, easing = LinearOutSlowInEasing)
                                            ) { currentRotation = value }
                                        }
                                    }
                                }

                                BlurredCover(
                                    thumbnailUrl = queuedMediaItemAt(it).mediaMetadata.artworkUri.toString(),
                                    blurAdjuster = blurAdjuster,
                                    showThumbnail = showthumbnail,
                                    noBlur = noblur,
                                    isShowingLyrics = isShowingLyrics,
                                    isShowingVisualizer = isShowingVisualizer,
                                    contentScale = if (albumCoverRotation && (isShowingLyrics || showthumbnail)) ContentScale.Fit else ContentScale.Crop,
                                    modifier = Modifier
                                        .zIndex(if (it == pagerStateFS.currentPage) 1f else 0.9f)
                                        .conditional(albumCoverRotation) {
                                            graphicsLayer {
                                                scaleX = if (isShowingLyrics || showthumbnail) (screenWidth / screenHeight) + 0.5f else 1f
                                                scaleY = if (isShowingLyrics || showthumbnail) (screenWidth / screenHeight) + 0.5f else 1f
                                                rotationZ = if ((it == pagerStateFS.settledPage) && (isShowingLyrics || showthumbnail)) rotation.value else 0f
                                            }
                                        }
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                if (thumbnailTapEnabled && !showthumbnail) {
                                                    if (isShowingVisualizer) isShowingVisualizer = false
                                                    isShowingLyrics = !isShowingLyrics
                                                }
                                            },
                                            onDoubleClick = {
                                                if (!showlyricsthumbnail && !showvisthumbnail)
                                                    showthumbnail = !showthumbnail
                                            },
                                            onLongClick = {
                                                blurAdjuster.isActive = showthumbnail || (isShowingLyrics && !isShowingVisualizer) || !noblur
                                            }
                                        )
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0.0f to Color.Transparent,
                                            1.0f to if (bottomgradient) if (lightTheme) Color.White.copy(
                                                if (isLandscape) 0.8f else 0.75f
                                            ) else Color.Black.copy(if (isLandscape) 0.8f else 0.75f) else Color.Transparent,
                                            startY = if (isLandscape) 600f else if (expandedplayer) 1300f else 950f,
                                            endY = POSITIVE_INFINITY
                                        )
                                    )
                                    .background(
                                        if (bottomgradient) if (isLandscape) if (lightTheme) Color.White.copy(0.25f)
                                        else Color.Black.copy(0.25f) else Color.Transparent else Color.Transparent
                                    )
                            ) {}
                        }

                        BlurredCover(
                            thumbnailUrl = displayedArtworkUrl,
                            blurAdjuster = blurAdjuster,
                            showThumbnail = showthumbnail,
                            noBlur = noblur,
                            isShowingLyrics = isShowingLyrics,
                            isShowingVisualizer = isShowingVisualizer,
                            contentScale = ContentScale.FillHeight,
                            modifier = Modifier.fillMaxSize().zIndex(-1f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = if (!shouldShowCanvas) {
                            containerModifier
                                .padding(top = if (playerType == PlayerType.Essential) 40.dp else 20.dp)
                                .padding(top = if (extraspace) 10.dp else 0.dp)
                                .progressBarBackground()
                                .zIndex(1f)
                        } else {
                            Modifier
                                .padding(top = if (playerType == PlayerType.Essential) 40.dp else 20.dp)
                                .padding(top = if (extraspace) 10.dp else 0.dp)
                                .progressBarBackground()
                                .zIndex(1f)
                        }
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxHeight().animateContentSize()
                        ) {
                            if (showthumbnail && (playerType == PlayerType.Essential)) {
                                Box(contentAlignment = Alignment.Center) {
                                    if ((!isShowingLyrics && !isShowingVisualizer) ||
                                        (isShowingVisualizer && showvisthumbnail) ||
                                        (isShowingLyrics && showlyricsthumbnail)
                                    ) thumbnailContent()
                                }
                            }
                            if (isShowingVisualizer && !showvisthumbnail && playerType == PlayerType.Essential) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.5f)
                                        .pointerInput(Unit) {
                                            detectHorizontalDragGestures(
                                                onHorizontalDrag = { _, dragAmount -> deltaX = dragAmount },
                                                onDragStart = {},
                                                onDragEnd = {
                                                    if (!disablePlayerHorizontalSwipe && playerType == PlayerType.Essential) {
                                                        if (deltaX > 5) binder.player.playPrevious()
                                                        else if (deltaX < -5) binder.player.playNext()
                                                    }
                                                }
                                            )
                                        }
                                ) {
                                    NextVisualizer(isDisplayed = isShowingVisualizer)
                                }
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.weight(1f).navigationBarsPadding()
                            ) {
                                PlayerLyricsAndVisualizerOverlay(
                                    mediaItem = mediaItem,
                                    player = player,
                                    isShowingLyrics = isShowingLyrics,
                                    onDismissLyrics = { isShowingLyrics = false },
                                    isLandscape = isLandscape,
                                    clickLyricsText = clickLyricsText,
                                    showLyricsThumbnail = showlyricsthumbnail,
                                    isShowingVisualizer = isShowingVisualizer,
                                    showVisualizerThumbnail = showvisthumbnail,
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onHorizontalDrag = { _, dragAmount -> deltaX = dragAmount },
                                            onDragStart = {},
                                            onDragEnd = {
                                                if (!disablePlayerHorizontalSwipe) {
                                                    if (deltaX > 5) binder.player.playPrevious()
                                                    else if (deltaX < -5) binder.player.playNext()
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (playerType == PlayerType.Modern) {
                                BoxWithConstraints(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (showthumbnail && !isShowingVisualizer) {
                                        val fling = PagerDefaults.flingBehavior(state = pagerState, snapPositionalThreshold = 0.25f)
                                        val pageSpacing = thumbnailSpacingL.toInt() * 0.01 * (screenWidth) - (2.5 * playerThumbnailSizeL.size.dp)

                                        LaunchedEffect(pagerState, displayedMediaItemIndex) {
                                            pagerState.scrollToPage(displayedMediaItemIndex)
                                        }

                                        LaunchedEffect(pagerState) {
                                            var previousPage = pagerState.settledPage
                                            snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect {
                                                if (previousPage != it && it != displayedMediaItemIndex)
                                                    binder.player.playAtIndex(it)
                                                previousPage = it
                                            }
                                        }

                                        HorizontalPager(
                                            state = pagerState,
                                            pageSize = PageSize.Fixed(Dimensions.thumbnails.player.song),
                                            pageSpacing = thumbnailSpacingL.toInt() * 0.01 * (screenWidth) - (2.5 * playerThumbnailSizeL.size.dp),
                                            contentPadding = PaddingValues(
                                                start = ((maxWidth - maxHeight) / 2).coerceAtLeast(0.dp),
                                                end = ((maxWidth - maxHeight) / 2 + if (pageSpacing < 0.dp) (-(pageSpacing)) else 0.dp).coerceAtLeast(0.dp)
                                            ),
                                            beyondViewportPageCount = 3,
                                            flingBehavior = fling,
                                            userScrollEnabled = !disablePlayerHorizontalSwipe,
                                            modifier = Modifier
                                                .padding(all = (if (thumbnailType == ThumbnailType.Modern) -(10.dp) else 0.dp).coerceAtLeast(0.dp))
                                                .conditional(fadingedge) { horizontalFadingEdge() }
                                        ) {
                                            val coverPainter = ImageCacheFactory.Painter(
                                                thumbnailUrl = queuedMediaItemAt(it).mediaMetadata.artworkUri.toString()
                                            )
                                            val coverModifier = Modifier
                                                .aspectRatio(1f)
                                                .padding(all = playerThumbnailSizeL.size.dp)
                                                .graphicsLayer {
                                                    val pageOffSet = ((pagerState.currentPage - it) + pagerState.currentPageOffsetFraction).absoluteValue
                                                    alpha = lerp(start = 0.9f, stop = 1f, fraction = 1f - pageOffSet.coerceIn(0f, 1f))
                                                    scaleY = lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffSet.coerceIn(0f, 5f))
                                                    scaleX = lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffSet.coerceIn(0f, 5f))
                                                }
                                                .conditional(thumbnailType == ThumbnailType.Modern) { padding(all = 10.dp) }
                                                .conditional(thumbnailType == ThumbnailType.Modern) {
                                                    doubleShadowDrop(
                                                        if (showCoverThumbnailAnimation) CircleShape else thumbnailRoundness.shape,
                                                        4.dp, 8.dp
                                                    )
                                                }
                                                .clip(thumbnailRoundness.shape)
                                                .combinedClickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = {
                                                        if (it == pagerState.settledPage && thumbnailTapEnabled) {
                                                            if (isShowingVisualizer) isShowingVisualizer = false
                                                            isShowingLyrics = !isShowingLyrics
                                                        }
                                                        if (it != pagerState.settledPage) binder.player.playAtIndex(it)
                                                    },
                                                    onLongClick = {
                                                        if (it == pagerState.settledPage) showThumbnailOffsetDialog = true
                                                    }
                                                )

                                            val zIndex = remember(it, pagerState.currentPage) {
                                                when {
                                                    it == pagerState.currentPage -> 1f
                                                    it == (pagerState.currentPage + 1) || it == (pagerState.currentPage - 1) -> .85f
                                                    it == (pagerState.currentPage + 2) || it == (pagerState.currentPage - 2) -> .78f
                                                    it == (pagerState.currentPage + 3) || it == (pagerState.currentPage - 3) -> .73f
                                                    it == (pagerState.currentPage + 4) || it == (pagerState.currentPage - 4) -> .68f
                                                    it == (pagerState.currentPage + 5) || it == (pagerState.currentPage - 5) -> .63f
                                                    else -> .57f
                                                }
                                            }

                                            if (showCoverThumbnailAnimation)
                                                RotateThumbnailCoverAnimationModern(
                                                    painter = coverPainter,
                                                    isSongPlaying = player.isPlaying,
                                                    modifier = coverModifier.zIndex(zIndex),
                                                    state = pagerState,
                                                    it = it,
                                                    imageCoverSize = imageCoverSize,
                                                    type = coverThumbnailAnimation
                                                )
                                            else
                                                Box(Modifier.zIndex(zIndex)) {
                                                    Image(
                                                        painter = coverPainter,
                                                        contentDescription = "",
                                                        contentScale = ContentScale.Fit,
                                                        modifier = coverModifier
                                                    )
                                                    if (isDragged && it == displayedMediaItemIndex) {
                                                        Box(modifier = Modifier.align(Alignment.Center).matchParentSize()) {
                                                            NowPlayingSongIndicator(
                                                                queuedMediaItemAt(displayedMediaItemIndex).mediaId,
                                                                binder.player,
                                                                Dimensions.thumbnails.album
                                                            )
                                                        }
                                                    }
                                                }
                                        }
                                    }

                                    if (isShowingVisualizer) {
                                        Box(
                                            modifier = Modifier.pointerInput(Unit) {
                                                detectHorizontalDragGestures(
                                                    onHorizontalDrag = { _, dragAmount -> deltaX = dragAmount },
                                                    onDragStart = {},
                                                    onDragEnd = {
                                                        if (!disablePlayerHorizontalSwipe && playerType == PlayerType.Essential) {
                                                            if (deltaX > 5) binder.player.playPrevious()
                                                            else if (deltaX < -5) binder.player.playNext()
                                                        }
                                                    }
                                                )
                                            }
                                        ) {
                                            NextVisualizer(isDisplayed = isShowingVisualizer)
                                        }
                                    }
                                }
                            }

                            if (playerType == PlayerType.Essential || isShowingVisualizer) {
                                Controller(
                                    mediaItem,
                                    Modifier.padding(vertical = 8.dp)
                                        .conditional(playerType == PlayerType.Essential) {
                                            fillMaxHeight().weight(1f)
                                        },
                                    isBuffering = isBuffering
                                )
                            } else {
                                // ── FIX 4: guard mediaItemCount == 0 before coerceIn
                                val index = run {
                                    val raw = if (!showthumbnail) {
                                        if (pagerStateFS.currentPage > binder.player.currentTimeline.windowCount) 0
                                        else pagerStateFS.currentPage
                                    } else if (pagerState.currentPage > binder.player.currentTimeline.windowCount) {
                                        0
                                    } else pagerState.currentPage
                                    val count = player.mediaItemCount
                                    if (count > 0) raw.coerceIn(0, count - 1) else 0
                                }
                                Controller(
                                    queuedMediaItemAt(index),
                                    Modifier.padding(vertical = 8.dp),
                                    isBuffering = isBuffering
                                )
                            }

                            if (!showthumbnail || playerType == PlayerType.Modern) {
                                StatsForNerds(
                                    mediaId = mediaItem.mediaId,
                                    isDisplayed = statsfornerds,
                                    onDismiss = {}
                                )
                            }
                            ActionsBar()
                        }
                    }
                }
            }
        } else {
            PortraitPlayerContent {
                Box {
                    val shouldShowCanvas = rememberShouldShowPlayerCanvas(
                        spotifyCanvasEnabled = spotifyCanvasEnabled,
                        playerVideoModeActive = playerVideoModeActive,
                        mediaItem = mediaItem,
                        isShowingLyrics = isShowingLyrics,
                        isShowingVisualizer = isShowingVisualizer,
                        showthumbnail = showthumbnail,
                    )

                    PlayerCanvasLayer(
                        shouldShowCanvas = shouldShowCanvas,
                        showSpotifyCanvasLogs = showSpotifyCanvasLogs,
                        screenWidth = screenWidth,
                        showVideoButton = showButtonPlayerVideo,
                        onVideoClick = {
                            CanvasPlayerManager.pauseKeepingState()
                            playerVideoModeActive = true
                        },
                        modifier = Modifier.fillMaxSize().zIndex(0f)
                    )

                    if (!shouldShowCanvas) {
                        if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor &&
                            playerType == PlayerType.Modern && (!showthumbnail || albumCoverRotation)
                        ) {
                            val fling = PagerDefaults.flingBehavior(state = pagerStateFS, snapPositionalThreshold = 0.30f)
                            val scaleAnimationFloat by animateFloatAsState(if (isDraggedFS) 0.85f else 1f, label = "")
                            pagerStateFS.LaunchedEffectScrollToPage(displayedMediaItemIndex)

                            LaunchedEffect(pagerStateFS) {
                                var previousPage = pagerStateFS.settledPage
                                snapshotFlow { pagerStateFS.settledPage }.distinctUntilChanged().collect {
                                    if (previousPage != it) {
                                        delay(if (swipeAnimationNoThumbnail == SwipeAnimationNoThumbnail.Fade) 0 else 400)
                                        if (it != displayedMediaItemIndex) binder.player.playAtIndex(it)
                                    }
                                    previousPage = it
                                }
                            }

                            HorizontalPager(
                                state = pagerStateFS,
                                beyondViewportPageCount = if (swipeAnimationNoThumbnail != SwipeAnimationNoThumbnail.Circle ||
                                    albumCoverRotation && (isShowingLyrics || showthumbnail)
                                ) 1 else 0,
                                flingBehavior = fling,
                                userScrollEnabled = !(albumCoverRotation && (isShowingLyrics || showthumbnail)),
                                modifier = Modifier
                                    .background(if (shouldShowCanvas) Color.Transparent else colorPalette().background1)
                                    .pointerInteropFilter { circleOffsetY = it.y; false }
                            ) {
                                var currentRotation by remember { mutableFloatStateOf(0f) }
                                val rotation = remember { Animatable(currentRotation) }

                                LaunchedEffect(player.isPlaying, pagerStateFS.settledPage) {
                                    if (player.isPlaying && it == pagerStateFS.settledPage) {
                                        rotation.animateTo(
                                            targetValue = currentRotation + 360f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(30000, easing = LinearEasing),
                                                repeatMode = RepeatMode.Restart
                                            )
                                        ) { currentRotation = value }
                                    } else {
                                        if (currentRotation > 0f && it == pagerStateFS.settledPage) {
                                            rotation.animateTo(
                                                targetValue = currentRotation + 10,
                                                animationSpec = tween(1250, easing = LinearOutSlowInEasing)
                                            ) { currentRotation = value }
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .conditional(albumCoverRotation && (isShowingLyrics || showthumbnail)) {
                                            zIndex(if (it == pagerStateFS.currentPage) 2f else 1.5f)
                                        }
                                        .conditional(swipeAnimationNoThumbnail == SwipeAnimationNoThumbnail.Scale && isDraggedFS) {
                                            graphicsLayer { scaleY = scaleAnimationFloat; scaleX = scaleAnimationFloat }
                                        }
                                ) {
                                    BlurredCover(
                                        thumbnailUrl = queuedMediaItemAt(it).mediaMetadata.artworkUri.toString(),
                                        blurAdjuster = blurAdjuster,
                                        showThumbnail = showthumbnail,
                                        noBlur = noblur,
                                        isShowingLyrics = isShowingLyrics,
                                        isShowingVisualizer = isShowingVisualizer,
                                        contentScale = if (albumCoverRotation && (isShowingLyrics || showthumbnail)) ContentScale.Fit else ContentScale.Crop,
                                        modifier = Modifier
                                            .conditional(albumCoverRotation) {
                                                graphicsLayer {
                                                    scaleX = if (isShowingLyrics || showthumbnail) (screenHeight / screenWidth) + 0.5f else 1f
                                                    scaleY = if (isShowingLyrics || showthumbnail) (screenHeight / screenWidth) + 0.5f else 1f
                                                    rotationZ = if ((it == pagerStateFS.settledPage) && (isShowingLyrics || showthumbnail)) rotation.value else 0f
                                                }
                                            }
                                            .conditional(swipeAnimationNoThumbnail == SwipeAnimationNoThumbnail.Fade && !showthumbnail) {
                                                graphicsLayer {
                                                    val pageOffset = pagerStateFS.currentPageOffsetFraction
                                                    translationX = pageOffset * size.width
                                                    alpha = 1 - pageOffset.absoluteValue
                                                }
                                            }
                                            .conditional(swipeAnimationNoThumbnail == SwipeAnimationNoThumbnail.Carousel && isDraggedFS) {
                                                graphicsLayer {
                                                    val startOffset = pagerStateFS.startOffsetForPage(it)
                                                    translationX = size.width * (startOffset * .99f)
                                                    alpha = (2f - startOffset) / 2f
                                                    val blur = (startOffset * 20f).coerceAtLeast(0.1f)
                                                    renderEffect = RenderEffect.createBlurEffect(blur, blur, android.graphics.Shader.TileMode.DECAL).asComposeRenderEffect()
                                                    val scale = 1f - (startOffset * .1f)
                                                    scaleX = scale; scaleY = scale
                                                }
                                            }
                                            .conditional(swipeAnimationNoThumbnail == SwipeAnimationNoThumbnail.Circle && !showthumbnail) {
                                                graphicsLayer {
                                                    val pageOffset = pagerStateFS.offsetForPage(it)
                                                    translationX = size.width * pageOffset
                                                    val endOffset = pagerStateFS.endOffsetForPage(it)
                                                    shadowElevation = 20f
                                                    shape = CirclePath(
                                                        progress = 1f - endOffset.absoluteValue,
                                                        origin = Offset(size.width, circleOffsetY)
                                                    )
                                                    clip = true
                                                    val absoluteOffset = pagerStateFS.offsetForPage(it).absoluteValue
                                                    val scale = 1f + (absoluteOffset.absoluteValue * .4f)
                                                    scaleX = scale; scaleY = scale
                                                    val startOffset = pagerStateFS.startOffsetForPage(it)
                                                    alpha = (2f - startOffset) / 2f
                                                }
                                            }
                                            .clip(RoundedCornerShape(20.dp))
                                            .combinedClickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    if (thumbnailTapEnabled && !showthumbnail) {
                                                        if (isShowingVisualizer) isShowingVisualizer = false
                                                        isShowingLyrics = !isShowingLyrics
                                                    }
                                                },
                                                onDoubleClick = {
                                                    if (!showlyricsthumbnail && !showvisthumbnail) showthumbnail = !showthumbnail
                                                },
                                                onLongClick = {
                                                    blurAdjuster.isActive = showthumbnail || (isShowingLyrics && !isShowingVisualizer) || !noblur
                                                }
                                            )
                                    )

                                    if ((swipeAnimationNoThumbnail == SwipeAnimationNoThumbnail.Scale) && isDraggedFS) {
                                        Column {
                                            Spacer(
                                                modifier = Modifier
                                                    .conditional((screenWidth <= (screenHeight / 2)) && (showlyricsthumbnail || (!expandedplayer && !isShowingLyrics))) { height(screenWidth) }
                                                    .conditional((screenWidth > (screenHeight / 2)) || expandedplayer || (isShowingLyrics && !showlyricsthumbnail)) { weight(1f) }
                                            )
                                            Box(modifier = Modifier.conditional(!expandedplayer && (!isShowingLyrics || showlyricsthumbnail)) { weight(1f) }) {
                                                Controls(
                                                    navController = navController,
                                                    onCollapse = onDismiss,
                                                    expandedplayer = expandedplayer,
                                                    titleExpanded = titleExpanded,
                                                    timelineExpanded = timelineExpanded,
                                                    controlsExpanded = controlsExpanded,
                                                    isShowingLyrics = isShowingLyrics,
                                                    media = mediaItem.toUiMedia(displayedPositionAndDuration.second),
                                                    mediaId = mediaItem.mediaId,
                                                    title = cleanPrefix(queuedMediaItemAt(it).mediaMetadata.title.toString()),
                                                    artist = cleanPrefix(queuedMediaItemAt(it).mediaMetadata.artist.toString()),
                                                    artistIds = artistInfos,
                                                    albumId = albumId,
                                                    shouldBePlaying = shouldBePlaying,
                                                    isBuffering = isBuffering,
                                                    position = displayedPositionAndDuration.first,
                                                    duration = displayedPositionAndDuration.second,
                                                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                                                    onBlurScaleChange = { blurAdjuster.strength = it },
                                                    isExplicit = mediaItem.isExplicit
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0.0f to Color.Transparent,
                                            1.0f to if (bottomgradient) if (colorPaletteMode == ColorPaletteMode.Light) Color.White.copy(if (isLandscape) 0.8f else 0.75f)
                                            else Color.Black.copy(if (isLandscape) 0.8f else 0.75f) else Color.Transparent,
                                            startY = if (isLandscape) 600f else if (expandedplayer) 1300f else 950f,
                                            endY = POSITIVE_INFINITY
                                        )
                                    )
                                    .background(
                                        if (bottomgradient) if (isLandscape) if (colorPaletteMode == ColorPaletteMode.Light) Color.White.copy(0.25f)
                                        else Color.Black.copy(0.25f) else Color.Transparent else Color.Transparent
                                    )
                            ) {}
                        }

                        BlurredCover(
                            thumbnailUrl = displayedArtworkUrl,
                            blurAdjuster = blurAdjuster,
                            showThumbnail = showthumbnail,
                            noBlur = noblur,
                            isShowingLyrics = isShowingLyrics,
                            isShowingVisualizer = isShowingVisualizer,
                            contentScale = ContentScale.FillHeight
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = if (!shouldShowCanvas) {
                            containerModifier.progressBarBackground()
                        } else {
                            Modifier.progressBarBackground()
                        }
                    ) {
                        if (showTopActionsBar) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .padding(windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal).asPaddingValues())
                                    .fillMaxWidth(0.9f)
                                    .height(30.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.chevron_down),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(colorPalette().collapsedPlayerProgressBar),
                                    modifier = Modifier.clickable { onDismiss() }.rotate(rotationAngle).size(24.dp)
                                )
                                Image(
                                    painter = painterResource(R.drawable.ic_launcher_monochrome),
                                    colorFilter = ColorFilter.tint(colorPalette().collapsedPlayerProgressBar),
                                    contentDescription = "app icon in player",
                                    modifier = Modifier.size(24.dp).clickable {
                                        onDismiss()
                                        navController.navigate(NavRoutes.home.name)
                                    }
                                )
                                if (!showButtonPlayerMenu)
                                    Image(
                                        painter = painterResource(R.drawable.ellipsis_vertical),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(colorPalette().collapsedPlayerProgressBar),
                                        modifier = Modifier
                                            .clickable {
                                                menuState.display {
                                                    PlayerMenu(
                                                        navController = navController,
                                                        onDismiss = menuState::hide,
                                                        mediaItem = mediaItem,
                                                        binder = binder,
                                                        onClosePlayer = { onDismiss() },
                                                        onShowSleepTimer = {
                                                            isShowingSleepTimerDialog = true
                                                            menuState.hide()
                                                        },
                                                        disableScrollingText = disableScrollingText
                                                    )
                                                }
                                            }
                                            .rotate(rotationAngle)
                                            .size(24.dp)
                                    )
                            }
                            Spacer(
                                modifier = Modifier
                                    .height(5.dp)
                                    .padding(windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal).asPaddingValues())
                            )
                        }

                        if (topPadding && !showTopActionsBar) {
                            Spacer(
                                modifier = Modifier
                                    .padding(windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal).asPaddingValues())
                                    .height(35.dp)
                            )
                        }

                        BoxWithConstraints(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .conditional((screenWidth <= (screenHeight / 2)) && (showlyricsthumbnail || (!expandedplayer && !isShowingLyrics))) { height(screenWidth) }
                                .conditional((screenWidth > (screenHeight / 2)) || expandedplayer || (isShowingLyrics && !showlyricsthumbnail)) { weight(1f) }
                        ) {
                            if (showthumbnail && !shouldShowCanvas) {
                                if ((!isShowingLyrics && !isShowingVisualizer) ||
                                    (isShowingVisualizer && showvisthumbnail) ||
                                    (isShowingLyrics && showlyricsthumbnail)
                                ) {
                                    if (playerType == PlayerType.Modern) {
                                        val fling = PagerDefaults.flingBehavior(state = pagerState, snapPositionalThreshold = 0.25f)
                                        pagerState.LaunchedEffectScrollToPage(displayedMediaItemIndex)

                                        LaunchedEffect(pagerState) {
                                            var previousPage = pagerState.settledPage
                                            snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect {
                                                if (previousPage != it && it != displayedMediaItemIndex)
                                                    binder.player.playAtIndex(it)
                                                previousPage = it
                                            }
                                        }

                                        val pageSpacing = (thumbnailSpacing.toInt() * 0.01 * (screenHeight) - if (carousel) (3 * carouselSize.size.dp) else (2 * playerThumbnailSize.size.dp))
                                        val animatePageSpacing by animateDpAsState(
                                            if (expandedplayer) (thumbnailSpacing.toInt() * 0.01 * (screenHeight) - if (carousel) (3 * carouselSize.size.dp) else (2 * carouselSize.size.dp)) else 10.dp,
                                            label = ""
                                        )
                                        val animatePadding by animateDpAsState(
                                            if (expandedplayer) carouselSize.size.dp else playerThumbnailSize.size.dp
                                        )

                                        VerticalPager(
                                            state = pagerState,
                                            pageSize = PageSize.Fixed(if (maxWidth < maxHeight) maxWidth else maxHeight),
                                            contentPadding = PaddingValues(
                                                top = (maxHeight - (if (maxWidth < maxHeight) maxWidth else maxHeight)) / 2,
                                                bottom = (maxHeight - (if (maxWidth < maxHeight) maxWidth else maxHeight)) / 2 + if (pageSpacing < 0.dp) (-(pageSpacing)) else 0.dp
                                            ),
                                            pageSpacing = animatePageSpacing,
                                            beyondViewportPageCount = 2,
                                            flingBehavior = fling,
                                            userScrollEnabled = expandedplayer || !disablePlayerHorizontalSwipe,
                                            modifier = Modifier
                                                .padding(all = (if (expandedplayer) 0.dp else if (thumbnailType == ThumbnailType.Modern) -(10.dp) else 0.dp).coerceAtLeast(0.dp))
                                                .conditional(fadingedge) {
                                                    VerticalfadingEdge2(
                                                        fade = (if (expandedplayer) thumbnailFadeEx else thumbnailFade) * 0.05f,
                                                        showTopActionsBar, topPadding, expandedplayer
                                                    )
                                                }
                                        ) {
                                            val coverPainter = ImageCacheFactory.Painter(
                                                thumbnailUrl = queuedMediaItemAt(it).mediaMetadata.artworkUri.toString()
                                            )
                                            val coverModifier = Modifier
                                                .aspectRatio(1f)
                                                .padding(all = animatePadding)
                                                .conditional(carousel) {
                                                    graphicsLayer {
                                                        val pageOffSet = ((pagerState.currentPage - it) + pagerState.currentPageOffsetFraction).absoluteValue
                                                        alpha = lerp(start = 0.9f, stop = 1f, fraction = 1f - pageOffSet.coerceIn(0f, 1f))
                                                        scaleY = lerp(start = 0.9f, stop = 1f, fraction = 1f - pageOffSet.coerceIn(0f, 5f))
                                                        scaleX = lerp(start = 0.9f, stop = 1f, fraction = 1f - pageOffSet.coerceIn(0f, 5f))
                                                    }
                                                }
                                                .conditional(thumbnailType == ThumbnailType.Modern) { padding(all = 10.dp) }
                                                .conditional(thumbnailType == ThumbnailType.Modern) {
                                                    doubleShadowDrop(
                                                        if (showCoverThumbnailAnimation) CircleShape else thumbnailRoundness.shape,
                                                        4.dp, 8.dp
                                                    )
                                                }
                                                .clip(thumbnailRoundness.shape)
                                                .combinedClickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = {
                                                        if (it == pagerState.settledPage && thumbnailTapEnabled) {
                                                            if (isShowingVisualizer) isShowingVisualizer = false
                                                            isShowingLyrics = !isShowingLyrics
                                                        }
                                                        if (it != pagerState.settledPage) binder.player.playAtIndex(it)
                                                    },
                                                    onLongClick = {
                                                        if (it == pagerState.settledPage && (expandedplayer || fadingedge))
                                                            showThumbnailOffsetDialog = true
                                                    }
                                                )

                                            val zIndex = remember(it, pagerState.currentPage) {
                                                when {
                                                    it == pagerState.currentPage -> 1f
                                                    it == (pagerState.currentPage + 1) || it == (pagerState.currentPage - 1) -> .85f
                                                    it == (pagerState.currentPage + 2) || it == (pagerState.currentPage - 2) -> .78f
                                                    it == (pagerState.currentPage + 3) || it == (pagerState.currentPage - 3) -> .73f
                                                    it == (pagerState.currentPage + 4) || it == (pagerState.currentPage - 4) -> .68f
                                                    it == (pagerState.currentPage + 5) || it == (pagerState.currentPage - 5) -> .63f
                                                    else -> .57f
                                                }
                                            }

                                            if (showCoverThumbnailAnimation)
                                                RotateThumbnailCoverAnimationModern(
                                                    painter = coverPainter,
                                                    isSongPlaying = player.isPlaying,
                                                    modifier = coverModifier.zIndex(zIndex),
                                                    state = pagerState,
                                                    it = it,
                                                    imageCoverSize = imageCoverSize,
                                                    type = coverThumbnailAnimation
                                                )
                                            else
                                                Box(Modifier.zIndex(zIndex)) {
                                                    Image(
                                                        painter = coverPainter,
                                                        contentDescription = "",
                                                        contentScale = ContentScale.Fit,
                                                        modifier = coverModifier
                                                    )
                                                    if (isDragged && expandedplayer && it == displayedMediaItemIndex) {
                                                        Box(modifier = Modifier.align(Alignment.Center).matchParentSize()) {
                                                            NowPlayingSongIndicator(
                                                                queuedMediaItemAt(displayedMediaItemIndex).mediaId,
                                                                binder.player,
                                                                Dimensions.thumbnails.album
                                                            )
                                                        }
                                                    }
                                                }
                                        }
                                    } else {
                                        thumbnailContent()
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier.pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { _, dragAmount -> deltaX = dragAmount },
                                        onDragStart = {},
                                        onDragEnd = {
                                            if (!disablePlayerHorizontalSwipe) {
                                                if (deltaX > 5) binder.player.playPrevious()
                                                else if (deltaX < -5) binder.player.playNext()
                                            }
                                        }
                                    )
                                }
                            ) {
                                PlayerLyricsAndVisualizerOverlay(
                                    mediaItem = mediaItem,
                                    player = player,
                                    isShowingLyrics = isShowingLyrics,
                                    onDismissLyrics = { isShowingLyrics = false },
                                    isLandscape = isLandscape,
                                    clickLyricsText = clickLyricsText,
                                    showLyricsThumbnail = showlyricsthumbnail,
                                    isShowingVisualizer = isShowingVisualizer,
                                    showVisualizerThumbnail = showvisthumbnail,
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.conditional(!expandedplayer && (!isShowingLyrics || showlyricsthumbnail)) { weight(1f) }
                        ) {
                            if (!expandedplayer || !isShowingLyrics || queueDurationExpanded) {
                                if (showTotalTimeQueue)
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.time),
                                            colorFilter = ColorFilter.tint(colorPalette().accent),
                                            modifier = Modifier.size(20.dp).padding(horizontal = 5.dp),
                                            contentDescription = "Background Image",
                                            contentScale = ContentScale.Fit
                                        )
                                        Box {
                                            BasicText(
                                                text = " ${formatAsTime(totalPlayTimes)}",
                                                style = typography().xxs.semiBold.merge(
                                                    TextStyle(textAlign = TextAlign.Center, color = colorPalette().text)
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            BasicText(
                                                text = " ${formatAsTime(totalPlayTimes)}",
                                                style = typography().xxs.semiBold.merge(
                                                    TextStyle(
                                                        textAlign = TextAlign.Center,
                                                        drawStyle = Stroke(width = 1f, join = StrokeJoin.Round),
                                                        color = if (!textoutline) Color.Transparent
                                                        else if (colorPaletteMode == ColorPaletteMode.Light ||
                                                            (colorPaletteMode == ColorPaletteMode.System && (!isSystemInDarkTheme()))
                                                        ) Color.White.copy(0.5f)
                                                        else Color.Black,
                                                    )
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Box(modifier = Modifier.conditional(!expandedplayer && (!isShowingLyrics || showlyricsthumbnail)) { weight(1f) }) {
                                if (playerType == PlayerType.Essential || isShowingLyrics || isShowingVisualizer) {
                                    Controller(
                                        mediaItem,
                                        Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                                        isBuffering = isBuffering
                                    )
                                } else if (!(swipeAnimationNoThumbnail == SwipeAnimationNoThumbnail.Scale && isDraggedFS)) {
                                    // ── FIX 4: guard mediaItemCount == 0 before coerceIn
                                    val index = run {
                                        val raw = if (!showthumbnail) {
                                            if (pagerStateFS.currentPage > binder.player.currentTimeline.windowCount) 0
                                            else pagerStateFS.currentPage
                                        } else if (pagerState.currentPage > binder.player.currentTimeline.windowCount) {
                                            0
                                        } else pagerState.currentPage
                                        val count = player.mediaItemCount
                                        if (count > 0) raw.coerceIn(0, count - 1) else 0
                                    }
                                    Controller(
                                        queuedMediaItemAt(index),
                                        Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                                        isBuffering = isBuffering
                                    )
                                }
                            }

                            if (!showthumbnail || playerType == PlayerType.Modern) {
                                if (!isShowingLyrics || statsExpanded) {
                                    StatsForNerds(
                                        mediaId = mediaItem.mediaId,
                                        isDisplayed = statsfornerds,
                                        onDismiss = {}
                                    )
                                }
                            }
                            ActionsBar()
                        }
                    }
                }
            }
        }

        CustomModalBottomSheet(
            showSheet = showQueue,
            onDismissRequest = { showQueue = false },
            containerColor = if (queueType == QueueType.Modern) Color.Transparent else colorPalette().background2,
            contentColor = if (queueType == QueueType.Modern) Color.Transparent else colorPalette().background2,
            modifier = Modifier.fillMaxWidth(),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 0.dp),
                    color = colorPalette().background0,
                    shape = thumbnailShape()
                ) {}
            },
            shape = thumbnailRoundness.shape
        ) {
            Queue(
                navController = navController,
                onDismiss = { queueLoopState.value = it; showQueue = false },
                onDiscoverClick = { discoverState.value = it }
            )
        }

        CustomModalBottomSheet(
            showSheet = showSearchEntity,
            onDismissRequest = { showSearchEntity = false },
            containerColor = if (playerType == PlayerType.Modern) Color.Transparent else colorPalette().background2,
            contentColor = if (playerType == PlayerType.Modern) Color.Transparent else colorPalette().background2,
            modifier = Modifier.fillMaxWidth(),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 0.dp),
                    color = colorPalette().background0,
                    shape = thumbnailShape()
                ) {}
            },
            shape = thumbnailRoundness.shape
        ) {
            SearchYoutubeEntity(
                navController = navController,
                onDismiss = { showSearchEntity = false },
                query = "${mediaItem.mediaMetadata.artist} - ${mediaItem.mediaMetadata.title}",
                disableScrollingText = disableScrollingText
            )
        }

        PlaybackError(
            isDisplayed = playbackErrorMessage != null,
            messageProvider = { playbackErrorMessage.orEmpty() },
            onDismiss = { playbackErrorMessage = null },
            actionLabel = when {
                playerError != null && isCurrentSongDownloaded && hasNetworkConnection ->
                    stringResource(R.string.redownload_song)
                playerError != null && !isCurrentSongDownloaded && hasNetworkConnection ->
                    stringResource(R.string.retry_with_other_sources)
                else -> null
            },
            actionHint = when {
                playerError != null && isCurrentSongDownloaded && hasNetworkConnection ->
                    stringResource(R.string.redownload_song_hint)
                playerError != null && isCurrentSongDownloaded ->
                    stringResource(R.string.downloaded_song_corrupt_offline_message)
                playerError != null && !isCurrentSongDownloaded && hasNetworkConnection ->
                    stringResource(R.string.retry_with_other_sources_hint)
                else -> null
            },
            onAction = if (playerError != null) {
                {
                    playbackErrorMessage = null
                    currentErrorItem?.let { mediaItem ->
                        if (isCurrentSongDownloaded) {
                            MyDownloadHelper.redownloadSong(context, mediaItem)
                        } else {
                            lastSearchFallbackMediaId = null
                            retryWithAlternateSourcesNonce++
                        }
                    }
                }
            } else null,
        )
    }
}

@Composable
private fun PlayerLyricsAndVisualizerOverlay(
    mediaItem: MediaItem,
    player: Player,
    isShowingLyrics: Boolean,
    onDismissLyrics: () -> Unit,
    isLandscape: Boolean,
    clickLyricsText: Boolean,
    showLyricsThumbnail: Boolean,
    isShowingVisualizer: Boolean,
    showVisualizerThumbnail: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (!showLyricsThumbnail) {
            Lyrics(
                mediaId = mediaItem.mediaId,
                isDisplayed = isShowingLyrics,
                onDismiss = onDismissLyrics,
                ensureSongInserted = { insertIgnore(mediaItem) },
                size = 1000.dp,
                mediaMetadataProvider = mediaItem::mediaMetadata,
                durationProvider = player::getDuration,
                isLandscape = isLandscape,
                clickLyricsText = clickLyricsText,
            )
        }
        if (!showVisualizerThumbnail) {
            NextVisualizer(isDisplayed = isShowingVisualizer)
        }
    }
}

@Composable
private fun rememberShouldShowPlayerCanvas(
    spotifyCanvasEnabled: Boolean,
    playerVideoModeActive: Boolean,
    mediaItem: MediaItem,
    isShowingLyrics: Boolean,
    isShowingVisualizer: Boolean,
    showthumbnail: Boolean,
): Boolean {
    val currentMediaItemId = mediaItem.mediaId
    val isCanvasForCurrentSong = SpotifyCanvasState.currentMediaItemId == currentMediaItemId
    return spotifyCanvasEnabled &&
            !playerVideoModeActive &&
            SpotifyCanvasState.currentCanvasUrl != null &&
            isCanvasForCurrentSong &&
            !isShowingLyrics &&
            !isShowingVisualizer &&
            showthumbnail
}

@Composable
private fun PlayerCanvasLayer(
    shouldShowCanvas: Boolean,
    showSpotifyCanvasLogs: Boolean,
    screenWidth: Dp,
    showVideoButton: Boolean,
    onVideoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!shouldShowCanvas) return
    OptimizedSpotifyCanvasPlayer(
        canvasUrl = SpotifyCanvasState.currentCanvasUrl!!,
        mediaItemId = SpotifyCanvasState.currentMediaItemId,
        isPlaying = SpotifyCanvasState.isPlaying,
        showLogs = showSpotifyCanvasLogs,
        maxWidth = screenWidth,
        showVideoButton = showVideoButton,
        onVideoClick = onVideoClick,
        modifier = modifier
    )
}

@Composable
private fun OptimizedSpotifyCanvasPlayer(
    canvasUrl: String,
    mediaItemId: String?,
    isPlaying: Boolean,
    showLogs: Boolean,
    maxWidth: Dp,
    showVideoButton: Boolean,
    onVideoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentCanvasUrl by rememberUpdatedState(canvasUrl)
    val currentMediaItemId by rememberUpdatedState(mediaItemId)
    val currentIsPlaying by rememberUpdatedState(isPlaying)

    val playerKey = remember(currentCanvasUrl, currentMediaItemId) {
        "canvas_${currentCanvasUrl.hashCode()}_${currentMediaItemId.hashCode()}"
    }
    val topPadding = remember(maxWidth) {
        when {
            maxWidth < 360.dp -> 6.dp
            maxWidth < 480.dp -> 8.dp
            maxWidth < 600.dp -> 10.dp
            else -> 12.dp
        }
    }
    val bottomPadding = topPadding * 0.3f
    val cornerRadius = remember(maxWidth) { if (maxWidth >= 600.dp) 10.dp else 12.dp }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding, bottom = bottomPadding)
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color.Black)
        ) {
            OptimizedCanvasVideoPlayer(
                canvasUrl = currentCanvasUrl,
                mediaItemId = currentMediaItemId,
                isPlaying = currentIsPlaying,
                playerKey = playerKey,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.matchParentSize().drawBehind {
                    drawLine(color = Color.White.copy(alpha = 0.1f), start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                    drawLine(color = Color.White.copy(alpha = 0.1f), start = Offset(0f, size.height), end = Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                    drawRoundRect(color = Color.White.copy(alpha = 0.08f), style = Stroke(width = 1.dp.toPx()), cornerRadius = CornerRadius(cornerRadius.toPx()))
                }
            )
        }
        if (showLogs) {
            OptimizedCanvasLogPanel(
                modifier = Modifier.align(Alignment.TopStart).padding(top = 48.dp, start = 12.dp).width(280.dp)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 32.dp, end = 12.dp)
        ) {
            if (showVideoButton) {
                OptimizedCanvasVideoButton(onClick = onVideoClick)
            }
            OptimizedCanvasBadge()
        }
    }
}

@Composable
@androidx.annotation.OptIn(UnstableApi::class)
private fun OptimizedCanvasVideoPlayer(
    canvasUrl: String,
    mediaItemId: String?,
    isPlaying: Boolean,
    playerKey: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    DisposableEffect(playerKey) {
        onDispose { CanvasPlayerManager.pauseKeepingState() }
    }
    LaunchedEffect(isPlaying) {
        delay(100)
        CanvasPlayerManager.updatePlayState(isPlaying)
    }
    AndroidView(
        factory = { context ->
            CanvasPlayerManager.setupPlayer(context = context, canvasUrl = canvasUrl, isPlaying = isPlaying, mediaItemId = mediaItemId)
        },
        update = { playerView ->
            CanvasPlayerManager.attachOrUpdate(
                playerView = playerView,
                context = context,
                canvasUrl = canvasUrl,
                isPlaying = isPlaying,
                mediaItemId = mediaItemId
            )
        },
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .graphicsLayer {
                scaleX = 0.98f; scaleY = 0.98f
                shape = RoundedCornerShape(16.dp); clip = true
                alpha = if (isDarkTheme) 0.95f else 0.98f
            }
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawRoundRect(color = Color.Black.copy(alpha = 0.1f), style = Stroke(width = 1.dp.toPx()), cornerRadius = CornerRadius(16.dp.toPx()))
                }
            }
    )
}

@Composable
private fun OptimizedCanvasLogPanel(modifier: Modifier = Modifier) {
    val logs by rememberUpdatedState<List<LogEntry>>(SpotifyCanvasState.logEntries)
    val lastLogs = remember(logs) { logs.takeLast(8) }
    val logCount = logs.size

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRoundRect(color = Color.Black.copy(alpha = 0.85f), cornerRadius = CornerRadius(12.dp.toPx()))
                drawRoundRect(color = Color.White.copy(alpha = 0.1f), style = Stroke(width = 1.dp.toPx()), cornerRadius = CornerRadius(12.dp.toPx()))
            }
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicText(text = "Canvas Logs", style = typography().xs.copy(color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp))
            Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                BasicText(text = "$logCount", style = typography().xxs.copy(color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (lastLogs.isNotEmpty()) {
            LazyColumn(modifier = Modifier.height(160.dp)) {
                items(items = lastLogs) { log -> OptimizedLogEntryItem(log = log) }
            }
        } else {
            BasicText(text = "No logs yet", style = typography().xxs.copy(color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp), modifier = Modifier.padding(vertical = 16.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().height(12.dp).drawBehind {
            drawRect(brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
        })
    }
}

@Composable
private fun OptimizedLogEntryItem(log: LogEntry) {
    val logColor = remember(log.type) {
        when (log.type) {
            LogType.ERROR -> Color.Red
            LogType.SUCCESS -> Color.Green
            LogType.LOADING -> Color.Cyan
            LogType.WARNING -> Color.Yellow
            LogType.INFO -> Color.White
            else -> Color.White
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(logColor.copy(alpha = 0.7f)))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(text = log.message, style = typography().xxs.copy(color = logColor.copy(alpha = 0.9f), fontSize = 10.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            BasicText(text = formatAsTime(log.timestamp), style = typography().xxs.copy(color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp))
        }
    }
}

@Composable
private fun OptimizedCanvasBadge(modifier: Modifier = Modifier) {
    var currentSlide by remember { mutableStateOf(0) }
    val slides = listOf(SlideData("CANVAS", "VISUAL", "🎨"), SlideData("SUPPORT", "DONATE", "❤️"), SlideData("SETTINGS", "DISABLE", "⚙️"))
    val current = slides[currentSlide]
    LaunchedEffect(Unit) { while (true) { delay(10000); currentSlide = (currentSlide + 1) % slides.size } }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .drawBehind { drawRoundRect(color = Color.White.copy(alpha = 0.15f), style = Stroke(width = 0.5.dp.toPx()), cornerRadius = CornerRadius(8.dp.toPx())) }
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        BasicText(text = current.emoji, style = typography().xxs.copy(fontSize = 10.sp), modifier = Modifier.padding(bottom = 2.dp))
        BasicText(text = current.title, style = typography().xxs.copy(color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center))
        BasicText(text = current.subtitle, style = typography().xxs.copy(color = Color.White.copy(alpha = 0.6f), fontSize = 7.sp, textAlign = TextAlign.Center), modifier = Modifier.padding(top = 1.dp))
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            slides.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 1.dp)
                        .size(if (index == currentSlide) 3.dp else 2.dp)
                        .clip(CircleShape)
                        .background(if (index == currentSlide) Color.White else Color.White.copy(alpha = 0.3f))
                )
            }
        }
    }
}

private data class SlideData(val title: String, val subtitle: String, val emoji: String = "")

@Composable
private fun LogEntryItem(log: LogEntry) {
    val logColor = when (log.type) {
        LogType.ERROR -> Color.Red
        LogType.SUCCESS -> Color.Green
        LogType.LOADING -> Color.Cyan
        LogType.WARNING -> Color.Yellow
        LogType.INFO -> Color.White
        else -> Color.White
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(logColor.copy(alpha = 0.7f)))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(text = log.message, style = typography().xxs.copy(color = logColor.copy(alpha = 0.9f), fontSize = 10.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            BasicText(text = formatAsTime(log.timestamp), style = typography().xxs.copy(color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp))
        }
    }
}

private fun formatAsTime(timestamp: Long): String {
    val minutes = (timestamp / 60000) % 60
    val seconds = (timestamp / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun setupCanvasPlayer(context: Context, canvasUrl: String, isPlaying: Boolean): PlayerView {
    return PlayerView(context).apply {
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        useController = false
        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        val player = ExoPlayer.Builder(context)
            .setSeekForwardIncrementMs(15000)
            .setSeekBackIncrementMs(5000)
            .build().apply {
                playWhenReady = isPlaying
                repeatMode = Player.REPEAT_MODE_ALL
                val mediaItem = MediaItem.Builder().setUri(canvasUrl).setMimeType(MimeTypes.VIDEO_MP4).build()
                runCatching { setMediaItem(mediaItem) }
                runCatching { prepare() }
                videoScalingMode = 2
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            runCatching { seekTo(0) }
                            if (isPlaying) runCatching { play() }
                        }
                    }
                })
            }
        this.player = player
    }
}

@Composable
fun rememberCanvasPlayerState(canvasUrl: String?, mediaItemId: String?): CanvasPlayerState {
    return remember(canvasUrl, mediaItemId) { CanvasPlayerState(canvasUrl, mediaItemId) }
}

data class CanvasPlayerState(val canvasUrl: String?, val mediaItemId: String?) {
    val key: String get() = "${canvasUrl.hashCode()}_$mediaItemId"
}

@Composable
@androidx.annotation.OptIn(UnstableApi::class)
fun PagerState.LaunchedEffectScrollToPage(index: Int) {
    val pagerState = this
    LaunchedEffect(pagerState, index) {
        pagerState.scrollToPage(index)
    }
}

@Composable
private fun OptimizedCanvasVideoButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .shadow(3.dp, RoundedCornerShape(8.dp), clip = false)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.86f))
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.16f),
                    style = Stroke(width = 0.5.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
            }
            .size(width = 42.dp, height = 34.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.video),
            contentDescription = "Show video",
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(18.dp)
        )
    }
}
