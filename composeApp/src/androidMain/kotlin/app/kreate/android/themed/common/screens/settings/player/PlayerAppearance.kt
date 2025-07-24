package app.kreate.android.themed.common.screens.settings.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.themed.common.component.settings.SettingComponents
import app.kreate.android.themed.common.component.settings.SettingEntrySearch
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.AnimatedGradient
import it.fast4x.rimusic.enums.CarouselSize
import it.fast4x.rimusic.enums.ColorPaletteMode
import it.fast4x.rimusic.enums.ColorPaletteName
import it.fast4x.rimusic.enums.PlayerBackgroundColors
import it.fast4x.rimusic.enums.PlayerControlsType
import it.fast4x.rimusic.enums.PlayerInfoType
import it.fast4x.rimusic.enums.PlayerPlayButtonType
import it.fast4x.rimusic.enums.PlayerThumbnailSize
import it.fast4x.rimusic.enums.PlayerTimelineSize
import it.fast4x.rimusic.enums.PlayerTimelineType
import it.fast4x.rimusic.enums.PlayerType
import it.fast4x.rimusic.enums.ThumbnailRoundness
import it.fast4x.rimusic.enums.ThumbnailType
import it.fast4x.rimusic.ui.components.themed.AppearancePresetDialog
import it.fast4x.rimusic.utils.isLandscape

