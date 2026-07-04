package com.ksensor.plugins.states.bluetooth

import com.ksensor.core.Permission
import com.ksensor.core.PluginId
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.StateData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * No-op BluetoothPlugin for the web (Wasm) target. KSensor has no web (Wasm) sensor
 * backend yet, so observations emit nothing and state values are placeholders.
 */
class WasmBluetoothPlugin : BluetoothPlugin {
    override val id: PluginId = PluginId.BLUETOOTH
    override val requiredPermissions: List<Permission> = emptyList()

    override fun connections(): StatePlugin<StateData.BleConnectionStatus> = noopState(StateData.BleConnectionStatus(emptyList()))
    override fun discoveries(): StatePlugin<StateData.BleDiscoversStatus> = noopState(StateData.BleDiscoversStatus(emptyList()))
}

private fun <T> noopState(value: T): StatePlugin<T> = object : StatePlugin<T> {
    override val id: PluginId = PluginId.BLUETOOTH
    override val requiredPermissions: List<Permission> = emptyList()
    override val currentState: KSensorResponse<T> = KSensorResponse(value)
    override fun observe(): Flow<KSensorResponse<T>> = emptyFlow()
}

actual fun createBluetoothPlugin(): BluetoothPlugin = WasmBluetoothPlugin()
