package com.ksensor.plugins.states.network

import com.ksensor.core.Permission
import com.ksensor.core.model.PluginId
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.StateData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Network.*
import platform.darwin.dispatch_get_main_queue

class IosNetworkPlugin : NetworkPlugin {
    override val id: PluginId = PluginId.NETWORK
    override val requiredPermissions: List<Permission> = emptyList()

    override fun connectivity(): StatePlugin<StateData.ConnectivityStatus> {
        return object : StatePlugin<StateData.ConnectivityStatus> {
            override val id: PluginId = PluginId.NETWORK
            override val requiredPermissions: List<Permission> = emptyList()
            override val currentState: KSensorResponse<StateData.ConnectivityStatus>
                get() = KSensorResponse(StateData.ConnectivityStatus(false)) // Placeholder

            override fun observe(): Flow<KSensorResponse<StateData.ConnectivityStatus>> = callbackFlow {
                val monitor = nw_path_monitor_create()
                nw_path_monitor_set_update_handler(monitor) { path ->
                    val status = nw_path_get_status(path)
                    val sensorData = StateData.ConnectivityStatus(status == nw_path_status_satisfied)
                    trySend(KSensorResponse(sensorData))
                }
                nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
                nw_path_monitor_start(monitor)
                awaitClose { nw_path_monitor_cancel(monitor) }
            }
        }
    }

    override fun activeNetwork(): StatePlugin<StateData.CurrentActiveNetwork> {
        return object : StatePlugin<StateData.CurrentActiveNetwork> {
            override val id: PluginId = PluginId.NETWORK
            override val requiredPermissions: List<Permission> = emptyList()
            override val currentState: KSensorResponse<StateData.CurrentActiveNetwork>
                get() = KSensorResponse(StateData.CurrentActiveNetwork(StateData.CurrentActiveNetwork.ActiveNetwork.NONE))

            override fun observe(): Flow<KSensorResponse<StateData.CurrentActiveNetwork>> = callbackFlow {
                val monitor = nw_path_monitor_create()
                nw_path_monitor_set_update_handler(monitor) { path ->
                    val activeNetwork = when {
                        nw_path_uses_interface_type(path, nw_interface_type_wifi) -> StateData.CurrentActiveNetwork.ActiveNetwork.WIFI
                        nw_path_uses_interface_type(path, nw_interface_type_cellular) -> StateData.CurrentActiveNetwork.ActiveNetwork.CELLULAR
                        else -> StateData.CurrentActiveNetwork.ActiveNetwork.NONE
                    }
                    trySend(KSensorResponse(StateData.CurrentActiveNetwork(activeNetwork)))
                }
                nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
                nw_path_monitor_start(monitor)
                awaitClose { nw_path_monitor_cancel(monitor) }
            }
        }
    }
}

actual fun createNetworkPlugin(): NetworkPlugin = IosNetworkPlugin()
