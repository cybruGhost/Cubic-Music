package it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import app.kreate.android.R
import me.knighthat.enums.TextView

enum class NotificationType(
    @StringRes override val textId: Int
): TextView {

    Default( R.string.notification_type_default ),
    Advanced( R.string.notification_type_advanced );
}