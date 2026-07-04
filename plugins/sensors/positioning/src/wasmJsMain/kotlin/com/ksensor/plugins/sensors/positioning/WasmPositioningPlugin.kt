package com.ksensor.plugins.sensors.positioning

import com.ksensor.core.Permission
import com.ksensor.core.PluginId
import com.ksensor.core.SensorConfig
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import com.ksensor.core.model.StateData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * No-op PositioningPlugin for the web (Wasm) target. KSensor has no web (Wasm) sensor
 * backend yet, so observations emit nothing and state values are placeholders.
 */
class WasmPositioningPlugin : PositioningPlugin {
    override val id: PluginId = PluginId.POSITIONING
    override val requiredPermissions: List<Permission> = emptyList()

    override fun location(config: SensorConfig): Flow<KSensorResponse<SensorData.Location>> = emptyFlow()
    override fun magnetometer(config: SensorConfig): Flow<KSensorResponse<SensorData.Magnetometer>> = emptyFlow()
    override fun orientation(config: SensorConfig): Flow<KSensorResponse<SensorData.Orientation>> = emptyFlow()
    override fun locationStatus(): StatePlugin<StateData.LocationStatus> = noopState(StateData.LocationStatus(false))
}

private fun <T> noopState(value: T): StatePlugin<T> = object : StatePlugin<T> {
    override val id: PluginId = PluginId.POSITIONING
    override val requiredPermissions: List<Permission> = emptyList()
    override val currentState: KSensorResponse<T> = KSensorResponse(value)
    override fun observe(): Flow<KSensorResponse<T>> = emptyFlow()
}

actual fun createPositioningPlugin(): PositioningPlugin = WasmPositioningPlugin()
