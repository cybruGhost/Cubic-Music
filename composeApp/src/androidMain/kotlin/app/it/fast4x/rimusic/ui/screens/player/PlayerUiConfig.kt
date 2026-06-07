package app.it.fast4x.rimusic.ui.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import app.it.fast4x.rimusic.enums.AnimatedGradient
import app.it.fast4x.rimusic.enums.BackgroundProgress
import app.it.fast4x.rimusic.enums.CarouselSize
import app.it.fast4x.rimusic.enums.ColorPaletteMode
import app.it.fast4x.rimusic.enums.DurationInMilliseconds
import app.it.fast4x.rimusic.enums.PlayerBackgroundColors
import app.it.fast4x.rimusic.enums.PlayerThumbnailSize
import app.it.fast4x.rimusic.enums.PlayerType
import app.it.fast4x.rimusic.enums.QueueLoopType
import app.it.fast4x.rimusic.enums.QueueType
import app.it.fast4x.rimusic.enums.SwipeAnimationNoThumbnail
import app.it.fast4x.rimusic.enums.ThumbnailCoverType
import app.it.fast4x.rimusic.enums.ThumbnailRoundness
import app.it.fast4x.rimusic.enums.ThumbnailType
import app.it.fast4x.rimusic.utils.VinylSizeKey
import app.it.fast4x.rimusic.utils.albumCoverRotationKey
import app.it.fast4x.rimusic.utils.animatedGradientKey
import app.it.fast4x.rimusic.utils.backgroundProgressKey
import app.it.fast4x.rimusic.utils.blackgradientKey
import app.it.fast4x.rimusic.utils.bottomgradientKey
import app.it.fast4x.rimusic.utils.carouselKey
import app.it.fast4x.rimusic.utils.carouselSizeKey
import app.it.fast4x.rimusic.utils.clickOnLyricsTextKey
import app.it.fast4x.rimusic.utils.colorPaletteModeKey
import app.it.fast4x.rimusic.utils.controlsExpandedKey
import app.it.fast4x.rimusic.utils.coverThumbnailAnimationKey
import app.it.fast4x.rimusic.utils.disablePlayerHorizontalSwipeKey
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.discoverKey
import app.it.fast4x.rimusic.utils.effectRotationKey
import app.it.fast4x.rimusic.utils.expandedplayerKey
import app.it.fast4x.rimusic.utils.extraspaceKey
import app.it.fast4x.rimusic.utils.fadingedgeKey
import app.it.fast4x.rimusic.utils.noblurKey
import app.it.fast4x.rimusic.utils.playerBackgroundColorsKey
import app.it.fast4x.rimusic.utils.playerThumbnailSizeKey
import app.it.fast4x.rimusic.utils.playerThumbnailSizeLKey
import app.it.fast4x.rimusic.utils.playerTypeKey
import app.it.fast4x.rimusic.utils.queueDurationExpandedKey
import app.it.fast4x.rimusic.utils.queueLoopTypeKey
import app.it.fast4x.rimusic.utils.queueTypeKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.showButtonPlayerMenuKey
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

@Stable
internal class PlayerUiConfig(
    val disablePlayerHorizontalSwipe: Boolean,
    val showlyricsthumbnail: Boolean,
    val effectRotationEnabled: Boolean,
    val playerThumbnailSizeState: MutableState<PlayerThumbnailSize>,
    val playerThumbnailSizeLState: MutableState<PlayerThumbnailSize>,
    val showvisthumbnail: Boolean,
    val thumbnailSpacingState: MutableState<Float>,
    val thumbnailSpacingLState: MutableState<Float>,
    val thumbnailFadeState: MutableState<Float>,
    val thumbnailFadeExState: MutableState<Float>,
    val imageCoverSizeState: MutableState<Float>,
    val queueDurationExpanded: Boolean,
    val statsExpanded: Boolean,
    val showthumbnailState: MutableState<Boolean>,
    val showButtonPlayerMenu: Boolean,
    val showTotalTimeQueue: Boolean,
    val backgroundProgress: BackgroundProgress,
    val queueLoopState: MutableState<QueueLoopType>,
    val playerType: PlayerType,
    val queueType: QueueType,
    val noblur: Boolean,
    val fadingedge: Boolean,
    val colorPaletteMode: ColorPaletteMode,
    val playerBackgroundColors: PlayerBackgroundColors,
    val animatedGradient: AnimatedGradient,
    val thumbnailTapEnabled: Boolean,
    val showTopActionsBar: Boolean,
    val blackgradient: Boolean,
    val bottomgradient: Boolean,
    val disableScrollingText: Boolean,
    val discoverState: MutableState<Boolean>,
    val titleExpanded: Boolean,
    val timelineExpanded: Boolean,
    val controlsExpanded: Boolean,
    val showCoverThumbnailAnimation: Boolean,
    val coverThumbnailAnimationState: MutableState<ThumbnailCoverType>,
    val albumCoverRotationState: MutableState<Boolean>,
    val textoutline: Boolean,
    val carousel: Boolean,
    val carouselSize: CarouselSize,
    val clickLyricsText: Boolean,
    val extraspaceState: MutableState<Boolean>,
    val thumbnailRoundness: ThumbnailRoundness,
    val thumbnailType: ThumbnailType,
    val statsfornerds: Boolean,
    val topPadding: Boolean,
    val swipeAnimationNoThumbnailState: MutableState<SwipeAnimationNoThumbnail>,
    val expandedPlayerState: MutableState<Boolean>,
    val spotifyCanvasEnabled: Boolean,
    val showSpotifyCanvasLogs: Boolean,
    val alternateSourceRetryEnabled: Boolean,
    val playbackFadeAudioDuration: DurationInMilliseconds,
)

