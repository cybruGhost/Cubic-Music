package me.knighthat.component.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.fast4x.rimusic.colorPalette

interface ConfirmDialog: InteractiveDialog {

    fun onConfirm()

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
            onConfirm = ::onConfirm
        )
    }
}