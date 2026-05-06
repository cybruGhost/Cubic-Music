package app.kreate.android.me.knighthat.utils

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import es.dmoral.toasty.Toasty
import app.kreate.android.R
import app.it.fast4x.rimusic.appContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.MagicConstant

/**
 * This singleton handles displaying **toast** to users.
 *
 * It sits on top of [Toasty] (which already an abstraction
 * of [Toast.makeText]).
 *
 * Additional features include:
 * - Ensure toasts are displayed on **_main_** thread
 * - Customizable background and foreground colors
 *
 * Future plans:
 * - Handles console outputs for debugging
 */
object Toaster {

    enum class Type(
        @ColorInt val background: Int,
        @ColorInt val foreground: Int,
        @DrawableRes private val iconId: Int
    ) {
        NORMAL(
            Color.rgb( 54, 58, 66 ),
            Color.WHITE,
            -1
        ),
        SUCCESS(
            Color.rgb( 32, 122, 86 ),
            Color.WHITE,
            R.drawable.checkmark
        ),
        INFO(
            Color.rgb( 61, 66, 76 ),
            Color.WHITE,
            R.drawable.information
        ),
        WARNING(
            Color.rgb( 181, 116, 34 ),
            Color.WHITE,
            R.drawable.alert
        ),
        ERROR(
            Color.rgb( 166, 52, 67 ),
            Color.WHITE,
            R.drawable.close
        );

        val icon: Drawable by lazy {
            when( this ) {
                NORMAL -> ColorDrawable(Color.TRANSPARENT)
                else -> AppCompatResources.getDrawable( appContext(), iconId )!!
            }
        }
    }

    @MainThread
    fun toast(
        message: String,
        type: Type = Type.NORMAL,
        @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT,
        icon: Drawable = type.icon,
        background: Int,
        foreground: Int
    ) {
        CoroutineScope( Dispatchers.Main ).launch {
            Toasty.custom(
                appContext(), message, icon, background, foreground, duration, icon != Type.NORMAL.icon, true
            ).apply {
                setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 116)
            }.show()
        }
    }

    @MainThread
    fun toast(
        message: String,
        type: Type = Type.NORMAL,
        @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT,
        icon: Drawable = type.icon
    ) = this.toast( message, type, duration, icon, type.background, type.foreground )

    @MainThread
    fun toast(
        @StringRes messageId: Int,
        type: Type = Type.NORMAL,
        @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT,
        icon: Drawable = type.icon,
        vararg formatArgs: Any?
    ) = this.toast( appContext().getString( messageId, *formatArgs ), type, duration, icon )

    @MainThread
    fun toast(
        @StringRes messageId: Int,
        @DrawableRes iconId: Int,
        type: Type = Type.NORMAL,
        @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT
    ) = this.toast(
        message = appContext().getString( messageId ),
        type = type,
        duration = duration,
        icon = AppCompatResources.getDrawable( appContext(), iconId )!!
    )

    fun n( message: String, @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT ) =
        this.toast( message, Type.NORMAL, duration )

    fun n(
        @StringRes messageId: Int,
        vararg formatArgs: Any?,
        @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT
    ) = this.toast( messageId, Type.NORMAL, duration, formatArgs = formatArgs )

    fun s( message: String, @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT ) =
        this.toast( message, Type.SUCCESS, duration )

    fun s( @StringRes messageId: Int, @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT ) =
        this.toast( messageId, Type.SUCCESS, duration )

    fun s(
        @StringRes messageId: Int,
        vararg formatArgs: Any?,
        @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT
    ) = this.toast( messageId, Type.SUCCESS, duration, formatArgs = formatArgs )

    fun i( message: String, @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT ) =
        this.toast( message, Type.INFO, duration )

    fun i( @StringRes messageId: Int, @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT ) =
        this.toast( messageId, Type.INFO, duration )

    fun i(
        @StringRes messageId: Int,
        vararg formatArgs: Any?,
        @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT
    ) = this.toast( messageId, Type.INFO, duration, formatArgs = formatArgs )

    fun w( message: String, @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT ) =
        this.toast( message, Type.WARNING, duration )

    fun w( @StringRes messageId: Int, @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT ) =
        this.toast( messageId, Type.WARNING, duration )

    fun w(
        @StringRes messageId: Int,
        vararg formatArgs: Any?,
        @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT
    ) = this.toast( messageId, Type.WARNING, duration, formatArgs = formatArgs )

    fun e( message: String, @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT ) =
        this.toast( message, Type.ERROR, duration )

    fun e( @StringRes messageId: Int, @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT ) =
        this.toast( messageId, Type.ERROR, duration )

    fun e(
        @StringRes messageId: Int,
        vararg formatArgs: Any?,
        @MagicConstant(valuesFromClass = Toast::class) duration: Int = Toast.LENGTH_SHORT
    ) = this.toast( messageId, Type.ERROR, duration, formatArgs = formatArgs )

    /*

        PRE-DEFINED TOASTS

     */

    fun done() = this.s( R.string.done )

    fun noInternet() = this.e( R.string.no_connection )
}
