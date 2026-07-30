package com.ksensor.plugins.sensors.positioning

import com.ksensor.core.KSensorPlugin
import com.ksensor.core.SensorConfig
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import com.ksensor.core.model.StateData
import kotlinx.coroutines.flow.Flow

interface PositioningPlugin : KSensorPlugin {
    fun location(config: SensorConfig = SensorConfig.Default): Flow<KSensorResponse<SensorData.Location>>
    fun magnetometer(config: SensorConfig = SensorConfig.Default): Flow<KSensorResponse<SensorData.Magnetometer>>
    fun orientation(config: SensorConfig = SensorConfig.Default): Flow<KSensorResponse<SensorData.Orientation>>
    fun heading(config: SensorConfig = SensorConfig.Default): Flow<KSensorResponse<SensorData.Heading>>
    fun locationStatus(): StatePlugin<StateData.LocationStatus>
}

expect fun createPositioningPlugin(): PositioningPlugin
