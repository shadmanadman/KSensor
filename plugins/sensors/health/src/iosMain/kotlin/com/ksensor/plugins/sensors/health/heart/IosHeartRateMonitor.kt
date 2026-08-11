package com.ksensor.plugins.sensors.health.heart

import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import kotlinx.coroutines.flow.Flow

interface IosHeartRateMonitor {
    fun start(): Flow<KSensorResponse<SensorData.HeartRate>>
    fun isSupported(): Boolean
}
