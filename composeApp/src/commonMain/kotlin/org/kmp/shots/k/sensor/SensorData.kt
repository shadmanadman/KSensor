package org.kmp.shots.k.sensor

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