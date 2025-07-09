package org.kmp.shots.k.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.OrientationEventListener
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.kmp.shots.k.sensor.SensorData.*
import org.kmp.shots.k.sensor.SensorUpdate.*

internal actual class SensorHandler : SensorController {
    private val context: Context by lazy { AppContext.get() }

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val activeSensorListeners = mutableMapOf<SensorType, Any>()

    actual override fun registerSensors(
        sensorType: List<SensorType>,
        locationIntervalMillis: SensorTimeInterval
    ): Flow<SensorUpdate> = callbackFlow {

        sensorType.forEach { sensorType ->
            if (activeSensorListeners.containsKey(sensorType)) return@forEach

            when (sensorType) {
                SensorType.ACCELEROMETER -> {
                    val listener = object : SensorEventListener {

                        override fun onSensorChanged(event: SensorEvent) {
                            trySend(
                                Data(
                                    sensorType,
                                    Accelerometer(
                                        event.values[0],
                                        event.values[1],
                                        event.values[2],
                                        PlatformType.Android
                                    )
                                )
                            ).isSuccess
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                    }
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).also {
                        sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
                        activeSensorListeners[sensorType] = listener
                    } ?: println("ACCELEROMETER not available")
                }

                SensorType.GYROSCOPE -> {
                    val listener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            trySend(
                                Data(
                                    type = sensorType, data = Gyroscope(
                                        event.values[0],
                                        event.values[1],
                                        event.values[2],
                                        PlatformType.Android
                                    )
                                )
                            ).isSuccess
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                    }
                    sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE).also {
                        sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
                        activeSensorListeners[sensorType] = listener
                    } ?: println("GYROSCOPE not available")
                }

                SensorType.MAGNETOMETER -> {
                    val listener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            trySend(
                                Data(
                                    sensorType,
                                    Magnetometer(
                                        event.values[0],
                                        event.values[1],
                                        event.values[2],
                                        PlatformType.Android
                                    )
                                )
                            ).isSuccess
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                    }
                    sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD).also {
                        sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
                        activeSensorListeners[sensorType] = listener
                    } ?: println("MAGNETOMETER not available")
                }

                SensorType.BAROMETER -> {
                    val listener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            trySend(
                                Data(
                                    sensorType, Barometer(
                                        event.values[0],
                                        PlatformType.Android
                                    )
                                )
                            ).isSuccess
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                    }
                    sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE).also {
                        sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
                        activeSensorListeners[sensorType] = listener
                    } ?: println("BAROMETER not available")
                }

                SensorType.STEP_COUNTER -> {
                    val listener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            trySend(
                                Data(
                                    sensorType,
                                    StepCounter(
                                        event.values[0].toInt(),
                                        PlatformType.Android
                                    )
                                )
                            ).isSuccess
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                    }
                    sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER).also {
                        sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
                        activeSensorListeners[sensorType] = listener
                    } ?: println("Step counter not available")
                }

                SensorType.LOCATION -> {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            trySend(
                                Data(
                                    sensorType,
                                    SensorData.Location(
                                        latitude = location.latitude,
                                        longitude = location.longitude,
                                        altitude = location.altitude,
                                        platformType = PlatformType.Android
                                    )
                                )
                            ).isSuccess
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
                        locationManager = locationManager,
                        listener = listener,
                        timeInterval = locationIntervalMillis,
                        onSuccess = {
                            activeSensorListeners[sensorType] = listener
                        },
                        onError = { exception ->
                            trySend(Error(exception)).isFailure
                        }
                    )
                }

                SensorType.DEVICE_ORIENTATION -> {
                    val listener = object : OrientationEventListener(context){
                        override fun onOrientationChanged(orientation: Int) {
                            val newOrientation = when (orientation) {
                                in 45..134 -> Data(
                                    type = SensorType.DEVICE_ORIENTATION,
                                    data = Orientation(orientation = DeviceOrientation.LANDSCAPE)
                                )
                                in 135..224 -> Data(
                                    type = SensorType.DEVICE_ORIENTATION,
                                    data = Orientation(orientation = DeviceOrientation.PORTRAIT)
                                )
                                in 225..314 -> Data(
                                    type = SensorType.DEVICE_ORIENTATION,
                                    data = Orientation(orientation = DeviceOrientation.LANDSCAPE)
                                )
                                in 315..360, in 0..44 -> Data(
                                    type = SensorType.DEVICE_ORIENTATION,
                                    data = Orientation(orientation = DeviceOrientation.PORTRAIT)
                                )
                                else -> Data(
                                    type = SensorType.DEVICE_ORIENTATION,
                                    data = Orientation(orientation = DeviceOrientation.UNKNOWN)
                                )
                            }
                            trySend(newOrientation).isSuccess

                        }
                    }
                    listener.enable()
                    awaitClose { listener.disable() }
                }
            }
        }
        awaitClose {
            unregisterSensors(sensorType)
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

    @Composable
    actual override fun HandelPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    ) {
        PermissionsManager().askPermission(permission, onPermissionStatus)
    }
}

@RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
inline fun requestLocationUpdatesSafely(
    locationManager: LocationManager,
    listener: LocationListener,
    timeInterval: SensorTimeInterval,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            timeInterval,
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