package app.it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import app.kreate.android.R
import app.kreate.android.me.knighthat.enums.TextView

enum class MessageType(
    @field:StringRes override val textId: Int
): TextView {

    Essential( R.string.message_type_essential ),
    Modern( R.string.message_type_modern );
}