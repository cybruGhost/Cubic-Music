package app.it.fast4x.rimusic.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.it.fast4x.rimusic.ui.components.themed.ValueSelectorDialog
import app.it.fast4x.rimusic.ui.screens.settings.ColorSettingEntry
import app.it.fast4x.rimusic.ui.screens.settings.ButtonBarSettingEntry
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.AlbumSwipeAction
import app.it.fast4x.rimusic.enums.BackgroundProgress
import app.it.fast4x.rimusic.enums.CarouselSize
import app.it.fast4x.rimusic.enums.ColorPaletteMode
import app.it.fast4x.rimusic.enums.ColorPaletteName
import app.it.fast4x.rimusic.enums.DurationInMilliseconds
import app.it.fast4x.rimusic.enums.DurationInMinutes
import app.it.fast4x.rimusic.enums.ExoPlayerMinTimeForEvent
import app.it.fast4x.rimusic.enums.FontType
import app.it.fast4x.rimusic.enums.HomeScreenTabs
import app.it.fast4x.rimusic.enums.IconLikeType
import app.it.fast4x.rimusic.enums.MaxSongs


import app.it.fast4x.rimusic.enums.MenuStyle
import app.it.fast4x.rimusic.enums.MessageType
import app.it.fast4x.rimusic.enums.MiniPlayerType
import app.it.fast4x.rimusic.enums.NavigationBarPosition
import app.it.fast4x.rimusic.enums.NavigationBarType
import app.it.fast4x.rimusic.enums.PauseBetweenSongs
import app.it.fast4x.rimusic.enums.PlayerBackgroundColors
import app.it.fast4x.rimusic.enums.PlayerControlsType
import app.it.fast4x.rimusic.enums.PlayerInfoType
import app.it.fast4x.rimusic.enums.PlayerPlayButtonType
import app.it.fast4x.rimusic.enums.PlayerPosition
import app.it.fast4x.rimusic.enums.PlayerThumbnailSize
import app.it.fast4x.rimusic.enums.PlayerTimelineSize
import app.it.fast4x.rimusic.enums.PlayerTimelineType
import app.it.fast4x.rimusic.enums.PlayerType
import app.it.fast4x.rimusic.enums.PlaylistSwipeAction
import app.it.fast4x.rimusic.enums.QueueSwipeAction
import app.it.fast4x.rimusic.enums.QueueType

import app.it.fast4x.rimusic.enums.ThumbnailRoundness
import app.it.fast4x.rimusic.enums.ThumbnailType
import app.it.fast4x.rimusic.enums.TransitionEffect
import app.it.fast4x.rimusic.enums.UiType
import app.it.fast4x.rimusic.ui.components.themed.ConfirmationDialog
import app.it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import app.it.fast4x.rimusic.ui.styling.DefaultDarkColorPalette
import app.it.fast4x.rimusic.ui.styling.DefaultLightColorPalette
import app.it.fast4x.rimusic.ui.styling.Dimensions

import app.it.fast4x.rimusic.utils.UiTypeKey
import app.it.fast4x.rimusic.utils.actionspacedevenlyKey
import app.it.fast4x.rimusic.utils.albumSwipeLeftActionKey
import app.it.fast4x.rimusic.utils.albumSwipeRightActionKey
import app.it.fast4x.rimusic.utils.applyFontPaddingKey
import app.it.fast4x.rimusic.utils.backgroundProgressKey
import app.it.fast4x.rimusic.utils.blackgradientKey
import app.it.fast4x.rimusic.utils.buttonzoomoutKey
import app.it.fast4x.rimusic.utils.carouselKey
import app.it.fast4x.rimusic.utils.carouselSizeKey
import app.it.fast4x.rimusic.utils.clickOnLyricsTextKey
import app.it.fast4x.rimusic.utils.closeWithBackButtonKey
import app.it.fast4x.rimusic.utils.closebackgroundPlayerKey
import app.it.fast4x.rimusic.utils.colorPaletteModeKey
import app.it.fast4x.rimusic.utils.colorPaletteNameKey
import app.it.fast4x.rimusic.utils.customColorKey
import app.it.fast4x.rimusic.utils.customThemeDark_Background0Key
import app.it.fast4x.rimusic.utils.customThemeDark_Background1Key
import app.it.fast4x.rimusic.utils.customThemeDark_Background2Key
import app.it.fast4x.rimusic.utils.customThemeDark_Background3Key
import app.it.fast4x.rimusic.utils.customThemeDark_Background4Key
import app.it.fast4x.rimusic.utils.customThemeDark_TextKey
import app.it.fast4x.rimusic.utils.customThemeDark_accentKey
import app.it.fast4x.rimusic.utils.customThemeDark_iconButtonPlayerKey
import app.it.fast4x.rimusic.utils.customThemeDark_textDisabledKey
import app.it.fast4x.rimusic.utils.customThemeDark_textSecondaryKey
import app.it.fast4x.rimusic.utils.customThemeLight_Background0Key
import app.it.fast4x.rimusic.utils.customThemeLight_Background1Key
import app.it.fast4x.rimusic.utils.customThemeLight_Background2Key
import app.it.fast4x.rimusic.utils.customThemeLight_Background3Key
import app.it.fast4x.rimusic.utils.customThemeLight_Background4Key
import app.it.fast4x.rimusic.utils.customThemeLight_TextKey
import app.it.fast4x.rimusic.utils.customThemeLight_accentKey
import app.it.fast4x.rimusic.utils.customThemeLight_iconButtonPlayerKey
import app.it.fast4x.rimusic.utils.customThemeLight_textDisabledKey
import app.it.fast4x.rimusic.utils.customThemeLight_textSecondaryKey
import app.it.fast4x.rimusic.utils.disableClosingPlayerSwipingDownKey
import app.it.fast4x.rimusic.utils.disableIconButtonOnTopKey
import app.it.fast4x.rimusic.utils.disablePlayerHorizontalSwipeKey
import app.it.fast4x.rimusic.utils.disableScrollingTextKey
import app.it.fast4x.rimusic.utils.discoverKey
import app.it.fast4x.rimusic.utils.effectRotationKey

import app.it.fast4x.rimusic.utils.excludeSongsWithDurationLimitKey
import app.it.fast4x.rimusic.utils.exoPlayerMinTimeForEventKey
import app.it.fast4x.rimusic.utils.expandedplayertoggleKey
import app.it.fast4x.rimusic.utils.fadingedgeKey
import app.it.fast4x.rimusic.utils.fontTypeKey
import app.it.fast4x.rimusic.utils.iconLikeTypeKey
import app.it.fast4x.rimusic.utils.indexNavigationTabKey
import app.it.fast4x.rimusic.utils.isPauseOnVolumeZeroEnabledKey
import app.it.fast4x.rimusic.utils.isSwipeToActionEnabledKey
import app.it.fast4x.rimusic.utils.keepPlayerMinimizedKey
import app.it.fast4x.rimusic.utils.lastPlayerPlayButtonTypeKey
import app.it.fast4x.rimusic.utils.lastPlayerThumbnailSizeKey
import app.it.fast4x.rimusic.utils.lastPlayerTimelineTypeKey
import app.it.fast4x.rimusic.utils.maxSongsInQueueKey

import app.it.fast4x.rimusic.utils.menuStyleKey
import app.it.fast4x.rimusic.utils.messageTypeKey
import app.it.fast4x.rimusic.utils.miniPlayerTypeKey
import app.it.fast4x.rimusic.utils.minimumSilenceDurationKey
import app.it.fast4x.rimusic.utils.navigationBarPositionKey
import app.it.fast4x.rimusic.utils.navigationBarTypeKey
import app.it.fast4x.rimusic.utils.pauseBetweenSongsKey
import app.it.fast4x.rimusic.utils.pauseListenHistoryKey
import app.it.fast4x.rimusic.utils.persistentQueueKey
import app.it.fast4x.rimusic.utils.playbackFadeAudioDurationKey
import app.it.fast4x.rimusic.utils.playerBackgroundColorsKey
import app.it.fast4x.rimusic.utils.playerControlsTypeKey
import app.it.fast4x.rimusic.utils.playerEnableLyricsPopupMessageKey
import app.it.fast4x.rimusic.utils.playerInfoShowIconsKey
import app.it.fast4x.rimusic.utils.playerInfoTypeKey
import app.it.fast4x.rimusic.utils.playerPlayButtonTypeKey
import app.it.fast4x.rimusic.utils.playerPositionKey
import app.it.fast4x.rimusic.utils.playerSwapControlsWithTimelineKey
import app.it.fast4x.rimusic.utils.playerThumbnailSizeKey
import app.it.fast4x.rimusic.utils.playerTimelineSizeKey
import app.it.fast4x.rimusic.utils.playerTimelineTypeKey
import app.it.fast4x.rimusic.utils.playerTypeKey
import app.it.fast4x.rimusic.utils.playlistSwipeLeftActionKey
import app.it.fast4x.rimusic.utils.playlistSwipeRightActionKey
import app.it.fast4x.rimusic.utils.playlistindicatorKey
import app.it.fast4x.rimusic.utils.queueSwipeLeftActionKey
import app.it.fast4x.rimusic.utils.queueSwipeRightActionKey
import app.it.fast4x.rimusic.utils.queueTypeKey

