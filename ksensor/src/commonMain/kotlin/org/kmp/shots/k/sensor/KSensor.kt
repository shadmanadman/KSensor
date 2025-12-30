package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

object KSensor : SensorController {
    private val sensorHandler: SensorHandler = SensorHandler()

    override fun registerSensors(
        types: List<SensorType>,
        locationIntervalMillis: SensorTimeInterval
    ) = sensorHandler.registerSensors(types,locationIntervalMillis)

    override fun unregisterSensors(types: List<SensorType>) = sensorHandler.unregisterSensors(types)

    @Composable
    override fun HandelPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    ) = sensorHandler.HandelPermissions(permission, onPermissionStatus)
}