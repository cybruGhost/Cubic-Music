package app.it.fast4x.rimusic.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.AnimatedGradient
import app.it.fast4x.rimusic.enums.BackgroundProgress
import app.it.fast4x.rimusic.enums.CarouselSize
import app.it.fast4x.rimusic.enums.ColorPaletteMode
import app.it.fast4x.rimusic.enums.ColorPaletteName
import app.it.fast4x.rimusic.enums.IconLikeType
import app.it.fast4x.rimusic.enums.MiniPlayerType
import app.it.fast4x.rimusic.enums.NavigationBarPosition
import app.it.fast4x.rimusic.enums.NotificationButtons
import app.it.fast4x.rimusic.enums.PlayerBackgroundColors
import app.it.fast4x.rimusic.enums.PlayerControlsType
import app.it.fast4x.rimusic.enums.PlayerInfoType
import app.it.fast4x.rimusic.enums.PlayerPlayButtonType
import app.it.fast4x.rimusic.enums.PlayerThumbnailSize
import app.it.fast4x.rimusic.enums.PlayerTimelineSize
import app.it.fast4x.rimusic.enums.PlayerTimelineType
import app.it.fast4x.rimusic.enums.PlayerType
import app.it.fast4x.rimusic.enums.PrevNextSongs
import app.it.fast4x.rimusic.enums.QueueType
import app.it.fast4x.rimusic.enums.SongsNumber
import app.it.fast4x.rimusic.enums.SwipeAnimationNoThumbnail
import app.it.fast4x.rimusic.enums.ThumbnailCoverType
import app.it.fast4x.rimusic.enums.ThumbnailRoundness
import app.it.fast4x.rimusic.enums.ThumbnailType
import app.it.fast4x.rimusic.enums.WallpaperType
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.themed.AppearancePresetDialog
import app.it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import app.it.fast4x.rimusic.ui.styling.Dimensions
import app.it.fast4x.rimusic.utils.RestartPlayerService
import app.it.fast4x.rimusic.utils.actionExpandedKey
import app.it.fast4x.rimusic.utils.actionspacedevenlyKey
import app.it.fast4x.rimusic.utils.albumCoverRotationKey
import app.it.fast4x.rimusic.utils.animatedGradientKey
import app.it.fast4x.rimusic.utils.backgroundProgressKey
import app.it.fast4x.rimusic.utils.blackgradientKey
import app.it.fast4x.rimusic.utils.blurStrengthKey
import app.it.fast4x.rimusic.utils.bottomgradientKey
import app.it.fast4x.rimusic.utils.buttonzoomoutKey
import app.it.fast4x.rimusic.utils.carouselKey
import app.it.fast4x.rimusic.utils.carouselSizeKey
import app.it.fast4x.rimusic.utils.clickOnLyricsTextKey
import app.it.fast4x.rimusic.utils.colorPaletteModeKey
import app.it.fast4x.rimusic.utils.colorPaletteNameKey
import app.it.fast4x.rimusic.utils.controlsExpandedKey
import app.it.fast4x.rimusic.utils.coverThumbnailAnimationKey
import app.it.fast4x.rimusic.utils.disablePlayerHorizontalSwipeKey
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.effectRotationKey
import app.it.fast4x.rimusic.utils.enableWallpaperKey
import app.it.fast4x.rimusic.utils.expandedplayerKey
import app.it.fast4x.rimusic.utils.expandedplayertoggleKey
import app.it.fast4x.rimusic.utils.fadingedgeKey
import app.it.fast4x.rimusic.utils.iconLikeTypeKey
import app.it.fast4x.rimusic.utils.isAtLeastAndroid7
import app.it.fast4x.rimusic.utils.isLandscape
import app.it.fast4x.rimusic.utils.isShowingThumbnailInLockscreenKey
import app.it.fast4x.rimusic.utils.keepPlayerMinimizedKey
import app.it.fast4x.rimusic.utils.lastPlayerPlayButtonTypeKey
import app.it.fast4x.rimusic.utils.miniPlayerTypeKey
import app.it.fast4x.rimusic.utils.miniQueueExpandedKey
import app.it.fast4x.rimusic.utils.navigationBarPositionKey
import app.it.fast4x.rimusic.utils.noblurKey
import app.it.fast4x.rimusic.utils.notificationPlayerFirstIconKey
import app.it.fast4x.rimusic.utils.notificationPlayerSecondIconKey
import app.it.fast4x.rimusic.utils.playerBackgroundColorsKey
import app.it.fast4x.rimusic.utils.playerControlsTypeKey
import app.it.fast4x.rimusic.utils.playerEnableLyricsPopupMessageKey
import app.it.fast4x.rimusic.utils.playerInfoShowIconsKey
import app.it.fast4x.rimusic.utils.playerInfoTypeKey
import app.it.fast4x.rimusic.utils.playerPlayButtonTypeKey
import app.it.fast4x.rimusic.utils.playerSwapControlsWithTimelineKey
import app.it.fast4x.rimusic.utils.playerThumbnailSizeKey
import app.it.fast4x.rimusic.utils.playerThumbnailSizeLKey
import app.it.fast4x.rimusic.utils.playerTimelineSizeKey
import app.it.fast4x.rimusic.utils.playerTimelineTypeKey
import app.it.fast4x.rimusic.utils.playerTypeKey
import app.it.fast4x.rimusic.utils.prevNextSongsKey
import app.it.fast4x.rimusic.utils.queueDurationExpandedKey
import app.it.fast4x.rimusic.utils.queueTypeKey
import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.rotatingAlbumCoverKey
import app.it.fast4x.rimusic.utils.semiBold
import app.it.fast4x.rimusic.utils.showBackgroundLyricsKey
import app.it.fast4x.rimusic.utils.showButtonPlayerAddToPlaylistKey
import app.it.fast4x.rimusic.utils.showButtonPlayerArrowKey
import app.it.fast4x.rimusic.utils.showButtonPlayerDiscoverKey
import app.it.fast4x.rimusic.utils.showButtonPlayerDownloadKey
import app.it.fast4x.rimusic.utils.showButtonPlayerLoopKey
import app.it.fast4x.rimusic.utils.showButtonPlayerLyricsKey
import app.it.fast4x.rimusic.utils.showButtonPlayerMenuKey
import app.it.fast4x.rimusic.utils.showButtonPlayerShuffleKey
import app.it.fast4x.rimusic.utils.showButtonPlayerSleepTimerKey
import app.it.fast4x.rimusic.utils.showButtonPlayerStartRadioKey
import app.it.fast4x.rimusic.utils.showButtonPlayerSystemEqualizerKey
import app.it.fast4x.rimusic.utils.showButtonPlayerVideoKey
import app.it.fast4x.rimusic.utils.showCoverThumbnailAnimationKey
import app.it.fast4x.rimusic.utils.showDownloadButtonBackgroundPlayerKey
import app.it.fast4x.rimusic.utils.showLikeButtonBackgroundPlayerKey
import app.it.fast4x.rimusic.utils.showLyricsStateKey
import app.it.fast4x.rimusic.utils.showNextSongsInPlayerKey
import app.it.fast4x.rimusic.utils.showPlaybackSpeedButtonKey
import app.it.fast4x.rimusic.utils.showRemainingSongTimeKey
import app.it.fast4x.rimusic.utils.showTopActionsBarKey
import app.it.fast4x.rimusic.utils.showTotalTimeQueueKey
import app.it.fast4x.rimusic.utils.showVisualizerStateKey
import app.it.fast4x.rimusic.utils.showalbumcoverKey
import app.it.fast4x.rimusic.utils.showlyricsthumbnailKey
import app.it.fast4x.rimusic.utils.showsongsKey
import app.it.fast4x.rimusic.utils.showthumbnailKey
import app.it.fast4x.rimusic.utils.showvisthumbnailKey
import app.it.fast4x.rimusic.utils.statsExpandedKey
import app.it.fast4x.rimusic.utils.statsfornerdsKey
import app.it.fast4x.rimusic.utils.swipeAnimationsNoThumbnailKey
import app.it.fast4x.rimusic.utils.swipeUpQueueKey
import app.it.fast4x.rimusic.utils.tapqueueKey
import app.it.fast4x.rimusic.utils.textoutlineKey
import app.it.fast4x.rimusic.utils.thumbnailFadeExKey
import app.it.fast4x.rimusic.utils.thumbnailFadeKey
import app.it.fast4x.rimusic.utils.thumbnailRoundnessKey
import app.it.fast4x.rimusic.utils.thumbnailSpacingKey
import app.it.fast4x.rimusic.utils.thumbnailTapEnabledKey
import app.it.fast4x.rimusic.utils.thumbnailTypeKey
import app.it.fast4x.rimusic.utils.thumbnailpauseKey
import app.it.fast4x.rimusic.utils.timelineExpandedKey
import app.it.fast4x.rimusic.utils.titleExpandedKey
import app.it.fast4x.rimusic.utils.topPaddingKey
import app.it.fast4x.rimusic.utils.transparentBackgroundPlayerActionBarKey
import app.it.fast4x.rimusic.utils.transparentbarKey
import app.it.fast4x.rimusic.utils.visualizerEnabledKey
import app.it.fast4x.rimusic.utils.wallpaperTypeKey
import app.kreate.android.me.knighthat.component.tab.Search
import app.kreate.android.me.knighthat.utils.Toaster
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.shadow

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Updated FunctionalPresetChip with selection indicators (glow + underline)
@Composable
fun FunctionalPresetChip(
    colors: List<Color>,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(42.dp) // Increased height for underline
            .background(
                brush = Brush.linearGradient(colors),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) 
                    Color.White 
                    else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .shadow(
                elevation = if (isSelected) 10.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = colors[0].copy(alpha = 0.7f),
                spotColor = colors[0].copy(alpha = 0.7f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BasicText(
                text = label,
                style = typography().s.semiBold.copy(
                    color = Color.White
                ),
                maxLines = 1
            )
            
            // Underline indicator for selected preset
            if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}
@Composable
fun DefaultAppearanceSettings() {
    var isShowingThumbnailInLockscreen by rememberPreference(
        isShowingThumbnailInLockscreenKey,
        true
    )
    isShowingThumbnailInLockscreen = true
    var showthumbnail by rememberPreference(showthumbnailKey, true)
    showthumbnail = true
    var transparentbar by rememberPreference(transparentbarKey, true)
    transparentbar = true
    var blackgradient by rememberPreference(blackgradientKey, false)

    blackgradient = false
    var showlyricsthumbnail by rememberPreference(showlyricsthumbnailKey, false)
    showlyricsthumbnail = false
    var playerPlayButtonType by rememberPreference(
        playerPlayButtonTypeKey,
        PlayerPlayButtonType.Disabled
    )
    playerPlayButtonType = PlayerPlayButtonType.Disabled
    var bottomgradient by rememberPreference(bottomgradientKey, false)
    bottomgradient = false
    var textoutline by rememberPreference(textoutlineKey, false)
    textoutline = false
    var lastPlayerPlayButtonType by rememberPreference(
        lastPlayerPlayButtonTypeKey,
        PlayerPlayButtonType.Rectangular
    )
    lastPlayerPlayButtonType = PlayerPlayButtonType.Rectangular
    var disablePlayerHorizontalSwipe by rememberPreference(disablePlayerHorizontalSwipeKey, false)
    disablePlayerHorizontalSwipe = false
    var disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    disableScrollingText = false
    var showLikeButtonBackgroundPlayer by rememberPreference(
        showLikeButtonBackgroundPlayerKey,
        true
    )
    showLikeButtonBackgroundPlayer = true
    var showDownloadButtonBackgroundPlayer by rememberPreference(
        showDownloadButtonBackgroundPlayerKey,
        true
    )
    showDownloadButtonBackgroundPlayer = true
    var visualizerEnabled by rememberPreference(visualizerEnabledKey, false)
    visualizerEnabled = false
    var playerTimelineType by rememberPreference(playerTimelineTypeKey, PlayerTimelineType.FakeAudioBar)
    playerTimelineType = PlayerTimelineType.FakeAudioBar
    var playerThumbnailSize by rememberPreference(
        playerThumbnailSizeKey,
        PlayerThumbnailSize.Biggest
    )
    playerThumbnailSize = PlayerThumbnailSize.Biggest
    var playerTimelineSize by rememberPreference(
        playerTimelineSizeKey,
        PlayerTimelineSize.Biggest
    )
    playerTimelineSize = PlayerTimelineSize.Biggest
    var effectRotationEnabled by rememberPreference(effectRotationKey, true)
    effectRotationEnabled = true
    var thumbnailTapEnabled by rememberPreference(thumbnailTapEnabledKey, true)
    thumbnailTapEnabled = true
    var showButtonPlayerAddToPlaylist by rememberPreference(showButtonPlayerAddToPlaylistKey, true)
    showButtonPlayerAddToPlaylist = true
    var showButtonPlayerArrow by rememberPreference(showButtonPlayerArrowKey, true)
    showButtonPlayerArrow = false
    var showButtonPlayerDownload by rememberPreference(showButtonPlayerDownloadKey, true)
    showButtonPlayerDownload = true
    var showButtonPlayerLoop by rememberPreference(showButtonPlayerLoopKey, false)
    showButtonPlayerLoop = false
    var showButtonPlayerLyrics by rememberPreference(showButtonPlayerLyricsKey, true)
    showButtonPlayerLyrics = true
    var expandedplayertoggle by rememberPreference(expandedplayertoggleKey, false)
    expandedplayertoggle = false
    var showButtonPlayerShuffle by rememberPreference(showButtonPlayerShuffleKey, true)
    showButtonPlayerShuffle = true
    var showButtonPlayerSleepTimer by rememberPreference(showButtonPlayerSleepTimerKey, false)
    showButtonPlayerSleepTimer = false
    var showButtonPlayerMenu by rememberPreference(showButtonPlayerMenuKey, false)
    showButtonPlayerMenu = true
    var showButtonPlayerSystemEqualizer by rememberPreference(
        showButtonPlayerSystemEqualizerKey,
        false
    )
    showButtonPlayerSystemEqualizer = false
    var showButtonPlayerDiscover by rememberPreference(showButtonPlayerDiscoverKey, false)
    showButtonPlayerDiscover = false
    var showButtonPlayerVideo by rememberPreference(showButtonPlayerVideoKey, false)
    showButtonPlayerVideo = false
    var navigationBarPosition by rememberPreference(
        navigationBarPositionKey,
        NavigationBarPosition.Bottom
    )
    navigationBarPosition = NavigationBarPosition.Bottom
    var showTotalTimeQueue by rememberPreference(showTotalTimeQueueKey, true)
    showTotalTimeQueue = true
    var backgroundProgress by rememberPreference(
        backgroundProgressKey,
        BackgroundProgress.MiniPlayer
    )
    backgroundProgress = BackgroundProgress.MiniPlayer
    var showNextSongsInPlayer by rememberPreference(showNextSongsInPlayerKey, true)
    showNextSongsInPlayer = true
    var showRemainingSongTime by rememberPreference(showRemainingSongTimeKey, true)
    showRemainingSongTime = true
    var clickLyricsText by rememberPreference(clickOnLyricsTextKey, true)
    clickLyricsText = true
    var showBackgroundLyrics by rememberPreference(showBackgroundLyricsKey, true)
    showBackgroundLyrics = false
    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )
    thumbnailRoundness = ThumbnailRoundness.Heavy
    var miniPlayerType by rememberPreference(
        miniPlayerTypeKey,
        MiniPlayerType.Modern
    )
    miniPlayerType = MiniPlayerType.Modern
    var playerBackgroundColors by rememberPreference(
        playerBackgroundColorsKey,
        PlayerBackgroundColors.BlurredCoverColor
    )
    playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
    var showTopActionsBar by rememberPreference(showTopActionsBarKey, true)
    showTopActionsBar = true
    var playerControlsType by rememberPreference(playerControlsTypeKey, PlayerControlsType.Essential)
    playerControlsType = PlayerControlsType.Modern
    var playerInfoType by rememberPreference(playerInfoTypeKey, PlayerInfoType.Essential)
    playerInfoType = PlayerInfoType.Modern
    var transparentBackgroundActionBarPlayer by rememberPreference(
        transparentBackgroundPlayerActionBarKey,
        true
    )
    transparentBackgroundActionBarPlayer = true
    var iconLikeType by rememberPreference(iconLikeTypeKey, IconLikeType.Essential)
    iconLikeType = IconLikeType.Essential
    var playerSwapControlsWithTimeline by rememberPreference(
        playerSwapControlsWithTimelineKey,
        false
    )
    playerSwapControlsWithTimeline = false
    var playerEnableLyricsPopupMessage by rememberPreference(
        playerEnableLyricsPopupMessageKey,
        true
    )
    playerEnableLyricsPopupMessage = true
    var actionspacedevenly by rememberPreference(actionspacedevenlyKey, false)
    actionspacedevenly = true
    var thumbnailType by rememberPreference(thumbnailTypeKey, ThumbnailType.Modern)
    thumbnailType = ThumbnailType.Modern
    var showvisthumbnail by rememberPreference(showvisthumbnailKey, false)
    showvisthumbnail = false
    var buttonzoomout by rememberPreference(buttonzoomoutKey, false)
    buttonzoomout = false
    var thumbnailpause by rememberPreference(thumbnailpauseKey, false)
    thumbnailpause = false
    var showsongs by rememberPreference(showsongsKey, SongsNumber.`2`)
    showsongs = SongsNumber.`2`
    var showalbumcover by rememberPreference(showalbumcoverKey, true)
    showalbumcover = true
    var prevNextSongs by rememberPreference(prevNextSongsKey, PrevNextSongs.twosongs)
    prevNextSongs = PrevNextSongs.twosongs
    var tapqueue by rememberPreference(tapqueueKey, true)
    tapqueue = true
    var swipeUpQueue by rememberPreference(swipeUpQueueKey, true)
    swipeUpQueue = true
    var statsfornerds by rememberPreference(statsfornerdsKey, false)
    statsfornerds = false
    var playerType by rememberPreference(playerTypeKey, PlayerType.Essential)
    playerType = PlayerType.Essential
    var queueType by rememberPreference(queueTypeKey, QueueType.Essential)
    queueType = QueueType.Essential
    var noblur by rememberPreference(noblurKey, true)
    noblur = true
    var fadingedge by rememberPreference(fadingedgeKey, false)
    fadingedge = false
    var carousel by rememberPreference(carouselKey, true)
    carousel = true
    var carouselSize by rememberPreference(carouselSizeKey, CarouselSize.Biggest)
    carouselSize = CarouselSize.Biggest
    var keepPlayerMinimized by rememberPreference(keepPlayerMinimizedKey,false)
    keepPlayerMinimized = false
    var playerInfoShowIcons by rememberPreference(playerInfoShowIconsKey, true)
    playerInfoShowIcons = true
}

@ExperimentalAnimationApi
@UnstableApi
@Composable
fun AppearanceSettings(
    navController: NavController,
) {

    var isShowingThumbnailInLockscreen by rememberPreference(
        isShowingThumbnailInLockscreenKey,
        true
    )

    var showthumbnail by rememberPreference(showthumbnailKey, true)
    var transparentbar by rememberPreference(transparentbarKey, true)
    var blackgradient by rememberPreference(blackgradientKey, false)
    var showlyricsthumbnail by rememberPreference(showlyricsthumbnailKey, false)
    var expandedplayer by rememberPreference(expandedplayerKey, false)
    var playerPlayButtonType by rememberPreference(
        playerPlayButtonTypeKey,
        PlayerPlayButtonType.Disabled
    )
    var bottomgradient by rememberPreference(bottomgradientKey, false)
    var textoutline by rememberPreference(textoutlineKey, false)

    var lastPlayerPlayButtonType by rememberPreference(
        lastPlayerPlayButtonTypeKey,
        PlayerPlayButtonType.Rectangular
    )
    var disablePlayerHorizontalSwipe by rememberPreference(disablePlayerHorizontalSwipeKey, false)

    var disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    var showLikeButtonBackgroundPlayer by rememberPreference(
        showLikeButtonBackgroundPlayerKey,
        true
    )
    var showDownloadButtonBackgroundPlayer by rememberPreference(
        showDownloadButtonBackgroundPlayerKey,
        true
    )
    var visualizerEnabled by rememberPreference(visualizerEnabledKey, false)
    /*
    var playerVisualizerType by rememberPreference(
        playerVisualizerTypeKey,
        PlayerVisualizerType.Disabled
    )
    */
    var playerTimelineType by rememberPreference(playerTimelineTypeKey, PlayerTimelineType.FakeAudioBar)
    var playerThumbnailSize by rememberPreference(
        playerThumbnailSizeKey,
        PlayerThumbnailSize.Biggest
    )
    var playerThumbnailSizeL by rememberPreference(
        playerThumbnailSizeLKey,
        PlayerThumbnailSize.Biggest
    )
    var playerTimelineSize by rememberPreference(
        playerTimelineSizeKey,
        PlayerTimelineSize.Biggest
    )
    //

    var effectRotationEnabled by rememberPreference(effectRotationKey, true)

    var thumbnailTapEnabled by rememberPreference(thumbnailTapEnabledKey, true)


    var showButtonPlayerAddToPlaylist by rememberPreference(showButtonPlayerAddToPlaylistKey, true)
    var showButtonPlayerArrow by rememberPreference(showButtonPlayerArrowKey, true)
    var showButtonPlayerDownload by rememberPreference(showButtonPlayerDownloadKey, true)
    var showButtonPlayerLoop by rememberPreference(showButtonPlayerLoopKey, false)
    var showButtonPlayerLyrics by rememberPreference(showButtonPlayerLyricsKey, true)
    var expandedplayertoggle by rememberPreference(expandedplayertoggleKey, false)
    var showButtonPlayerShuffle by rememberPreference(showButtonPlayerShuffleKey, true)
    var showButtonPlayerSleepTimer by rememberPreference(showButtonPlayerSleepTimerKey, false)
    var showButtonPlayerMenu by rememberPreference(showButtonPlayerMenuKey, false)
    var showButtonPlayerStartradio by rememberPreference(showButtonPlayerStartRadioKey, false)
    var showButtonPlayerSystemEqualizer by rememberPreference(
        showButtonPlayerSystemEqualizerKey,
        false
    )
    var showButtonPlayerDiscover by rememberPreference(showButtonPlayerDiscoverKey, false)
    var showButtonPlayerVideo by rememberPreference(showButtonPlayerVideoKey, false)

    val navigationBarPosition by rememberPreference(
        navigationBarPositionKey,
        NavigationBarPosition.Bottom
    )

    //var isGradientBackgroundEnabled by rememberPreference(isGradientBackgroundEnabledKey, false)
    var showTotalTimeQueue by rememberPreference(showTotalTimeQueueKey, true)
    var backgroundProgress by rememberPreference(
        backgroundProgressKey,
        BackgroundProgress.MiniPlayer
    )
    var showNextSongsInPlayer by rememberPreference(showNextSongsInPlayerKey, false)
    var showRemainingSongTime by rememberPreference(showRemainingSongTimeKey, true)
    var clickLyricsText by rememberPreference(clickOnLyricsTextKey, true)
    var showBackgroundLyrics by rememberPreference(showBackgroundLyricsKey, false)

    val search = Search()

    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )

    var miniPlayerType by rememberPreference(
        miniPlayerTypeKey,
        MiniPlayerType.Modern
    )
    var playerBackgroundColors by rememberPreference(
        playerBackgroundColorsKey,
        PlayerBackgroundColors.BlurredCoverColor
    )

    var showTopActionsBar by rememberPreference(showTopActionsBarKey, true)
    var playerControlsType by rememberPreference(playerControlsTypeKey, PlayerControlsType.Essential)
    var playerInfoType by rememberPreference(playerInfoTypeKey, PlayerInfoType.Essential)
    var transparentBackgroundActionBarPlayer by rememberPreference(
        transparentBackgroundPlayerActionBarKey,
        false
    )
    var iconLikeType by rememberPreference(iconLikeTypeKey, IconLikeType.Essential)
    var playerSwapControlsWithTimeline by rememberPreference(
        playerSwapControlsWithTimelineKey,
        false
    )
    var playerEnableLyricsPopupMessage by rememberPreference(
        playerEnableLyricsPopupMessageKey,
        true
    )
    var actionspacedevenly by rememberPreference(actionspacedevenlyKey, false)
    var thumbnailType by rememberPreference(thumbnailTypeKey, ThumbnailType.Modern)
    var showvisthumbnail by rememberPreference(showvisthumbnailKey, false)
    var buttonzoomout by rememberPreference(buttonzoomoutKey, false)
    var thumbnailpause by rememberPreference(thumbnailpauseKey, false)
    var showsongs by rememberPreference(showsongsKey, SongsNumber.`2`)
    var showalbumcover by rememberPreference(showalbumcoverKey, true)
    var prevNextSongs by rememberPreference(prevNextSongsKey, PrevNextSongs.twosongs)
    var tapqueue by rememberPreference(tapqueueKey, true)
    var swipeUpQueue by rememberPreference(swipeUpQueueKey, true)
    var statsfornerds by rememberPreference(statsfornerdsKey, false)

    var playerType by rememberPreference(playerTypeKey, PlayerType.Essential)
    var queueType by rememberPreference(queueTypeKey, QueueType.Essential)
    var noblur by rememberPreference(noblurKey, true)
    var fadingedge by rememberPreference(fadingedgeKey, false)
    var carousel by rememberPreference(carouselKey, true)
    var carouselSize by rememberPreference(carouselSizeKey, CarouselSize.Biggest)
    var keepPlayerMinimized by rememberPreference(keepPlayerMinimizedKey,false)
    var playerInfoShowIcons by rememberPreference(playerInfoShowIconsKey, true)
    var queueDurationExpanded by rememberPreference(queueDurationExpandedKey, true)
    var titleExpanded by rememberPreference(titleExpandedKey, true)
    var timelineExpanded by rememberPreference(timelineExpandedKey, true)
    var controlsExpanded by rememberPreference(controlsExpandedKey, true)
    var miniQueueExpanded by rememberPreference(miniQueueExpandedKey, true)
    var statsExpanded by rememberPreference(statsExpandedKey, true)
    var actionExpanded by rememberPreference(actionExpandedKey, true)
    var restartService by rememberSaveable { mutableStateOf(false) }
    var showCoverThumbnailAnimation by rememberPreference(showCoverThumbnailAnimationKey, false)
    var coverThumbnailAnimation by rememberPreference(coverThumbnailAnimationKey, ThumbnailCoverType.Vinyl)
    var showLyricsStateKey by rememberPreference(showLyricsStateKey, false)
    var showVisualizerStateKey by rememberPreference(showVisualizerStateKey, false)

    var notificationPlayerFirstIcon by rememberPreference(notificationPlayerFirstIconKey, NotificationButtons.Download)
    var notificationPlayerSecondIcon by rememberPreference(notificationPlayerSecondIconKey, NotificationButtons.Favorites)
    var enableWallpaper by rememberPreference(enableWallpaperKey, false)
    var wallpaperType by rememberPreference(wallpaperTypeKey, WallpaperType.Lockscreen)
    var topPadding by rememberPreference(topPaddingKey, true)
    var animatedGradient by rememberPreference(
        animatedGradientKey,
        AnimatedGradient.Linear
    )
    var appearanceChooser by remember{ mutableStateOf(false)}
    var albumCoverRotation by rememberPreference(albumCoverRotationKey, false)

    Column(
        modifier = Modifier
            .background(colorPalette().background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(
                if (navigationBarPosition == NavigationBarPosition.Left ||
                    navigationBarPosition == NavigationBarPosition.Top ||
                    navigationBarPosition == NavigationBarPosition.Bottom
                ) 1f
                else Dimensions.contentWidthRightBar
            )
            .verticalScroll(rememberScrollState())
        /*
        .padding(
            LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                .asPaddingValues()
        )

         */
    ) {
        HeaderWithIcon(
            title = stringResource(R.string.player_appearance),
            iconId = R.drawable.color_palette,
            enabled = false,
            showIcon = true,
            modifier = Modifier,
            onClick = {}
        )

        search.ToolBarButton()
        search.SearchBar( this )

        //SettingsEntryGroupText(stringResource(R.string.user_interface))

        //SettingsGroupSpacer()
        SettingsEntryGroupText(title = stringResource(R.string.player))

        if (playerBackgroundColors != PlayerBackgroundColors.BlurredCoverColor)
            showthumbnail = true
        if (!visualizerEnabled) showvisthumbnail = false
        if (!showthumbnail) {showlyricsthumbnail = false; showvisthumbnail = false}
        if (playerType == PlayerType.Modern) {
            showlyricsthumbnail = false
            showvisthumbnail = false
            thumbnailpause = false
            //keepPlayerMinimized = false
        }
        var blurStrength by rememberPreference(blurStrengthKey, 25f)
        var thumbnailFadeEx  by rememberPreference(thumbnailFadeExKey, 5f)
        var thumbnailFade  by rememberPreference(thumbnailFadeKey, 5f)
        var thumbnailSpacing  by rememberPreference(thumbnailSpacingKey, 0f)
        var selectedPresetIndex by remember { mutableStateOf(-1) }
        var colorPaletteName by rememberPreference(colorPaletteNameKey, ColorPaletteName.Dynamic)
        var colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.Dark)
        var swipeAnimationNoThumbnail by rememberPreference(swipeAnimationsNoThumbnailKey, SwipeAnimationNoThumbnail.Sliding)

        if (appearanceChooser){
            AppearancePresetDialog(
            onDismiss = {appearanceChooser = false},
            onClick0 = {
                showTopActionsBar = true
                showthumbnail = true
                playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
                blurStrength = 50f
                thumbnailRoundness = ThumbnailRoundness.Heavy
                playerInfoType = PlayerInfoType.Essential
                playerTimelineType = PlayerTimelineType.Wavy
                playerTimelineSize = PlayerTimelineSize.Biggest
                playerControlsType = PlayerControlsType.Essential
                playerPlayButtonType = PlayerPlayButtonType.Disabled
                transparentbar = true
                playerType = PlayerType.Essential
                showlyricsthumbnail = false
                expandedplayer = true
                thumbnailType = ThumbnailType.Essential
                playerThumbnailSize = PlayerThumbnailSize.Big
                showTotalTimeQueue = false
                bottomgradient = true
                showRemainingSongTime = true
                showNextSongsInPlayer = true
                colorPaletteName = ColorPaletteName.Dynamic
                colorPaletteMode = ColorPaletteMode.System
                ///////ACTION BAR BUTTONS////////////////
                transparentBackgroundActionBarPlayer = true
                actionspacedevenly = true
                showButtonPlayerVideo = false
                showButtonPlayerDiscover = false
                showButtonPlayerDownload = true
                showButtonPlayerAddToPlaylist = true
                showButtonPlayerLoop = false
                showButtonPlayerShuffle = true
                showButtonPlayerLyrics = true
                expandedplayertoggle = false
                showButtonPlayerSleepTimer = false
                visualizerEnabled = false
                appearanceChooser = false
                showButtonPlayerArrow = false
                showButtonPlayerStartradio = true
                showButtonPlayerMenu = true
                ///////////////////////////
                appearanceChooser = false
            },
            onClick1 = {
                showTopActionsBar = true
                showthumbnail = true
                playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
                blurStrength = 50f
                playerInfoType = PlayerInfoType.Essential
                playerPlayButtonType = PlayerPlayButtonType.Disabled
                playerTimelineType = PlayerTimelineType.ThinBar
                playerControlsType = PlayerControlsType.Essential
                transparentbar = true
                playerType = PlayerType.Modern
                expandedplayer = true
                fadingedge = true
                thumbnailFadeEx = 4f
                thumbnailSpacing = -32f
                thumbnailType = ThumbnailType.Essential
                carouselSize = CarouselSize.Big
                playerThumbnailSize = PlayerThumbnailSize.Biggest
                showTotalTimeQueue = false
                transparentBackgroundActionBarPlayer = true
                showRemainingSongTime = true
                bottomgradient = true
                showlyricsthumbnail = false
                thumbnailRoundness = ThumbnailRoundness.Medium
                showNextSongsInPlayer = true
                colorPaletteName = ColorPaletteName.Dynamic
                colorPaletteMode = ColorPaletteMode.System
                ///////ACTION BAR BUTTONS////////////////
                transparentBackgroundActionBarPlayer = true
                actionspacedevenly = true
                showButtonPlayerVideo = false
                showButtonPlayerDiscover = false
                showButtonPlayerDownload = true
                showButtonPlayerAddToPlaylist = true
                showButtonPlayerLoop = false
                showButtonPlayerShuffle = false
                showButtonPlayerLyrics =  true
                expandedplayertoggle = false
                showButtonPlayerSleepTimer = false
                visualizerEnabled = false
                appearanceChooser = false
                showButtonPlayerArrow = false
                showButtonPlayerStartradio = true
                showButtonPlayerMenu = true
                ///////////////////////////
                appearanceChooser = false
            },
            onClick2 = {
                showTopActionsBar = false
                showthumbnail = false
                noblur = true
                topPadding = false
                playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
                blurStrength = 50f
                playerPlayButtonType = PlayerPlayButtonType.Disabled
                playerInfoType = PlayerInfoType.Modern
                playerInfoShowIcons = false
                playerTimelineType = PlayerTimelineType.ThinBar
                playerControlsType = PlayerControlsType.Essential
                transparentbar = true
                playerType = PlayerType.Modern
                expandedplayer = true
                showTotalTimeQueue = false
                transparentBackgroundActionBarPlayer = true
                showRemainingSongTime = true
                bottomgradient = true
                showlyricsthumbnail = false
                showNextSongsInPlayer = true
                colorPaletteName = ColorPaletteName.Dynamic
                colorPaletteMode = ColorPaletteMode.System
                ///////ACTION BAR BUTTONS////////////////
                transparentBackgroundActionBarPlayer = true
                actionspacedevenly = true
                showButtonPlayerVideo = false
                showButtonPlayerDiscover = false
                showButtonPlayerDownload = true
                showButtonPlayerAddToPlaylist = false
                showButtonPlayerLoop = false
                showButtonPlayerShuffle = false
                showButtonPlayerLyrics =  true
                expandedplayertoggle = false
                showButtonPlayerSleepTimer = false
                visualizerEnabled = false
                appearanceChooser = false
                showButtonPlayerArrow = false
                showButtonPlayerStartradio = true
                showButtonPlayerMenu = true
                ///////////////////////////
                appearanceChooser = false
            },
            onClick3 = {
                showTopActionsBar = false
                topPadding = false
                showthumbnail = true
                playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
                blurStrength = 50f
                playerInfoType = PlayerInfoType.Essential
                playerTimelineType = PlayerTimelineType.FakeAudioBar
                playerTimelineSize = PlayerTimelineSize.Biggest
                playerControlsType = PlayerControlsType.Modern
                playerPlayButtonType = PlayerPlayButtonType.Disabled
                colorPaletteName = ColorPaletteName.PureBlack
                transparentbar = false
                playerType = PlayerType.Essential
                expandedplayer = false
                playerThumbnailSize = PlayerThumbnailSize.Expanded
                showTotalTimeQueue = false
                transparentBackgroundActionBarPlayer = true
                showRemainingSongTime = true
                bottomgradient = true
                showlyricsthumbnail = false
                thumbnailType = ThumbnailType.Essential
                thumbnailRoundness = ThumbnailRoundness.Light
                playerType = PlayerType.Modern
                fadingedge = true
                thumbnailFade = 5f
                showNextSongsInPlayer = true
                ///////ACTION BAR BUTTONS////////////////
                transparentBackgroundActionBarPlayer = true
                actionspacedevenly = true
                showButtonPlayerVideo = false
                showButtonPlayerDiscover = false
                showButtonPlayerDownload = true
                showButtonPlayerAddToPlaylist = false
                showButtonPlayerLoop = true
                showButtonPlayerShuffle = true
                showButtonPlayerLyrics =  true
                expandedplayertoggle = false
                showButtonPlayerSleepTimer = false
                visualizerEnabled = false
                appearanceChooser = false
                showButtonPlayerArrow = true
                showButtonPlayerStartradio = true
                showButtonPlayerMenu = true
                ///////////////////////////
                appearanceChooser = false
            },
            onClick4 = {
                showTopActionsBar = false
                topPadding = true
                showthumbnail = true
                playerBackgroundColors = PlayerBackgroundColors.AnimatedGradient
                animatedGradient = AnimatedGradient.Linear
                playerInfoType = PlayerInfoType.Essential
                playerTimelineType = PlayerTimelineType.PinBar
                playerTimelineSize = PlayerTimelineSize.Biggest
                playerControlsType = PlayerControlsType.Essential
                playerPlayButtonType = PlayerPlayButtonType.Square
                colorPaletteName = ColorPaletteName.Dynamic
                colorPaletteMode = ColorPaletteMode.PitchBlack
                transparentbar = false
                playerType = PlayerType.Modern
                expandedplayer = false
                playerThumbnailSize = PlayerThumbnailSize.Biggest
                showTotalTimeQueue = false
                transparentBackgroundActionBarPlayer = true
                showRemainingSongTime = true
                showlyricsthumbnail = false
                thumbnailType = ThumbnailType.Modern
                thumbnailRoundness = ThumbnailRoundness.Heavy
                fadingedge = true
                thumbnailFade = 0f
                thumbnailFadeEx = 5f
                thumbnailSpacing = -32f
                showNextSongsInPlayer = true
                ///////ACTION BAR BUTTONS////////////////
                transparentBackgroundActionBarPlayer = true
                actionspacedevenly = true
                showButtonPlayerVideo = false
                showButtonPlayerDiscover = false
                showButtonPlayerDownload = true
                showButtonPlayerAddToPlaylist = false
                showButtonPlayerLoop = false
                showButtonPlayerShuffle = false
                showButtonPlayerLyrics =  true
                expandedplayertoggle = false
                showButtonPlayerSleepTimer = false
                visualizerEnabled = false
                appearanceChooser = false
                showButtonPlayerArrow =false
                showButtonPlayerStartradio = true
                showButtonPlayerMenu = true
                ///////////////////////////
                appearanceChooser = false
            },
            onClick5 = {
                showTopActionsBar = true
                showthumbnail = true
                playerBackgroundColors = PlayerBackgroundColors.CoverColorGradient
                playerInfoType = PlayerInfoType.Essential
                playerTimelineType = PlayerTimelineType.Ocean
                playerTimelineSize = PlayerTimelineSize.Biggest
                playerControlsType = PlayerControlsType.Essential
                playerPlayButtonType = PlayerPlayButtonType.CircularRibbed
                colorPaletteName = ColorPaletteName.Dynamic
                colorPaletteMode = ColorPaletteMode.System
                transparentbar = false
                playerType = PlayerType.Essential
                expandedplayer = true
                playerThumbnailSize = PlayerThumbnailSize.Big
                showTotalTimeQueue = false
                transparentBackgroundActionBarPlayer = true
                showRemainingSongTime = true
                showlyricsthumbnail = false
                thumbnailType = ThumbnailType.Modern
                thumbnailRoundness = ThumbnailRoundness.Heavy
                showNextSongsInPlayer = true
                ///////ACTION BAR BUTTONS////////////////
                transparentBackgroundActionBarPlayer = true
                actionspacedevenly = true
                showButtonPlayerVideo = false
                showButtonPlayerDiscover = false
                showButtonPlayerDownload = true
                showButtonPlayerAddToPlaylist = false
                showButtonPlayerLoop = false
                showButtonPlayerShuffle = true
                showButtonPlayerLyrics = true
                expandedplayertoggle = false
                showButtonPlayerSleepTimer = false
                visualizerEnabled = false
                appearanceChooser = false
                showButtonPlayerArrow =false
                showButtonPlayerStartradio = true
                showButtonPlayerMenu = true
                ///////////////////////////
                appearanceChooser = false
            }
            )
        }
        
if (!isLandscape) {
    // Enhanced Appearance Presets Selector with FUNCTIONAL presets
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colorPalette().background1,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = colorPalette().accent.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(vertical = 4.dp)
    ) {
        // Header - clickable to open dialog
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { appearanceChooser = true }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Main title with accent color
                BasicText(
                    text = stringResource(R.string.appearancepresets),
                    style = typography().l.semiBold.copy(
                        color = colorPalette().accent
                    )
                )
                
                // Secondary description
                BasicText(
                    text = stringResource(R.string.appearancepresetssecondary),
                    style = typography().s.semiBold.copy(
                        color = colorPalette().textSecondary
                    )
                )
                
                // Visual indicator of presets count
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        colorPalette().accent,
                        colorPalette().accent.copy(alpha = 0.7f),
                        colorPalette().accent.copy(alpha = 0.4f)
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = color,
                                    shape = CircleShape
                                )
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    BasicText(
                        text = "6 ${stringResource(R.string.appearancepresets)}",
                        style = typography().xs.semiBold.copy(
                            color = colorPalette().textSecondary
                        )
                    )
                }
            }
            
            // Enhanced arrow with layered background
            Box(
                modifier = Modifier
                    .background(
                        color = colorPalette().accent.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
                    .size(48.dp)
                    .clickable { appearanceChooser = true },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = colorPalette().accent.copy(alpha = 0.25f),
                            shape = CircleShape
                        )
                        .size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_forward),
                        contentDescription = stringResource(R.string.appearancepresets),
                        modifier = Modifier.size(22.dp),
                        tint = colorPalette().accent
                    )
                }
            }
        }
        
        // Horizontal scrollable row for preset chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Preset 1 - Classic
                FunctionalPresetChip(
                    colors = listOf(
                        colorPalette().accent,
                        colorPalette().accent.copy(alpha = 0.7f)
                    ),
                    label = stringResource(R.string.preset_classic).take(3),
                    isSelected = selectedPresetIndex == 0,
                    onClick = {
                        selectedPresetIndex = 0
                        showTopActionsBar = true
                        showthumbnail = true
                        playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
                        blurStrength = 50f
                        thumbnailRoundness = ThumbnailRoundness.Heavy
                        playerInfoType = PlayerInfoType.Essential
                        playerTimelineType = PlayerTimelineType.Wavy
                        playerTimelineSize = PlayerTimelineSize.Biggest
                        playerControlsType = PlayerControlsType.Essential
                        playerPlayButtonType = PlayerPlayButtonType.Disabled
                        transparentbar = true
                        playerType = PlayerType.Essential
                        showlyricsthumbnail = false
                        expandedplayer = true
                        thumbnailType = ThumbnailType.Essential
                        playerThumbnailSize = PlayerThumbnailSize.Big
                        showTotalTimeQueue = false
                        bottomgradient = true
                        showRemainingSongTime = true
                        showNextSongsInPlayer = true
                        colorPaletteName = ColorPaletteName.Dynamic
                        colorPaletteMode = ColorPaletteMode.System
                    }
                )
                
                // Preset 2 - Modern
                FunctionalPresetChip(
                    colors = listOf(
                        colorPalette().text,
                        colorPalette().textSecondary
                    ),
                    label = stringResource(R.string.preset_modern).take(3),
                    isSelected = selectedPresetIndex == 1,
                    onClick = {
                        selectedPresetIndex = 1
                        showTopActionsBar = true
                        showthumbnail = true
                        playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
                        blurStrength = 50f
                        playerInfoType = PlayerInfoType.Essential
                        playerPlayButtonType = PlayerPlayButtonType.Disabled
                        playerTimelineType = PlayerTimelineType.ThinBar
                        playerControlsType = PlayerControlsType.Essential
                        transparentbar = true
                        playerType = PlayerType.Modern
                        expandedplayer = true
                        fadingedge = true
                        thumbnailFadeEx = 4f
                        thumbnailSpacing = -32f
                        thumbnailType = ThumbnailType.Essential
                        carouselSize = CarouselSize.Big
                        playerThumbnailSize = PlayerThumbnailSize.Biggest
                        showTotalTimeQueue = false
                        showRemainingSongTime = true
                        bottomgradient = true
                        showlyricsthumbnail = false
                        thumbnailRoundness = ThumbnailRoundness.Medium
                        showNextSongsInPlayer = true
                        colorPaletteName = ColorPaletteName.Dynamic
                        colorPaletteMode = ColorPaletteMode.System
                    }
                )
                
                // Preset 3 - Minimal
                FunctionalPresetChip(
                    colors = listOf(
                        colorPalette().background2,
                        colorPalette().background1
                    ),
                    label = stringResource(R.string.preset_minimal).take(3),
                    isSelected = selectedPresetIndex == 2,
                    onClick = {
                        selectedPresetIndex = 2
                        showTopActionsBar = false
                        showthumbnail = false
                        noblur = true
                        topPadding = false
                        playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
                        blurStrength = 50f
                        playerPlayButtonType = PlayerPlayButtonType.Disabled
                        playerInfoType = PlayerInfoType.Modern
                        playerInfoShowIcons = false
                        playerTimelineType = PlayerTimelineType.ThinBar
                        playerControlsType = PlayerControlsType.Essential
                        transparentbar = true
                        playerType = PlayerType.Modern
                        expandedplayer = true
                        showTotalTimeQueue = false
                        showRemainingSongTime = true
                        bottomgradient = true
                        showlyricsthumbnail = false
                        showNextSongsInPlayer = true
                        colorPaletteName = ColorPaletteName.Dynamic
                        colorPaletteMode = ColorPaletteMode.System
                    }
                )
                
                // Preset 4 - Dark Edge
                FunctionalPresetChip(
                    colors = listOf(
                        Color.Black,
                        Color.DarkGray
                    ),
                    label = stringResource(R.string.preset_dark_edge).take(3),
                    isSelected = selectedPresetIndex == 3,
                    onClick = {
                        selectedPresetIndex = 3
                        showTopActionsBar = false
                        topPadding = false
                        showthumbnail = true
                        playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
                        blurStrength = 50f
                        playerInfoType = PlayerInfoType.Essential
                        playerTimelineType = PlayerTimelineType.FakeAudioBar
                        playerTimelineSize = PlayerTimelineSize.Biggest
                        playerControlsType = PlayerControlsType.Modern
                        playerPlayButtonType = PlayerPlayButtonType.Disabled
                        colorPaletteName = ColorPaletteName.PureBlack
                        transparentbar = false
                        playerType = PlayerType.Essential
                        expandedplayer = false
                        playerThumbnailSize = PlayerThumbnailSize.Expanded
                        showTotalTimeQueue = false
                        showRemainingSongTime = true
                        bottomgradient = true
                        showlyricsthumbnail = false
                        thumbnailType = ThumbnailType.Essential
                        thumbnailRoundness = ThumbnailRoundness.Light
                        playerType = PlayerType.Modern
                        fadingedge = true
                        thumbnailFade = 5f
                        showNextSongsInPlayer = true
                    }
                )
                
                // Preset 5 - Gradient
                FunctionalPresetChip(
                    colors = listOf(
                        Color(0xFF4158D0),
                        Color(0xFFC850C0)
                    ),
                    label = stringResource(R.string.preset_gradient).take(3),
                    isSelected = selectedPresetIndex == 4,
                    onClick = {
                        selectedPresetIndex = 4
                        showTopActionsBar = false
                        topPadding = true
                        showthumbnail = true
                        playerBackgroundColors = PlayerBackgroundColors.AnimatedGradient
                        animatedGradient = AnimatedGradient.Linear
                        playerInfoType = PlayerInfoType.Essential
                        playerTimelineType = PlayerTimelineType.PinBar
                        playerTimelineSize = PlayerTimelineSize.Biggest
                        playerControlsType = PlayerControlsType.Essential
                        playerPlayButtonType = PlayerPlayButtonType.Square
                        colorPaletteName = ColorPaletteName.Dynamic
                        colorPaletteMode = ColorPaletteMode.PitchBlack
                        transparentbar = false
                        playerType = PlayerType.Modern
                        expandedplayer = false
                        playerThumbnailSize = PlayerThumbnailSize.Biggest
                        showTotalTimeQueue = false
                        showRemainingSongTime = true
                        showlyricsthumbnail = false
                        thumbnailType = ThumbnailType.Modern
                        thumbnailRoundness = ThumbnailRoundness.Heavy
                        fadingedge = true
                        thumbnailFade = 0f
                        thumbnailFadeEx = 5f
                        thumbnailSpacing = -32f
                        showNextSongsInPlayer = true
                    }
                )
                
                // Preset 6 - Vibrant
                FunctionalPresetChip(
                    colors = listOf(
                        Color(0xFFFF6B6B),
                        Color(0xFF4ECDC4)
                    ),
                    label = stringResource(R.string.preset_vibrant).take(3),
                    isSelected = selectedPresetIndex == 5,
                    onClick = {
                        selectedPresetIndex = 5
                        showTopActionsBar = true
                        showthumbnail = true
                        playerBackgroundColors = PlayerBackgroundColors.CoverColorGradient
                        playerInfoType = PlayerInfoType.Essential
                        playerTimelineType = PlayerTimelineType.Ocean
                        playerTimelineSize = PlayerTimelineSize.Biggest
                        playerControlsType = PlayerControlsType.Essential
                        playerPlayButtonType = PlayerPlayButtonType.CircularRibbed
                        colorPaletteName = ColorPaletteName.Dynamic
                        colorPaletteMode = ColorPaletteMode.System
                        transparentbar = false
                        playerType = PlayerType.Essential
                        expandedplayer = true
                        playerThumbnailSize = PlayerThumbnailSize.Big
                        showTotalTimeQueue = false
                        showRemainingSongTime = true
                        showlyricsthumbnail = false
                        thumbnailType = ThumbnailType.Modern
                        thumbnailRoundness = ThumbnailRoundness.Heavy
                        showNextSongsInPlayer = true
                    }
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))


    if (search.inputValue.isBlank() || stringResource(R.string.show_player_top_actions_bar).contains(
            search.inputValue,
            true
        )
    )
        SwitchSettingEntry(
            title = stringResource(R.string.show_player_top_actions_bar),
            text = "",
            isChecked = showTopActionsBar,
            onCheckedChange = { showTopActionsBar = it }
        )

    if (!showTopActionsBar) {
        if (search.inputValue.isBlank() || stringResource(R.string.blankspace).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.blankspace),
                text = "",
                isChecked = topPadding,
                onCheckedChange = { topPadding = it }
            )
    }
}
        if (search.inputValue.isBlank() || stringResource(R.string.playertype).contains(
                search.inputValue,
                true
            )
        )
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.playertype),
                selectedValue = playerType,
                onValueSelected = {
                    playerType = it
                },
                valueText = { it.text },
            )

        if (search.inputValue.isBlank() || stringResource(R.string.queuetype).contains(
                search.inputValue,
                true
            )
        )
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.queuetype),
                selectedValue = queueType,
                onValueSelected = {
                    queueType = it
                },
                valueText = { it.text },
            )

        if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) {
            if (search.inputValue.isBlank() || stringResource(R.string.show_thumbnail).contains(
                    search.inputValue,
                    true
                )
            )
                SwitchSettingEntry(
                    title = stringResource(R.string.show_thumbnail),
                    text = "",
                    isChecked = showthumbnail,
                    onCheckedChange = {showthumbnail = it},
                )
        }
        AnimatedVisibility(visible = !showthumbnail && playerType == PlayerType.Modern && !isLandscape) {
            if (search.inputValue.isBlank() || stringResource(R.string.swipe_Animation_No_Thumbnail).contains(
                    search.inputValue,
                    true
                )
            )
                EnumValueSelectorSettingsEntry(
                    title = stringResource(R.string.swipe_Animation_No_Thumbnail),
                    selectedValue = swipeAnimationNoThumbnail,
                    onValueSelected = { swipeAnimationNoThumbnail = it },
                    valueText = {
                        when (it) {
                            SwipeAnimationNoThumbnail.Sliding -> stringResource(R.string.te_slide_vertical)
                            SwipeAnimationNoThumbnail.Fade -> stringResource(R.string.te_fade)
                            SwipeAnimationNoThumbnail.Scale -> stringResource(R.string.te_scale)
                            SwipeAnimationNoThumbnail.Carousel -> stringResource(R.string.carousel)
                            SwipeAnimationNoThumbnail.Circle -> stringResource(R.string.vt_circular)
                        }
                    },
                    modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 25.dp else 0.dp)
                )
        }
        AnimatedVisibility(visible = showthumbnail) {
            Column {
                if (playerType == PlayerType.Modern) {
                    if (search.inputValue.isBlank() || stringResource(R.string.fadingedge).contains(
                            search.inputValue,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.fadingedge),
                            text = "",
                            isChecked = fadingedge,
                            onCheckedChange = { fadingedge = it },
                            modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 25.dp else 0.dp)
                        )
                }

                if (playerType == PlayerType.Modern && !isLandscape && (expandedplayertoggle || expandedplayer)) {
                    if (search.inputValue.isBlank() || stringResource(R.string.carousel).contains(
                            search.inputValue,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.carousel),
                            text = "",
                            isChecked = carousel,
                            onCheckedChange = { carousel = it },
                            modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 25.dp else 0.dp)
                        )

                    if (search.inputValue.isBlank() || stringResource(R.string.carouselsize).contains(
                            search.inputValue,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.carouselsize),
                            selectedValue = carouselSize,
                            onValueSelected = { carouselSize = it },
                            valueText = { it.text },
                            modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 25.dp else 0.dp)
                        )
                }
                if (playerType == PlayerType.Essential) {

                    if (search.inputValue.isBlank() || stringResource(R.string.thumbnailpause).contains(
                            search.inputValue,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.thumbnailpause),
                            text = "",
                            isChecked = thumbnailpause,
                            onCheckedChange = { thumbnailpause = it },
                            modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 25.dp else 0.dp)
                        )

                    if (search.inputValue.isBlank() || stringResource(R.string.show_lyrics_thumbnail).contains(
                            search.inputValue,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.show_lyrics_thumbnail),
                            text = "",
                            isChecked = showlyricsthumbnail,
                            onCheckedChange = { showlyricsthumbnail = it },
                            modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 25.dp else 0.dp)
                        )
                    if (visualizerEnabled) {
                        if (search.inputValue.isBlank() || stringResource(R.string.showvisthumbnail).contains(
                                search.inputValue,
                                true
                            )
                        )
                            SwitchSettingEntry(
                                title = stringResource(R.string.showvisthumbnail),
                                text = "",
                                isChecked = showvisthumbnail,
                                onCheckedChange = { showvisthumbnail = it },
                                modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 25.dp else 0.dp)
                            )
                    }
                }

                if (search.inputValue.isBlank() || stringResource(R.string.show_cover_thumbnail_animation).contains(
                        search.inputValue,
                        true
                    )
                ) {
                    SwitchSettingEntry(
                        title = stringResource(R.string.show_cover_thumbnail_animation),
                        text = "",
                        isChecked = showCoverThumbnailAnimation,
                        onCheckedChange = { showCoverThumbnailAnimation = it },
                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 25.dp else 0.dp)
                    )
                    AnimatedVisibility(visible = showCoverThumbnailAnimation) {
                        Column {
                            EnumValueSelectorSettingsEntry(
                                title = stringResource(R.string.cover_thumbnail_animation_type),
                                selectedValue = coverThumbnailAnimation,
                                onValueSelected = { coverThumbnailAnimation = it },
                                valueText = { it.text },
                                modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 50.dp else 25.dp)
                            )
                        }
                    }
                }

                if (isLandscape) {
                    if (search.inputValue.isBlank() || stringResource(R.string.player_thumbnail_size).contains(
                            search.inputValue,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.player_thumbnail_size),
                            selectedValue = playerThumbnailSizeL,
                            onValueSelected = { playerThumbnailSizeL = it },
                            valueText = { it.text },
                            modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 25.dp else 0.dp)
                        )
                } else {
                    if (search.inputValue.isBlank() || stringResource(R.string.player_thumbnail_size).contains(
                            search.inputValue,
                            true
                        )
                    )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(R.string.player_thumbnail_size),
                            selectedValue = playerThumbnailSize,
                            onValueSelected = { playerThumbnailSize = it },
                            valueText = { it.text },
                            modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 25.dp else 0.dp)
                        )
                }
                if (search.inputValue.isBlank() || stringResource(R.string.thumbnailtype).contains(
                        search.inputValue,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.thumbnailtype),
                        selectedValue = thumbnailType,
                        onValueSelected = {
                            thumbnailType = it
                        },
                        valueText = { it.text },
                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 25.dp else 0.dp)
                    )

                if (search.inputValue.isBlank() || stringResource(R.string.thumbnail_roundness).contains(
                        search.inputValue,
                        true
                    )
                )
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.thumbnail_roundness),
                        selectedValue = thumbnailRoundness,
                        onValueSelected = { thumbnailRoundness = it },
                        trailingContent = {
                            Spacer(
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = colorPalette().accent,
                                        shape = thumbnailRoundness.shape
                                    )
                                    .background(
                                        color = colorPalette().background1,
                                        shape = thumbnailRoundness.shape
                                    )
                                    .size(36.dp)
                            )
                        },
                        valueText = { it.text },
                        modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) 25.dp else 0.dp)
                    )
            }
        }

        if (!showthumbnail) {
            if (search.inputValue.isBlank() || stringResource(R.string.noblur).contains(
                    search.inputValue,
                    true
                )
            )
                SwitchSettingEntry(
                    title = stringResource(R.string.noblur),
                    text = "",
                    isChecked = noblur,
                    onCheckedChange = { noblur = it }
                )


        }

        if (!(showthumbnail && playerType == PlayerType.Essential)){
            if (search.inputValue.isBlank() || stringResource(R.string.statsfornerdsplayer).contains(
                    search.inputValue,
                    true
                )
            )
                SwitchSettingEntry(
                    title = stringResource(R.string.statsfornerdsplayer),
                    text = "",
                    isChecked = statsfornerds,
                    onCheckedChange = { statsfornerds = it }
                )
        }

        if (search.inputValue.isBlank() || stringResource(R.string.timelinesize).contains(
                search.inputValue,
                true
            )
        )
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.timelinesize),
                selectedValue = playerTimelineSize,
                onValueSelected = { playerTimelineSize = it },
                valueText = { it.text }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.pinfo_type).contains(
                search.inputValue,
                true
            )
        ) {
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.pinfo_type),
                selectedValue = playerInfoType,
                onValueSelected = {
                    playerInfoType = it
                },
                valueText = { it.text },
            )
            SettingsDescription(text = stringResource(R.string.pinfo_album_and_artist_name))

            AnimatedVisibility( visible = playerInfoType == PlayerInfoType.Modern) {
                Column {
                    if (search.inputValue.isBlank() || stringResource(R.string.pinfo_show_icons).contains(
                            search.inputValue,
                            true
                        )
                    )
                        SwitchSettingEntry(
                            title = stringResource(R.string.pinfo_show_icons),
                            text = "",
                            isChecked = playerInfoShowIcons,
                            onCheckedChange = { playerInfoShowIcons = it },
                            modifier = Modifier
                                .padding(start = 25.dp)
                        )
                }
            }

        }



        if (search.inputValue.isBlank() || stringResource(R.string.miniplayertype).contains(
                search.inputValue,
                true
            )
        )
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.miniplayertype),
                selectedValue = miniPlayerType,
                onValueSelected = {
                    miniPlayerType = it
                },
                valueText = { it.text },
            )

             if (search.inputValue.isBlank() || stringResource(R.string.player_swap_controls_with_timeline).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.player_swap_controls_with_timeline),
                text = "",
                isChecked = playerSwapControlsWithTimeline,
                onCheckedChange = { playerSwapControlsWithTimeline = it }
            )

        // Enhanced Timeline UI Section
        if (search.inputValue.isBlank() || 
            stringResource(R.string.timeline).contains(search.inputValue, true) ||
            stringResource(R.string.transparentbar).contains(search.inputValue, true)) {
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(
                        color = colorPalette().background1.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                // Timeline Header with simple preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        BasicText(
                            text = "Timeline Settings",
                            style = typography().s.semiBold.copy(color = colorPalette().text)
                        )
                        BasicText(
                            text = "Customize progress bar appearance",
                            style = typography().xs.semiBold.copy(color = colorPalette().textSecondary)
                        )
                    }
                    
                    // Simple preview box
                    Box(
                        modifier = Modifier
                            .size(48.dp, 24.dp)
                            .background(
                                color = if (transparentbar) 
                                    colorPalette().accent.copy(alpha = 0.3f) 
                                    else colorPalette().accent,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
                
                // Timeline Type Selection using EnumValueSelectorSettingsEntry
               if (
                    search.inputValue.isBlank() ||
                    stringResource(R.string.timeline).contains(search.inputValue, true)
                ) {
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.timeline_style),
                        selectedValue = playerTimelineType,
                        onValueSelected = { playerTimelineType = it },
                        valueText = { it.name }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Transparent bar toggle with visual feedback
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { transparentbar = !transparentbar }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = if (transparentbar) 
                                        colorPalette().accent.copy(alpha = 0.3f)
                                        else colorPalette().textDisabled,
                                    shape = CircleShape
                                )
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        BasicText(
                            text = stringResource(R.string.transparentbar),
                            style = typography().s.semiBold.copy(
                                color = if (transparentbar)
                                    colorPalette().text
                                else
                                    colorPalette().textDisabled
                            )
                        )

                        BasicText(
                            text = stringResource(R.string.transparentbar_description),
                            style = typography().xs.semiBold.copy(
                                color = colorPalette().textSecondary
                            )
                        )
                    }
                     }
                    // Simple switch indicator
                    Box(
                        modifier = Modifier
                            .size(40.dp, 24.dp)
                            .background(
                                color = if (transparentbar) colorPalette().accent else colorPalette().background2,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 2.dp),
                        contentAlignment = if (transparentbar) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = colorPalette().background0,
                                    shape = CircleShape
                                )
                        )
                    }
                }
                
                // Timeline Size Selector (using existing enum values)
                if (search.inputValue.isBlank() || stringResource(R.string.timelinesize).contains(search.inputValue, true)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                text = "Timeline Thickness",
                                style = typography().s.semiBold.copy(color = colorPalette().text)
                            )
                            
                            BasicText(
                                text = playerTimelineSize.name,
                                style = typography().s.semiBold.copy(color = colorPalette().accent)
                            )
                        }
                        
                        // Simple size indicator row using only available enum values
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Use only the enum values that exist in your project
                            listOf(
                                PlayerTimelineSize.Small,
                                PlayerTimelineSize.Medium,
                                PlayerTimelineSize.Big,
                                PlayerTimelineSize.Biggest
                            ).forEach { size ->
                                val height = when (size) {
                                    PlayerTimelineSize.Small -> 8.dp
                                    PlayerTimelineSize.Medium -> 12.dp
                                    PlayerTimelineSize.Big -> 16.dp
                                    PlayerTimelineSize.Biggest -> 20.dp
                                    else -> 12.dp
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(24.dp)
                                        .clickable { playerTimelineSize = size }
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(height)
                                            .background(
                                                color = if (size == playerTimelineSize) 
                                                    colorPalette().accent 
                                                    else colorPalette().textDisabled.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Keep the original settings as they are - don't remove these
        if (search.inputValue.isBlank() || stringResource(R.string.timeline).contains(
                search.inputValue,
                true
            )
        )
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.timeline),
                selectedValue = playerTimelineType,
                onValueSelected = { playerTimelineType = it },
                valueText = { it.text }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.transparentbar).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.transparentbar),
                text = "",
                isChecked = transparentbar,
                onCheckedChange = { transparentbar = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.pcontrols_type).contains(
                search.inputValue,
                true
            )
        )
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.pcontrols_type),
                selectedValue = playerControlsType,
                onValueSelected = {
                    playerControlsType = it
                },
                valueText = { it.text }
            )


        if (search.inputValue.isBlank() || stringResource(R.string.play_button).contains(
                search.inputValue,
                true
            )
        )
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.play_button),
                selectedValue = playerPlayButtonType,
                onValueSelected = {
                    playerPlayButtonType = it
                    lastPlayerPlayButtonType = it
                },
                valueText = { it.text }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.buttonzoomout).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.buttonzoomout),
                text = "",
                isChecked = buttonzoomout,
                onCheckedChange = { buttonzoomout = it }
            )


        if (search.inputValue.isBlank() || stringResource(R.string.play_button).contains(
                search.inputValue,
                true
            )
        )
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.icon_like_button),
                selectedValue = iconLikeType,
                onValueSelected = {
                    iconLikeType = it
                },
                valueText = { it.text },
            )

        /*

        if (filter.isNullOrBlank() || stringResource(R.string.use_gradient_background).contains(filterCharSequence,true))
            SwitchSettingEntry(
                title = stringResource(R.string.use_gradient_background),
                text = "",
                isChecked = isGradientBackgroundEnabled,
                onCheckedChange = { isGradientBackgroundEnabled = it }
            )
         */

        if (search.inputValue.isBlank() || stringResource(R.string.background_colors).contains(
                search.inputValue,
                true
            )
        )
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.background_colors),
                selectedValue = playerBackgroundColors,
                onValueSelected = {
                    playerBackgroundColors = it
                },
                valueText = { it.text }
            )

        AnimatedVisibility(visible = playerBackgroundColors == PlayerBackgroundColors.AnimatedGradient) {
            if (search.inputValue.isBlank() || stringResource(R.string.gradienttype).contains(
                    search.inputValue,
                    true
                )
            )
                EnumValueSelectorSettingsEntry(
                    title = stringResource(R.string.gradienttype),
                    selectedValue = animatedGradient,
                    onValueSelected = {
                        animatedGradient = it
                    },
                    valueText = { it.text },
                    modifier = Modifier.padding(start = if (playerBackgroundColors == PlayerBackgroundColors.AnimatedGradient) 25.dp else 0.dp)
                )
        }
        var isRotatingCoverEnabled by rememberPreference( rotatingAlbumCoverKey, false )
        AnimatedVisibility( playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor ) {
            if ( search.inputValue.isBlank() || stringResource( R.string.rotating_cover_title ).contains(search.inputValue, true) )
                SwitchSettingEntry(
                    title = stringResource( R.string.rotating_cover_title ),
                    text = "",
                    isChecked = isRotatingCoverEnabled,
                    onCheckedChange = { isRotatingCoverEnabled = it },
                    modifier = Modifier.padding( start = 25.dp )
                )
        }


        if ((playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient) || (playerBackgroundColors == PlayerBackgroundColors.ThemeColorGradient))
            if (search.inputValue.isBlank() || stringResource(R.string.blackgradient).contains(
                    search.inputValue,
                    true
                )
            )
                SwitchSettingEntry(
                    title = stringResource(R.string.blackgradient),
                    text = "",
                    isChecked = blackgradient,
                    onCheckedChange = { blackgradient = it }
                )

        if ((playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor) && (playerType == PlayerType.Modern))
            if (search.inputValue.isBlank() || stringResource(R.string.albumCoverRotation).contains(
                    search.inputValue,
                    true
                )
            )
                SwitchSettingEntry(
                    title = stringResource(R.string.albumCoverRotation),
                    text = "",
                    isChecked = albumCoverRotation,
                    onCheckedChange = { albumCoverRotation = it },
                    modifier = Modifier
                        .padding(start = 25.dp)
                )

        if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor)
            if (search.inputValue.isBlank() || stringResource(R.string.bottomgradient).contains(
                    search.inputValue,
                    true
                )
            )
                SwitchSettingEntry(
                    title = stringResource(R.string.bottomgradient),
                    text = "",
                    isChecked = bottomgradient,
                    onCheckedChange = { bottomgradient = it }
                )
        if (search.inputValue.isBlank() || stringResource(R.string.textoutline).contains(
              search.inputValue,
              true
              )
         )
             SwitchSettingEntry(
                 title = stringResource(R.string.textoutline),
                 text = "",
                 isChecked = textoutline,
                 onCheckedChange = { textoutline = it }
             )

       if (search.inputValue.isBlank() || stringResource(R.string.show_total_time_of_queue).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.show_total_time_of_queue),
                text = "",
                isChecked = showTotalTimeQueue,
                onCheckedChange = { showTotalTimeQueue = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.show_remaining_song_time).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.show_remaining_song_time),
                text = "",
                isChecked = showRemainingSongTime,
                onCheckedChange = { showRemainingSongTime = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.show_next_songs_in_player).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.show_next_songs_in_player),
                text = "",
                isChecked = showNextSongsInPlayer,
                onCheckedChange = { showNextSongsInPlayer = it }
            )
        AnimatedVisibility( visible = showNextSongsInPlayer) {
          Column {
              if (search.inputValue.isBlank() || stringResource(R.string.showtwosongs).contains(search.inputValue,true))
                  EnumValueSelectorSettingsEntry(
                      title = stringResource(R.string.songs_number_to_show),
                      selectedValue = showsongs,
                      onValueSelected = {
                          showsongs = it
                      },
                      valueText = { it.name },
                      modifier = Modifier
                          .padding(start = 25.dp)
                  )


            if (search.inputValue.isBlank() || stringResource(R.string.showalbumcover).contains(
                    search.inputValue,
                    true
                )
            )
                SwitchSettingEntry(
                    title = stringResource(R.string.showalbumcover),
                    text = "",
                    isChecked = showalbumcover,
                    onCheckedChange = { showalbumcover = it },
                      modifier = Modifier.padding(start = 25.dp)
                  )
          }
        }

        if (search.inputValue.isBlank() || stringResource(R.string.disable_scrolling_text).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.disable_scrolling_text),
                text = stringResource(R.string.scrolling_text_is_used_for_long_texts),
                isChecked = disableScrollingText,
                onCheckedChange = { disableScrollingText = it }
            )

        if (search.inputValue.isBlank() || stringResource(if (playerType == PlayerType.Modern && !isLandscape) R.string.disable_horizontal_swipe else R.string.disable_vertical_swipe).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(if (playerType == PlayerType.Modern && !isLandscape) R.string.disable_vertical_swipe else R.string.disable_horizontal_swipe),
                text = stringResource(if (playerType == PlayerType.Modern && !isLandscape) R.string.disable_vertical_swipe_secondary else R.string.disable_song_switching_via_swipe),
                isChecked = disablePlayerHorizontalSwipe,
                onCheckedChange = { disablePlayerHorizontalSwipe = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.player_rotating_buttons).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.player_rotating_buttons),
                text = stringResource(R.string.player_enable_rotation_buttons),
                isChecked = effectRotationEnabled,
                onCheckedChange = { effectRotationEnabled = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.toggle_lyrics).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.toggle_lyrics),
                text = stringResource(R.string.by_tapping_on_the_thumbnail),
                isChecked = thumbnailTapEnabled,
                onCheckedChange = { thumbnailTapEnabled = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.click_lyrics_text).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.click_lyrics_text),
                text = "",
                isChecked = clickLyricsText,
                onCheckedChange = { clickLyricsText = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.save_lyrics_state).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.save_lyrics_state),
                text = stringResource(R.string.save_lyrics_state_description),
                isChecked = showLyricsStateKey,
                onCheckedChange = { showLyricsStateKey = it }
            )

        if (showlyricsthumbnail)
            if (search.inputValue.isBlank() || stringResource(R.string.show_background_in_lyrics).contains(
                    search.inputValue,
                    true
                )
            )
                SwitchSettingEntry(
                    title = stringResource(R.string.show_background_in_lyrics),
                    text = "",
                    isChecked = showBackgroundLyrics,
                    onCheckedChange = { showBackgroundLyrics = it }
                )

        if (search.inputValue.isBlank() || stringResource(R.string.player_enable_lyrics_popup_message).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.player_enable_lyrics_popup_message),
                text = "",
                isChecked = playerEnableLyricsPopupMessage,
                onCheckedChange = { playerEnableLyricsPopupMessage = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.background_progress_bar).contains(
                search.inputValue,
                true
            )
        )
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.background_progress_bar),
                selectedValue = backgroundProgress,
                onValueSelected = {
                    backgroundProgress = it
                },
                valueText = { it.text },
            )

        if (search.inputValue.isBlank() || stringResource(R.string.visualizer).contains(
                search.inputValue,
                true
            )
        ) {
            SwitchSettingEntry(
                title = stringResource(R.string.visualizer),
                text = "",
                isChecked = visualizerEnabled,
                onCheckedChange = { visualizerEnabled = it }
            )


            AnimatedVisibility(visible = visualizerEnabled) {
                Column {
                    SwitchSettingEntry(
                        title = stringResource(R.string.save_visualizer_state),
                        text = stringResource(R.string.save_visualizer_state_description),
                        isChecked = showVisualizerStateKey,
                        onCheckedChange = { showVisualizerStateKey = it },
                        modifier = Modifier.padding(start = 25.dp)
                    )
                }
            }
        }

        ImportantSettingsDescription(text = stringResource(R.string.visualizer_require_mic_permission))

        SettingsGroupSpacer()
        SettingsEntryGroupText(title = stringResource(R.string.player_action_bar))

        if (search.inputValue.isBlank() || stringResource(R.string.action_bar_transparent_background).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.action_bar_transparent_background),
                text = "",
                isChecked = transparentBackgroundActionBarPlayer,
                onCheckedChange = { transparentBackgroundActionBarPlayer = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.actionspacedevenly).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.actionspacedevenly),
                text = "",
                isChecked = actionspacedevenly,
                onCheckedChange = { actionspacedevenly = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.tapqueue).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.tapqueue),
                text = "",
                isChecked = tapqueue,
                onCheckedChange = { tapqueue = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.swipe_up_to_open_the_queue).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.swipe_up_to_open_the_queue),
                text = "",
                isChecked = swipeUpQueue,
                onCheckedChange = { swipeUpQueue = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.action_bar_show_video_button).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.action_bar_show_video_button),
                text = "",
                isChecked = showButtonPlayerVideo,
                onCheckedChange = { showButtonPlayerVideo = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.action_bar_show_discover_button).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.action_bar_show_discover_button),
                text = "",
                isChecked = showButtonPlayerDiscover,
                onCheckedChange = { showButtonPlayerDiscover = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.action_bar_show_download_button).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.action_bar_show_download_button),
                text = "",
                isChecked = showButtonPlayerDownload,
                onCheckedChange = { showButtonPlayerDownload = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.action_bar_show_add_to_playlist_button).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.action_bar_show_add_to_playlist_button),
                text = "",
                isChecked = showButtonPlayerAddToPlaylist,
                onCheckedChange = { showButtonPlayerAddToPlaylist = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.action_bar_show_loop_button).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.action_bar_show_loop_button),
                text = "",
                isChecked = showButtonPlayerLoop,
                onCheckedChange = { showButtonPlayerLoop = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.action_bar_show_shuffle_button).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.action_bar_show_shuffle_button),
                text = "",
                isChecked = showButtonPlayerShuffle,
                onCheckedChange = { showButtonPlayerShuffle = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.action_bar_show_lyrics_button).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.action_bar_show_lyrics_button),
                text = "",
                isChecked = showButtonPlayerLyrics,
                onCheckedChange = { showButtonPlayerLyrics = it }
            )
        if (!isLandscape || !showthumbnail) {
            if (!showlyricsthumbnail) {
                if (search.inputValue.isBlank() || stringResource(R.string.expandedplayer).contains(
                        search.inputValue,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = stringResource(R.string.expandedplayer),
                        text = "",
                        isChecked = expandedplayertoggle,
                        onCheckedChange = { expandedplayertoggle = it }
                    )
            }
        }

        if (search.inputValue.isBlank() || stringResource(R.string.action_bar_show_sleep_timer_button).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.action_bar_show_sleep_timer_button),
                text = "",
                isChecked = showButtonPlayerSleepTimer,
                onCheckedChange = { showButtonPlayerSleepTimer = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.show_equalizer).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.show_equalizer),
                text = "",
                isChecked = showButtonPlayerSystemEqualizer,
                onCheckedChange = { showButtonPlayerSystemEqualizer = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.action_bar_show_arrow_button_to_open_queue).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.action_bar_show_arrow_button_to_open_queue),
                text = "",
                isChecked = showButtonPlayerArrow,
                onCheckedChange = { showButtonPlayerArrow = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.action_bar_show_start_radio_button).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.action_bar_show_start_radio_button),
                text = "",
                isChecked = showButtonPlayerStartradio,
                onCheckedChange = { showButtonPlayerStartradio = it }
            )

        if (search.inputValue.isBlank() || stringResource(R.string.action_bar_show_menu_button).contains(
                search.inputValue,
                true
            )
        )
            SwitchSettingEntry(
                title = stringResource(R.string.action_bar_show_menu_button),
                text = "",
                isChecked = showButtonPlayerMenu,
                onCheckedChange = { showButtonPlayerMenu = it }
            )

        if (!showlyricsthumbnail) {
            SettingsGroupSpacer()
            SettingsEntryGroupText(title = stringResource(R.string.full_screen_lyrics_components))

            if (showTotalTimeQueue) {
                if (search.inputValue.isBlank() || stringResource(R.string.show_total_time_of_queue).contains(
                        search.inputValue,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = stringResource(R.string.show_total_time_of_queue),
                        text = "",
                        isChecked = queueDurationExpanded,
                        onCheckedChange = { queueDurationExpanded = it }
                    )
            }

            if (search.inputValue.isBlank() || stringResource(R.string.titleartist).contains(
                    search.inputValue,
                    true
                )
            )
                SwitchSettingEntry(
                    title = stringResource(R.string.titleartist),
                    text = "",
                    isChecked = titleExpanded,
                    onCheckedChange = { titleExpanded = it }
                )

            if (search.inputValue.isBlank() || stringResource(R.string.timeline).contains(
                    search.inputValue,
                    true
                )
            )
                SwitchSettingEntry(
                    title = stringResource(R.string.timeline),
                    text = "",
                    isChecked = timelineExpanded,
                    onCheckedChange = { timelineExpanded = it }
                )

            if (search.inputValue.isBlank() || stringResource(R.string.controls).contains(
                    search.inputValue,
                    true
                )
            )
                SwitchSettingEntry(
                    title = stringResource(R.string.controls),
                    text = "",
                    isChecked = controlsExpanded,
                    onCheckedChange = { controlsExpanded = it }
                )

            if (statsfornerds && (!(showthumbnail && playerType == PlayerType.Essential))){
                if (search.inputValue.isBlank() || stringResource(R.string.statsfornerds).contains(
                        search.inputValue,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = stringResource(R.string.statsfornerds),
                        text = "",
                        isChecked = statsExpanded,
                        onCheckedChange = { statsExpanded = it }
                    )
            }

            if (
                showButtonPlayerDownload ||
                showButtonPlayerAddToPlaylist ||
                showButtonPlayerLoop ||
                showButtonPlayerShuffle ||
                showButtonPlayerLyrics ||
                showButtonPlayerSleepTimer ||
                showButtonPlayerSystemEqualizer ||
                showButtonPlayerArrow ||
                showButtonPlayerMenu ||
                expandedplayertoggle ||
                showButtonPlayerDiscover ||
                showButtonPlayerVideo
            ){
                if (search.inputValue.isBlank() || stringResource(R.string.actionbar).contains(
                        search.inputValue,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = stringResource(R.string.actionbar),
                        text = "",
                        isChecked = actionExpanded,
                        onCheckedChange = {
                            actionExpanded = it
                        }
                    )
            }
            if (showNextSongsInPlayer && actionExpanded) {
                if (search.inputValue.isBlank() || stringResource(R.string.miniqueue).contains(
                        search.inputValue,
                        true
                    )
                )
                    SwitchSettingEntry(
                        title = stringResource(R.string.miniqueue),
                        text = "",
                        isChecked = miniQueueExpanded,
                        onCheckedChange = { miniQueueExpanded = it }
                    )
            }

        }

        var showPlaybackSpeedButton by rememberPreference( showPlaybackSpeedButtonKey, false )
        if( search.inputValue.isBlank() || stringResource( R.string.title_playback_speed ).contains( search.inputValue, true ) )
            SwitchSettingEntry(
                title = stringResource( R.string.title_playback_speed ),
                text = stringResource( R.string.description_playback_speed ),
                isChecked = showPlaybackSpeedButton,
                onCheckedChange = { showPlaybackSpeedButton = it }
            )

        SettingsGroupSpacer()
        SettingsEntryGroupText(title = stringResource(R.string.notification_player))

        if (search.inputValue.isBlank() || stringResource(R.string.notification_player).contains(
                search.inputValue,
                true
            )
        ) {
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.notificationPlayerFirstIcon),
                selectedValue = notificationPlayerFirstIcon,
                onValueSelected = {
                    notificationPlayerFirstIcon = it
                    restartService = true
                },
                valueText = { it.text },
            )
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.notificationPlayerSecondIcon),
                selectedValue = notificationPlayerSecondIcon,
                onValueSelected = {
                    notificationPlayerSecondIcon = it
                    restartService = true
                },
                valueText = { it.text },
            )
            RestartPlayerService(restartService, onRestart = { restartService = false })
        }


