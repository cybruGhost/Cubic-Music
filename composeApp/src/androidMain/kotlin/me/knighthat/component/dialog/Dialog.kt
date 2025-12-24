package me.knighthat.component.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.bold
import it.fast4x.rimusic.utils.isLandscape
import it.fast4x.rimusic.utils.semiBold

interface Dialog {

    companion object {
        /**
         * Space between top and bottom line of dialog.
         */
        const val VERTICAL_PADDING = 15

        /**
         * Represents the maximum height allowed
         * for this component in portrait mode.
         *
         * Larger number may cause accessibility issue
         */
        const val MAX_WIDTH_PORTRAIT = .95f

        /**
         * Represents the maximum height allowed
         * for this component in landscape mode.
         *
         * Larger number may cause accessibility issue
         */
        const val MAX_WIDTH_LANDSCAPE = .6f

        /**
         * Represents the maximum width allowed
         * for this component in portrait mode.
         *
         * Larger number may cause accessibility issue
         */
        const val MAX_HEIGHT_PORTRAIT = .5f

        /**
         * Represents the maximum width allowed
         * for this component in landscape mode.
         *
         * Larger number may cause accessibility issue
         */
        const val MAX_HEIGHT_LANDSCAPE = .7f

        /**
         * Space between sections in dialog.
         *
         * A section is an individual part of dialog,
         * i.e. title, body, buttons
         */
        const val SPACE_BETWEEN_SECTIONS = 20
    }

    @get:Composable
    val dialogTitle: String

    /**
     * Whether the dialog should be shown or not.
     *
     * **Avoid** changing this value directly, instead,
     * use [showDialog] and/or [hideDialog] to
     * modify dialog's visibility.
     */
    var isActive: Boolean

    fun showDialog() { isActive = true }

    fun hideDialog() { isActive = false }

    @Composable
    fun DialogBody()

    @Composable
    fun Render() {
        if( !isActive ) return

        val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
        val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
        val maxWidth =
            if( isLandscape ) MAX_WIDTH_LANDSCAPE else MAX_WIDTH_PORTRAIT
        val maxHeight =
            if( isLandscape ) MAX_HEIGHT_LANDSCAPE else MAX_HEIGHT_PORTRAIT

        androidx.compose.ui.window.Dialog( ::hideDialog ) dialogComp@ {
            Card(
                modifier = Modifier.wrapContentSize()
                                   .sizeIn(
                                       maxWidth = screenWidthDp * maxWidth,
                                       maxHeight = screenHeightDp * maxHeight
                                   )
                                   .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorPalette().background1
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.padding( bottom = 8.dp )
                                .fillMaxWidth( .9f )
                    ) {
                        BasicText(
                            text = dialogTitle,
                            style = typography().l.semiBold.copy(
                                color = colorPalette().text
                            ),
                        )
                    }

                    Spacer( Modifier.height( SPACE_BETWEEN_SECTIONS.dp ) )

                    DialogBody()

                    if( this@Dialog is InteractiveDialog ) {
                        Spacer( Modifier.height( SPACE_BETWEEN_SECTIONS.dp ) )
                        Buttons()
                    }
                }
            }
        }
    }
}