package app.cubic.android.cast

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.mediarouter.app.MediaRouteChooserDialog
import androidx.mediarouter.app.MediaRouteChooserDialogFragment
import androidx.mediarouter.app.MediaRouteControllerDialog
import androidx.mediarouter.app.MediaRouteControllerDialogFragment
import androidx.mediarouter.app.MediaRouteDialogFactory
import androidx.mediarouter.app.MediaRouteDynamicChooserDialog
import androidx.mediarouter.app.MediaRouteDynamicControllerDialog

internal class CubicRouteDialogFactory(
    private val dark: Boolean,
    private val surface: Int,
    private val accent: Int,
) : MediaRouteDialogFactory() {
    private fun dialogArguments() = Bundle().apply {
        putBoolean("cubic_dark", dark)
        putInt("cubic_surface", surface)
        putInt("cubic_accent", accent)
    }

    override fun onCreateChooserDialogFragment() =
        CubicRouteChooserFragment().apply { arguments = dialogArguments() }

    override fun onCreateControllerDialogFragment() =
        CubicRouteControllerFragment().apply { arguments = dialogArguments() }
}

// Public, no-argument fragments allow Android to restore an open route dialog.
class CubicRouteChooserFragment : MediaRouteChooserDialogFragment() {
    override fun onCreateChooserDialog(context: Context, savedInstanceState: Bundle?) =
        MediaRouteChooserDialog(context, routeTheme())

    override fun onCreateDynamicChooserDialog(context: Context) =
        MediaRouteDynamicChooserDialog(context, routeTheme())

    override fun onStart() {
        super.onStart()
        applyRouteGlass()
    }
}

class CubicRouteControllerFragment : MediaRouteControllerDialogFragment() {
    override fun onCreateControllerDialog(context: Context, savedInstanceState: Bundle?) =
        MediaRouteControllerDialog(context, routeTheme())

    override fun onCreateDynamicControllerDialog(context: Context) =
        MediaRouteDynamicControllerDialog(context, routeTheme())

    override fun onStart() {
        super.onStart()
        applyRouteGlass()
    }
}

private fun DialogFragment.routeTheme(): Int =
    if (arguments?.getBoolean("cubic_dark", true) != false) {
        androidx.mediarouter.R.style.Theme_MediaRouter
    } else {
        androidx.mediarouter.R.style.Theme_MediaRouter_Light
    }

private fun DialogFragment.applyRouteGlass() {
    val window = dialog?.window ?: return
    val density = resources.displayMetrics.density
    val surface = arguments?.getInt("cubic_surface") ?: 0xFF202124.toInt()
    val accent = arguments?.getInt("cubic_accent") ?: 0xFFAAAAAA.toInt()
    window.setBackgroundDrawable(GradientDrawable().apply {
        setColor((surface and 0x00FFFFFF) or 0xEF000000.toInt())
        cornerRadius = 24f * density
        setStroke(density.toInt().coerceAtLeast(1), (accent and 0x00FFFFFF) or 0x50000000)
    })
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        window.setBackgroundBlurRadius((20f * density).toInt())
    }
}
