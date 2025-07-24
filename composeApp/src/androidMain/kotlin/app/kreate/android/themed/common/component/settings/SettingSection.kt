package app.kreate.android.themed.common.component.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import it.fast4x.rimusic.appContext

fun LazyListScope.section(
    headerTitle: String,
    subtitle: String = "",
    key: Any? = null,
    contentType: Any? = null,
    content: @Composable LazyItemScope.() -> Unit
) {
    stickyHeader {
        SettingHeader(
            headerTitle,
            subtitle = subtitle
        )
    }
    item( key, contentType ) {
        content.invoke( this )
    }
}

fun LazyListScope.section(
    headerTitle: String,
    @StringRes subtitleId: Int,
    key: Any? = null,
    contentType: Any? = null,
    content: @Composable LazyItemScope.() -> Unit
) = section ( headerTitle, appContext().getString( subtitleId ), key, contentType, content )

fun LazyListScope.section(
    @StringRes headerTitleId: Int,
    @StringRes subtitleId: Int,
    key: Any? = null,
    contentType: Any? = null,
    content: @Composable LazyItemScope.() -> Unit
) = section (headerTitleId, appContext().getString( subtitleId ), key, contentType, content )

fun LazyListScope.section(
    @StringRes headerTitleId: Int,
    subtitle: String = "",
    key: Any? = null,
    contentType: Any? = null,
    content: @Composable LazyItemScope.() -> Unit
) = section( appContext().getString( headerTitleId ), subtitle, key, contentType, content )