package org.dailyislam.android.utilities

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.annotation.RequiresApi
import java.net.InetAddress

class ConnectivityUtilSdk29(private val applicationContext: Context) {
    private val connectivityManager: ConnectivityManager
        get() = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @RequiresApi(android.os.Build.VERSION_CODES.M)
    fun getNetwork(): Network? = connectivityManager.activeNetwork

    private fun getCapabilities(): NetworkCapabilities? {
        val network = connectivityManager.activeNetwork ?: return null
        return connectivityManager.getNetworkCapabilities(network)
    }

    fun isConnected(): Boolean =
        getCapabilities()?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    @RequiresApi(android.os.Build.VERSION_CODES.M)
    fun isConnected1(): Boolean = getNetwork() != null

    fun isConnectedWifi(): Boolean =
        getCapabilities()?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

    fun isConnectedMobile(): Boolean =
        getCapabilities()?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

    fun isConnectedFast(): Boolean {
        val capabilities = getCapabilities() ?: return false
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        ) return true

        val downstreamKbps = capabilities.linkDownstreamBandwidthKbps
        return downstreamKbps >= 1_000
    }

    fun isConnectionFast(type: Int, subType: Int): Boolean = isConnectedFast()

    fun isInternetAvailable(): Boolean = runCatching {
        InetAddress.getByName("google.com").hostAddress?.isNotBlank() == true
    }.getOrDefault(false)
}

@RequiresApi(android.os.Build.VERSION_CODES.N)
fun getNetwork(context: Context): String {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val nw = connectivityManager.activeNetwork ?: return "-"
    val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return "-"
    return when {
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
        else -> "?"
    }
}
