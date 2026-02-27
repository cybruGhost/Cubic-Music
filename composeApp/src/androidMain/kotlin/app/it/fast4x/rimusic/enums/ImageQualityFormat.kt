package app.it.fast4x.rimusic.enums

import androidx.annotation.StringRes
import app.kreate.android.R
import app.kreate.android.me.knighthat.enums.TextView

enum class ImageQualityFormat(
    @field:StringRes override val textId: Int
): TextView {

    Auto( R.string.audio_quality_automatic ),

    High( R.string.audio_quality_format_high ),

    Medium( R.string.audio_quality_format_medium ),

    Low( R.string.audio_quality_format_low );
}