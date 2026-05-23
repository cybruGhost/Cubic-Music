package app.it.fast4x.rimusic.ui.screens.find

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.exoplayer.offline.Download
import app.cubic.android.core.network.isNetworkConnected
import app.it.fast4x.rimusic.LocalPlayerServiceBinder
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.recognition.AudioRecorder
import app.it.fast4x.rimusic.recognition.RecognizedTrack
import app.it.fast4x.rimusic.recognition.ShazamRepository
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.utils.asMediaItem
import app.it.fast4x.rimusic.utils.asSong
import app.it.fast4x.rimusic.utils.forcePlay
import app.it.fast4x.rimusic.utils.getDownloadProgress
import app.it.fast4x.rimusic.utils.getDownloadState
import app.it.fast4x.rimusic.utils.isDownloadedSong
import app.it.fast4x.rimusic.utils.manageDownload
import app.kreate.android.R
import androidx.compose.foundation.layout.fillMaxSize
import coil.compose.AsyncImage
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.SearchBody
import it.fast4x.innertube.requests.searchPage
import it.fast4x.innertube.utils.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// State
// ─────────────────────────────────────────────────────────────────────────────

private sealed interface FindUiState {
    data object Idle : FindUiState
    data object Listening : FindUiState
    data class Success(val track: RecognizedTrack) : FindUiState
    data class Error(val message: String) : FindUiState
}

