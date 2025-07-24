package it.fast4x.rimusic.ui.styling

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import app.kreate.android.Preferences
import it.fast4x.rimusic.enums.ColorPaletteMode


fun customColorPalette(colorPalette: ColorPalette, context: Context, isSystemInDarkTheme: Boolean): ColorPalette {
    val colorPaletteMode by Preferences.THEME_MODE

    val customThemeLight = colorPalette.copy(
        background0 = Color(Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_0_HASH_CODE.value),
        background1 = Color(Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_1_HASH_CODE.value),
        background2 = Color(Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_2_HASH_CODE.value),
        background3 = Color(Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_3_HASH_CODE.value),
        background4 = Color(Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_4_HASH_CODE.value),
        text = Color(Preferences.CUSTOM_LIGHT_TEXT_HASH_CODE.value),
        textSecondary = Color(Preferences.CUSTOM_LIGHT_TEXT_SECONDARY_HASH_CODE.value),
        textDisabled = Color(Preferences.CUSTOM_LIGHT_TEXT_DISABLED_HASH_CODE.value),
        iconButtonPlayer = Color(Preferences.CUSTOM_LIGHT_PLAY_BUTTON_HASH_CODE.value),
        accent = Color(Preferences.CUSTOM_LIGHT_ACCENT_HASH_CODE.value)
    )

    val customThemeDark = colorPalette.copy(
        background0 = Color(Preferences.CUSTOM_DARK_THEME_BACKGROUND_0_HASH_CODE.value),
        background1 = Color(Preferences.CUSTOM_DARK_THEME_BACKGROUND_1_HASH_CODE.value),
        background2 = Color(Preferences.CUSTOM_DARK_THEME_BACKGROUND_2_HASH_CODE.value),
        background3 = Color(Preferences.CUSTOM_DARK_THEME_BACKGROUND_3_HASH_CODE.value),
        background4 = Color(Preferences.CUSTOM_DARK_THEME_BACKGROUND_4_HASH_CODE.value),
        text = Color(Preferences.CUSTOM_DARK_TEXT_HASH_CODE.value),
        textSecondary = Color(Preferences.CUSTOM_DARK_TEXT_SECONDARY_HASH_CODE.value),
        textDisabled = Color(Preferences.CUSTOM_DARK_TEXT_DISABLED_HASH_CODE.value),
        iconButtonPlayer = Color(Preferences.CUSTOM_DARK_PLAY_BUTTON_HASH_CODE.value),
        accent = Color(Preferences.CUSTOM_DARK_ACCENT_HASH_CODE.value)
    )

    return when (colorPaletteMode) {
        ColorPaletteMode.Dark, ColorPaletteMode.PitchBlack -> customThemeDark
        ColorPaletteMode.Light -> customThemeLight
        ColorPaletteMode.System -> when (isSystemInDarkTheme) {
            true -> customThemeDark
            false -> customThemeLight
        }
    }
}
