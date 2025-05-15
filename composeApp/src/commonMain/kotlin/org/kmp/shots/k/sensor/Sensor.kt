package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable

const val DEFAULT_INTERVAL_MILLIS = 1000L
typealias SensorTimeInterval = Long

internal interface SensorController {
    fun registerSensors(
        sensorType: List<SensorType>,
        locationIntervalMillis: SensorTimeInterval = DEFAULT_INTERVAL_MILLIS,
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
        sensorType: List<SensorType>,
        locationIntervalMillis: SensorTimeInterval,
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
    val registeredSensors = mutableListOf<SensorType>()

    override fun registerSensors(
        sensorType: List<SensorType>,
        locationIntervalMillis: SensorTimeInterval,
        onSensorData: (SensorType, SensorData) -> Unit,
        onSensorError: (Exception) -> Unit
    ) {
        registeredSensors.addAll(sensorType)
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