// ─────────────────────────────────────────────────────────────────────────────
// Root Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FindScreen(
    onDismiss: () -> Unit,
    onOpenSearch: (String) -> Unit,
    miniPlayer: @Composable () -> Unit = {}
) {
    val context       = LocalContext.current
    val binder        = LocalPlayerServiceBinder.current
    val scope         = rememberCoroutineScope()
    val recorder      = remember { AudioRecorder(scope) }
    val repository    = remember { ShazamRepository() }
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = configuration.screenHeightDp.dp * 0.88f

    val duration       by recorder.duration.collectAsState(0)
    val recordedBuffer by recorder.buffer.collectAsState(ByteArray(0))
    var state          by remember { mutableStateOf<FindUiState>(FindUiState.Idle) }
    var recognitionJob by remember { mutableStateOf<Job?>(null) }
    var lastAttemptSecond    by remember { mutableStateOf(0) }
    var isMatching           by remember { mutableStateOf(false) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }
    var suggestionsError     by remember { mutableStateOf<String?>(null) }
    val suggestions          = remember { mutableStateListOf<Innertube.SongItem>() }

    fun stopListening(resetToIdle: Boolean = true) {
        recognitionJob?.cancel(); recognitionJob = null
        recorder.stop(); isMatching = false; lastAttemptSecond = 0
        if (resetToIdle) state = FindUiState.Idle
    }

    fun startListening() {
        if (state is FindUiState.Listening) return
        if (!context.isNetworkConnected) {
            state = FindUiState.Error(context.getString(R.string.error_no_internet))
            return
        }
        suggestions.clear(); suggestionsError = null; isLoadingSuggestions = false
        state = FindUiState.Listening; isMatching = false; lastAttemptSecond = 0
        recorder.start()
        recognitionJob?.cancel()
        recognitionJob = scope.launch {
            delay(12_000L)
            if (state is FindUiState.Listening) {
                recorder.stop(); isMatching = false
                state = FindUiState.Error(context.getString(R.string.find_error_could_not_identify))
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
        else state = FindUiState.Error(context.getString(R.string.find_error_microphone_permission))
    }

    LaunchedEffect(duration, recordedBuffer, state) {
        if (state !is FindUiState.Listening) return@LaunchedEffect
        if (duration < 3 || recordedBuffer.isEmpty()) return@LaunchedEffect
        if (isMatching || duration == lastAttemptSecond || duration - lastAttemptSecond < 2) return@LaunchedEffect
        isMatching = true; lastAttemptSecond = duration
        scope.launch {
            val track = withContext(Dispatchers.IO) { repository.identify(duration, recordedBuffer) }
            if (track != null) {
                recorder.stop(); recognitionJob?.cancel(); recognitionJob = null
                isMatching = false; state = FindUiState.Success(track)
            } else if (state is FindUiState.Listening) { isMatching = false }
        }
    }

    LaunchedEffect(state) {
        val success = state as? FindUiState.Success ?: return@LaunchedEffect
        suggestions.clear(); isLoadingSuggestions = true; suggestionsError = null
        if (!context.isNetworkConnected) {
            suggestionsError = context.getString(R.string.error_no_internet)
            isLoadingSuggestions = false
            return@LaunchedEffect
        }
        val query = listOf(success.track.title, success.track.subtitle)
            .filter { it.isNotBlank() }.joinToString(" ")
        val result = withContext(Dispatchers.IO) {
            Innertube.searchPage(
                body = SearchBody(query = query, params = ""),
                fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
            )
        }
        val matchedSongs = result?.getOrNull()?.items?.distinctBy { it.key }?.take(5).orEmpty()
        suggestions.clear(); suggestions.addAll(matchedSongs)
        if (matchedSongs.isEmpty()) suggestionsError = context.getString(R.string.find_no_playable_matches)
        isLoadingSuggestions = false
    }

    DisposableEffect(Unit) { onDispose { recognitionJob?.cancel(); recorder.stop() } }

    // ── Sheet container ───────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = sheetMaxHeight)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        colorPalette().background1,
                        colorPalette().background0
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(colorPalette().textDisabled.copy(alpha = 0.4f))
        )

        Spacer(Modifier.height(2.dp))

        // Header
        FindHeader(onDismiss = onDismiss)

        // Hero card
        FindHeroCard(
            state    = state,
            duration = duration,
            onStart  = {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) startListening()
                else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onStop = { stopListening() }
        )

        // Results
        val success = state as? FindUiState.Success
        AnimatedVisibility(
            visible = success != null,
            enter   = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 },
            exit    = fadeOut(tween(200))
        ) {
            if (success != null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RecognizedTrackCard(
                        track    = success.track,
                        onSearch = {
                            val q = listOf(success.track.title, success.track.subtitle)
                                .filter { it.isNotBlank() }.joinToString(" ")
                            onOpenSearch(q)
                        }
                    )

                    SectionDivider(label = stringResource(R.string.find_cubic_matches))

                    when {
                        isLoadingSuggestions -> SuggestionsLoading()
                        suggestions.isNotEmpty() -> {
                            suggestions.forEachIndexed { index, item ->
                                val mediaItem = item.asMediaItem
                                val isDownloaded = isDownloadedSong(mediaItem.mediaId)
                                val downloadState = getDownloadState(mediaItem.mediaId)
                                val downloadProgress = getDownloadProgress(mediaItem.mediaId)
                                SuggestionCard(
                                    item = item,
                                    index = index,
                                    isDownloaded = isDownloaded,
                                    downloadState = downloadState,
                                    downloadProgress = downloadProgress,
                                    onPlay = {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlay(mediaItem)
                                    },
                                    onDownload = { manageDownload(context, mediaItem, isDownloaded) }
                                )
                            }
                        }
                        else -> {
                            Text(
                                suggestionsError ?: stringResource(R.string.find_no_suggestions_available),
                                style = typography().xs,
                                color = colorPalette().textSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = state is FindUiState.Listening) {
            Text(
                stringResource(R.string.find_microphone_active_hint),
                style = typography().xxs,
                color = colorPalette().textDisabled,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        miniPlayer()
        Spacer(Modifier.height(4.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FindHeader(onDismiss: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colorPalette().accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.equalizer),
                contentDescription = null,
                tint = colorPalette().accent,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.find_title),
                style = typography().l,
                color = colorPalette().text,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.find_subtitle),
                style = typography().xxs,
                color = colorPalette().textSecondary
            )
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(colorPalette().background2)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = stringResource(R.string.close),
                tint = colorPalette().textSecondary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FindHeroCard(
    state: FindUiState,
    duration: Int,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val isListening = state is FindUiState.Listening

    val inf = rememberInfiniteTransition(label = "pulse")
    val pulseScale by inf.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.18f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val ringAlpha by inf.animateFloat(
        initialValue  = 0.07f,
        targetValue   = 0.25f,
        animationSpec = infiniteRepeatable(
            tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        colorPalette().accent.copy(alpha = 0.13f),
                        colorPalette().background2.copy(alpha = 0.97f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        colorPalette().accent.copy(alpha = if (isListening) 0.45f else 0.12f),
                        colorPalette().background3.copy(alpha = 0.25f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Status — animated crossfade
            AnimatedContent(
                targetState  = state,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "status"
            ) { currentState ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val (title, subtitle) = when (currentState) {
                        FindUiState.Idle ->
                            stringResource(R.string.find_status_ready_title) to stringResource(R.string.find_status_ready_subtitle)
                        FindUiState.Listening ->
                            stringResource(R.string.find_status_listening_title) to stringResource(R.string.find_status_listening_subtitle, duration)
                        is FindUiState.Success ->
                            stringResource(R.string.find_status_success_title) to stringResource(R.string.find_status_success_subtitle)
                        is FindUiState.Error ->
                            stringResource(R.string.find_status_error_title) to currentState.message
                    }
                    Text(
                        title,
                        style = typography().m,
                        color = when (currentState) {
                            is FindUiState.Success -> colorPalette().accent
                            is FindUiState.Error   -> colorPalette().red
                            else                   -> colorPalette().text
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        subtitle,
                        style = typography().xs,
                        color = colorPalette().textSecondary
                    )
                }
            }

            // Circular button with blended image
            Box(contentAlignment = Alignment.Center) {

                // ── Pulsing glow rings (listening only) ──────────────────────
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size((152 * pulseScale).dp)
                            .clip(CircleShape)
                            .background(colorPalette().accent.copy(alpha = ringAlpha * 0.18f))
                    )
                    Box(
                        modifier = Modifier
                            .size((124 * pulseScale).dp)
                            .clip(CircleShape)
                            .background(colorPalette().accent.copy(alpha = ringAlpha * 0.28f))
                    )
                }

                // ── Main button container ────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    colorPalette().background0.copy(alpha = 0.0f),
                                    colorPalette().background0.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.04f),
                                    colorPalette().background0.copy(alpha = 0.6f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // ── Glass mid ring ───────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isListening) 0.14f else 0.08f),
                                    colorPalette().background2.copy(alpha = 0.55f),
                                    colorPalette().background0.copy(alpha = 0.70f)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isListening) 0.30f else 0.18f),
                                    colorPalette().accent.copy(alpha = if (isListening) 0.55f else 0.10f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // ── Inner glassy button with blended image ───────────────────
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .scale(if (isListening) pulseScale * 0.97f else 1f)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isListening) 0.22f else 0.13f),
                                    colorPalette().background2.copy(alpha = 0.40f),
                                    colorPalette().background0.copy(alpha = 0.55f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isListening) 0.55f else 0.35f),
                                    Color.White.copy(alpha = 0.03f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { if (isListening) onStop() else onStart() },
                    contentAlignment = Alignment.Center
                ) {
                    // Image fills entire button - no padding, no visible edges
                    AsyncImage(
                        model = R.drawable.findsong,
                        contentDescription = if (isListening) stringResource(R.string.stop) else stringResource(R.string.find_start_listening),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                    
                    // Glass shine overlay on top of image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.15f),
                                        Color.Transparent,
                                        Color.Transparent
                                    ),
                                    center = androidx.compose.ui.geometry.Offset(0.3f, 0.3f),
                                    radius = 0.7f
                                )
                            )
                    )
                    
                    // Top-left specular highlight
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .align(Alignment.TopStart)
                            .padding(start = 8.dp, top = 8.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            // Progress bar while listening
            if (isListening) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress    = { (duration / 12f).coerceIn(0f, 1f) },
                        modifier    = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color       = colorPalette().accent,
                        trackColor  = colorPalette().background3,
                        strokeCap   = StrokeCap.Round
                    )
                    Text(
                        stringResource(R.string.find_stop_early_hint),
                        style = typography().xxs,
                        color = colorPalette().textDisabled
                    )
                }
            }

            // Retry / next hint
            if (state is FindUiState.Error || state is FindUiState.Success) {
                Text(
                    if (state is FindUiState.Error)
                        stringResource(R.string.find_try_again_hint)
                    else
                        stringResource(R.string.find_identify_another_hint),
                    style = typography().xxs,
                    color = colorPalette().textDisabled
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Recognized Track Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecognizedTrackCard(
    track: RecognizedTrack,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colorPalette().background2)
            .border(
                1.dp,
                colorPalette().accent.copy(alpha = 0.18f),
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = track.coverUrl ?: track.backgroundUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                track.title,
                style = typography().s,
                color = colorPalette().text,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.subtitle,
                style = typography().xs,
                color = colorPalette().textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            track.genre?.takeIf { it.isNotBlank() }?.let { genre ->
                Text(genre, style = typography().xxs, color = colorPalette().accent)
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorPalette().accent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSearch
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    stringResource(R.string.find_more_details),
                    style = typography().xxs,
                    color = colorPalette().onAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section divider
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionDivider(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, colorPalette().background3)
                    )
                )
        )
        Text(
            label.uppercase(),
            style = typography().xxs,
            color = colorPalette().textDisabled,
            letterSpacing = 1.4.sp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(colorPalette().background3, Color.Transparent)
                    )
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Suggestions loading
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SuggestionsLoading() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorPalette().background2)
            .padding(18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color      = colorPalette().accent,
            trackColor = colorPalette().background3,
            modifier   = Modifier.size(20.dp),
            strokeWidth = 2.5.dp
        )
        Spacer(Modifier.width(14.dp))
        Text(
            stringResource(R.string.find_loading_matches),
            style = typography().xs,
            color = colorPalette().textSecondary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Suggestion Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SuggestionCard(
    item: Innertube.SongItem,
    index: Int,
    isDownloaded: Boolean,
    downloadState: Int,
    downloadProgress: Float,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    val song = item.asSong
    val isDownloading = downloadState == Download.STATE_DOWNLOADING ||
        downloadState == Download.STATE_QUEUED ||
        downloadState == Download.STATE_RESTARTING
    val progressValue = downloadProgress.coerceIn(0f, 1f)
    val progressLabel = when {
        isDownloaded -> stringResource(R.string.find_saved_offline)
        isDownloading && progressValue > 0f -> stringResource(R.string.find_download_percent, (progressValue * 100).toInt())
        isDownloading -> stringResource(R.string.find_preparing_download)
        else -> song.durationText?.takeIf { it.isNotBlank() } ?: stringResource(R.string.find_tap_to_download)
    }

    // staggered entrance
    val alphaTarget by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(350, delayMillis = index * 60),
        label         = "cardAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(0.985f + (alphaTarget * 0.015f))
            .clip(RoundedCornerShape(16.dp))
            .background(colorPalette().background1)
            .border(
                1.dp,
                colorPalette().background3.copy(alpha = 0.45f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onPlay)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        // Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                song.title,
                style = typography().s,
                color = colorPalette().text,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artistsText.orEmpty(),
                style = typography().xxs,
                color = colorPalette().textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                progressLabel,
                style = typography().xxs,
                color = when {
                    isDownloaded -> colorPalette().accent
                    isDownloading -> colorPalette().text
                    else -> colorPalette().textDisabled
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { if (progressValue > 0f) progressValue else 0.08f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = colorPalette().accent,
                    trackColor = colorPalette().background3,
                    strokeCap = StrokeCap.Round
                )
            }
        }

        // Play button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colorPalette().accent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPlay
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = stringResource(R.string.play),
                tint = colorPalette().onAccent,
                modifier = Modifier.size(15.dp)
            )
        }

        Spacer(Modifier.width(4.dp))

        // Download / Remove button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isDownloaded)
                        colorPalette().red.copy(alpha = 0.13f)
                    else if (isDownloading)
                        colorPalette().accent.copy(alpha = 0.16f)
                    else
                        colorPalette().background3
                )
                .border(
                    1.dp,
                    if (isDownloaded) colorPalette().red.copy(alpha = 0.45f)
                    else if (isDownloading) colorPalette().accent.copy(alpha = 0.42f)
                    else colorPalette().background3,
                    CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (!isDownloading || isDownloaded) onDownload() }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDownloading) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { if (progressValue > 0f) progressValue else 0.08f },
                        modifier = Modifier.size(18.dp),
                        color = colorPalette().accent,
                        trackColor = Color.Transparent,
                        strokeWidth = 2.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = stringResource(R.string.find_downloading),
                        tint = colorPalette().accent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else {
                Icon(
                    painter = painterResource(
                        if (isDownloaded) R.drawable.trash else R.drawable.download
                    ),
                    contentDescription = if (isDownloaded) stringResource(R.string.delete) else stringResource(R.string.download),
                    tint = if (isDownloaded) colorPalette().red else colorPalette().text,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
