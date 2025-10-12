package org.kmp.shots.k.sensor

enum class PlatformType {
    iOS,
    Android
}

enum class DeviceOrientation {
    PORTRAIT, LANDSCAPE, UNKNOWN
}

enum class ScreenStatus{
    ON, OFF
}

enum class AppStatus {
    FOREGROUND, BACKGROUND
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
    SCREEN_STATE,
    APP_STATE
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

    data class Orientation(
        val orientation: DeviceOrientation,
        val orientationInt: Int = 0,
        val platformType: PlatformType
    ) : SensorData()

    data class Proximity(
        val distanceInCM: Float,
        val isNear: Boolean,
        val platformType: PlatformType
    ): SensorData()

    /**
     * On iOS, there’s no direct equivalent for screen on/off events. Using private api will cause issue with app store
     * privacy.
     */
    data class ScreenState(
        val screenStatus: ScreenStatus,
        val platformType: PlatformType
    ): SensorData()

    data class AppState(
        val appStatus: AppStatus,
        val platformType: PlatformType
    ): SensorData()

    data class LightIlluminance(
        val illuminance: Float,
        val platformType: PlatformType
    ): SensorData()
}