import app.it.fast4x.rimusic.utils.rememberPreference
import app.it.fast4x.rimusic.utils.resumePlaybackOnStartKey
import app.it.fast4x.rimusic.utils.resumePlaybackWhenDeviceConnectedKey
import app.it.fast4x.rimusic.utils.shakeEventEnabledKey
import app.it.fast4x.rimusic.utils.showButtonPlayerAddToPlaylistKey
import app.it.fast4x.rimusic.utils.showButtonPlayerArrowKey
import app.it.fast4x.rimusic.utils.showButtonPlayerDiscoverKey
import app.it.fast4x.rimusic.utils.showButtonPlayerDownloadKey
import app.it.fast4x.rimusic.utils.showButtonPlayerLoopKey
import app.it.fast4x.rimusic.utils.showButtonPlayerLyricsKey
import app.it.fast4x.rimusic.utils.showButtonPlayerMenuKey
import app.it.fast4x.rimusic.utils.showButtonPlayerShuffleKey
import app.it.fast4x.rimusic.utils.showButtonPlayerSleepTimerKey
import app.it.fast4x.rimusic.utils.showButtonPlayerSystemEqualizerKey
import app.it.fast4x.rimusic.utils.showCachedPlaylistKey
import app.it.fast4x.rimusic.utils.showDownloadedPlaylistKey
import app.it.fast4x.rimusic.utils.showFavoritesPlaylistKey
import app.it.fast4x.rimusic.utils.showFloatingIconKey


import app.it.fast4x.rimusic.utils.showNextSongsInPlayerKey
import app.it.fast4x.rimusic.utils.showOnDevicePlaylistKey
import app.it.fast4x.rimusic.utils.showPinnedPlaylistsKey
import app.it.fast4x.rimusic.utils.showPipedPlaylistsKey
import app.it.fast4x.rimusic.utils.showRemainingSongTimeKey
import app.it.fast4x.rimusic.utils.showSearchTabKey
import app.it.fast4x.rimusic.utils.showStatsInNavbarKey

import app.it.fast4x.rimusic.utils.showTopActionsBarKey
import app.it.fast4x.rimusic.utils.showTotalTimeQueueKey
import app.it.fast4x.rimusic.utils.showthumbnailKey
import app.it.fast4x.rimusic.utils.skipMediaOnErrorKey
import app.it.fast4x.rimusic.utils.skipSilenceKey
import app.it.fast4x.rimusic.utils.swipeUpQueueKey
import app.it.fast4x.rimusic.utils.tapqueueKey
import app.it.fast4x.rimusic.utils.thumbnailRoundnessKey
import app.it.fast4x.rimusic.utils.thumbnailTapEnabledKey
import app.it.fast4x.rimusic.utils.thumbnailTypeKey
import app.it.fast4x.rimusic.utils.transitionEffectKey
import app.it.fast4x.rimusic.utils.transparentBackgroundPlayerActionBarKey
import app.it.fast4x.rimusic.utils.useSystemFontKey
import app.it.fast4x.rimusic.utils.useVolumeKeysToChangeSongKey
import app.it.fast4x.rimusic.utils.visualizerEnabledKey
import app.it.fast4x.rimusic.utils.volumeNormalizationKey
import app.kreate.android.me.knighthat.component.dialog.RestartAppDialog
import app.kreate.android.me.knighthat.component.tab.Search
import app.kreate.android.me.knighthat.utils.Toaster
import app.it.fast4x.rimusic.enums.SearchDisplayOrder
import app.it.fast4x.rimusic.utils.searchDisplayOrderKey

