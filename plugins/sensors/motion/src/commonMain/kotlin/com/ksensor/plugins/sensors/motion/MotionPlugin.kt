package com.ksensor.plugins.sensors.motion

import com.ksensor.core.KSensorPlugin
import com.ksensor.core.SensorConfig
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import kotlinx.coroutines.flow.Flow

interface MotionPlugin : KSensorPlugin {
    fun accelerometer(config: SensorConfig = SensorConfig.Default): Flow<KSensorResponse<SensorData.Accelerometer>>
    fun gyroscope(config: SensorConfig = SensorConfig.Default): Flow<KSensorResponse<SensorData.Gyroscope>>
    fun stepCounter(config: SensorConfig = SensorConfig.Default): Flow<KSensorResponse<SensorData.StepCounter>>
    fun motionDetector(config: SensorConfig = SensorConfig.Default): Flow<KSensorResponse<SensorData.MotionDetector>>
}

expect fun createMotionPlugin(): MotionPlugin
