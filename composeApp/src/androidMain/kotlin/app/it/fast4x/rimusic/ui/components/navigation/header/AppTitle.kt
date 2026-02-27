package app.it.fast4x.rimusic.ui.components.navigation.header

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.kreate.android.R
import app.kreate.android.drawable.APP_ICON_IMAGE_BITMAP
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.NavRoutes
import app.it.fast4x.rimusic.typography
import app.it.fast4x.rimusic.ui.components.themed.Button
import app.it.fast4x.rimusic.utils.semiBold
import app.kreate.android.me.knighthat.utils.Toaster
import androidx.compose.ui.unit.sp

private fun appIconClickAction(
    navController: NavController,
    countToReveal: MutableIntState,
    context: Context
) {
    countToReveal.intValue++

    val message: String =
        when( countToReveal.intValue ) {
            10 -> {
                countToReveal.intValue = 0
                navController.navigate( NavRoutes.gameSnake.name )
                ""
            }
            3 -> context.getString(R.string.easter_egg_click_message)
            6 -> context.getString(R.string.easter_egg_keep_going)
            9 -> context.getString(R.string.easter_egg_number_one)
            else -> ""
        }
    if( message.isNotEmpty() )
        Toaster.n( message, Toast.LENGTH_LONG )
}

private fun appIconLongClickAction(
    navController: NavController,
    context: Context
) {
    Toaster.n( context.getString(R.string.easter_egg_last), Toast.LENGTH_LONG )
    navController.navigate( NavRoutes.gamePacman.name )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppLogo(
    navController: NavController,
    context: Context
) {
    val countToReveal = remember { mutableIntStateOf(0) }
    val modifier = Modifier.combinedClickable(
        onClick = { appIconClickAction( navController, countToReveal, context ) },
        onLongClick = { appIconLongClickAction( navController, context ) }
    )

    Image(
        bitmap = APP_ICON_IMAGE_BITMAP,
        contentDescription = "App's icon",
        modifier = modifier.size( 36.dp ) 
    )
}

@Composable
private fun AppLogoText( navController: NavController ) {
    val iconTextClick: () -> Unit = {
        if ( NavRoutes.home.isNotHere( navController ) )
            navController.navigate(NavRoutes.home.name)
    }

    BasicText(
        text = "Cubic-Music", // Changed from "Cubic-Music" to just "Cubic"
        style = TextStyle(
            fontSize = 20.sp, // Slightly higher than typical medium (~16-18sp), but smaller than xl (~24sp)
            fontWeight = typography().xl.semiBold.fontWeight, // Using xl fontWeight
            fontFamily = typography().xl.semiBold.fontFamily, // Using xl fontFamily
            color = AppBar.contentColor()
        ),
        modifier = Modifier
            .clickable { iconTextClick() }
    )
}

@Composable
private fun NetworkStatusIcon() {
    val context = LocalContext.current
    var networkIcon by remember { mutableStateOf(R.drawable.explicit) }
    
    DisposableEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        updateNetworkIcon(connectivityManager) { iconRes ->
            networkIcon = iconRes
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
            
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateNetworkIcon(connectivityManager) { iconRes ->
                    networkIcon = iconRes
                }
            }
            
            override fun onLost(network: Network) {
                updateNetworkIcon(connectivityManager) { iconRes ->
                    networkIcon = iconRes
                }
            }
            
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                updateNetworkIcon(connectivityManager) { iconRes ->
                    networkIcon = iconRes
                }
            }
        }
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        onDispose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    Image(
        painter = painterResource(id = networkIcon),
        contentDescription = "Network status",
        modifier = Modifier.size(14.dp), // Reduced from 16.dp
        colorFilter = ColorFilter.tint(AppBar.contentColor())
    )
}

private fun updateNetworkIcon(
    connectivityManager: ConnectivityManager,
    onIconUpdate: (Int) -> Unit
) {
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    
    val iconRes = when {
        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> 
            R.drawable.datawifi
        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> 
            R.drawable.datamobile
        else -> R.drawable.explicit
    }
    
    onIconUpdate(iconRes)
}

@Composable
fun AppTitle(
    navController: NavController,
    context: Context
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp), // Reduced from 8.dp
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 0.dp)
    ) {
        AppLogo(navController, context)
        AppLogoText(navController)
        
        NetworkStatusIcon()

        if(Preference.parentalControl())
            Button(
                iconId = R.drawable.shield_checkmark,
                color = AppBar.contentColor(),
                padding = 0.dp,
                size = 20.dp // Reduced from 20.dp
            ).Draw()

        if (Preference.debugLog())
            BasicText(
                text = "DBG", // Ultra-minimal debug indicator
                style = TextStyle(
                    fontSize = typography().xxxs.semiBold.fontSize,
                    fontWeight = typography().xxxs.semiBold.fontWeight,
                    fontFamily = typography().xxxs.semiBold.fontFamily,
                    color = colorPalette().red
                ),
                modifier = Modifier
                    .clickable {
                        Toaster.s(R.string.info_debug_mode_is_enabled)
                        navController.navigate(NavRoutes.settings.name)
                    }
            )
    }
}
