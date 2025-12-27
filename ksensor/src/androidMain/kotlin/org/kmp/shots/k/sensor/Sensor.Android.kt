package org.kmp.shots.k.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.view.OrientationEventListener
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.kmp.shots.k.sensor.SensorData.Accelerometer
import org.kmp.shots.k.sensor.SensorData.Barometer
import org.kmp.shots.k.sensor.SensorData.Gyroscope
import org.kmp.shots.k.sensor.SensorData.Magnetometer
import org.kmp.shots.k.sensor.SensorData.Orientation
import org.kmp.shots.k.sensor.SensorData.StepCounter
import org.kmp.shots.k.sensor.SensorUpdate.Data
import org.kmp.shots.k.sensor.SensorUpdate.Error

internal actual class SensorHandler : SensorController {
    private val context: Context by lazy { AppContext.get() }

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val touchGestureMonitor = TouchGesturesMonitor

    private val activeSensorListeners = mutableMapOf<SensorType, Any>()

    actual override val sensorUpdates: MutableStateFlow<SensorUpdate?>
        get() = super.sensorUpdates

    actual override fun registerSensors(
        types: List<SensorType>,
        locationIntervalMillis: SensorTimeInterval
    ) {
        types.forEach { sensorType ->
            if (activeSensorListeners.containsKey(sensorType)) return@forEach

            when (sensorType) {
                SensorType.ACCELEROMETER -> registerAccelerometer { sensorUpdates.value = it }
                SensorType.GYROSCOPE -> registerGyroscope { sensorUpdates.value = it }
                SensorType.MAGNETOMETER -> registerMagnetometer { sensorUpdates.value = it }
                SensorType.BAROMETER -> registerBarometer { sensorUpdates.value = it }
                SensorType.STEP_COUNTER -> registerStepCounter { sensorUpdates.value = it }
                SensorType.LOCATION -> registerLocation(locationIntervalMillis) {
                    sensorUpdates.value = it
                }

                SensorType.DEVICE_ORIENTATION -> registerDeviceOrientation {
                    sensorUpdates.value = it
                }

                SensorType.PROXIMITY -> registerProximity { sensorUpdates.value = it }
                SensorType.LIGHT -> registerLight { sensorUpdates.value = it }
                SensorType.TOUCH_GESTURES -> registerTouchGestures { sensorUpdates.value = it }
            }.also {
                println("Sensor registered for $sensorType on Android")
            }
        }
    }

    actual override fun unregisterSensors(types: List<SensorType>) {
        types.forEach { sensorType ->
            when (val listener = activeSensorListeners.remove(sensorType)) {
                is SensorEventListener -> sensorManager.unregisterListener(listener)
                is LocationListener -> locationManager.removeUpdates(listener)
                is OrientationEventListener -> listener.disable()
                is ScreenStateReceiver -> context.unregisterReceiver(listener)
                is TouchGesturesMonitor -> touchGestureMonitor.removeObserver()
                else -> println("Sensor type not found for $sensorType on Android")
            }.also {
                println("Unregistered sensor for $sensorType on Android")
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

    private fun registerAccelerometer(onData: (SensorUpdate) -> Unit) {
        val maximumRange =
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.maximumRange ?: 0F
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                onData(
                    Data(
                        SensorType.ACCELEROMETER,
                        Accelerometer(
                            event.values[0] / maximumRange,
                            event.values[1] / maximumRange,
                            event.values[2] / maximumRange
                        ),
                        PlatformType.Android
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).also {
            sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
            activeSensorListeners[SensorType.ACCELEROMETER] = listener
        } ?: println("ACCELEROMETER not available")
    }

    private fun registerGyroscope(onData: (SensorUpdate) -> Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                onData(
                    Data(
                        type = SensorType.GYROSCOPE, data = Gyroscope(
                            event.values[0],
                            event.values[1],
                            event.values[2]
                        ),
                        PlatformType.Android
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE).also {
            sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
            activeSensorListeners[SensorType.GYROSCOPE] = listener
        } ?: println("GYROSCOPE not available")
    }

    private fun registerMagnetometer(onData: (SensorUpdate) -> Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                onData(
                    Data(
                        SensorType.MAGNETOMETER,
                        Magnetometer(
                            event.values[0],
                            event.values[1],
                            event.values[2],
                        ),
                        PlatformType.Android
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD).also {
            sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
            activeSensorListeners[SensorType.MAGNETOMETER] = listener
        } ?: println("MAGNETOMETER not available")
    }

    private fun registerBarometer(onData: (SensorUpdate) -> Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                onData(
                    Data(
                        SensorType.BAROMETER, Barometer(
                            event.values[0],
                        ),
                        PlatformType.Android
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE).also {
            sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
            activeSensorListeners[SensorType.BAROMETER] = listener
        } ?: println("BAROMETER not available")
    }

    private fun registerStepCounter(onData: (SensorUpdate) -> Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                onData(
                    Data(
                        SensorType.STEP_COUNTER,
                        StepCounter(
                            event.values[0].toInt()
                        ),
                        PlatformType.Android
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER).also {
            sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
            activeSensorListeners[SensorType.STEP_COUNTER] = listener
        } ?: println("Step counter not available")
    }

    @SuppressLint("MissingPermission")
    private fun registerLocation(
        locationIntervalMillis: SensorTimeInterval,
        onData: (SensorUpdate) -> Unit
    ) {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onData(
                    Data(
                        SensorType.LOCATION,
                        SensorData.Location(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = location.altitude
                        ),
                        PlatformType.Android
                    )
                )
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        requestLocationUpdatesSafely(
            locationManager = locationManager,
            listener = listener,
            timeInterval = locationIntervalMillis,
            onSuccess = {
                activeSensorListeners[SensorType.LOCATION] = listener
            },
            onError = { exception ->
                onData(Error(exception))
            }
        )
    }

    private fun registerDeviceOrientation(onData: (SensorUpdate) -> Unit) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                val newOrientation = when (orientation) {
                    in 45..134 -> Data(
                        type = SensorType.DEVICE_ORIENTATION,
                        data = Orientation(
                            orientation = DeviceOrientation.LANDSCAPE
                        ),
                        PlatformType.Android
                    )

                    in 135..224 -> Data(
                        type = SensorType.DEVICE_ORIENTATION,
                        data = Orientation(
                            orientation = DeviceOrientation.PORTRAIT
                        ),
                        platformType = PlatformType.Android
                    )

                    in 225..314 -> Data(
                        type = SensorType.DEVICE_ORIENTATION,
                        data = Orientation(
                            orientation = DeviceOrientation.LANDSCAPE
                        ),
                        platformType = PlatformType.Android
                    )

                    in 315..360, in 0..44 -> Data(
                        type = SensorType.DEVICE_ORIENTATION,
                        data = Orientation(
                            orientation = DeviceOrientation.PORTRAIT
                        ),
                        platformType = PlatformType.Android
                    )

                    else -> Data(
                        type = SensorType.DEVICE_ORIENTATION,
                        data = Orientation(
                            orientation = DeviceOrientation.UNKNOWN
                        ),
                        platformType = PlatformType.Android
                    )
                }
                onData(newOrientation)
            }
        }
        listener.enable()
        activeSensorListeners[SensorType.DEVICE_ORIENTATION] = listener
    }

    private fun registerProximity(onData: (SensorUpdate) -> Unit) {
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val distanceInCM = event.values[0]
                onData(
                    Data(
                        type = SensorType.PROXIMITY, data = SensorData.Proximity(
                            distanceInCM = distanceInCM,
                            isNear = distanceInCM < (proximitySensor?.maximumRange
                                ?: distanceInCM)
                        ),
                        platformType = PlatformType.Android
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY).also {
            sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
            activeSensorListeners[SensorType.PROXIMITY] = listener
        } ?: println("Proximity sensor not available")
    }

    private fun registerLight(onData: (SensorUpdate) -> Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                onData(
                    Data(
                        type = SensorType.LIGHT, data = SensorData.LightIlluminance(
                            illuminance = event.values[0]
                        ),
                        platformType = PlatformType.Android
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT).also {
            sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL)
            activeSensorListeners[SensorType.LIGHT] = listener
        } ?: println("Light sensor not available")
    }

    private fun registerTouchGestures(onData: (SensorUpdate) -> Unit) {
        touchGestureMonitor.registerObserver(onData)
        activeSensorListeners[SensorType.TOUCH_GESTURES] = touchGestureMonitor
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