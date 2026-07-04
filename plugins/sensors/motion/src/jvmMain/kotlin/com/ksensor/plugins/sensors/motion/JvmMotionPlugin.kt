package com.ksensor.plugins.sensors.motion

import com.ksensor.core.Permission
import com.ksensor.core.PluginId
import com.ksensor.core.SensorConfig
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * No-op MotionPlugin for the desktop (JVM) target. KSensor has no desktop (JVM) sensor
 * backend yet, so observations emit nothing and state values are placeholders.
 */
class JvmMotionPlugin : MotionPlugin {
    override val id: PluginId = PluginId.MOTION
    override val requiredPermissions: List<Permission> = emptyList()

    override fun accelerometer(config: SensorConfig): Flow<KSensorResponse<SensorData.Accelerometer>> = emptyFlow()
    override fun gyroscope(config: SensorConfig): Flow<KSensorResponse<SensorData.Gyroscope>> = emptyFlow()
    override fun stepCounter(config: SensorConfig): Flow<KSensorResponse<SensorData.StepCounter>> = emptyFlow()
    override fun stepDetector(config: SensorConfig): Flow<KSensorResponse<SensorData.StepDetector>> = emptyFlow()
}

actual fun createMotionPlugin(): MotionPlugin = JvmMotionPlugin()
