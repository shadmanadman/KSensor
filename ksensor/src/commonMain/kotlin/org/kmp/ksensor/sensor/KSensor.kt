package org.kmp.ksensor.sensor

import androidx.compose.runtime.Composable
import org.kmp.ksensor.permission.PermissionStatus
import org.kmp.ksensor.permission.PermissionType

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