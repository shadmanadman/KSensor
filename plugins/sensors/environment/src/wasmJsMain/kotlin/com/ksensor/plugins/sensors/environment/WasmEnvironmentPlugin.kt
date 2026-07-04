package com.ksensor.plugins.sensors.environment

import com.ksensor.core.Permission
import com.ksensor.core.PluginId
import com.ksensor.core.SensorConfig
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * No-op EnvironmentPlugin for the web (Wasm) target. KSensor has no web (Wasm) sensor
 * backend yet, so observations emit nothing and state values are placeholders.
 */
class WasmEnvironmentPlugin : EnvironmentPlugin {
    override val id: PluginId = PluginId.ENVIRONMENT
    override val requiredPermissions: List<Permission> = emptyList()

    override fun barometer(config: SensorConfig): Flow<KSensorResponse<SensorData.Barometer>> = emptyFlow()
    override fun light(config: SensorConfig): Flow<KSensorResponse<SensorData.LightIlluminance>> = emptyFlow()
    override fun proximity(config: SensorConfig): Flow<KSensorResponse<SensorData.Proximity>> = emptyFlow()
}

actual fun createEnvironmentPlugin(): EnvironmentPlugin = WasmEnvironmentPlugin()