@Composable
internal fun rememberPlayerUiConfig(): PlayerUiConfig {
    val playerThumbnailSizeState = rememberPreference(playerThumbnailSizeKey, PlayerThumbnailSize.Biggest)
    val playerThumbnailSizeLState = rememberPreference(playerThumbnailSizeLKey, PlayerThumbnailSize.Biggest)
    val thumbnailSpacingState = rememberPreference(thumbnailSpacingKey, 0f)
    val thumbnailSpacingLState = rememberPreference(thumbnailSpacingLKey, 0f)
    val thumbnailFadeState = rememberPreference(thumbnailFadeKey, 5f)
    val thumbnailFadeExState = rememberPreference(thumbnailFadeExKey, 5f)
    val imageCoverSizeState = rememberPreference(VinylSizeKey, 50f)
    val showthumbnailState = rememberPreference(showthumbnailKey, true)
    val queueLoopState = rememberPreference(queueLoopTypeKey, QueueLoopType.Default)
    val discoverState = rememberPreference(discoverKey, false)
    val coverThumbnailAnimationState = rememberPreference(coverThumbnailAnimationKey, ThumbnailCoverType.Vinyl)
    val albumCoverRotationState = rememberPreference(albumCoverRotationKey, false)
    val extraspaceState = rememberPreference(extraspaceKey, false)
    val swipeAnimationNoThumbnailState = rememberPreference(swipeAnimationsNoThumbnailKey, SwipeAnimationNoThumbnail.Sliding)
    val expandedPlayerState = rememberPreference(expandedplayerKey, false)

    return PlayerUiConfig(
        disablePlayerHorizontalSwipe = rememberPreference(disablePlayerHorizontalSwipeKey, false).value,
        showlyricsthumbnail = rememberPreference(showlyricsthumbnailKey, false).value,
        effectRotationEnabled = rememberPreference(effectRotationKey, true).value,
        playerThumbnailSizeState = playerThumbnailSizeState,
        playerThumbnailSizeLState = playerThumbnailSizeLState,
        showvisthumbnail = rememberPreference(showvisthumbnailKey, false).value,
        thumbnailSpacingState = thumbnailSpacingState,
        thumbnailSpacingLState = thumbnailSpacingLState,
        thumbnailFadeState = thumbnailFadeState,
        thumbnailFadeExState = thumbnailFadeExState,
        imageCoverSizeState = imageCoverSizeState,
        queueDurationExpanded = rememberPreference(queueDurationExpandedKey, true).value,
        statsExpanded = rememberPreference(statsExpandedKey, true).value,
        showthumbnailState = showthumbnailState,
        showButtonPlayerMenu = rememberPreference(showButtonPlayerMenuKey, false).value,
        showTotalTimeQueue = rememberPreference(showTotalTimeQueueKey, true).value,
        backgroundProgress = rememberPreference(backgroundProgressKey, BackgroundProgress.MiniPlayer).value,
        queueLoopState = queueLoopState,
        playerType = rememberPreference(playerTypeKey, PlayerType.Essential).value,
        queueType = rememberPreference(queueTypeKey, QueueType.Essential).value,
        noblur = rememberPreference(noblurKey, true).value,
        fadingedge = rememberPreference(fadingedgeKey, false).value,
        colorPaletteMode = rememberPreference(colorPaletteModeKey, ColorPaletteMode.Dark).value,
        playerBackgroundColors = rememberPreference(playerBackgroundColorsKey, PlayerBackgroundColors.BlurredCoverColor).value,
        animatedGradient = rememberPreference(animatedGradientKey, AnimatedGradient.Linear).value,
        thumbnailTapEnabled = rememberPreference(thumbnailTapEnabledKey, true).value,
        showTopActionsBar = rememberPreference(showTopActionsBarKey, true).value,
        blackgradient = rememberPreference(blackgradientKey, false).value,
        bottomgradient = rememberPreference(bottomgradientKey, false).value,
        disableScrollingText = rememberPreference(disableScrollingTextKey, false).value,
        discoverState = discoverState,
        titleExpanded = rememberPreference(titleExpandedKey, true).value,
        timelineExpanded = rememberPreference(timelineExpandedKey, true).value,
        controlsExpanded = rememberPreference(controlsExpandedKey, true).value,
        showCoverThumbnailAnimation = rememberPreference(showCoverThumbnailAnimationKey, false).value,
        coverThumbnailAnimationState = coverThumbnailAnimationState,
        albumCoverRotationState = albumCoverRotationState,
        textoutline = rememberPreference(textoutlineKey, false).value,
        carousel = rememberPreference(carouselKey, true).value,
        carouselSize = rememberPreference(carouselSizeKey, CarouselSize.Biggest).value,
        clickLyricsText = rememberPreference(clickOnLyricsTextKey, true).value,
        extraspaceState = extraspaceState,
        thumbnailRoundness = rememberPreference(thumbnailRoundnessKey, ThumbnailRoundness.Heavy).value,
        thumbnailType = rememberPreference(thumbnailTypeKey, ThumbnailType.Modern).value,
        statsfornerds = rememberPreference(statsfornerdsKey, false).value,
        topPadding = rememberPreference(topPaddingKey, true).value,
        swipeAnimationNoThumbnailState = swipeAnimationNoThumbnailState,
        expandedPlayerState = expandedPlayerState,
        spotifyCanvasEnabled = rememberPreference("spotifyCanvasEnabled", false).value,
        showSpotifyCanvasLogs = rememberPreference("showSpotifyCanvasLogs", false).value,
        alternateSourceRetryEnabled = rememberPreference("alternateSourceRetryKey", true).value,
        playbackFadeAudioDuration = rememberPreference("playbackFadeAudioDurationKey", DurationInMilliseconds.Disabled).value,
    )
}