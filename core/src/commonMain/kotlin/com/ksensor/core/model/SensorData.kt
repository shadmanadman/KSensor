package com.ksensor.core.model


enum class DeviceOrientation {
    PORTRAIT, LANDSCAPE, UNKNOWN
}

enum class TouchGestureType{
    ACTION_DOWN,ACTION_UP,ACTION_MOVE
}

enum class SensorType {
    ACCELEROMETER,
    GYROSCOPE,
    MAGNETOMETER,
    BAROMETER,
    STEP_COUNTER,
    MOTION_DETECTOR,
    LOCATION,
    DEVICE_ORIENTATION,
    PROXIMITY,
    LIGHT,
    TOUCH_GESTURES
}

sealed class SensorData {
    data class Accelerometer(val values: Vector3) : SensorData()
    data class Gyroscope(val values: Vector3) : SensorData()
    data class Magnetometer(val values: Vector3) : SensorData()

    data class Barometer(val pressure: Float) : SensorData()
    data class StepCounter(val steps: Int) : SensorData()
    
    enum class MotionType {
        STATIONARY, WALKING, RUNNING, CYCLING, AUTOMOTIVE, UNKNOWN
    }
    data class MotionDetector(val type: MotionType) : SensorData()

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

    data class TouchGestures(val x: Float,val y: Float,val type: TouchGestureType): SensorData()
}
