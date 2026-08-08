package com.ksensor.plugins.sensors.health

import com.ksensor.core.Permission
import com.ksensor.core.model.PluginId
import com.ksensor.core.SensorConfig
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class WasmJsHealthPlugin : HealthPlugin {
    override val id: PluginId = PluginId.HEALTH
    override val requiredPermissions: List<Permission> = emptyList()

    override fun heartRate(config: SensorConfig): Flow<KSensorResponse<SensorData.HeartRate>> = emptyFlow()
}

actual fun createHealthPlugin(): HealthPlugin = WasmJsHealthPlugin()
