package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable

const val DEFAULT_INTERVAL_MILLIS = 1000L
typealias SensorTimeInterval = Long

internal interface SensorController {
    fun registerSensors(
        sensorTypesWithIntervals: Map<SensorType, SensorTimeInterval?>,
        defaultIntervalMillis: SensorTimeInterval = DEFAULT_INTERVAL_MILLIS,
        onSensorData: (SensorType, SensorData) -> Unit,
        onSensorError: (Exception) -> Unit
    )

    fun unregisterSensors(types: List<SensorType>)

    @Composable
    fun HandelPermissions(
        permission: PermissionType = PermissionType.LOCATION,
        onPermissionStatus: (PermissionStatus) -> Unit
    )
}

internal expect class SensorHandler() : SensorController {
    override fun registerSensors(
        sensorTypesWithIntervals: Map<SensorType, SensorTimeInterval?>,
        defaultIntervalMillis: SensorTimeInterval,
        onSensorData: (SensorType, SensorData) -> Unit,
        onSensorError: (Exception) -> Unit
    )

    override fun unregisterSensors(types: List<SensorType>)

    @Composable
    override fun HandelPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    )
}


internal class FakeSensorManager : SensorController {
    private val registeredSensors = mutableMapOf<SensorType, SensorTimeInterval?>()

    override fun registerSensors(
        sensorTypesWithIntervals: Map<SensorType, SensorTimeInterval?>,
        defaultIntervalMillis: SensorTimeInterval,
        onSensorData: (SensorType, SensorData) -> Unit,
        onSensorError: (Exception) -> Unit
    ) {
        registeredSensors.putAll(sensorTypesWithIntervals)
    }

    override fun unregisterSensors(types: List<SensorType>) {
        types.forEach { registeredSensors.remove(it) }
    }

    @Composable
    override fun HandelPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    ) {
    }
}