@Composable
fun PlayerAppearance( search: SettingEntrySearch ) {
    var showthumbnail by Preferences.PLAYER_SHOW_THUMBNAIL
    var transparentbar by Preferences.TRANSPARENT_TIMELINE
    var showlyricsthumbnail by Preferences.LYRICS_SHOW_THUMBNAIL
    var expandedplayer by Preferences.PLAYER_EXPANDED
    var playerPlayButtonType by Preferences.PLAYER_PLAY_BUTTON_TYPE
    var bottomgradient by Preferences.PLAYER_BOTTOM_GRADIENT
    var visualizerEnabled by Preferences.PLAYER_VISUALIZER
    var playerTimelineType by Preferences.PLAYER_TIMELINE_TYPE
    var playerThumbnailSize by Preferences.PLAYER_PORTRAIT_THUMBNAIL_SIZE
    var playerTimelineSize by Preferences.PLAYER_TIMELINE_SIZE
    var showButtonPlayerAddToPlaylist by Preferences.PLAYER_ACTION_ADD_TO_PLAYLIST
    var showButtonPlayerArrow by Preferences.PLAYER_ACTION_OPEN_QUEUE_ARROW
    var showButtonPlayerDownload by Preferences.PLAYER_ACTION_DOWNLOAD
    var showButtonPlayerLoop by Preferences.PLAYER_ACTION_LOOP
    var showButtonPlayerLyrics by Preferences.PLAYER_ACTION_SHOW_LYRICS
    var expandedplayertoggle by Preferences.PLAYER_ACTION_TOGGLE_EXPAND
    var showButtonPlayerShuffle by Preferences.PLAYER_ACTION_SHUFFLE
    var showButtonPlayerSleepTimer by Preferences.PLAYER_ACTION_SLEEP_TIMER
    var showButtonPlayerMenu by Preferences.PLAYER_ACTION_SHOW_MENU
    var showButtonPlayerStartradio by Preferences.PLAYER_ACTION_START_RADIO
    var showButtonPlayerDiscover by Preferences.PLAYER_ACTION_DISCOVER
    var showButtonPlayerVideo by Preferences.PLAYER_ACTION_TOGGLE_VIDEO
    var showTotalTimeQueue by Preferences.PLAYER_SHOW_TOTAL_QUEUE_TIME
    var showNextSongsInPlayer by Preferences.PLAYER_SHOW_NEXT_IN_QUEUE
    var showRemainingSongTime by Preferences.PLAYER_SHOW_SONGS_REMAINING_TIME
    var thumbnailRoundness by Preferences.THUMBNAIL_BORDER_RADIUS
    var playerBackgroundColors by Preferences.PLAYER_BACKGROUND
    var showTopActionsBar by Preferences.PLAYER_SHOW_TOP_ACTIONS_BAR
    var playerControlsType by Preferences.PLAYER_CONTROLS_TYPE
    var playerInfoType by Preferences.PLAYER_INFO_TYPE
    var transparentBackgroundActionBarPlayer by Preferences.PLAYER_TRANSPARENT_ACTIONS_BAR
    var actionspacedevenly by Preferences.PLAYER_ACTION_BUTTONS_SPACED_EVENLY
    var thumbnailType by Preferences.THUMBNAIL_TYPE
    var playerType by Preferences.PLAYER_TYPE
    var noblur by Preferences.PLAYER_BACKGROUND_BLUR
    var fadingedge by Preferences.PLAYER_BACKGROUND_FADING_EDGE
    var carouselSize by Preferences.CAROUSEL_SIZE
    var playerInfoShowIcons by Preferences.PLAYER_SONG_INFO_ICON
    var showCoverThumbnailAnimation by Preferences.PLAYER_THUMBNAIL_ANIMATION
    var topPadding by Preferences.PLAYER_TOP_PADDING
    var animatedGradient by Preferences.ANIMATED_GRADIENT
    var blurStrength by Preferences.PLAYER_BACKGROUND_BLUR_STRENGTH
    var thumbnailFadeEx  by Preferences.PLAYER_THUMBNAIL_FADE_EX
    var thumbnailFade  by Preferences.PLAYER_THUMBNAIL_FADE
    var thumbnailSpacing  by Preferences.PLAYER_THUMBNAIL_SPACING
    var colorPaletteName by Preferences.COLOR_PALETTE
    var colorPaletteMode by Preferences.THEME_MODE

    var appearanceChooser by remember{ mutableStateOf(false)}
    if (appearanceChooser) {
        AppearancePresetDialog(
            onDismiss = {appearanceChooser = false},
            onClick0 = {
                showTopActionsBar = true
                showthumbnail = true
                playerBackgroundColors = PlayerBackgroundColors.BlurredCoverColor
                blurStrength = 50f
                thumbnailRoundness = ThumbnailRoundness.None
                playerInfoType = PlayerInfoType.Essential
                playerTimelineType = PlayerTimelineType.ThinBar
                playerTimelineSize = PlayerTimelineSize.Biggest
                playerControlsType = PlayerControlsType.Essential
                playerPlayButtonType = PlayerPlayButtonType.Disabled
                transparentbar = true
                playerType = PlayerType.Essential
                showlyricsthumbnail = false
                expandedplayer = true
                thumbnailType = ThumbnailType.Modern
                playerThumbnailSize = PlayerThumbnailSize.Big
                showTotalTimeQueue = false
                bottomgradient = true
                showRemainingSongTime = true
                showNextSongsInPlayer = false
                colorPaletteName = ColorPaletteName.Dynamic
                colorPaletteMode = ColorPaletteMode.System
                ///////ACTION BAR BUTTONS////////////////
                transparentBackgroundActionBarPlayer = true
                actionspacedevenly = true
                showButtonPlayerVideo = false
                showButtonPlayerDiscover = false
                showButtonPlayerDownload = false
                showButtonPlayerAddToPlaylist = true
                showButtonPlayerLoop = false
                showButtonPlayerShuffle = true
                showButtonPlayerLyrics = false
                expandedplayertoggle = false
                showButtonPlayerSleepTimer = false
                visualizerEnabled = false
                appearanceChooser = false
                showButtonPlayerArrow = false
                showButtonPlayerStartradio = false
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
                showButtonPlayerDownload = false
                showButtonPlayerAddToPlaylist = true
                showButtonPlayerLoop = false
                showButtonPlayerShuffle = false
                showButtonPlayerLyrics = false
                expandedplayertoggle = true
                showButtonPlayerSleepTimer = false
                visualizerEnabled = false
                appearanceChooser = false
                showButtonPlayerArrow = false
                showButtonPlayerStartradio = false
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
                showNextSongsInPlayer = false
                colorPaletteName = ColorPaletteName.Dynamic
                colorPaletteMode = ColorPaletteMode.System
                ///////ACTION BAR BUTTONS////////////////
                transparentBackgroundActionBarPlayer = true
                actionspacedevenly = true
                showButtonPlayerVideo = false
                showButtonPlayerDiscover = false
                showButtonPlayerDownload = false
                showButtonPlayerAddToPlaylist = false
                showButtonPlayerLoop = false
                showButtonPlayerShuffle = false
                showButtonPlayerLyrics = false
                expandedplayertoggle = false
                showButtonPlayerSleepTimer = false
                visualizerEnabled = false
                appearanceChooser = false
                showButtonPlayerArrow = false
                showButtonPlayerStartradio = false
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
                showNextSongsInPlayer = false
                ///////ACTION BAR BUTTONS////////////////
                transparentBackgroundActionBarPlayer = true
                actionspacedevenly = true
                showButtonPlayerVideo = false
                showButtonPlayerDiscover = false
                showButtonPlayerDownload = false
                showButtonPlayerAddToPlaylist = false
                showButtonPlayerLoop = true
                showButtonPlayerShuffle = true
                showButtonPlayerLyrics = false
                expandedplayertoggle = false
                showButtonPlayerSleepTimer = false
                visualizerEnabled = false
                appearanceChooser = false
                showButtonPlayerArrow = true
                showButtonPlayerStartradio = false
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
                showNextSongsInPlayer = false
                ///////ACTION BAR BUTTONS////////////////
                transparentBackgroundActionBarPlayer = true
                actionspacedevenly = true
                showButtonPlayerVideo = false
                showButtonPlayerDiscover = false
                showButtonPlayerDownload = true
                showButtonPlayerAddToPlaylist = false
                showButtonPlayerLoop = false
                showButtonPlayerShuffle = false
                showButtonPlayerLyrics = false
                expandedplayertoggle = true
                showButtonPlayerSleepTimer = false
                visualizerEnabled = false
                appearanceChooser = false
                showButtonPlayerArrow =false
                showButtonPlayerStartradio = false
                showButtonPlayerMenu = true
                ///////////////////////////
                appearanceChooser = false
            },
            onClick5 = {
                showTopActionsBar = true
                showthumbnail = true
                playerBackgroundColors = PlayerBackgroundColors.CoverColorGradient
                playerInfoType = PlayerInfoType.Essential
                playerTimelineType = PlayerTimelineType.Wavy
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
                showNextSongsInPlayer = false
                ///////ACTION BAR BUTTONS////////////////
                transparentBackgroundActionBarPlayer = true
                actionspacedevenly = true
                showButtonPlayerVideo = false
                showButtonPlayerDiscover = false
                showButtonPlayerDownload = false
                showButtonPlayerAddToPlaylist = false
                showButtonPlayerLoop = false
                showButtonPlayerShuffle = true
                showButtonPlayerLyrics = true
                expandedplayertoggle = false
                showButtonPlayerSleepTimer = false
                visualizerEnabled = false
                appearanceChooser = false
                showButtonPlayerArrow =false
                showButtonPlayerStartradio = false
                showButtonPlayerMenu = true
                ///////////////////////////
                appearanceChooser = false
            }
        )
    }

    if (!isLandscape) {
        if( search appearsIn R.string.appearancepresets )
            SettingComponents.Text(
                stringResource( R.string.appearancepresets ),
                { appearanceChooser = true },
                subtitle = stringResource( R.string.appearancepresetssecondary )
            )

        if ( search appearsIn R.string.show_player_top_actions_bar )
            SettingComponents.BooleanEntry(
                Preferences.PLAYER_SHOW_TOP_ACTIONS_BAR,
                titleId = R.string.show_player_top_actions_bar
            )

        if( !showTopActionsBar && search appearsIn R.string.blankspace )
            SettingComponents.BooleanEntry(
                Preferences.PLAYER_TOP_PADDING,
                R.string.blankspace
            )
    }
    if ( search appearsIn R.string.playertype )
        SettingComponents.EnumEntry(
            Preferences.PLAYER_TYPE,
            R.string.playertype
        )

    if ( search appearsIn R.string.queuetype )
        SettingComponents.EnumEntry(
            Preferences.QUEUE_TYPE,
            R.string.queuetype
        )

    if (playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor
        && search appearsIn R.string.show_thumbnail
    )
        SettingComponents.BooleanEntry(
            Preferences.PLAYER_SHOW_THUMBNAIL,
            R.string.show_thumbnail
        )
    AnimatedVisibility(visible = !showthumbnail && playerType == PlayerType.Modern && !isLandscape) {
        if ( search appearsIn R.string.swipe_Animation_No_Thumbnail )
            SettingComponents.EnumEntry(
                Preferences.PLAYER_NO_THUMBNAIL_SWIPE_ANIMATION,
                R.string.swipe_Animation_No_Thumbnail,
                Modifier.padding(
                    start = if ( playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor )
                        25.dp
                    else
                        0.dp
                ),
            )
    }
    AnimatedVisibility(visible = showthumbnail) {
        Column(
            Modifier.padding(
                start = if ( playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor )
                    25.dp
                else
                    0.dp
            )
        ) {
            if ( playerType == PlayerType.Modern && search appearsIn R.string.fadingedge )
                SettingComponents.BooleanEntry(
                    Preferences.PLAYER_BACKGROUND_FADING_EDGE,
                    R.string.fadingedge
                )

            if (playerType == PlayerType.Modern && !isLandscape && (expandedplayertoggle || expandedplayer)) {
                if ( search appearsIn R.string.carousel )
                    SettingComponents.BooleanEntry(
                        Preferences.PLAYER_THUMBNAILS_CAROUSEL,
                        R.string.carousel
                    )

                if ( search appearsIn R.string.carouselsize )
                    SettingComponents.EnumEntry(
                        Preferences.CAROUSEL_SIZE,
                        R.string.carouselsize
                    )
            }
            if (playerType == PlayerType.Essential) {
                if ( search appearsIn R.string.thumbnailpause )
                    SettingComponents.BooleanEntry(
                        Preferences.PLAYER_SHRINK_THUMBNAIL_ON_PAUSE,
                        R.string.thumbnailpause
                    )
                if ( search appearsIn R.string.show_lyrics_thumbnail )
                    SettingComponents.BooleanEntry(
                        Preferences.LYRICS_SHOW_THUMBNAIL,
                        R.string.show_lyrics_thumbnail
                    )
                if ( visualizerEnabled && search appearsIn R.string.showvisthumbnail )
                    SettingComponents.BooleanEntry(
                        Preferences.PLAYER_SHOW_THUMBNAIL_ON_VISUALIZER,
                        R.string.showvisthumbnail
                    )
            }

            if ( search appearsIn R.string.show_cover_thumbnail_animation ) {
                SettingComponents.BooleanEntry(
                    Preferences.PLAYER_THUMBNAIL_ANIMATION,
                    R.string.show_cover_thumbnail_animation
                )
                AnimatedVisibility(visible = showCoverThumbnailAnimation) {
                    if( search appearsIn R.string.cover_thumbnail_animation_type )
                        SettingComponents.EnumEntry(
                            Preferences.PLAYER_THUMBNAIL_TYPE,
                            R.string.cover_thumbnail_animation_type,
                            Modifier.padding(
                                start = if ( playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor )
                                    25.dp
                                else
                                    0.dp
                            )
                        )
                }
            }

            if (isLandscape) {
                if ( search appearsIn R.string.player_thumbnail_size )
                    SettingComponents.EnumEntry(
                        Preferences.PLAYER_LANDSCAPE_THUMBNAIL_SIZE,
                        R.string.player_thumbnail_size
                    )
            } else {
                if ( search appearsIn R.string.player_thumbnail_size )
                    SettingComponents.EnumEntry(
                        Preferences.PLAYER_PORTRAIT_THUMBNAIL_SIZE,
                        R.string.player_thumbnail_size
                    )
            }
            if ( search appearsIn R.string.thumbnailtype )
                SettingComponents.EnumEntry(
                    Preferences.THUMBNAIL_TYPE,
                    R.string.thumbnailtype
                )

            if ( search appearsIn R.string.thumbnail_roundness )
                SettingComponents.EnumEntry(
                    Preferences.THUMBNAIL_BORDER_RADIUS,
                    R.string.thumbnail_roundness,
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
                    }
                )
        }
    }

    if ( !showthumbnail && search appearsIn R.string.noblur )
        SettingComponents.BooleanEntry(
            Preferences.PLAYER_BACKGROUND_BLUR,
            R.string.noblur
        )

    if (!(showthumbnail && playerType == PlayerType.Essential)
        && search appearsIn R.string.statsfornerdsplayer
    )
        SettingComponents.BooleanEntry(
            Preferences.PLAYER_STATS_FOR_NERDS,
            R.string.statsfornerdsplayer
        )

    if ( search appearsIn R.string.timelinesize )
        SettingComponents.EnumEntry(
            Preferences.PLAYER_TIMELINE_SIZE,
            R.string.timelinesize
        )

    if ( search appearsIn R.string.pinfo_type ) {
        SettingComponents.EnumEntry(
            Preferences.PLAYER_INFO_TYPE,
            R.string.pinfo_type
        )
        SettingComponents.Description( R.string.pinfo_album_and_artist_name )

        AnimatedVisibility( playerInfoType == PlayerInfoType.Modern ) {
            if ( search appearsIn R.string.pinfo_show_icons )
                SettingComponents.BooleanEntry(
                    Preferences.PLAYER_SONG_INFO_ICON,
                    R.string.pinfo_show_icons,
                    Modifier.padding( start = 25.dp )
                )
        }

    }
    if ( search appearsIn R.string.miniplayertype )
        SettingComponents.EnumEntry(
            Preferences.MINI_PLAYER_TYPE,
            R.string.miniplayertype
        )
    if ( search appearsIn R.string.player_swap_controls_with_timeline )
        SettingComponents.BooleanEntry(
            Preferences.PLAYER_IS_CONTROL_AND_TIMELINE_SWAPPED,
            R.string.player_swap_controls_with_timeline
        )
    if ( search appearsIn R.string.timeline )
        SettingComponents.EnumEntry(
            Preferences.PLAYER_TIMELINE_TYPE,
            R.string.timeline
        )
    if ( search appearsIn R.string.transparentbar )
        SettingComponents.BooleanEntry(
            Preferences.TRANSPARENT_TIMELINE,
            R.string.transparentbar
        )
    if ( search appearsIn R.string.pcontrols_type )
        SettingComponents.EnumEntry(
            Preferences.PLAYER_CONTROLS_TYPE,
            R.string.pcontrols_type
        )
    if ( search appearsIn R.string.play_button )
        SettingComponents.EnumEntry(
            Preferences.PLAYER_PLAY_BUTTON_TYPE,
            R.string.play_button
        )
    if ( search appearsIn R.string.buttonzoomout )
        SettingComponents.BooleanEntry(
            Preferences.ZOOM_OUT_ANIMATION,
            R.string.buttonzoomout
        )
    if ( search appearsIn R.string.play_button )
        SettingComponents.EnumEntry(
            Preferences.LIKE_ICON,
            R.string.play_button
        )
    if ( search appearsIn R.string.background_colors )
        SettingComponents.EnumEntry(
            Preferences.PLAYER_BACKGROUND,
            R.string.background_colors
        )

    AnimatedVisibility(visible = playerBackgroundColors == PlayerBackgroundColors.AnimatedGradient) {
        if ( search appearsIn R.string.gradienttype )
            SettingComponents.EnumEntry(
                Preferences.ANIMATED_GRADIENT,
                R.string.gradienttype,
                Modifier.padding(
                    start = if ( playerBackgroundColors == PlayerBackgroundColors.AnimatedGradient )
                        25.dp
                    else
                        0.dp
                )
            )
    }
    AnimatedVisibility( playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor ) {
       Column(
           Modifier.padding( start = 25.dp )
       ) {
           if( search appearsIn R.string.rotating_cover_title )
               SettingComponents.BooleanEntry(
                   Preferences.PLAYER_ROTATING_ALBUM_COVER,
                   R.string.rotating_cover_title
               )

           if ( search appearsIn R.string.bottomgradient )
               SettingComponents.BooleanEntry(
                   Preferences.PLAYER_BOTTOM_GRADIENT,
                   R.string.bottomgradient
               )

           if ( playerType == PlayerType.Modern && search appearsIn R.string.albumCoverRotation )
               SettingComponents.BooleanEntry(
                   Preferences.PLAYER_THUMBNAIL_ROTATION,
                   R.string.albumCoverRotation
               )
       }
    }

    AnimatedVisibility(
        Preferences.PLAYER_BACKGROUND.either( PlayerBackgroundColors.CoverColorGradient, PlayerBackgroundColors.ThemeColorGradient )
    ) {
        if( search appearsIn R.string.blackgradient )
            SettingComponents.BooleanEntry(
                Preferences.BLACK_GRADIENT,
                R.string.blackgradient,
                Modifier.padding( start = 25.dp )
            )
    }

    if ( search appearsIn R.string.textoutline )
        SettingComponents.BooleanEntry(
            Preferences.TEXT_OUTLINE,
            R.string.textoutline
        )

    if ( search appearsIn R.string.show_total_time_of_queue )
        SettingComponents.BooleanEntry(
            Preferences.PLAYER_SHOW_TOTAL_QUEUE_TIME,
            R.string.show_total_time_of_queue
        )

    if ( search appearsIn R.string.show_remaining_song_time )
        SettingComponents.BooleanEntry(
            Preferences.PLAYER_SHOW_SONGS_REMAINING_TIME,
            R.string.show_remaining_song_time
        )

    if ( search appearsIn R.string.show_next_songs_in_player )
        SettingComponents.BooleanEntry(
            Preferences.PLAYER_SHOW_NEXT_IN_QUEUE,
            R.string.show_next_songs_in_player
        )
    AnimatedVisibility( visible = showNextSongsInPlayer ) {
        Column(
            Modifier.padding( start = 25.dp )
        ) {
            if( search appearsIn R.string.showtwosongs )
                SettingComponents.EnumEntry(
                    Preferences.MAX_NUMBER_OF_NEXT_IN_QUEUE,
                    R.string.songs_number_to_show
                )

            if ( search appearsIn R.string.showalbumcover )
                SettingComponents.BooleanEntry(
                    Preferences.PLAYER_SHOW_NEXT_IN_QUEUE_THUMBNAIL,
                    R.string.showalbumcover
                )
        }
    }

    if ( search appearsIn R.string.disable_scrolling_text )
        SettingComponents.BooleanEntry(
            Preferences.SCROLLING_TEXT_DISABLED,
            R.string.disable_scrolling_text,
            R.string.scrolling_text_is_used_for_long_texts
        )

    val (titleId, subtitleId) = if( playerType == PlayerType.Modern && !isLandscape )
        R.string.disable_vertical_swipe to R.string.disable_vertical_swipe_secondary
    else
        R.string.disable_horizontal_swipe to R.string.disable_song_switching_via_swipe
    if ( search appearsIn titleId )
        SettingComponents.BooleanEntry(
            Preferences.PLAYER_THUMBNAIL_HORIZONTAL_SWIPE_DISABLED,
            titleId,
            subtitleId
        )

    if ( search appearsIn R.string.player_rotating_buttons )
        SettingComponents.BooleanEntry(
            Preferences.ROTATION_EFFECT,
            R.string.player_rotating_buttons,
            R.string.player_enable_rotation_buttons
        )

    if ( search appearsIn R.string.toggle_lyrics )
        SettingComponents.BooleanEntry(
            Preferences.PLAYER_TAP_THUMBNAIL_FOR_LYRICS,
            R.string.toggle_lyrics,
            R.string.by_tapping_on_the_thumbnail
        )

    if ( search appearsIn R.string.click_lyrics_text )
        SettingComponents.BooleanEntry(
            Preferences.LYRICS_JUMP_ON_TAP,
            R.string.click_lyrics_text
        )
    if ( showlyricsthumbnail && search appearsIn R.string.show_background_in_lyrics )
        SettingComponents.BooleanEntry(
            Preferences.LYRICS_SHOW_ACCENT_BACKGROUND,
            R.string.show_background_in_lyrics
        )

    if ( search appearsIn R.string.player_enable_lyrics_popup_message )
        SettingComponents.BooleanEntry(
            Preferences.PLAYER_ACTION_LYRICS_POPUP_MESSAGE,
            R.string.player_enable_lyrics_popup_message
        )

    if ( search appearsIn R.string.background_progress_bar )
        SettingComponents.EnumEntry(
            Preferences.MINI_PLAYER_PROGRESS_BAR,
            R.string.background_progress_bar
        )

    if ( search appearsIn R.string.visualizer ) {
        SettingComponents.BooleanEntry(
            Preferences.PLAYER_VISUALIZER,
            R.string.visualizer
        )

        SettingComponents.Description( R.string.visualizer_require_mic_permission, isImportant = true )
    }
}