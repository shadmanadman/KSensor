package com.ksensor.plugins.sensors.health

import com.ksensor.core.Permission
import com.ksensor.core.model.PluginId
import com.ksensor.core.SensorConfig
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class IosHealthPlugin : HealthPlugin {
    override val id: PluginId = PluginId.HEALTH
    override val requiredPermissions: List<Permission> = listOf(Permission.BODY_SENSORS)

    override fun heartRate(config: SensorConfig): Flow<KSensorResponse<SensorData.HeartRate>> {
        // Implementation using HealthKit would go here
        return emptyFlow()
    }
}

actual fun createHealthPlugin(): HealthPlugin = IosHealthPlugin()
