package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow

const val DEFAULT_INTERVAL_MILLIS = 1000L
typealias SensorTimeInterval = Long

sealed class SensorUpdate {
    data class Data(val type: SensorType, val data: SensorData,val platformType: PlatformType) : SensorUpdate()
    data class Error(val exception: Exception) : SensorUpdate()
}

internal interface SensorController {

    fun registerSensors(
        types: List<SensorType>,
        locationIntervalMillis: SensorTimeInterval = DEFAULT_INTERVAL_MILLIS
    ): Flow<SensorUpdate>

    fun unregisterSensors(types: List<SensorType>)

    @Composable
    fun HandelPermissions(
        permission: PermissionType = PermissionType.LOCATION,
        onPermissionStatus: (PermissionStatus) -> Unit
    )
}

internal expect class SensorHandler() : SensorController {

    override fun registerSensors(
        types: List<SensorType>,
        locationIntervalMillis: SensorTimeInterval
    ): Flow<SensorUpdate>

    override fun unregisterSensors(types: List<SensorType>)

    @Composable
    override fun HandelPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    )
}


internal class FakeSensorHandler : SensorController {
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
    override fun HandelPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    ) {
    }
}