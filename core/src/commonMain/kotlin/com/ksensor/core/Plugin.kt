package com.ksensor.core

import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.PluginId
import kotlinx.coroutines.flow.Flow

/**
 * Base interface for all KSensor plugins.
 */
interface KSensorPlugin {
    val id: PluginId
    val requiredPermissions: List<Permission>
}

/**
 * Interface for sensors that provide a stream of data.
 */
interface SensorPlugin<T> : KSensorPlugin {
    fun observe(config: SensorConfig = SensorConfig.Default): Flow<KSensorResponse<T>>
}

/**
 * Interface for state providers that have a current value and a stream of changes.
 */
interface StatePlugin<T> : KSensorPlugin {
    val currentState: KSensorResponse<T>
    fun observe(): Flow<KSensorResponse<T>>
}