@Composable
fun DefaultUiSettings() {
    var exoPlayerMinTimeForEvent by rememberPreference(
        exoPlayerMinTimeForEventKey,
        ExoPlayerMinTimeForEvent.`20s`
    )
    exoPlayerMinTimeForEvent = ExoPlayerMinTimeForEvent.`20s`
    var persistentQueue by rememberPreference(persistentQueueKey, false)
    persistentQueue = false
    var resumePlaybackOnStart by rememberPreference(resumePlaybackOnStartKey, false)
    resumePlaybackOnStart = false
    var closebackgroundPlayer by rememberPreference(closebackgroundPlayerKey, false)
    closebackgroundPlayer = false
    var closeWithBackButton by rememberPreference(closeWithBackButtonKey, true)
    closeWithBackButton = true
    var resumePlaybackWhenDeviceConnected by rememberPreference(
        resumePlaybackWhenDeviceConnectedKey,
        false
    )
    resumePlaybackWhenDeviceConnected = false

    var skipSilence by rememberPreference(skipSilenceKey, false)
    skipSilence = false
    var skipMediaOnError by rememberPreference(skipMediaOnErrorKey, false)
    skipMediaOnError = false
    var volumeNormalization by rememberPreference(volumeNormalizationKey, false)
    volumeNormalization = false

    var keepPlayerMinimized by rememberPreference(keepPlayerMinimizedKey,   false)
    keepPlayerMinimized = false
    var disableIconButtonOnTop by rememberPreference(disableIconButtonOnTopKey, false)
    disableIconButtonOnTop = false
    var lastPlayerTimelineType by rememberPreference(lastPlayerTimelineTypeKey, PlayerTimelineType.Default)
    lastPlayerTimelineType = PlayerTimelineType.Default
    var lastPlayerThumbnailSize by rememberPreference(lastPlayerThumbnailSizeKey, PlayerThumbnailSize.Medium)
    lastPlayerThumbnailSize = PlayerThumbnailSize.Medium
    var uiType  by rememberPreference(UiTypeKey, UiType.RiMusic)
    uiType = UiType.RiMusic
    var disablePlayerHorizontalSwipe by rememberPreference(disablePlayerHorizontalSwipeKey, false)
    disablePlayerHorizontalSwipe = false
    var lastPlayerPlayButtonType by rememberPreference(lastPlayerPlayButtonTypeKey, PlayerPlayButtonType.Rectangular)
    lastPlayerPlayButtonType = PlayerPlayButtonType.Rectangular
    var colorPaletteName by rememberPreference(colorPaletteNameKey, ColorPaletteName.Dynamic)
    colorPaletteName = ColorPaletteName.Dynamic
    var colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.Dark)
    colorPaletteMode = ColorPaletteMode.Dark
    var indexNavigationTab by rememberPreference(
        indexNavigationTabKey,
        HomeScreenTabs.Default
    )
    indexNavigationTab = HomeScreenTabs.Default
    var fontType by rememberPreference(fontTypeKey, FontType.Rubik)
    fontType = FontType.Rubik
    var useSystemFont by rememberPreference(useSystemFontKey, false)
    useSystemFont = false
    var applyFontPadding by rememberPreference(applyFontPaddingKey, false)
    applyFontPadding = false
    var isSwipeToActionEnabled by rememberPreference(isSwipeToActionEnabledKey, true)
    isSwipeToActionEnabled = true
    var disableClosingPlayerSwipingDown by rememberPreference(disableClosingPlayerSwipingDownKey, false)
    disableClosingPlayerSwipingDown = false
    var showSearchTab by rememberPreference(showSearchTabKey, false)
    showSearchTab = false
    var showStatsInNavbar by rememberPreference(showStatsInNavbarKey, false)
    showStatsInNavbar = false



    var navigationBarPosition by rememberPreference(navigationBarPositionKey, NavigationBarPosition.Bottom)
    navigationBarPosition = NavigationBarPosition.Bottom
    var navigationBarType by rememberPreference(navigationBarTypeKey, NavigationBarType.IconAndText)
    navigationBarType = NavigationBarType.IconAndText
    var pauseBetweenSongs  by rememberPreference(pauseBetweenSongsKey, PauseBetweenSongs.`0`)
    pauseBetweenSongs = PauseBetweenSongs.`0`
    var maxSongsInQueue  by rememberPreference(maxSongsInQueueKey, MaxSongs.`500`)
    maxSongsInQueue = MaxSongs.`500`
    var thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )
    thumbnailRoundness = ThumbnailRoundness.Heavy
    var showFavoritesPlaylist by rememberPreference(showFavoritesPlaylistKey, true)
    showFavoritesPlaylist = true

    var showDownloadedPlaylist by rememberPreference(showDownloadedPlaylistKey, true)
    showDownloadedPlaylist = true
    var showOnDevicePlaylist by rememberPreference(showOnDevicePlaylistKey, true)
    showOnDevicePlaylist = true
    var shakeEventEnabled by rememberPreference(shakeEventEnabledKey, false)
    shakeEventEnabled = false
    var useVolumeKeysToChangeSong by rememberPreference(useVolumeKeysToChangeSongKey, false)
    useVolumeKeysToChangeSong = false
    var showFloatingIcon by rememberPreference(showFloatingIconKey, false)
    showFloatingIcon = false
    var menuStyle by rememberPreference(menuStyleKey, MenuStyle.List)
    menuStyle = MenuStyle.List
    var transitionEffect by rememberPreference(transitionEffectKey, TransitionEffect.SlideHorizontal)
    transitionEffect = TransitionEffect.SlideHorizontal

    var showPipedPlaylists by rememberPreference(showPipedPlaylistsKey, true)
    showPipedPlaylists = true
    var showPinnedPlaylists by rememberPreference(showPinnedPlaylistsKey, true)
    showPinnedPlaylists = true

    var customThemeLight_Background0 by rememberPreference(customThemeLight_Background0Key, DefaultLightColorPalette.background0.hashCode())
    customThemeLight_Background0 = DefaultLightColorPalette.background0.hashCode()
    var customThemeLight_Background1 by rememberPreference(customThemeLight_Background1Key, DefaultLightColorPalette.background1.hashCode())
    customThemeLight_Background1 = DefaultLightColorPalette.background1.hashCode()
    var customThemeLight_Background2 by rememberPreference(customThemeLight_Background2Key, DefaultLightColorPalette.background2.hashCode())
    customThemeLight_Background2 = DefaultLightColorPalette.background2.hashCode()
    var customThemeLight_Background3 by rememberPreference(customThemeLight_Background3Key, DefaultLightColorPalette.background3.hashCode())
    customThemeLight_Background3 = DefaultLightColorPalette.background3.hashCode()
    var customThemeLight_Background4 by rememberPreference(customThemeLight_Background4Key, DefaultLightColorPalette.background4.hashCode())
    customThemeLight_Background4 = DefaultLightColorPalette.background4.hashCode()
    var customThemeLight_Text by rememberPreference(customThemeLight_TextKey, DefaultLightColorPalette.text.hashCode())
    customThemeLight_Text = DefaultLightColorPalette.text.hashCode()
    var customThemeLight_TextSecondary by rememberPreference(customThemeLight_textSecondaryKey, DefaultLightColorPalette.textSecondary.hashCode())
    customThemeLight_TextSecondary = DefaultLightColorPalette.textSecondary.hashCode()
    var customThemeLight_TextDisabled by rememberPreference(customThemeLight_textDisabledKey, DefaultLightColorPalette.textDisabled.hashCode())
    customThemeLight_TextDisabled = DefaultLightColorPalette.textDisabled.hashCode()
    var customThemeLight_IconButtonPlayer by rememberPreference(customThemeLight_iconButtonPlayerKey, DefaultLightColorPalette.iconButtonPlayer.hashCode())
    customThemeLight_IconButtonPlayer = DefaultLightColorPalette.iconButtonPlayer.hashCode()
    var customThemeLight_Accent by rememberPreference(customThemeLight_accentKey, DefaultLightColorPalette.accent.hashCode())
    customThemeLight_Accent = DefaultLightColorPalette.accent.hashCode()
    var customThemeDark_Background0 by rememberPreference(customThemeDark_Background0Key, DefaultDarkColorPalette.background0.hashCode())
    customThemeDark_Background0 = DefaultDarkColorPalette.background0.hashCode()
    var customThemeDark_Background1 by rememberPreference(customThemeDark_Background1Key, DefaultDarkColorPalette.background1.hashCode())
    customThemeDark_Background1 = DefaultDarkColorPalette.background1.hashCode()
    var customThemeDark_Background2 by rememberPreference(customThemeDark_Background2Key, DefaultDarkColorPalette.background2.hashCode())
    customThemeDark_Background2 = DefaultDarkColorPalette.background2.hashCode()
    var customThemeDark_Background3 by rememberPreference(customThemeDark_Background3Key, DefaultDarkColorPalette.background3.hashCode())
    customThemeDark_Background3 = DefaultDarkColorPalette.background3.hashCode()
    var customThemeDark_Background4 by rememberPreference(customThemeDark_Background4Key, DefaultDarkColorPalette.background4.hashCode())
    customThemeDark_Background4 = DefaultDarkColorPalette.background4.hashCode()
    var customThemeDark_Text by rememberPreference(customThemeDark_TextKey, DefaultDarkColorPalette.text.hashCode())
    customThemeDark_Text = DefaultDarkColorPalette.text.hashCode()
    var customThemeDark_TextSecondary by rememberPreference(customThemeDark_textSecondaryKey, DefaultDarkColorPalette.textSecondary.hashCode())
    customThemeDark_TextSecondary = DefaultDarkColorPalette.textSecondary.hashCode()
    var customThemeDark_TextDisabled by rememberPreference(customThemeDark_textDisabledKey, DefaultDarkColorPalette.textDisabled.hashCode())
    customThemeDark_TextDisabled = DefaultDarkColorPalette.textDisabled.hashCode()
    var customThemeDark_IconButtonPlayer by rememberPreference(customThemeDark_iconButtonPlayerKey, DefaultDarkColorPalette.iconButtonPlayer.hashCode())
    customThemeDark_IconButtonPlayer = DefaultDarkColorPalette.iconButtonPlayer.hashCode()
    var customThemeDark_Accent by rememberPreference(customThemeDark_accentKey, DefaultDarkColorPalette.accent.hashCode())
    customThemeDark_Accent = DefaultDarkColorPalette.accent.hashCode()
    var resetCustomLightThemeDialog by rememberSaveable { mutableStateOf(false) }
    resetCustomLightThemeDialog = false
    var resetCustomDarkThemeDialog by rememberSaveable { mutableStateOf(false) }
    resetCustomDarkThemeDialog = false
    var playbackFadeAudioDuration by rememberPreference(playbackFadeAudioDurationKey, DurationInMilliseconds.Disabled)
    playbackFadeAudioDuration = DurationInMilliseconds.Disabled
    var playerPosition by rememberPreference(playerPositionKey, PlayerPosition.Bottom)
    playerPosition = PlayerPosition.Bottom
    var excludeSongWithDurationLimit by rememberPreference(excludeSongsWithDurationLimitKey, DurationInMinutes.Disabled)
    excludeSongWithDurationLimit = DurationInMinutes.Disabled
    var playlistindicator by rememberPreference(playlistindicatorKey, false)
    playlistindicator = false
    var discoverIsEnabled by rememberPreference(discoverKey, false)
    discoverIsEnabled = false
    var isPauseOnVolumeZeroEnabled by rememberPreference(isPauseOnVolumeZeroEnabledKey, false)
    isPauseOnVolumeZeroEnabled = false
    var messageType by rememberPreference(messageTypeKey, MessageType.Modern)
    messageType = MessageType.Modern
    var minimumSilenceDuration by rememberPreference(minimumSilenceDurationKey, 2_000_000L)
    minimumSilenceDuration = 2_000_000L
    var pauseListenHistory by rememberPreference(pauseListenHistoryKey, false)
    pauseListenHistory = false
    var showTopActionsBar by rememberPreference(showTopActionsBarKey, true)
    showTopActionsBar = true
    var playerControlsType by rememberPreference(playerControlsTypeKey, PlayerControlsType.Essential)
    playerControlsType = PlayerControlsType.Modern
    var playerInfoType by rememberPreference(playerInfoTypeKey, PlayerInfoType.Essential)
    playerInfoType = PlayerInfoType.Modern
    var playerType by rememberPreference(playerTypeKey, PlayerType.Essential)
    playerType = PlayerType.Essential
    var queueType by rememberPreference(queueTypeKey, QueueType.Essential)
    queueType = QueueType.Essential
    var fadingedge by rememberPreference(fadingedgeKey, false)
    fadingedge = false
    var carousel by rememberPreference(carouselKey, true)
    carousel = true
    var carouselSize by rememberPreference(carouselSizeKey, CarouselSize.Biggest)
    carouselSize = CarouselSize.Biggest
    var thumbnailType by rememberPreference(thumbnailTypeKey, ThumbnailType.Modern)
    thumbnailType = ThumbnailType.Modern
    var playerTimelineType by rememberPreference(playerTimelineTypeKey, PlayerTimelineType.FakeAudioBar)
    playerTimelineType = PlayerTimelineType.Default
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
    var playerInfoShowIcons by rememberPreference(playerInfoShowIconsKey, true)
    playerInfoShowIcons = true
    var miniPlayerType by rememberPreference(
        miniPlayerTypeKey,
        MiniPlayerType.Modern
    )
    miniPlayerType = MiniPlayerType.Modern
    var playerSwapControlsWithTimeline by rememberPreference(
        playerSwapControlsWithTimelineKey,
        false
    )
    playerSwapControlsWithTimeline = false
    var playerPlayButtonType by rememberPreference(
        playerPlayButtonTypeKey,
        PlayerPlayButtonType.Disabled
    )
    playerPlayButtonType = PlayerPlayButtonType.Disabled
    var buttonzoomout by rememberPreference(buttonzoomoutKey, false)
    buttonzoomout = false
    var iconLikeType by rememberPreference(iconLikeTypeKey, IconLikeType.Essential)
    iconLikeType = IconLikeType.Essential
    var playerBackgroundColors by rememberPreference(
        playerBackgroundColorsKey,
        PlayerBackgroundColors.BlurredCoverColor
    )
    playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
    var blackgradient by rememberPreference(blackgradientKey, false)
    blackgradient = false
    var showTotalTimeQueue by rememberPreference(showTotalTimeQueueKey, true)
    showTotalTimeQueue = true
    var showNextSongsInPlayer by rememberPreference(showNextSongsInPlayerKey, false)
    showNextSongsInPlayer = false
    var showRemainingSongTime by rememberPreference(showRemainingSongTimeKey, true)
    showRemainingSongTime = true
    var disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    disableScrollingText = false
    var effectRotationEnabled by rememberPreference(effectRotationKey, true)
    effectRotationEnabled = true
    var thumbnailTapEnabled by rememberPreference(thumbnailTapEnabledKey, true)
    thumbnailTapEnabled = true
    var clickLyricsText by rememberPreference(clickOnLyricsTextKey, true)
    clickLyricsText = true
    var backgroundProgress by rememberPreference(
        backgroundProgressKey,
        BackgroundProgress.MiniPlayer
    )
    backgroundProgress = BackgroundProgress.MiniPlayer
    var transparentBackgroundActionBarPlayer by rememberPreference(
        transparentBackgroundPlayerActionBarKey,
        false
    )
    transparentBackgroundActionBarPlayer = false
    var actionspacedevenly by rememberPreference(actionspacedevenlyKey, false)
    actionspacedevenly = false
    var tapqueue by rememberPreference(tapqueueKey, true)
    tapqueue = true
    var swipeUpQueue by rememberPreference(swipeUpQueueKey, true)
    swipeUpQueue = true
    var showButtonPlayerAddToPlaylist by rememberPreference(showButtonPlayerAddToPlaylistKey, true)
    showButtonPlayerAddToPlaylist = true
    var showButtonPlayerArrow by rememberPreference(showButtonPlayerArrowKey, true)
    showButtonPlayerArrow = false
    var showButtonPlayerDownload by rememberPreference(showButtonPlayerDownloadKey, true)
    showButtonPlayerDownload = true
    var showButtonPlayerLoop by rememberPreference(showButtonPlayerLoopKey, true)
    showButtonPlayerLoop = true
    var showButtonPlayerLyrics by rememberPreference(showButtonPlayerLyricsKey, true)
    showButtonPlayerLyrics = true
    var expandedplayertoggle by rememberPreference(expandedplayertoggleKey, true)
    expandedplayertoggle = true
    var showButtonPlayerShuffle by rememberPreference(showButtonPlayerShuffleKey, true)
    showButtonPlayerShuffle = true
    var showButtonPlayerSleepTimer by rememberPreference(showButtonPlayerSleepTimerKey, false)
    showButtonPlayerSleepTimer = false
    var showButtonPlayerMenu by rememberPreference(showButtonPlayerMenuKey, false)
    showButtonPlayerMenu = false
    var showButtonPlayerSystemEqualizer by rememberPreference(
        showButtonPlayerSystemEqualizerKey,
        false
    )
    showButtonPlayerSystemEqualizer = false
    var queueSwipeLeftAction by rememberPreference(queueSwipeLeftActionKey, QueueSwipeAction.RemoveFromQueue)
    queueSwipeLeftAction = QueueSwipeAction.RemoveFromQueue
    var queueSwipeRightAction by rememberPreference(queueSwipeRightActionKey, QueueSwipeAction.PlayNext)
    queueSwipeRightAction = QueueSwipeAction.PlayNext

    var playlistSwipeLeftAction by rememberPreference(playlistSwipeLeftActionKey, PlaylistSwipeAction.Favourite)
    playlistSwipeLeftAction = PlaylistSwipeAction.Favourite
    var playlistSwipeRightAction by rememberPreference(playlistSwipeRightActionKey, PlaylistSwipeAction.PlayNext)
    playlistSwipeRightAction = PlaylistSwipeAction.PlayNext

    var albumSwipeLeftAction by rememberPreference(albumSwipeLeftActionKey, AlbumSwipeAction.PlayNext)
    albumSwipeLeftAction = AlbumSwipeAction.PlayNext
    var albumSwipeRightAction by rememberPreference(albumSwipeRightActionKey, AlbumSwipeAction.Bookmark)
    albumSwipeRightAction = AlbumSwipeAction.Bookmark

    var showButtonPlayerDiscover by rememberPreference(showButtonPlayerDiscoverKey, false)
    showButtonPlayerDiscover = false
    var playerEnableLyricsPopupMessage by rememberPreference(
        playerEnableLyricsPopupMessageKey,
        true
    )
    playerEnableLyricsPopupMessage = true
    var visualizerEnabled by rememberPreference(visualizerEnabledKey, false)
    visualizerEnabled = false
    var showthumbnail by rememberPreference(showthumbnailKey, true)
    showthumbnail = true
    var searchDisplayOrder by rememberPreference(searchDisplayOrderKey, SearchDisplayOrder.SuggestionsFirst)
    searchDisplayOrder = SearchDisplayOrder.SuggestionsFirst
}

