package app.kreate.android.themed.common.screens.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.themed.common.component.settings.SettingComponents
import app.kreate.android.themed.common.component.settings.SettingEntrySearch
import app.kreate.android.themed.common.component.settings.SettingHeader
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.ColorPaletteMode
import it.fast4x.rimusic.enums.ColorPaletteName
import it.fast4x.rimusic.ui.components.themed.ConfirmationDialog
import it.fast4x.rimusic.ui.styling.DefaultDarkColorPalette
import it.fast4x.rimusic.ui.styling.DefaultLightColorPalette

@Composable
fun ThemeSettings( search: SettingEntrySearch ) {
    var colorPaletteName by Preferences.COLOR_PALETTE
    var colorPaletteMode by Preferences.THEME_MODE
    var customThemeLight_Background0 by Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_0_HASH_CODE
    var customThemeLight_Background1 by Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_1_HASH_CODE
    var customThemeLight_Background2 by Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_2_HASH_CODE
    var customThemeLight_Background3 by Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_3_HASH_CODE
    var customThemeLight_Background4 by Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_4_HASH_CODE
    var customThemeLight_Text by Preferences.CUSTOM_LIGHT_TEXT_HASH_CODE
    var customThemeLight_TextSecondary by Preferences.CUSTOM_LIGHT_TEXT_SECONDARY_HASH_CODE
    var customThemeLight_TextDisabled by Preferences.CUSTOM_LIGHT_TEXT_DISABLED_HASH_CODE
    var customThemeLight_IconButtonPlayer by Preferences.CUSTOM_LIGHT_PLAY_BUTTON_HASH_CODE
    var customThemeLight_Accent by Preferences.CUSTOM_LIGHT_ACCENT_HASH_CODE

    var customThemeDark_Background0 by Preferences.CUSTOM_DARK_THEME_BACKGROUND_0_HASH_CODE
    var customThemeDark_Background1 by Preferences.CUSTOM_DARK_THEME_BACKGROUND_1_HASH_CODE
    var customThemeDark_Background2 by Preferences.CUSTOM_DARK_THEME_BACKGROUND_2_HASH_CODE
    var customThemeDark_Background3 by Preferences.CUSTOM_DARK_THEME_BACKGROUND_3_HASH_CODE
    var customThemeDark_Background4 by Preferences.CUSTOM_DARK_THEME_BACKGROUND_4_HASH_CODE
    var customThemeDark_Text by Preferences.CUSTOM_DARK_TEXT_HASH_CODE
    var customThemeDark_TextSecondary by Preferences.CUSTOM_DARK_TEXT_SECONDARY_HASH_CODE
    var customThemeDark_TextDisabled by Preferences.CUSTOM_DARK_TEXT_DISABLED_HASH_CODE
    var customThemeDark_IconButtonPlayer by Preferences.CUSTOM_DARK_PLAY_BUTTON_HASH_CODE
    var customThemeDark_Accent by Preferences.CUSTOM_DARK_ACCENT_HASH_CODE

    var resetCustomLightThemeDialog by rememberSaveable { mutableStateOf(false) }
    var resetCustomDarkThemeDialog by rememberSaveable { mutableStateOf(false) }

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

    if( search appearsIn R.string.theme  )
        SettingComponents.EnumEntry(
            Preferences.COLOR_PALETTE,
            R.string.theme
        ) {
            if( colorPaletteName == ColorPaletteName.ModernBlack )
                colorPaletteMode = ColorPaletteMode.System
        }

    AnimatedVisibility(visible = colorPaletteName == ColorPaletteName.CustomColor) {
        SettingComponents.ColorPicker(
            preference = Preferences.CUSTOM_COLOR_HASH_CODE,
            titleId = R.string.customcolor,
            modifier = Modifier.padding( start = 25.dp ),
            subtitle = stringResource( R.string.restarting_rimusic_is_required )
        )
    }
    AnimatedVisibility(visible = colorPaletteName == ColorPaletteName.Customized) {
        Column {
            SettingHeader( R.string.title_customized_light_theme_colors )

            if( search appearsIn R.string.title_reset_customized_light_colors  )
                SettingComponents.Text(
                    title = stringResource( R.string.title_reset_customized_light_colors ),
                    subtitle = stringResource( R.string.info_click_to_reset_default_light_colors ),
                    onClick = { resetCustomLightThemeDialog = true },
                ) {
                    Icon(
                        painter = painterResource( R.drawable.trash ),
                        contentDescription = stringResource( R.string.settings_reset ),
                        tint = colorPalette().text,
                    )
                }
            if( search appearsIn R.string.color_background_1  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_0_HASH_CODE,
                    title = stringResource( R.string.color_background_1 )
                )
            if( search appearsIn R.string.color_background_2  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_1_HASH_CODE,
                    titleId = R.string.color_background_2
                )
            if( search appearsIn R.string.color_background_3  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_2_HASH_CODE,
                    titleId = R.string.color_background_3
                )
            if( search appearsIn R.string.color_background_4  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_3_HASH_CODE,
                    titleId = R.string.color_background_4
                )
            if( search appearsIn R.string.color_background_5  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_4_HASH_CODE,
                    titleId = R.string.color_background_5
                )
            if( search appearsIn R.string.color_text  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_LIGHT_TEXT_HASH_CODE,
                    titleId = R.string.color_text
                )
            if( search appearsIn R.string.color_text_secondary  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_LIGHT_TEXT_SECONDARY_HASH_CODE,
                    titleId = R.string.color_text_secondary
                )
            if( search appearsIn R.string.color_text_disabled  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_LIGHT_TEXT_DISABLED_HASH_CODE,
                    titleId = R.string.color_text_disabled
                )
            if( search appearsIn R.string.color_icon_button_player  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_LIGHT_PLAY_BUTTON_HASH_CODE,
                    titleId = R.string.color_icon_button_player
                )
            if( search appearsIn R.string.color_accent  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_LIGHT_ACCENT_HASH_CODE,
                    titleId = R.string.color_accent
                )

            SettingHeader( R.string.title_customized_dark_theme_colors )

            if( search appearsIn R.string.title_reset_customized_dark_colors  )
                SettingComponents.Text(
                    title = stringResource( R.string.title_reset_customized_dark_colors ),
                    subtitle = stringResource( R.string.click_to_reset_default_dark_colors ),
                    onClick = { resetCustomDarkThemeDialog = true },
                ) {
                    Icon(
                        painter = painterResource( R.drawable.trash ),
                        contentDescription = stringResource( R.string.settings_reset ),
                        tint = colorPalette().text,
                    )
                }
            if( search appearsIn R.string.color_background_1  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_DARK_THEME_BACKGROUND_0_HASH_CODE,
                    titleId = R.string.color_background_1
                )
            if( search appearsIn R.string.color_background_2  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_DARK_THEME_BACKGROUND_1_HASH_CODE,
                    titleId = R.string.color_background_2
                )
            if( search appearsIn R.string.color_background_3  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_DARK_THEME_BACKGROUND_2_HASH_CODE,
                    titleId = R.string.color_background_3
                )
            if( search appearsIn R.string.color_background_4  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_DARK_THEME_BACKGROUND_3_HASH_CODE,
                    titleId = R.string.color_background_4
                )
            if( search appearsIn R.string.color_background_5  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_DARK_THEME_BACKGROUND_4_HASH_CODE,
                    titleId = R.string.color_background_5
                )
            if( search appearsIn R.string.color_text  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_DARK_TEXT_HASH_CODE,
                    titleId = R.string.color_text
                )
            if( search appearsIn R.string.color_text_secondary  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_DARK_TEXT_SECONDARY_HASH_CODE,
                    titleId = R.string.color_text_secondary
                )
            if( search appearsIn R.string.color_text_disabled  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_DARK_TEXT_DISABLED_HASH_CODE,
                    titleId = R.string.color_text_disabled
                )
            if( search appearsIn R.string.color_icon_button_player  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_DARK_PLAY_BUTTON_HASH_CODE,
                    titleId = R.string.color_icon_button_player
                )
            if( search appearsIn R.string.color_accent  )
                SettingComponents.ColorPicker(
                    preference = Preferences.CUSTOM_DARK_ACCENT_HASH_CODE,
                    titleId = R.string.color_accent
                )
        }
    }
}