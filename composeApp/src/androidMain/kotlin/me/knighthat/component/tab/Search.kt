package me.knighthat.component.tab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import it.fast4x.rimusic.ui.components.themed.IconButton
import it.fast4x.rimusic.ui.styling.favoritesIcon
import it.fast4x.rimusic.utils.conditional
import it.fast4x.rimusic.utils.secondary
import it.fast4x.rimusic.utils.semiBold

class Search private constructor(
    inputState: MutableState<TextFieldValue>,
    visibleState: MutableState<Boolean>,
    focusState: MutableState<Boolean>,
    private val lazyListState: LazyListState?,
    private val lazyGridState: LazyGridState?
): MenuIcon, Descriptive{

    companion object {
        @Composable
        operator fun invoke( lazyListState: LazyListState? = null ) = Search(
            remember { mutableStateOf( TextFieldValue("") ) },
            remember { mutableStateOf( false ) },
            remember { mutableStateOf( false ) },
            lazyListState,
            null
        )

        @Composable
        operator fun invoke( lazyGridState: LazyGridState? = null ) = Search(
            remember { mutableStateOf( TextFieldValue("") ) },
            remember { mutableStateOf( false ) },
            remember { mutableStateOf( false ) },
            null,
            lazyGridState
        )

        @Composable
        operator fun invoke() = Search(
            remember { mutableStateOf( TextFieldValue("") ) },
            remember { mutableStateOf( false ) },
            remember { mutableStateOf( false ) },
            null,
            null
        )
    }

    val inputValue: String
        get() = input.text
    override val iconId: Int = R.drawable.search_circle
    override val messageId: Int = R.string.search
    override val menuIconTitle: String
        @Composable
        get() = stringResource( messageId )

    /**
     * [TextFieldValue] gives control over cursor.
     *
     * This prevents the cursor from being placed
     * at the beginning of search term.
     */
    var input: TextFieldValue by inputState
    var isVisible: Boolean by visibleState
    var isFocused: Boolean by focusState

    override fun onShortClick() {
        isVisible = !isVisible
        isFocused = isVisible
    }

    @Composable
    private fun ColumnScope.DecorationBox(
        innerTextField: @Composable () -> Unit,
        onBackClick: () -> Unit
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.padding( horizontal = 10.dp )
        ) {
            IconButton(
                onClick = {},
                icon = R.drawable.search,
                color = colorPalette().favoritesIcon,
                modifier = Modifier.align( Alignment.CenterStart )
                                   .size( 16.dp )
            )
        }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f)
                               .padding( horizontal = 30.dp )
        ) {
            // Placeholder
            BasicText(
                text = stringResource(R.string.search),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = typography().xs
                                    .semiBold
                                    .secondary
                                    .copy(
                                        color = colorPalette().textDisabled
                                    ),
                modifier = Modifier.conditional( inputValue.isNotBlank() ) { alpha( 0f ) }
            )

            // Actual text from user
            innerTextField()
        }
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier.padding( start = 30.dp, end = 15.dp )
        ) {
            IconButton(
                onClick = onBackClick,
                icon = R.drawable.backspace_outline,
                color = colorPalette().text.copy( alpha = .8f ), // A little dimmer to prevent eye-candy
                modifier = Modifier.align( Alignment.CenterEnd )
                                   .size( 16.dp )
            )
        }
    }

    /**
     * Attempt to hide search bar if it's empty
     */
    fun hideIfEmpty() {
        if ( isVisible )
            if ( inputValue.isBlank() )
                isVisible = false
            else
                isFocused = false
    }

    @Composable
    fun SearchBar( columnScope: ColumnScope ) {
        // Auto focus on search bar when it's visible
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        LaunchedEffect( isVisible, isFocused ) {
            if( !isVisible ) return@LaunchedEffect

            if( isFocused )
                focusRequester.requestFocus()
            else {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        }
        LaunchedEffect( input ) {
            lazyListState?.scrollToItem( 0, 0 )
            lazyGridState?.scrollToItem( 0, 0 )
        }

                 AnimatedVisibility(
             visible = isVisible,
             modifier = Modifier.padding( horizontal = 16.dp, vertical = 16.dp )
                                .fillMaxWidth()
         ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = colorPalette().accent.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorPalette().background1
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                                 BasicTextField(
                     value = input,
                     onValueChange = {
                         input = it.copy(
                             text = it.text,
                             selection = it.selection
                         )
                     },
                     textStyle = typography().xs.semiBold,
                     singleLine = true,
                     maxLines = 1,
                     keyboardOptions = KeyboardOptions( imeAction = ImeAction.Search ),
                     keyboardActions = KeyboardActions(onSearch = {
                         isVisible = inputValue.isNotBlank()
                         isFocused = false
                         keyboardController?.hide()
                     }),
                     cursorBrush = SolidColor(colorPalette().text),
                                           decorationBox = { innerTextField ->
                          Row(
                              modifier = Modifier
                                  .fillMaxWidth()
                                  .background(
                                      color = colorPalette().background1,
                                      shape = RoundedCornerShape(8.dp)
                                  )
                                  .padding(horizontal = 20.dp, vertical = 16.dp),
                              verticalAlignment = Alignment.CenterVertically
                          ) {
                             // Leading Icon
                             Box(
                                 modifier = Modifier
                                     .size(32.dp)
                                     .background(
                                         color = colorPalette().accent.copy(alpha = 0.1f),
                                         shape = RoundedCornerShape(8.dp)
                                     ),
                                 contentAlignment = Alignment.Center
                             ) {
                                 Icon(
                                     painter = painterResource(R.drawable.search),
                                     contentDescription = stringResource(R.string.search),
                                     tint = colorPalette().accent,
                                     modifier = Modifier.size(18.dp)
                                 )
                             }
                             
                             Spacer(modifier = Modifier.width(12.dp))
                             
                                                                                         // Text Field
                               Box(
                                   modifier = Modifier
                                       .weight(1f)
                                       .background(
                                           color = colorPalette().background2,
                                           shape = RoundedCornerShape(6.dp)
                                       )
                                       .padding(horizontal = 12.dp, vertical = 8.dp),
                                   contentAlignment = Alignment.CenterStart
                               ) {
                                   if (inputValue.isEmpty()) {
                                       BasicText(
                                           text = stringResource(R.string.search),
                                           maxLines = 1,
                                           overflow = TextOverflow.Ellipsis,
                                           style = typography().xs.semiBold.secondary
                                       )
                                   }
                                   
                                   innerTextField()
                               }
                               
                               Spacer(modifier = Modifier.width(8.dp))
                               
                                                               // Trailing Icon (toujours présent pour garder la taille)
                                Box(
                                    modifier = Modifier.width(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (inputValue.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    color = colorPalette().accent.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    input = TextFieldValue("")
                                                    isFocused = true
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.backspace_outline),
                                                    contentDescription = stringResource(R.string.clear),
                                                    tint = colorPalette().accent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                         }
                     },
                     modifier = Modifier
                         .fillMaxWidth()
                         .focusRequester(focusRequester)
                 )
            }
        }
    }
}