@ExperimentalAnimationApi
@UnstableApi
@Composable
fun UiSettings(
    navController: NavController
) {
    val binder = LocalPlayerServiceBinder.current



    var uiType by rememberPreference(UiTypeKey, UiType.RiMusic)
    var keepPlayerMinimized by rememberPreference(keepPlayerMinimizedKey,   false)

    var disableIconButtonOnTop by rememberPreference(disableIconButtonOnTopKey, false)
    var lastPlayerTimelineType by rememberPreference(lastPlayerTimelineTypeKey, PlayerTimelineType.Default)
    var lastPlayerThumbnailSize by rememberPreference(lastPlayerThumbnailSizeKey, PlayerThumbnailSize.Medium)
    var disablePlayerHorizontalSwipe by rememberPreference(disablePlayerHorizontalSwipeKey, false)

    var lastPlayerPlayButtonType by rememberPreference(lastPlayerPlayButtonTypeKey, PlayerPlayButtonType.Rectangular)

    var colorPaletteName by rememberPreference(colorPaletteNameKey, ColorPaletteName.Dynamic)
    var colorPaletteMode by rememberPreference(colorPaletteModeKey, ColorPaletteMode.Dark)
    var indexNavigationTab by rememberPreference(
        indexNavigationTabKey,
        HomeScreenTabs.Default
    )
    var fontType by rememberPreference(fontTypeKey, FontType.Rubik)
    var useSystemFont by rememberPreference(useSystemFontKey, false)
    var applyFontPadding by rememberPreference(applyFontPaddingKey, false)
    var isSwipeToActionEnabled by rememberPreference(isSwipeToActionEnabledKey, true)
    var showSearchTab by rememberPreference(showSearchTabKey, false)
    var showStatsInNavbar by rememberPreference(showStatsInNavbarKey, false)







    var navigationBarPosition by rememberPreference(navigationBarPositionKey, NavigationBarPosition.Bottom)
    var navigationBarType by rememberPreference(navigationBarTypeKey, NavigationBarType.IconAndText)
    val search = Search()

    var showFavoritesPlaylist by rememberPreference(showFavoritesPlaylistKey, true)
    var showCachedPlaylist by rememberPreference(showCachedPlaylistKey, true)

    var showDownloadedPlaylist by rememberPreference(showDownloadedPlaylistKey, true)
    var showOnDevicePlaylist by rememberPreference(showOnDevicePlaylistKey, true)
    var showFloatingIcon by rememberPreference(showFloatingIconKey, false)
    var menuStyle by rememberPreference(menuStyleKey, MenuStyle.List)
    var transitionEffect by rememberPreference(transitionEffectKey, TransitionEffect.SlideHorizontal)

    var showPipedPlaylists by rememberPreference(showPipedPlaylistsKey, true)
    var showPinnedPlaylists by rememberPreference(showPinnedPlaylistsKey, true)


    var customThemeLight_Background0 by rememberPreference(customThemeLight_Background0Key, DefaultLightColorPalette.background0.hashCode())
    var customThemeLight_Background1 by rememberPreference(customThemeLight_Background1Key, DefaultLightColorPalette.background1.hashCode())
    var customThemeLight_Background2 by rememberPreference(customThemeLight_Background2Key, DefaultLightColorPalette.background2.hashCode())
    var customThemeLight_Background3 by rememberPreference(customThemeLight_Background3Key, DefaultLightColorPalette.background3.hashCode())
    var customThemeLight_Background4 by rememberPreference(customThemeLight_Background4Key, DefaultLightColorPalette.background4.hashCode())
    var customThemeLight_Text by rememberPreference(customThemeLight_TextKey, DefaultLightColorPalette.text.hashCode())
    var customThemeLight_TextSecondary by rememberPreference(customThemeLight_textSecondaryKey, DefaultLightColorPalette.textSecondary.hashCode())
    var customThemeLight_TextDisabled by rememberPreference(customThemeLight_textDisabledKey, DefaultLightColorPalette.textDisabled.hashCode())
    var customThemeLight_IconButtonPlayer by rememberPreference(customThemeLight_iconButtonPlayerKey, DefaultLightColorPalette.iconButtonPlayer.hashCode())
    var customThemeLight_Accent by rememberPreference(customThemeLight_accentKey, DefaultLightColorPalette.accent.hashCode())

    var customThemeDark_Background0 by rememberPreference(customThemeDark_Background0Key, DefaultDarkColorPalette.background0.hashCode())
    var customThemeDark_Background1 by rememberPreference(customThemeDark_Background1Key, DefaultDarkColorPalette.background1.hashCode())
    var customThemeDark_Background2 by rememberPreference(customThemeDark_Background2Key, DefaultDarkColorPalette.background2.hashCode())
    var customThemeDark_Background3 by rememberPreference(customThemeDark_Background3Key, DefaultDarkColorPalette.background3.hashCode())
    var customThemeDark_Background4 by rememberPreference(customThemeDark_Background4Key, DefaultDarkColorPalette.background4.hashCode())
    var customThemeDark_Text by rememberPreference(customThemeDark_TextKey, DefaultDarkColorPalette.text.hashCode())
    var customThemeDark_TextSecondary by rememberPreference(customThemeDark_textSecondaryKey, DefaultDarkColorPalette.textSecondary.hashCode())
    var customThemeDark_TextDisabled by rememberPreference(customThemeDark_textDisabledKey, DefaultDarkColorPalette.textDisabled.hashCode())
    var customThemeDark_IconButtonPlayer by rememberPreference(customThemeDark_iconButtonPlayerKey, DefaultDarkColorPalette.iconButtonPlayer.hashCode())
    var customThemeDark_Accent by rememberPreference(customThemeDark_accentKey, DefaultDarkColorPalette.accent.hashCode())

    var resetCustomLightThemeDialog by rememberSaveable { mutableStateOf(false) }
    var resetCustomDarkThemeDialog by rememberSaveable { mutableStateOf(false) }
    var playerPosition by rememberPreference(playerPositionKey, PlayerPosition.Bottom)

    var messageType by rememberPreference(messageTypeKey, MessageType.Modern)
    var searchDisplayOrder by rememberPreference(searchDisplayOrderKey, SearchDisplayOrder.SuggestionsFirst)

    /*  ViMusic Mode Settings  */
    var showTopActionsBar by rememberPreference(showTopActionsBarKey, true)
    var playerControlsType by rememberPreference(playerControlsTypeKey, PlayerControlsType.Essential)
    var playerInfoType by rememberPreference(playerInfoTypeKey, PlayerInfoType.Essential)
    var playerType by rememberPreference(playerTypeKey, PlayerType.Essential)
    var queueType by rememberPreference(queueTypeKey, QueueType.Essential)
    var fadingedge by rememberPreference(fadingedgeKey, false)
    var carousel by rememberPreference(carouselKey, true)
    var carouselSize by rememberPreference(carouselSizeKey, CarouselSize.Biggest)
    var thumbnailType by rememberPreference(thumbnailTypeKey, ThumbnailType.Modern)
    var playerTimelineType by rememberPreference(playerTimelineTypeKey, PlayerTimelineType.FakeAudioBar)
    var playerThumbnailSize by rememberPreference(
        playerThumbnailSizeKey,
        PlayerThumbnailSize.Biggest
    )
    var playerTimelineSize by rememberPreference(
        playerTimelineSizeKey,
        PlayerTimelineSize.Biggest
    )
    var playerInfoShowIcons by rememberPreference(playerInfoShowIconsKey, true)
    var miniPlayerType by rememberPreference(
        miniPlayerTypeKey,
        MiniPlayerType.Modern
    )
    var playerSwapControlsWithTimeline by rememberPreference(
        playerSwapControlsWithTimelineKey,
        false
    )
    var playerPlayButtonType by rememberPreference(
        playerPlayButtonTypeKey,
        PlayerPlayButtonType.Disabled
    )
    var buttonzoomout by rememberPreference(buttonzoomoutKey, false)
    var iconLikeType by rememberPreference(iconLikeTypeKey, IconLikeType.Essential)
    var playerBackgroundColors by rememberPreference(
        playerBackgroundColorsKey,
        PlayerBackgroundColors.BlurredCoverColor
    )
    var blackgradient by rememberPreference(blackgradientKey, false)
    var showTotalTimeQueue by rememberPreference(showTotalTimeQueueKey, true)
    var showNextSongsInPlayer by rememberPreference(showNextSongsInPlayerKey, false)
    var showRemainingSongTime by rememberPreference(showRemainingSongTimeKey, true)
    var disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    var effectRotationEnabled by rememberPreference(effectRotationKey, true)
    var thumbnailTapEnabled by rememberPreference(thumbnailTapEnabledKey, true)
    var clickLyricsText by rememberPreference(clickOnLyricsTextKey, true)
    var backgroundProgress by rememberPreference(
        backgroundProgressKey,
        BackgroundProgress.MiniPlayer
    )
    var transparentBackgroundActionBarPlayer by rememberPreference(
        transparentBackgroundPlayerActionBarKey,
        false
    )
    var actionspacedevenly by rememberPreference(actionspacedevenlyKey, false)
    var tapqueue by rememberPreference(tapqueueKey, true)
    var swipeUpQueue by rememberPreference(swipeUpQueueKey, true)
    var showButtonPlayerAddToPlaylist by rememberPreference(showButtonPlayerAddToPlaylistKey, true)
    var showButtonPlayerArrow by rememberPreference(showButtonPlayerArrowKey, true)
    var showButtonPlayerDownload by rememberPreference(showButtonPlayerDownloadKey, true)
    var showButtonPlayerLoop by rememberPreference(showButtonPlayerLoopKey, true)
    var showButtonPlayerLyrics by rememberPreference(showButtonPlayerLyricsKey, true)
    var expandedplayertoggle by rememberPreference(expandedplayertoggleKey, true)
    var showButtonPlayerShuffle by rememberPreference(showButtonPlayerShuffleKey, true)
    var showButtonPlayerSleepTimer by rememberPreference(showButtonPlayerSleepTimerKey, false)
    var showButtonPlayerMenu by rememberPreference(showButtonPlayerMenuKey, false)
    var showButtonPlayerSystemEqualizer by rememberPreference(
        showButtonPlayerSystemEqualizerKey,
        false
    )
    var showButtonPlayerDiscover by rememberPreference(showButtonPlayerDiscoverKey, false)
    var playerEnableLyricsPopupMessage by rememberPreference(
        playerEnableLyricsPopupMessageKey,
        true
    )
    var visualizerEnabled by rememberPreference(visualizerEnabledKey, false)
    var showthumbnail by rememberPreference(showthumbnailKey, true)
    /*  ViMusic Mode Settings  */

    var queueSwipeLeftAction by rememberPreference(
        queueSwipeLeftActionKey,
        QueueSwipeAction.RemoveFromQueue
    )
    var queueSwipeRightAction by rememberPreference(
        queueSwipeRightActionKey,
        QueueSwipeAction.PlayNext
    )
    var playlistSwipeLeftAction by rememberPreference(
        playlistSwipeLeftActionKey,
        PlaylistSwipeAction.Favourite
    )
    var playlistSwipeRightAction by rememberPreference(
        playlistSwipeRightActionKey,
        PlaylistSwipeAction.PlayNext
    )
    var albumSwipeLeftAction by rememberPreference(
        albumSwipeLeftActionKey,
        AlbumSwipeAction.PlayNext
    )
    var albumSwipeRightAction by rememberPreference(
        albumSwipeRightActionKey,
        AlbumSwipeAction.Bookmark
    )

    var customColor by rememberPreference(customColorKey, Color.Green.hashCode())
    Column(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if (navigationBarPosition == NavigationBarPosition.Left ||
                    navigationBarPosition == NavigationBarPosition.Top ||
                    navigationBarPosition == NavigationBarPosition.Bottom
                ) 1f
                else Dimensions.contentWidthRightBar
            )
            .verticalScroll(rememberScrollState())
    ) {
        HeaderWithIcon(
            title = stringResource(R.string.user_interface),
            iconId = R.drawable.ic_launcher_monochrome,
            enabled = false,
            showIcon = true,
            modifier = Modifier,
            onClick = {}
        )

        SettingsDescription(
            text = stringResource(R.string.ui_settings_description),
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        search.ToolBarButton()
        search.SearchBar(this)

        if (resetCustomLightThemeDialog) {
            ConfirmationDialog(
                text = stringResource(R.string.do_you_really_want_to_reset_the_custom_light_theme_colors),
                onDismiss = { resetCustomLightThemeDialog = false },
                onConfirm = {
                    resetCustomLightThemeDialog = false
                    customThemeLight_Background0 = DefaultLightColorPalette.background0.hashCode()
                    customThemeLight_Background1 = DefaultLightColorPalette.background1.hashCode()
                    customThemeLight_Background2 = DefaultLightColorPalette.background2.hashCode()
                    customThemeLight_Background3 = DefaultLightColorPalette.background3.hashCode()
                    customThemeLight_Background4 = DefaultLightColorPalette.background4.hashCode()
                    customThemeLight_Text = DefaultLightColorPalette.text.hashCode()
                    customThemeLight_TextSecondary = DefaultLightColorPalette.textSecondary.hashCode()
                    customThemeLight_TextDisabled = DefaultLightColorPalette.textDisabled.hashCode()
                    customThemeLight_IconButtonPlayer = DefaultLightColorPalette.iconButtonPlayer.hashCode()
                    customThemeLight_Accent = DefaultLightColorPalette.accent.hashCode()
                }
            )
        }

        if (resetCustomDarkThemeDialog) {
            ConfirmationDialog(
                text = stringResource(R.string.do_you_really_want_to_reset_the_custom_dark_theme_colors),
                onDismiss = { resetCustomDarkThemeDialog = false },
                onConfirm = {
                    resetCustomDarkThemeDialog = false
                    customThemeDark_Background0 = DefaultDarkColorPalette.background0.hashCode()
                    customThemeDark_Background1 = DefaultDarkColorPalette.background1.hashCode()
                    customThemeDark_Background2 = DefaultDarkColorPalette.background2.hashCode()
                    customThemeDark_Background3 = DefaultDarkColorPalette.background3.hashCode()
                    customThemeDark_Background4 = DefaultDarkColorPalette.background4.hashCode()
                    customThemeDark_Text = DefaultDarkColorPalette.text.hashCode()
                    customThemeDark_TextSecondary = DefaultDarkColorPalette.textSecondary.hashCode()
                    customThemeDark_TextDisabled = DefaultDarkColorPalette.textDisabled.hashCode()
                    customThemeDark_IconButtonPlayer = DefaultDarkColorPalette.iconButtonPlayer.hashCode()
                    customThemeDark_Accent = DefaultDarkColorPalette.accent.hashCode()
                }
            )
        }

        val uiSearchContextMatch = search.inputValue.isBlank() || stringResource(R.string.theme).contains(search.inputValue, true) ||
            stringResource(R.string.interface_in_use).contains(search.inputValue, true) ||
            stringResource(R.string.theme).contains(search.inputValue, true) ||
            stringResource(R.string.theme_mode).contains(search.inputValue, true)

        AnimatedVisibility(
            visible = uiSearchContextMatch,
            enter = fadeIn(animationSpec = tween(600)) + scaleIn(animationSpec = tween(600), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.theme),
                icon = R.drawable.color_palette,
                content = {
                    var showUiTypeDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.interface_in_use).contains(search.inputValue,true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.interface_in_use),
                            text = uiType.text,
                            icon = R.drawable.ui,
                            onClick = { showUiTypeDialog = true }
                        )
                    }

                    if (showUiTypeDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.interface_in_use),
                            selectedValue = uiType,
                            onValueSelected = {
                                uiType = it
                                if (uiType == UiType.ViMusic) {
                                    disablePlayerHorizontalSwipe = true
                                    disableIconButtonOnTop = true
                                    playerTimelineType = PlayerTimelineType.FakeAudioBar
                                    visualizerEnabled = false
                                    playerThumbnailSize = PlayerThumbnailSize.Medium
                                    thumbnailTapEnabled = true
                                    showSearchTab = true
                                    showStatsInNavbar = true
                                    navigationBarPosition = NavigationBarPosition.Left
                                    showTopActionsBar = false
                                    playerType = PlayerType.Modern
                                    queueType = QueueType.Modern
                                    fadingedge = false
                                    carousel = true
                                    carouselSize = CarouselSize.Medium
                                    thumbnailType = ThumbnailType.Essential
                                    playerTimelineSize = PlayerTimelineSize.Medium
                                    playerInfoShowIcons = true
                                    miniPlayerType = MiniPlayerType.Modern
                                    playerSwapControlsWithTimeline = false
                                    transparentBackgroundActionBarPlayer = false
                                    playerControlsType = PlayerControlsType.Essential
                                    playerPlayButtonType = PlayerPlayButtonType.Disabled
                                    buttonzoomout = true
                                    iconLikeType = IconLikeType.Essential
                                    playerBackgroundColors = PlayerBackgroundColors.CoverColorGradient
                                    blackgradient = true
                                    showTotalTimeQueue = false
                                    showRemainingSongTime = false
                                    showNextSongsInPlayer = false
                                    disableScrollingText = false
                                    effectRotationEnabled = true
                                    clickLyricsText = true
                                    playerEnableLyricsPopupMessage = true
                                    backgroundProgress = BackgroundProgress.MiniPlayer
                                    transparentBackgroundActionBarPlayer = true
                                    actionspacedevenly = false
                                    tapqueue = false
                                    swipeUpQueue = true
                                    showButtonPlayerDiscover = false
                                    showButtonPlayerDownload = false
                                    showButtonPlayerAddToPlaylist = false
                                    showButtonPlayerLoop = false
                                    showButtonPlayerShuffle = false
                                    showButtonPlayerLyrics = false
                                    expandedplayertoggle = false
                                    showButtonPlayerSleepTimer = false
                                    showButtonPlayerSystemEqualizer = false
                                    showButtonPlayerArrow = false
                                    showButtonPlayerShuffle = false
                                    showButtonPlayerMenu = true
                                    showthumbnail = true
                                    keepPlayerMinimized = false
                                } else {
                                    disablePlayerHorizontalSwipe = false
                                    disableIconButtonOnTop = false
                                    playerTimelineType = lastPlayerTimelineType
                                    playerThumbnailSize = lastPlayerThumbnailSize
                                    playerPlayButtonType = lastPlayerPlayButtonType
                                    navigationBarPosition = NavigationBarPosition.Bottom
                                }
                                RestartAppDialog.showDialog()
                            },
                            valueText = { it.text },
                            values = UiType.values().toList(),
                            onDismiss = { showUiTypeDialog = false }
                        )
                    }

                    var showThemeDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.theme).contains(search.inputValue,true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.theme),
                            text = colorPaletteName.text,
                            icon = R.drawable.color_palette,
                            onClick = { showThemeDialog = true }
                        )
                    }

                    if (showThemeDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.theme),
                            selectedValue = colorPaletteName,
                            onValueSelected = {
                                colorPaletteName = it
                                when (it) {
                                    ColorPaletteName.PureBlack,
                                    ColorPaletteName.ModernBlack -> colorPaletteMode = ColorPaletteMode.System
                                    else -> {}
                                }
                            },
                            valueText = { it.text },
                            values = ColorPaletteName.values().toList(),
                            onDismiss = { showThemeDialog = false }
                        )
                    }

                    AnimatedVisibility(visible = colorPaletteName == ColorPaletteName.CustomColor) {
                        Column {
                            ColorSettingEntry(
                                title = stringResource(R.string.customcolor),
                                text = "",
                                color = Color(customColor),
                                onColorSelected = { customColor = it.hashCode() },
                                modifier = Modifier.padding(start = 25.dp)
                            )
                            ImportantSettingsDescription(
                                text = stringResource(R.string.restarting_rimusic_is_required),
                                modifier = Modifier.padding(start = 25.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = colorPaletteName == ColorPaletteName.Customized) {
                        Column(modifier = Modifier.padding(start = 25.dp)) {
                            // Using standard Custom Light colors

                            ButtonBarSettingEntry(
                                title = stringResource(R.string.title_reset_customized_light_colors),
                                text = stringResource(R.string.info_click_to_reset_default_light_colors),
                                icon = R.drawable.trash,
                                onClick = { resetCustomLightThemeDialog = true }
                            )

                            ColorSettingEntry(
                                title = stringResource(R.string.color_background_1),
                                text = "Light",
                                color = Color(customThemeLight_Background0),
                                onColorSelected = { customThemeLight_Background0 = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_background_2),
                                text = "Light",
                                color = Color(customThemeLight_Background1),
                                onColorSelected = { customThemeLight_Background1 = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_background_3),
                                text = "Light",
                                color = Color(customThemeLight_Background2),
                                onColorSelected = { customThemeLight_Background2 = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_background_4),
                                text = "Light",
                                color = Color(customThemeLight_Background3),
                                onColorSelected = { customThemeLight_Background3 = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_background_5),
                                text = "Light",
                                color = Color(customThemeLight_Background4),
                                onColorSelected = { customThemeLight_Background4 = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_text),
                                text = "Light",
                                color = Color(customThemeLight_Text),
                                onColorSelected = { customThemeLight_Text= it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_text_secondary),
                                text = "Light",
                                color = Color(customThemeLight_TextSecondary),
                                onColorSelected = { customThemeLight_TextSecondary = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_text_disabled),
                                text = "Light",
                                color = Color(customThemeLight_TextDisabled),
                                onColorSelected = { customThemeLight_TextDisabled = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_icon_button_player),
                                text = "Light",
                                color = Color(customThemeLight_IconButtonPlayer),
                                onColorSelected = { customThemeLight_IconButtonPlayer = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_accent),
                                text = "Light",
                                color = Color(customThemeLight_Accent),
                                onColorSelected = { customThemeLight_Accent = it.hashCode() }
                            )


                            ButtonBarSettingEntry(
                                title = stringResource(R.string.title_reset_customized_dark_colors),
                                text = stringResource(R.string.click_to_reset_default_dark_colors),
                                icon = R.drawable.trash,
                                onClick = { resetCustomDarkThemeDialog = true }
                            )

                            ColorSettingEntry(
                                title = stringResource(R.string.color_background_1),
                                text = "Dark",
                                color = Color(customThemeDark_Background0),
                                onColorSelected = { customThemeDark_Background0 = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_background_2),
                                text = "Dark",
                                color = Color(customThemeDark_Background1),
                                onColorSelected = { customThemeDark_Background1 = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_background_3),
                                text = "Dark",
                                color = Color(customThemeDark_Background2),
                                onColorSelected = { customThemeDark_Background2 = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_background_4),
                                text = "Dark",
                                color = Color(customThemeDark_Background3),
                                onColorSelected = { customThemeDark_Background3 = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_background_5),
                                text = "Dark",
                                color = Color(customThemeDark_Background4),
                                onColorSelected = { customThemeDark_Background4 = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_text),
                                text = "Dark",
                                color = Color(customThemeDark_Text),
                                onColorSelected = { customThemeDark_Text= it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_text_secondary),
                                text = "Dark",
                                color = Color(customThemeDark_TextSecondary),
                                onColorSelected = { customThemeDark_TextSecondary = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_text_disabled),
                                text = "Dark",
                                color = Color(customThemeDark_TextDisabled),
                                onColorSelected = { customThemeDark_TextDisabled = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_icon_button_player),
                                text = "Dark",
                                color = Color(customThemeDark_IconButtonPlayer),
                                onColorSelected = { customThemeDark_IconButtonPlayer = it.hashCode() }
                            )
                            ColorSettingEntry(
                                title = stringResource(R.string.color_accent),
                                text = "Dark",
                                color = Color(customThemeDark_Accent),
                                onColorSelected = { customThemeDark_Accent = it.hashCode() }
                            )

                        }
                    }

                    var showThemeModeDialog by remember { mutableStateOf(false) }
                    val themeModeEnabled = when (colorPaletteName) {
                        ColorPaletteName.PureBlack -> false
                        ColorPaletteName.ModernBlack -> false
                        else -> true
                    }
                    if (search.inputValue.isBlank() || stringResource(R.string.theme_mode).contains(search.inputValue,true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.theme_mode),
                            text = colorPaletteMode.text,
                            icon = R.drawable.sparkles,
                            onClick = { if(themeModeEnabled) showThemeModeDialog = true }
                        )
                    }

                    if (showThemeModeDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.theme_mode),
                            selectedValue = colorPaletteMode,
                            onValueSelected = { colorPaletteMode = it },
                            valueText = { it.text },
                            values = ColorPaletteMode.values().toList(),
                            onDismiss = { showThemeModeDialog = false }
                        )
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        val navSearchContextMatch = search.inputValue.isBlank() || 
            stringResource(R.string.search_display_order).contains(search.inputValue, true) ||
            stringResource(R.string.navigation_bar_position).contains(search.inputValue, true) ||
            stringResource(R.string.navigation_bar_type).contains(search.inputValue, true) ||
            stringResource(R.string.player_position).contains(search.inputValue, true) ||
            stringResource(R.string.menu_style).contains(search.inputValue, true) ||
            stringResource(R.string.message_type).contains(search.inputValue, true) ||
            stringResource(R.string.default_page).contains(search.inputValue, true) ||
            stringResource(R.string.transition_effect).contains(search.inputValue, true)

        AnimatedVisibility(
            visible = navSearchContextMatch,
            enter = fadeIn(animationSpec = tween(700)) + scaleIn(animationSpec = tween(700), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.user_interface),
                icon = R.drawable.ui,
                content = {
                    var showNavPosDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.navigation_bar_position).contains(search.inputValue,true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.navigation_bar_position),
                            text = navigationBarPosition.text,
                            icon = R.drawable.locate,
                            onClick = { if(uiType != UiType.ViMusic) showNavPosDialog = true }
                        )
                    }
                    if (showNavPosDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.navigation_bar_position),
                            selectedValue = navigationBarPosition,
                            onValueSelected = { navigationBarPosition = it },
                            valueText = { it.text },
                            values = NavigationBarPosition.values().toList(),
                            onDismiss = { showNavPosDialog = false }
                        )
                    }

                    var showNavTypeDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.navigation_bar_type).contains(search.inputValue,true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.navigation_bar_type),
                            text = navigationBarType.text,
                            icon = R.drawable.burger,
                            onClick = { showNavTypeDialog = true }
                        )
                    }
                    if (showNavTypeDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.navigation_bar_type),
                            selectedValue = navigationBarType,
                            onValueSelected = { navigationBarType = it },
                            valueText = { it.text },
                            values = NavigationBarType.values().toList(),
                            onDismiss = { showNavTypeDialog = false }
                        )
                    }

                    var showPlayerPosDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.player_position).contains(search.inputValue,true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.player_position),
                            text = playerPosition.text,
                            icon = R.drawable.position,
                            onClick = { showPlayerPosDialog = true }
                        )
                    }
                    if (showPlayerPosDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.player_position),
                            selectedValue = playerPosition,
                            onValueSelected = { playerPosition = it },
                            valueText = { it.text },
                            values = PlayerPosition.values().toList(),
                            onDismiss = { showPlayerPosDialog = false }
                        )
                    }

                    var showMenuStyleDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.menu_style).contains(search.inputValue,true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.menu_style),
                            text = menuStyle.text,
                            icon = R.drawable.reorder,
                            onClick = { showMenuStyleDialog = true }
                        )
                    }
                    if (showMenuStyleDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.menu_style),
                            selectedValue = menuStyle,
                            onValueSelected = { menuStyle = it },
                            valueText = { it.text },
                            values = MenuStyle.values().toList(),
                            onDismiss = { showMenuStyleDialog = false }
                        )
                    }

                    var showMessageTypeDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.message_type).contains(search.inputValue,true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.message_type),
                            text = messageType.text,
                            icon = R.drawable.information,
                            onClick = { showMessageTypeDialog = true }
                        )
                    }
                    if (showMessageTypeDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.message_type),
                            selectedValue = messageType,
                            onValueSelected = { messageType = it },
                            valueText = { it.text },
                            values = MessageType.values().toList(),
                            onDismiss = { showMessageTypeDialog = false }
                        )
                    }

                    var showDefPageDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.default_page).contains(search.inputValue,true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.default_page),
                            text = indexNavigationTab.text,
                            icon = R.drawable.star_brilliant,
                            onClick = { showDefPageDialog = true }
                        )
                    }
                    if (showDefPageDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.default_page),
                            selectedValue = indexNavigationTab,
                            onValueSelected = { indexNavigationTab = it },
                            valueText = { it.text },
                            values = HomeScreenTabs.values().toList(),
                            onDismiss = { showDefPageDialog = false }
                        )
                    }

                    var showTransDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.transition_effect).contains(search.inputValue,true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.transition_effect),
                            text = transitionEffect.text,
                            icon = R.drawable.images_sharp,
                            onClick = { showTransDialog = true }
                        )
                    }
                    if (showTransDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.transition_effect),
                            selectedValue = transitionEffect,
                            onValueSelected = { transitionEffect = it },
                            valueText = { it.text },
                            values = TransitionEffect.values().toList(),
                            onDismiss = { showTransDialog = false }
                        )
                    }

                    var showSearchOrderDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.search_display_order).contains(search.inputValue,true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.search_display_order),
                            text = stringResource(R.string.search_display_order_description),
                            icon = R.drawable.search,
                            onClick = { showSearchOrderDialog = true }
                        )
                    }
                    if (showSearchOrderDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.search_display_order),
                            selectedValue = searchDisplayOrder,
                            onValueSelected = { searchDisplayOrder = it },
                            valueText = {
                                when (it) {
                                    SearchDisplayOrder.SuggestionsFirst -> stringResource(R.string.search_display_order_suggestions_first)
                                    SearchDisplayOrder.SavedSearchesFirst -> stringResource(R.string.search_display_order_saved_searches_first)
                                }
                            },
                            values = SearchDisplayOrder.values().toList(),
                            onDismiss = { showSearchOrderDialog = false }
                        )
                    }

                    if ( UiType.ViMusic.isCurrent() ) {
                        if (search.inputValue.isBlank() || stringResource(R.string.vimusic_show_search_button_in_navigation_bar).contains(search.inputValue, true)) {
                            OtherSwitchSettingEntry(
                                title = stringResource(R.string.vimusic_show_search_button_in_navigation_bar),
                                text = stringResource(R.string.vismusic_only_in_left_right_navigation_bar),
                                isChecked = showSearchTab,
                                onCheckedChange = { showSearchTab = it },
                                icon = R.drawable.search
                            )
                        }
                        if (search.inputValue.isBlank() || stringResource(R.string.show_statistics_in_navigation_bar).contains(search.inputValue, true)) {
                            OtherSwitchSettingEntry(
                                title = stringResource(R.string.show_statistics_in_navigation_bar),
                                text = "",
                                isChecked = showStatsInNavbar,
                                onCheckedChange = { showStatsInNavbar = it },
                                icon = R.drawable.stats_chart
                            )
                        }
                    }




                    if (search.inputValue.isBlank() || stringResource(R.string.show_floating_icon).contains(search.inputValue,true)) {
                        OtherSwitchSettingEntry(
                            title = stringResource(R.string.show_floating_icon),
                            text = "",
                            isChecked = showFloatingIcon,
                            onCheckedChange = { showFloatingIcon = it },
                            icon = R.drawable.maximize
                        )
                    }

                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        val typoSearchContextMatch = search.inputValue.isBlank() || 
            stringResource(R.string.settings_use_font_type).contains(search.inputValue, true) ||
            stringResource(R.string.use_system_font).contains(search.inputValue, true) ||
            stringResource(R.string.apply_font_padding).contains(search.inputValue, true)

        AnimatedVisibility(
            visible = typoSearchContextMatch,
            enter = fadeIn(animationSpec = tween(800)) + scaleIn(animationSpec = tween(800), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.settings_use_font_type),
                icon = R.drawable.text,
                content = {
                    var showFontDialog by remember { mutableStateOf(false) }
                    if (search.inputValue.isBlank() || stringResource(R.string.settings_use_font_type).contains(search.inputValue,true)) {
                        OtherSettingsEntry(
                            title = stringResource(R.string.settings_use_font_type),
                            text = fontType.name,
                            icon = R.drawable.text,
                            onClick = { showFontDialog = true }
                        )
                    }
                    if (showFontDialog) {
                        ValueSelectorDialog(
                            title = stringResource(R.string.settings_use_font_type),
                            selectedValue = fontType,
                            onValueSelected = { fontType = it },
                            valueText = { it.name },
                            values = FontType.values().toList(),
                            onDismiss = { showFontDialog = false }
                        )
                    }

                    if (search.inputValue.isBlank() || stringResource(R.string.use_system_font).contains(search.inputValue,true)) {
                        OtherSwitchSettingEntry(
                            title = stringResource(R.string.use_system_font),
                            text = stringResource(R.string.use_font_by_the_system),
                            isChecked = useSystemFont,
                            onCheckedChange = { useSystemFont = it },
                            icon = R.drawable.settings
                        )
                    }

                    if (search.inputValue.isBlank() || stringResource(R.string.apply_font_padding).contains(search.inputValue,true)) {
                        OtherSwitchSettingEntry(
                            title = stringResource(R.string.apply_font_padding),
                            text = stringResource(R.string.add_spacing_around_texts),
                            isChecked = applyFontPadding,
                            onCheckedChange = { applyFontPadding = it },
                            icon = R.drawable.resize
                        )
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        val swipeSearchContextMatch = search.inputValue.isBlank() || 
            stringResource(R.string.swipe_to_action).contains(search.inputValue, true)

        AnimatedVisibility(
            visible = swipeSearchContextMatch,
            enter = fadeIn(animationSpec = tween(900)) + scaleIn(animationSpec = tween(900), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.swipe_to_action),
                icon = R.drawable.arrow_forward,
                content = {
                    if (search.inputValue.isBlank() || stringResource(R.string.swipe_to_action).contains(search.inputValue,true)) {
                        OtherSwitchSettingEntry(
                            title = stringResource(R.string.swipe_to_action),
                            text = stringResource(R.string.activate_the_action_menu_by_swiping_the_song_left_or_right),
                            isChecked = isSwipeToActionEnabled,
                            onCheckedChange = { isSwipeToActionEnabled = it },
                            icon = R.drawable.arrow_forward
                        )
                    }

                    AnimatedVisibility(visible = isSwipeToActionEnabled) {
                        Column(modifier = Modifier.padding(start = 25.dp)) {
                            var showQSLeftDialog by remember { mutableStateOf(false) }
                            OtherSettingsEntry(
                                title = stringResource(R.string.queue_and_local_playlists_left_swipe),
                                text = queueSwipeLeftAction.text,
                                icon = R.drawable.arrow_left,
                                onClick = { showQSLeftDialog = true }
                            )
                            if (showQSLeftDialog) {
                                ValueSelectorDialog(
                                    title = stringResource(R.string.queue_and_local_playlists_left_swipe),
                                    selectedValue = queueSwipeLeftAction,
                                    onValueSelected = { queueSwipeLeftAction = it },
                                    valueText = { it.text },
                                    values = QueueSwipeAction.values().toList(),
                                    onDismiss = { showQSLeftDialog = false }
                                )
                            }
                            
                            var showPLLeftDialog by remember { mutableStateOf(false) }
                            OtherSettingsEntry(
                                title = stringResource(R.string.playlist_left_swipe),
                                text = playlistSwipeLeftAction.text,
                                icon = R.drawable.arrow_left,
                                onClick = { showPLLeftDialog = true }
                            )
                            if (showPLLeftDialog) {
                                ValueSelectorDialog(
                                    title = stringResource(R.string.playlist_left_swipe),
                                    selectedValue = playlistSwipeLeftAction,
                                    onValueSelected = { playlistSwipeLeftAction = it },
                                    valueText = { it.text },
                                    values = PlaylistSwipeAction.values().toList(),
                                    onDismiss = { showPLLeftDialog = false }
                                )
                            }

                            var showPLRightDialog by remember { mutableStateOf(false) }
                            OtherSettingsEntry(
                                title = stringResource(R.string.playlist_right_swipe),
                                text = playlistSwipeRightAction.text,
                                icon = R.drawable.arrow_right,
                                onClick = { showPLRightDialog = true }
                            )
                            if (showPLRightDialog) {
                                ValueSelectorDialog(
                                    title = stringResource(R.string.playlist_right_swipe),
                                    selectedValue = playlistSwipeRightAction,
                                    onValueSelected = { playlistSwipeRightAction = it },
                                    valueText = { it.text },
                                    values = PlaylistSwipeAction.values().toList(),
                                    onDismiss = { showPLRightDialog = false }
                                )
                            }

                            var showALeftDialog by remember { mutableStateOf(false) }
                            OtherSettingsEntry(
                                title = stringResource(R.string.album_left_swipe),
                                text = albumSwipeLeftAction.text,
                                icon = R.drawable.arrow_left,
                                onClick = { showALeftDialog = true }
                            )
                            if (showALeftDialog) {
                                ValueSelectorDialog(
                                    title = stringResource(R.string.album_left_swipe),
                                    selectedValue = albumSwipeLeftAction,
                                    onValueSelected = { albumSwipeLeftAction = it },
                                    valueText = { it.text },
                                    values = AlbumSwipeAction.values().toList(),
                                    onDismiss = { showALeftDialog = false }
                                )
                            }

                            var showARightDialog by remember { mutableStateOf(false) }
                            OtherSettingsEntry(
                                title = stringResource(R.string.album_right_swipe),
                                text = albumSwipeRightAction.text,
                                icon = R.drawable.arrow_right,
                                onClick = { showARightDialog = true }
                            )
                            if (showARightDialog) {
                                ValueSelectorDialog(
                                    title = stringResource(R.string.album_right_swipe),
                                    selectedValue = albumSwipeRightAction,
                                    onValueSelected = { albumSwipeRightAction = it },
                                    valueText = { it.text },
                                    values = AlbumSwipeAction.values().toList(),
                                    onDismiss = { showARightDialog = false }
                                )
                            }

                            var showQSRightDialog by remember { mutableStateOf(false) }
                            OtherSettingsEntry(
                                title = stringResource(R.string.queue_and_local_playlists_right_swipe),
                                text = queueSwipeRightAction.text,
                                icon = R.drawable.arrow_right,
                                onClick = { showQSRightDialog = true }
                            )
                            if (showQSRightDialog) {
                                ValueSelectorDialog(
                                    title = stringResource(R.string.queue_and_local_playlists_right_swipe),
                                    selectedValue = queueSwipeRightAction,
                                    onValueSelected = { queueSwipeRightAction = it },
                                    valueText = { it.text },
                                    values = QueueSwipeAction.values().toList(),
                                    onDismiss = { showQSRightDialog = false }
                                )
                            }
                        }
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        val listsSearchContextMatch = search.inputValue.isBlank() || 
            stringResource(R.string.songs).contains(search.inputValue, true) ||
            stringResource(R.string.playlists).contains(search.inputValue, true)

        AnimatedVisibility(
            visible = listsSearchContextMatch,
            enter = fadeIn(animationSpec = tween(1000)) + scaleIn(animationSpec = tween(1000), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.library),
                icon = R.drawable.library,
                content = {
                    if (search.inputValue.isBlank() || "${stringResource(R.string.show)} ${stringResource(R.string.favorites)}".contains(search.inputValue,true))
                        OtherSwitchSettingEntry(
                            title = "${stringResource(R.string.show)} ${stringResource(R.string.favorites)}",
                            text = "",
                            isChecked = showFavoritesPlaylist,
                            onCheckedChange = { showFavoritesPlaylist = it },
                            icon = R.drawable.heart
                        )

                    if (search.inputValue.isBlank() || "${stringResource(R.string.show)} ${stringResource(R.string.cached)}".contains(search.inputValue,true))
                        OtherSwitchSettingEntry(
                            title = "${stringResource(R.string.show)} ${stringResource(R.string.cached)}",
                            text = "",
                            isChecked = showCachedPlaylist,
                            onCheckedChange = { showCachedPlaylist = it },
                            icon = R.drawable.server
                        )

                    if (search.inputValue.isBlank() || "${stringResource(R.string.show)} ${stringResource(R.string.downloaded)}".contains(search.inputValue,true))
                        OtherSwitchSettingEntry(
                            title = "${stringResource(R.string.show)} ${stringResource(R.string.downloaded)}",
                            text = "",
                            isChecked = showDownloadedPlaylist,
                            onCheckedChange = { showDownloadedPlaylist = it },
                            icon = R.drawable.downloaded
                        )

                    if (search.inputValue.isBlank() || "${stringResource(R.string.show)} ${stringResource(R.string.on_device)}".contains(search.inputValue,true))
                        OtherSwitchSettingEntry(
                            title = "${stringResource(R.string.show)} ${stringResource(R.string.on_device)}",
                            text = "",
                            isChecked = showOnDevicePlaylist,
                            onCheckedChange = { showOnDevicePlaylist = it },
                            icon = R.drawable.folder
                        )

                    if (search.inputValue.isBlank() || "${stringResource(R.string.show)} ${stringResource(R.string.piped_playlists)}".contains(search.inputValue,true))
                        OtherSwitchSettingEntry(
                            title = "${stringResource(R.string.show)} ${stringResource(R.string.piped_playlists)}",
                            text = "",
                            isChecked = showPipedPlaylists,
                            onCheckedChange = { showPipedPlaylists = it },
                            icon = R.drawable.piped_logo
                        )

                    if (search.inputValue.isBlank() || "${stringResource(R.string.show)} ${stringResource(R.string.pinned_playlists)}".contains(search.inputValue,true))
                        OtherSwitchSettingEntry(
                            title = "${stringResource(R.string.show)} ${stringResource(R.string.pinned_playlists)}",
                            text = "",
                            isChecked = showPinnedPlaylists,
                            onCheckedChange = { showPinnedPlaylists = it },
                            icon = R.drawable.pin_filled
                        )
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = search.inputValue.isBlank(),
            enter = fadeIn(animationSpec = tween(1100)) + scaleIn(animationSpec = tween(1100), initialScale = 0.9f)
        ) {
            SettingsSectionCard(
                title = stringResource(R.string.settings_reset),
                icon = R.drawable.refresh,
                content = {
                    var resetToDefault by remember { mutableStateOf(false) }
                    val context = LocalContext.current
                    OtherSettingsEntry(
                        title = stringResource(R.string.settings_reset),
                        text = stringResource(R.string.settings_restore_default_settings),
                        icon = R.drawable.refresh,
                        onClick = { resetToDefault = true }
                    )
                    if (resetToDefault) {
                        DefaultUiSettings()
                        resetToDefault = false
                        navController.popBackStack()
                        Toaster.done()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
    }
}