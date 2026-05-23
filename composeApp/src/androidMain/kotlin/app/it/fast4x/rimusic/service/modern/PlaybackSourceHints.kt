package app.it.fast4x.rimusic.service.modern

object PlaybackSourceHints {

    private const val CONTROLLER_HINT_WINDOW_MS = 2 * 60 * 1000L

    @Volatile
    private var preferredFallbackUntilMs: Long = 0L

    @Volatile
    private var controllerPackageName: String? = null

    @Volatile
    private var lastRequestedMediaIds: Set<String> = emptySet()

    fun markControllerPlaybackRequest(
        packageName: String?,
        mediaIds: List<String>
    ) {
        controllerPackageName = packageName
        lastRequestedMediaIds = mediaIds
            .flatMap { mediaId ->
                listOf(mediaId, mediaId.substringAfterLast("/", mediaId))
            }
            .filter { it.isNotBlank() }
            .toSet()

        preferredFallbackUntilMs = if (isAndroidAutoController(packageName)) {
            System.currentTimeMillis() + CONTROLLER_HINT_WINDOW_MS
        } else {
            0L
        }
    }

    fun currentControllerPackageName(): String? = controllerPackageName

    fun shouldPreferSearchFallback(videoId: String): Boolean {
        val now = System.currentTimeMillis()
        if (now > preferredFallbackUntilMs) return false
        return lastRequestedMediaIds.isEmpty() || videoId in lastRequestedMediaIds
    }

    fun isAndroidAutoController(packageName: String?): Boolean {
        val normalized = packageName?.lowercase().orEmpty()
        return normalized.contains("gearhead") ||
            normalized.contains("android.auto") ||
            normalized.contains("car")
    }
}
