package it.fast4x.rimusic.ui.screens.search

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import app.kreate.android.R
import it.fast4x.compose.persist.PersistMapCleanup
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.Skeleton
import it.fast4x.rimusic.ui.components.themed.IconButton
import it.fast4x.rimusic.ui.styling.favoritesIcon
import it.fast4x.rimusic.utils.Preference.enableVoiceInputKey
import it.fast4x.rimusic.utils.StartVoiceInput
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.secondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun SearchScreen(
    navController: NavController,
    miniPlayer: @Composable () -> Unit = {},
    initialTextInput: String,
    onSearch: (String) -> Unit,
    onViewPlaylist: (String) -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val (tabIndex, onTabChanged) = rememberSaveable {
        mutableStateOf(0)
    }

    val (textFieldValue, onTextFieldValueChanged) = rememberSaveable(
        initialTextInput,
        stateSaver = TextFieldValue.Saver
    ) {
        mutableStateOf(
            TextFieldValue(
                text = initialTextInput,
                selection = TextRange(initialTextInput.length)
            )
        )
    }

    PersistMapCleanup(tagPrefix = "search/")

    // Voice input preferences and state
    val isEnabledVoiceInput by rememberPreference(enableVoiceInputKey.key, enableVoiceInputKey.default)
    var startVoiceInput by remember { mutableStateOf(false) }
    
    // Track if the last input came from voice (to trigger auto-search only for voice)
    var isFromVoice by remember { mutableStateOf(false) }
    
    // Track if user has manually edited the text after voice input
    var userEditedAfterVoice by remember { mutableStateOf(false) }
    
    // Button animation
    var isMicPressed by remember { mutableStateOf(false) }
    val micScale by animateFloatAsState(
        targetValue = if (isMicPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 100)
    )

    // Auto-search ONLY for voice input, not for manual typing
    LaunchedEffect(textFieldValue.text) {
        if (textFieldValue.text.isNotEmpty() && isFromVoice && !userEditedAfterVoice) {
            delay(500) // Small delay to ensure user sees the text
            onSearch(textFieldValue.text)
            isFromVoice = false // Reset after search
        }
    }

    // Voice input handler
    if (startVoiceInput) {
        StartVoiceInput(
            onTextRecognized = { recognizedText ->
                onTextFieldValueChanged(TextFieldValue(recognizedText))
                isFromVoice = true
                userEditedAfterVoice = false
                startVoiceInput = false
            },
            onRecognitionError = {
                startVoiceInput = false
            },
            onListening = {}
        )
    }

    // Track user edits
    val onTextChange = { newValue: TextFieldValue ->
        onTextFieldValueChanged(newValue)
        if (isFromVoice && newValue.text != textFieldValue.text) {
            userEditedAfterVoice = true
        }
    }

    // Handle keyboard search action
    val onKeyboardSearch = {
        if (textFieldValue.text.isNotEmpty()) {
            onSearch(textFieldValue.text)
        }
    }

    // 📱 DEVICE-AWARE DIMENSIONS
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    // Calculate responsive values based on screen width
    val isTablet = screenWidth > 600.dp
    val isLargePhone = screenWidth > 380.dp && screenWidth <= 600.dp
    val isSmallPhone = screenWidth <= 380.dp
    
    // Responsive padding - outer
    val horizontalOuterPadding = when {
        isTablet -> 48.dp      // More padding on tablets
        isLargePhone -> 24.dp   // Medium padding on large phones
        else -> 16.dp           // Standard on small phones
    }
    
    val verticalOuterPadding = when {
        isTablet -> 12.dp       // Slightly taller on tablets
        else -> 8.dp            // Standard on phones
    }
    
    // Responsive corner radius
    val cornerRadius = when {
        isTablet -> 32.dp       // More rounded on tablets
        else -> 28.dp           // Still rounded on phones
    }
    
    // Responsive internal padding
    val horizontalInnerPadding = when {
        isTablet -> 24.dp
        else -> 16.dp
    }
    
    val verticalInnerPadding = when {
        isTablet -> 14.dp
        else -> 10.dp           // Slightly taller than before for better touch targets
    }
    
    // Responsive icon sizes
    val iconSize = when {
        isTablet -> 24.dp
        else -> 22.dp
    }
    
    val iconContainerSize = when {
        isTablet -> 32.dp
        else -> 28.dp
    }
    
    // Responsive font size
    val fontSize = when {
        isTablet -> 18.sp
        else -> 16.sp
    }
    
    // Responsive spacing between elements
    val iconSpacing = when {
        isTablet -> 20.dp
        else -> 16.dp
    }
    
    val searchIconSpacing = when {
        isTablet -> 16.dp
        else -> 12.dp
    }

    //  RESPONSIVE SEARCH BAR MODIFIER
  val searchBarModifier = Modifier
    .fillMaxWidth(
        if (isTablet) 0.8f else 1f
    )
    .padding(
        horizontal = horizontalOuterPadding,
        vertical = verticalOuterPadding
    )
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        color = colorPalette().background1.copy(alpha = 0.85f),
        shape = RoundedCornerShape(cornerRadius)
    )


    Skeleton(
        navController,
        tabIndex,
        onTabChanged,
        miniPlayer,
        navBarContent = { item ->
            item(0, stringResource(R.string.online), R.drawable.globe)
            item(1, stringResource(R.string.library), R.drawable.library)
            item(2, stringResource(R.string.go_to_link), R.drawable.link)
        }
    ) { currentTabIndex ->
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = if (isTablet) Alignment.CenterHorizontally else Alignment.Start
        ) {
            // 📱 RESPONSIVE SEARCH BAR
            Box(
                modifier = searchBarModifier
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = horizontalInnerPadding,
                            vertical = verticalInnerPadding
                        )
                ) {
                    // Search icon - responsive size
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                        tint = colorPalette().textSecondary,
                        modifier = Modifier.size(iconSize)
                    )
                    
                    Spacer(modifier = Modifier.width(searchIconSpacing))
                    
                    // Text field - takes remaining space
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = onTextChange,
                            textStyle = typography().m.copy(
                                color = colorPalette().text,
                                fontSize = fontSize
                            ),
                            cursorBrush = SolidColor(colorPalette().accent),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    onKeyboardSearch()
                                }
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (textFieldValue.text.isEmpty()) {
                                        BasicText(
                                            text = stringResource(R.string.search),
                                            style = typography().m.copy(
                                                color = colorPalette().textSecondary,
                                                fontSize = fontSize
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Action icons - responsive spacing
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(iconSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mic button - responsive
                        if (isEnabledVoiceInput) {
                            Box(
                                modifier = Modifier
                                    .size(iconContainerSize)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isMicPressed = true
                                                startVoiceInput = true
                                                tryAwaitRelease()
                                                isMicPressed = false
                                            },
                                            onTap = {
                                                isMicPressed = true
                                                startVoiceInput = true
                                                isMicPressed = false
                                            }
                                        )
                                    }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.mic),
                                    contentDescription = null,
                                    tint = if (startVoiceInput) 
                                        colorPalette().accent 
                                    else 
                                        colorPalette().textSecondary,
                                    modifier = Modifier
                                        .size(iconSize)
                                        .scale(micScale)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                        
                        // Clear button - responsive
                        if (textFieldValue.text.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(iconContainerSize)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = {
                                                onTextFieldValueChanged(TextFieldValue(""))
                                                isFromVoice = false
                                                userEditedAfterVoice = false
                                            }
                                        )
                                    }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = null,
                                    tint = colorPalette().textSecondary,
                                    modifier = Modifier
                                        .size(iconSize)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Rest of the content
            saveableStateHolder.SaveableStateProvider(currentTabIndex) {
                when (currentTabIndex) {
                    0 -> OnlineSearch(
                        navController = navController,
                        textFieldValue = textFieldValue,
                        onTextFieldValueChanged = onTextChange,
                        onSearch = onSearch,
                        decorationBox = { innerTextField -> }
                    )
                    1 -> LocalSongSearch(
                        navController = navController,
                        textFieldValue = textFieldValue,
                        onTextFieldValueChanged = onTextChange,
                        decorationBox = { innerTextField -> },
                        onAction1 = { onTabChanged(0) },
                        onAction2 = { onTabChanged(1) },
                        onAction3 = { onTabChanged(2) },
                        onAction4 = {}
                    )
                    2 -> GoToLink(
                        navController = navController,
                        textFieldValue = textFieldValue,
                        onTextFieldValueChanged = onTextChange,
                        decorationBox = { innerTextField -> },
                        onAction1 = { onTabChanged(0) },
                        onAction2 = { onTabChanged(1) },
                        onAction3 = { onTabChanged(2) },
                        onAction4 = {}
                    )
                }
            }
        }
    }
}