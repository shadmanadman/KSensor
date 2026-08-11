package com.ksensor.plugins.sensors.health

import com.ksensor.core.KSensorPlugin
import com.ksensor.core.SensorConfig
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import kotlinx.coroutines.flow.Flow

/**
 * Plugin for health-related observations.
 */
interface HealthPlugin : KSensorPlugin {
    /**
     * Monitors the user's heart rate.
     */
    fun heartRate(config: SensorConfig = SensorConfig.Default): Flow<KSensorResponse<SensorData.HeartRate>>
}

expect fun createHealthPlugin(): HealthPlugin
