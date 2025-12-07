package org.kmp.shots.k.sensor

enum class PlatformType {
    iOS,
    Android
}

enum class DeviceOrientation {
    PORTRAIT, LANDSCAPE, UNKNOWN
}

enum class SensorType {
    ACCELEROMETER,
    GYROSCOPE,
    MAGNETOMETER,
    BAROMETER,
    STEP_COUNTER,
    LOCATION,
    DEVICE_ORIENTATION,
    PROXIMITY,
    LIGHT,
    BATTERY
}

sealed class SensorData() {
    data class Accelerometer(
        val x: Float,
        val y: Float,
        val z: Float
    ) : SensorData()

    data class Gyroscope(val x: Float, val y: Float, val z: Float) :
        SensorData()

    data class Magnetometer(
        val x: Float,
        val y: Float,
        val z: Float
    ) : SensorData()

    data class Barometer(val pressure: Float) : SensorData()
    data class StepCounter(val steps: Int) : SensorData()
    data class Location(
        val latitude: Double? = null,
        val longitude: Double? = null,
        val altitude: Double? = null
    ) : SensorData()

    data class Orientation(
        val orientation: DeviceOrientation,
        val orientationInt: Int = 0
    ) : SensorData()

    data class Proximity(
        val distanceInCM: Float,
        val isNear: Boolean
    ): SensorData()

    data class LightIlluminance(
        val illuminance: Float
    ): SensorData()

    // Battery-related
    enum class ChargingState { UNKNOWN, DISCHARGING, CHARGING, FULL }
    enum class BatteryHealth { UNKNOWN, GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, UNSPECIFIED_FAILURE, COLD }

    data class BatteryStatus(
        val levelPercent: Int?,
        val chargingState: ChargingState,
        val health: BatteryHealth?,
        val temperatureC: Float?,
        val platformType: PlatformType
    ) : SensorData()
}