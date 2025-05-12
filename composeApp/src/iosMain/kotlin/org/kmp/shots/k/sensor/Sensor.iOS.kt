package org.kmp.shots.k.sensor

internal actual class SensorHandler: SensorManager {
    actual override fun registerSensors(types: List<SensorType>, onSensorData: (SensorType, SensorData) -> Unit){

    }

    actual override fun unregisterSensors(types: List<SensorType>){

    }
}