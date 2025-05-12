package org.kmp.shots.k.sensor

enum class SensorType {
    ACCELEROMETER,
    GYROSCOPE,
    MAGNETOMETER,
    BAROMETER,
    STEP_COUNTER,
    LOCATION
}

sealed class SensorData {
    data class Accelerometer(val x: Float, val y: Float, val z: Float) : SensorData()
    data class Gyroscope(val x: Float, val y: Float, val z: Float) : SensorData()
    data class Magnetometer(val x: Float, val y: Float, val z: Float) : SensorData()
    data class Barometer(val pressure: Float) : SensorData()
    data class StepCounter(val steps: Int) : SensorData()
    data class Location(
        val latitude: Double?=null,
        val longitude: Double?=null,
        val altitude: Double? = null,
        val onError: ((Throwable) -> Unit)? = null,
        val onSuccess: (() -> Unit)? = null
    ) : SensorData()
}
internal interface SensorManager {
    fun registerSensors(types: List<SensorType>, onSensorData: (SensorType, SensorData) -> Unit)
    fun unregisterSensors(types: List<SensorType>)
}

internal expect class SensorHandler: SensorManager {
    override fun registerSensors(
        types: List<SensorType>,
        onSensorData: (SensorType, SensorData) -> Unit
    )

    override fun unregisterSensors(types: List<SensorType>)
}