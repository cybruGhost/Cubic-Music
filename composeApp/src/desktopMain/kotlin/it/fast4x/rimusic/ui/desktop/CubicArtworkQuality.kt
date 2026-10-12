package app.it.fast4x.rimusic.ui.desktop

internal fun String?.cubicHighResolutionArtwork(): String? {
    val source = this ?: return null
    if (!source.contains("googleusercontent.com") && !source.contains("ytimg.com")) return source
    return source
        .replace(Regex("=w\\d+-h\\d+[^?]*"), "=w1200-h1200-l90-rj")
        .replace(Regex("/w\\d+-h\\d+[^/]*"), "/w1200-h1200-l90-rj")
}
