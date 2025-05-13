package org.kmp.shots.k.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat

internal actual class SensorHandler : SensorManager {
    private val context = AppContext.get()

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val activeSensorListeners = mutableMapOf<SensorType, Any>()

    actual override fun registerSensors(
        types: List<SensorType>,
        onSensorData: (SensorType, SensorData) -> Unit,
        onSensorError: (Exception) -> Unit
    ) {
        types.forEach { sensorType ->
            if (activeSensorListeners.containsKey(sensorType)) return@forEach

            when (sensorType) {
                SensorType.ACCELEROMETER -> {
                    val listener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            onSensorData(
                                sensorType,
                                SensorData.Accelerometer(
                                    event.values[0],
                                    event.values[1],
                                    event.values[2],
                                    PlatformType.Android
                                )
                            )
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                    }
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
                        sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
                        activeSensorListeners[sensorType] = listener
                    }
                }

                SensorType.GYROSCOPE -> {
                    val listener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            onSensorData(
                                sensorType,
                                SensorData.Gyroscope(
                                    event.values[0],
                                    event.values[1],
                                    event.values[2],
                                    PlatformType.Android
                                )
                            )
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                    }
                    sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also {
                        sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
                        activeSensorListeners[sensorType] = listener
                    }
                }

                SensorType.MAGNETOMETER -> {
                    val listener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            onSensorData(
                                sensorType,
                                SensorData.Magnetometer(
                                    event.values[0],
                                    event.values[1],
                                    event.values[2],
                                    PlatformType.Android
                                )
                            )
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                    }
                    sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
                        sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
                        activeSensorListeners[sensorType] = listener
                    }
                }

                SensorType.BAROMETER -> {
                    val listener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            onSensorData(
                                sensorType, SensorData.Barometer(
                                    event.values[0],
                                    PlatformType.Android
                                )
                            )
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                    }
                    sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.also {
                        sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
                        activeSensorListeners[sensorType] = listener
                    }
                }

                SensorType.STEP_COUNTER -> {
                    val listener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            onSensorData(
                                sensorType,
                                SensorData.StepCounter(
                                    event.values[0].toInt(),
                                    PlatformType.Android
                                )
                            )
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                    }
                    sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.also {
                        sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
                        activeSensorListeners[sensorType] = listener
                    }
                }

                SensorType.LOCATION -> {
                    if (!hasLocationPermission(context)) {
                        MainActivity().requestLocationPermission() { granted ->
                            if (granted) {
                                registerSensors(types, onSensorData, onSensorError)
                            }
                        }
                    }
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            onSensorData(
                                sensorType,
                                SensorData.Location(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    altitude = location.altitude,
                                    platformType = PlatformType.Android
                                )
                            )
                        }

                        override fun onStatusChanged(
                            provider: String?,
                            status: Int,
                            extras: Bundle?
                        ) {
                        }

                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }


                    @SuppressLint("MissingPermission")
                    requestLocationUpdatesSafely(
                        locationManager,
                        listener,
                        onSuccess = {
                            activeSensorListeners[sensorType] = listener
                        },
                        onError = { exception ->
                            onSensorError(exception)
                        }
                    )
                }
            }
        }
    }

    actual override fun unregisterSensors(types: List<SensorType>) {
        types.forEach { sensorType ->
            when (val listener = activeSensorListeners.remove(sensorType)) {
                is SensorEventListener -> sensorManager.unregisterListener(listener)
                is LocationListener -> locationManager.removeUpdates(listener)
            }
        }
    }
}

@RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
inline fun requestLocationUpdatesSafely(
    locationManager: LocationManager,
    listener: LocationListener,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,
            1f,
            listener
        )
        onSuccess()
    } catch (e: SecurityException) {
        onError(e)
    }
}


private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}