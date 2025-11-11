package org.kmp.shots.k.sensor

import org.kmp.shots.k.sensor.StateUpdate.Data
import platform.Network.nw_interface_type_cellular
import platform.Network.nw_interface_type_wifi
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_monitor_t
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_uses_interface_type
import platform.darwin.dispatch_queue_create
import kotlin.invoke

object ConnectivityMonitor {
    private val queue = dispatch_queue_create("NetworkMonitor", null)

    fun register(
        monitor: nw_path_monitor_t,
        isConnected: (Boolean) -> Unit,
        currentActiveNetwork: (StateData.CurrentActiveNetwork.ActiveNetwork) -> Unit
    ) {
        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = nw_path_get_status(path)
            val isConnected = status == nw_path_status_satisfied
            isConnected(isConnected)

            val activeNetwork = if (nw_path_uses_interface_type(path, nw_interface_type_wifi))
                StateData.CurrentActiveNetwork.ActiveNetwork.WIFI
            else if (nw_path_uses_interface_type(path, nw_interface_type_cellular))
                StateData.CurrentActiveNetwork.ActiveNetwork.CELLULAR
            else
                StateData.CurrentActiveNetwork.ActiveNetwork.NONE

            currentActiveNetwork(activeNetwork)
        }
        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_start(monitor)
    }

    fun unregister(monitor: nw_path_monitor_t){
        nw_path_monitor_cancel(monitor)
    }
}