//        if (search.inputValue.isBlank() || stringResource(R.string.show_song_cover).contains(
//                search.inputValue,
//                true
//            )
//        )
//            if (!isAtLeastAndroid13) {
//                SettingsGroupSpacer()
//
//                SettingsEntryGroupText(title = stringResource(R.string.lockscreen))
//
//                SwitchSettingEntry(
//                    title = stringResource(R.string.show_song_cover),
//                    text = stringResource(R.string.use_song_cover_on_lockscreen),
//                    isChecked = isShowingThumbnailInLockscreen,
//                    onCheckedChange = { isShowingThumbnailInLockscreen = it }
//                )
//            }

        if (isAtLeastAndroid7) {
            SettingsGroupSpacer()
            SettingsEntryGroupText(title = stringResource(R.string.wallpaper))
            SwitchSettingEntry(
                title = stringResource(R.string.enable_wallpaper),
                text = "",
                isChecked = enableWallpaper,
                onCheckedChange = { enableWallpaper = it }
            )
            AnimatedVisibility(visible = enableWallpaper) {
                Column {
                    EnumValueSelectorSettingsEntry(
                        title = stringResource(R.string.set_cover_thumbnail_as_wallpaper),
                        selectedValue = wallpaperType,
                        onValueSelected = {
                            wallpaperType = it
                            restartService = true
                        },
                        valueText = { it.text },
                        modifier = Modifier.padding(start = 25.dp)
                    )
                    RestartPlayerService(restartService, onRestart = { restartService = false })
                }
            }
        }

        SettingsGroupSpacer()
        var resetToDefault by remember { mutableStateOf(false) }
        val context = LocalContext.current
        ButtonBarSettingEntry(
            title = stringResource(R.string.settings_reset),
            text = stringResource(R.string.settings_restore_default_settings),
            icon = R.drawable.refresh,
            iconColor = colorPalette().text,
            onClick = { resetToDefault = true },
        )
        if (resetToDefault) {
            DefaultAppearanceSettings()
            resetToDefault = false
            navController.popBackStack()
            Toaster.done()
        }

        SettingsGroupSpacer(
            modifier = Modifier.height(Dimensions.bottomSpacer)
        )
    }
}
