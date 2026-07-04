package com.ksensor.plugins.states.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.ksensor.core.Permission
import com.ksensor.core.model.PluginId
import com.ksensor.core.StatePlugin
import com.ksensor.core.context.KSensorContext
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.StateData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn

class AndroidNetworkPlugin : NetworkPlugin {
    override val id: PluginId = PluginId.NETWORK
    override val requiredPermissions: List<Permission> = emptyList()

    private val connectivityManager by lazy {
        KSensorContext.get().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val connectivityFlow by lazy {
        callbackFlow {
            val monitor = ConnectivityMonitor(
                onStatusChanged = { trySend(KSensorResponse(StateData.ConnectivityStatus(it))) },
                onActiveNetworkChanged = {}
            )
            connectivityManager.registerDefaultNetworkCallback(monitor)
            awaitClose { connectivityManager.unregisterNetworkCallback(monitor) }
        }.shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
    }

    private val connectivityPlugin = object : StatePlugin<StateData.ConnectivityStatus> {
        override val id: PluginId = PluginId.NETWORK
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.ConnectivityStatus>
            get() = KSensorResponse(StateData.ConnectivityStatus(isConnected()))

        override fun observe(): Flow<KSensorResponse<StateData.ConnectivityStatus>> = connectivityFlow
    }

    override fun connectivity(): StatePlugin<StateData.ConnectivityStatus> = connectivityPlugin

    private val activeNetworkFlow by lazy {
        callbackFlow {
            val monitor = ConnectivityMonitor(
                onStatusChanged = {},
                onActiveNetworkChanged = { trySend(KSensorResponse(StateData.CurrentActiveNetwork(it))) }
            )
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, monitor)
            awaitClose { connectivityManager.unregisterNetworkCallback(monitor) }
        }.shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
    }

    private val activeNetworkPlugin = object : StatePlugin<StateData.CurrentActiveNetwork> {
        override val id: PluginId = PluginId.NETWORK
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.CurrentActiveNetwork>
            get() = KSensorResponse(StateData.CurrentActiveNetwork(getActiveNetworkType()))

        override fun observe(): Flow<KSensorResponse<StateData.CurrentActiveNetwork>> = activeNetworkFlow
    }

    override fun activeNetwork(): StatePlugin<StateData.CurrentActiveNetwork> = activeNetworkPlugin

    private fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getActiveNetworkType(): StateData.CurrentActiveNetwork.ActiveNetwork {
        val network = connectivityManager.activeNetwork ?: return StateData.CurrentActiveNetwork.ActiveNetwork.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return StateData.CurrentActiveNetwork.ActiveNetwork.NONE
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> StateData.CurrentActiveNetwork.ActiveNetwork.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> StateData.CurrentActiveNetwork.ActiveNetwork.CELLULAR
            else -> StateData.CurrentActiveNetwork.ActiveNetwork.NONE
        }
    }
}

actual fun createNetworkPlugin(): NetworkPlugin = AndroidNetworkPlugin()
