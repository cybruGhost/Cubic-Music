package it.fast4x.innertube.utils

object LocalePreferences {
    var preference: LocalePreferenceItem? = null
}

data class LocalePreferenceItem(
    var hl: String,
    var gl: String
)