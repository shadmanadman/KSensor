package org.kmp.ksensor.sensor

import androidx.compose.runtime.Composable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.kmp.ksensor.permission.PermissionStatus
import org.kmp.ksensor.permission.PermissionType
import org.kmp.ksensor.permission.createPermissionHandler

const val DEFAULT_INTERVAL_MILLIS = 1000L
typealias SensorTimeInterval = Long

sealed class SensorUpdate {
    data class Data(val type: SensorType, val data: SensorData,val platformType: PlatformType) : SensorUpdate()
    data class Error(val exception: Exception) : SensorUpdate()
}

interface SensorController {

    fun registerSensors(
        types: List<SensorType>,
        locationIntervalMillis: SensorTimeInterval = DEFAULT_INTERVAL_MILLIS
    ): Flow<SensorUpdate>

    fun unregisterSensors(types: List<SensorType>)

    @Composable
    fun AskPermission(permissionType: PermissionType, permissionStatus: (PermissionStatus)-> Unit)
    @Composable
    fun OpenSettingsForPermission()
}

expect fun createController() : SensorController

internal class FakeSensorController : SensorController {
    private val permissionHandler = createPermissionHandler()

    val registeredSensors = mutableListOf<SensorType>()

    override fun registerSensors(
        types: List<SensorType>,
        locationIntervalMillis: SensorTimeInterval
    ): Flow<SensorUpdate> = callbackFlow {
        registeredSensors.addAll(types)
        awaitClose { unregisterSensors(types)}
    }

    override fun unregisterSensors(types: List<SensorType>) {
        types.forEach { registeredSensors.remove(it) }
    }

    @Composable
    override fun AskPermission(
        permissionType: PermissionType,
        permissionStatus: (PermissionStatus) -> Unit
    ) {
        permissionHandler.AskPermission(permissionType,permissionStatus)
    }

    @Composable
    override fun OpenSettingsForPermission() {
        permissionHandler.OpenSettingsForPermission()
    }
}