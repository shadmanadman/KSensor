package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable

object KSensor : SensorController {
    private val sensorHandler: SensorHandler = SensorHandler()

    override fun registerSensors(
        types: List<SensorType>,
        onSensorData: (SensorType, SensorData) -> Unit,
        onSensorError: (Exception) -> Unit
    ) = sensorHandler.registerSensors(types, onSensorData, onSensorError)

    override fun unregisterSensors(types: List<SensorType>) = sensorHandler.unregisterSensors(types)

    @Composable
    override fun HandelPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    ) = sensorHandler.HandelPermissions(permission, onPermissionStatus)
}