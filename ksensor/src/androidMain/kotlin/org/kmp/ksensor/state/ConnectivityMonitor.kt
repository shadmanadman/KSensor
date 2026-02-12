package org.kmp.ksensor.state

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi

class ConnectivityMonitor(
    private val onStatusChanged: (Boolean) -> Unit,
    private val onActiveNetworkChanged: (StateData.CurrentActiveNetwork.ActiveNetwork)-> Unit
) : ConnectivityManager.NetworkCallback() {

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        onStatusChanged(true)
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        onStatusChanged(false)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        val isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        if (isWifi) {
            onActiveNetworkChanged(StateData.CurrentActiveNetwork.ActiveNetwork.WIFI)
            println("Active Network is WIFI")
        } else if (isCellular) {
            onActiveNetworkChanged(StateData.CurrentActiveNetwork.ActiveNetwork.CELLULAR)
            println("Active Network is Cellular")
        }
    }
}