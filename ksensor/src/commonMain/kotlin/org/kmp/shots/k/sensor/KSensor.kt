package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import permission.PermissionStatus
import permission.PermissionType
import sensor.SensorController
import sensor.SensorTimeInterval
import sensor.SensorType
import sensor.createController

object KSensor : SensorController {
    private val sensorController = createController()

    override fun registerSensors(
        types: List<SensorType>,
        locationIntervalMillis: SensorTimeInterval
    ) = sensorController.registerSensors(types, locationIntervalMillis)

    override fun unregisterSensors(types: List<SensorType>) =
        sensorController.unregisterSensors(types)

    @Composable
    override fun AskPermission(
        permissionType: PermissionType,
        permissionStatus: (PermissionStatus) -> Unit
    ) = sensorController.AskPermission(permissionType, permissionStatus)


    @Composable
    override fun OpenSettingsForPermission() = sensorController.OpenSettingsForPermission()
}