package com.ksensor.plugins.sensors.health.heart

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SensorManagerHeartRateMonitor(context: Context) : HeartRateMonitor {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override fun start(): Flow<KSensorResponse<SensorData.HeartRate>> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if (sensor == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.isNotEmpty()) {
                    val bpm = event.values[0]
                    trySend(KSensorResponse(SensorData.HeartRate(bpm, SensorData.HeartRateSource.HARDWARE_SENSOR)))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    override fun isSupported(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null
    }
}
