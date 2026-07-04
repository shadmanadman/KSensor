package com.ksensor.plugins.states.network

import com.ksensor.core.Permission
import com.ksensor.core.PluginId
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.StateData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * No-op NetworkPlugin for the web (Wasm) target. KSensor has no web (Wasm) sensor
 * backend yet, so observations emit nothing and state values are placeholders.
 */
class WasmNetworkPlugin : NetworkPlugin {
    override val id: PluginId = PluginId.NETWORK
    override val requiredPermissions: List<Permission> = emptyList()

    override fun connectivity(): StatePlugin<StateData.ConnectivityStatus> = noopState(StateData.ConnectivityStatus(false))
    override fun activeNetwork(): StatePlugin<StateData.CurrentActiveNetwork> = noopState(StateData.CurrentActiveNetwork(StateData.CurrentActiveNetwork.ActiveNetwork.NONE))
}

private fun <T> noopState(value: T): StatePlugin<T> = object : StatePlugin<T> {
    override val id: PluginId = PluginId.NETWORK
    override val requiredPermissions: List<Permission> = emptyList()
    override val currentState: KSensorResponse<T> = KSensorResponse(value)
    override fun observe(): Flow<KSensorResponse<T>> = emptyFlow()
}

actual fun createNetworkPlugin(): NetworkPlugin = WasmNetworkPlugin()
