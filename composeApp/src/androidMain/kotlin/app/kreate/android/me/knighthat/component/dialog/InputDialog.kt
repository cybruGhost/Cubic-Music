package app.kreate.android.me.knighthat.component.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.it.fast4x.rimusic.colorPalette

interface InputDialog: InteractiveDialog {

    companion object {
        @Composable
        @JvmStatic
        fun defaultTextFieldColors(): TextFieldColors =
            TextFieldDefaults.colors(
                focusedTextColor = colorPalette().text,
                unfocusedTextColor = colorPalette().textDisabled,
                focusedContainerColor = colorPalette().background1,
                unfocusedContainerColor = colorPalette().background0,
                focusedIndicatorColor = colorPalette().accent,
                unfocusedIndicatorColor = colorPalette().textDisabled,
            )
    }

    val keyboardOption: KeyboardOptions

    var value: TextFieldValue

    fun onValueChanged( newValue: String ): Boolean

    fun onSet( newValue: String )

    /**
     * When [value] is empty, this component
     * will be triggered to place a placeholder
     * to the text box.
     *
     * NOTE: It is not counted as valid value.
     */
    @Composable
    fun TextPlaceholder() {}

    @Composable
    fun LeadingIcon() {}

    @Composable
    fun TrailingIcon() {}

    @Composable
    override fun Buttons() = Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
                           .padding( horizontal = 5.dp )
    ) {
        InteractiveDialog.CancelButton(
            modifier = InteractiveDialog.ButtonModifier()
                                        .weight( 1f )       // Let size be flexible
                                        .fillMaxWidth( .98f )   // Creates some space between buttons
                                        .border(
                                            width = 2.dp,
                                            color = Color( android.graphics.Color.RED ).copy( alpha = .3f ),
                                            shape = RoundedCornerShape(20)
                                        )
                                        .padding( vertical = 10.dp ),
            onCancel = ::hideDialog
        )
        InteractiveDialog.ConfirmButton(
            modifier = InteractiveDialog.ButtonModifier()
                                        .weight( 1f )       // Let size be flexible
                                        .fillMaxWidth( .98f )       // Creates some space between buttons
                                        .background( colorPalette().accent )
                                        .padding( vertical = 10.dp ),
            onConfirm = { onSet( value.text ) }
        )
    }

    @Composable
    override fun DialogBody() {
        TextField(
            value = value,
            onValueChange = {
                if( onValueChanged( it.text ) )
                    value = it
            },
            placeholder = { TextPlaceholder() },
            maxLines = 1,
            keyboardOptions = keyboardOption,
            leadingIcon = { LeadingIcon() },
            trailingIcon = { TrailingIcon() },
            modifier = Modifier.padding( vertical = 20.dp )
                               .fillMaxWidth(),
            colors = defaultTextFieldColors()
        )
    }
}