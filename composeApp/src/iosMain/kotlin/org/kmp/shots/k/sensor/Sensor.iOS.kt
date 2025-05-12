package org.kmp.shots.k.sensor

internal actual class SensorManager {
    actual fun registerSensors(types: List<SensorType>, onSensorData: (SensorType, SensorData) -> Unit){

    }

    actual fun unregisterSensors(types: List<SensorType>){

    }
}