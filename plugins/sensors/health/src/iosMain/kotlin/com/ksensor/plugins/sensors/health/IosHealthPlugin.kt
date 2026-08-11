package com.ksensor.plugins.sensors.health

import com.ksensor.core.Permission
import com.ksensor.core.model.PluginId
import com.ksensor.core.SensorConfig
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import com.ksensor.plugins.sensors.health.heart.IosCameraHeartRateMonitor
import com.ksensor.plugins.sensors.health.heart.HealthKitHeartRateMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import platform.AVFoundation.AVMediaTypeVideo

class IosHealthPlugin : HealthPlugin {
    override val id: PluginId = PluginId.HEALTH
    override val requiredPermissions: List<Permission> = listOf(
        Permission.BODY_SENSORS,
        Permission.CAMERA
    )

    private val healthKitMonitor by lazy { HealthKitHeartRateMonitor() }
    private val cameraMonitor by lazy { IosCameraHeartRateMonitor() }

    override fun heartRate(config: SensorConfig): Flow<KSensorResponse<SensorData.HeartRate>> = flow {
        if (healthKitMonitor.isSupported()) {
            emitAll(healthKitMonitor.start())
        } else if (cameraMonitor.isSupported()) {
            // Signal that Camera PPG is active
            emit(KSensorResponse(SensorData.HeartRate(0f, SensorData.HeartRateSource.CAMERA_PPG)))
            emitAll(cameraMonitor.start())
        }
    }
}

actual fun createHealthPlugin(): HealthPlugin = IosHealthPlugin()
