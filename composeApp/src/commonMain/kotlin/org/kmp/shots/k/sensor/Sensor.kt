package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable

enum class PlatformType {
    iOS,
    Android
}

enum class SensorType {
    ACCELEROMETER,
    GYROSCOPE,
    MAGNETOMETER,
    BAROMETER,
    STEP_COUNTER,
    LOCATION
}

sealed class SensorData() {
    data class Accelerometer(
        val x: Float,
        val y: Float,
        val z: Float,
        val platformType: PlatformType
    ) : SensorData()

    data class Gyroscope(val x: Float, val y: Float, val z: Float, val platformType: PlatformType) :
        SensorData()

    data class Magnetometer(
        val x: Float,
        val y: Float,
        val z: Float,
        val platformType: PlatformType
    ) : SensorData()

    data class Barometer(val pressure: Float, val platformType: PlatformType) : SensorData()
    data class StepCounter(val steps: Int, val platformType: PlatformType) : SensorData()
    data class Location(
        val latitude: Double? = null,
        val longitude: Double? = null,
        val altitude: Double? = null,
        val platformType: PlatformType
    ) : SensorData()
}

internal interface SensorController {
    fun registerSensors(
        types: List<SensorType>,
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
        types: List<SensorType>,
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
    private val registeredSensors = mutableListOf<SensorType>()

    override fun registerSensors(
        types: List<SensorType>,
        onSensorData: (SensorType, SensorData) -> Unit,
        onSensorError: (Exception) -> Unit
    ) {
        registeredSensors.addAll(types)
    }

    override fun unregisterSensors(types: List<SensorType>) {
        registeredSensors.removeAll(types)
    }

    @Composable
    override fun HandelPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    ) {
    }
}