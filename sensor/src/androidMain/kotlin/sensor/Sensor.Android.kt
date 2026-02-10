package sensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.HandlerThread
import android.view.OrientationEventListener
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import permission.PermissionStatus
import permission.PermissionType
import permission.createPermissionHandler
import sensor.SensorData.Accelerometer
import sensor.SensorData.Barometer
import sensor.SensorData.Gyroscope
import sensor.SensorData.Magnetometer
import sensor.SensorData.Orientation
import sensor.SensorData.StepCounter
import kotlin.collections.forEach

actual fun createController() : SensorController = AndroidSensorHandler()
internal class AndroidSensorHandler : SensorController {
    private val context: Context by lazy { AppContext.get() }

    private val permissionHandler = createPermissionHandler()
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val touchGestureMonitor = TouchGesturesMonitor

    private val activeSensorListeners = mutableMapOf<SensorType, Any>()

    private val sensorListenerThread = HandlerThread("SensorListenerThread")
    private val sensorListenerHandler: Handler by lazy {
        sensorListenerThread.start()
        Handler(sensorListenerThread.looper)
    }

    override fun registerSensors(
        types: List<SensorType>,
        locationIntervalMillis: SensorTimeInterval
    ): Flow<SensorUpdate> = callbackFlow {
        types.forEach { sensorType ->
            if (activeSensorListeners.containsKey(sensorType)) return@forEach

            when (sensorType) {
                SensorType.ACCELEROMETER -> registerAccelerometer { trySend(it) }
                SensorType.GYROSCOPE -> registerGyroscope { trySend(it) }
                SensorType.MAGNETOMETER -> registerMagnetometer { trySend(it) }
                SensorType.BAROMETER -> registerBarometer { trySend(it) }
                SensorType.STEP_COUNTER -> registerStepCounter { trySend(it) }
                SensorType.LOCATION -> registerLocation(locationIntervalMillis) { trySend(it) }
                SensorType.DEVICE_ORIENTATION -> registerDeviceOrientation { trySend(it) }
                SensorType.PROXIMITY -> registerProximity { trySend(it) }
                SensorType.LIGHT -> registerLight { trySend(it) }
                SensorType.TOUCH_GESTURES -> registerTouchGestures { trySend(it) }
            }.also {
                println("Sensor registered for $sensorType on Android")
            }
        }

        awaitClose {
            unregisterSensors(types)
        }
    }

    override fun unregisterSensors(types: List<SensorType>) {
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
    fun AskPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    ) {
        permissionHandler.askPermission(permission, onPermissionStatus)
    }

    private fun registerAccelerometer(onData: (SensorUpdate) -> Unit) {
        val maximumRange =
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.maximumRange ?: 0F
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                onData(
                    SensorUpdate.Data(
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
            sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL,sensorListenerHandler)
            activeSensorListeners[SensorType.ACCELEROMETER] = listener
        } ?: println("ACCELEROMETER not available")
    }

    private fun registerGyroscope(onData: (SensorUpdate) -> Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                onData(
                    SensorUpdate.Data(
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
            sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL,sensorListenerHandler)
            activeSensorListeners[SensorType.GYROSCOPE] = listener
        } ?: println("GYROSCOPE not available")
    }

    private fun registerMagnetometer(onData: (SensorUpdate) -> Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                onData(
                    SensorUpdate.Data(
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
            sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL,sensorListenerHandler)
            activeSensorListeners[SensorType.MAGNETOMETER] = listener
        } ?: println("MAGNETOMETER not available")
    }

    private fun registerBarometer(onData: (SensorUpdate) -> Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                onData(
                    SensorUpdate.Data(
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
            sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL,sensorListenerHandler)
            activeSensorListeners[SensorType.BAROMETER] = listener
        } ?: println("BAROMETER not available")
    }

    private fun registerStepCounter(onData: (SensorUpdate) -> Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                onData(
                    SensorUpdate.Data(
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
            sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL,sensorListenerHandler)
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
                    SensorUpdate.Data(
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
                onData(SensorUpdate.Error(exception))
            }
        )
    }

    private fun registerDeviceOrientation(onData: (SensorUpdate) -> Unit) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                val newOrientation = when (orientation) {
                    in 45..134 -> SensorUpdate.Data(
                        type = SensorType.DEVICE_ORIENTATION,
                        data = Orientation(
                            orientation = DeviceOrientation.LANDSCAPE
                        ),
                        PlatformType.Android
                    )

                    in 135..224 -> SensorUpdate.Data(
                        type = SensorType.DEVICE_ORIENTATION,
                        data = Orientation(
                            orientation = DeviceOrientation.PORTRAIT
                        ),
                        platformType = PlatformType.Android
                    )

                    in 225..314 -> SensorUpdate.Data(
                        type = SensorType.DEVICE_ORIENTATION,
                        data = Orientation(
                            orientation = DeviceOrientation.LANDSCAPE
                        ),
                        platformType = PlatformType.Android
                    )

                    in 315..360, in 0..44 -> SensorUpdate.Data(
                        type = SensorType.DEVICE_ORIENTATION,
                        data = Orientation(
                            orientation = DeviceOrientation.PORTRAIT
                        ),
                        platformType = PlatformType.Android
                    )

                    else -> SensorUpdate.Data(
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
                    SensorUpdate.Data(
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
            sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL,sensorListenerHandler)
            activeSensorListeners[SensorType.PROXIMITY] = listener
        } ?: println("Proximity sensor not available")
    }

    private fun registerLight(onData: (SensorUpdate) -> Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                onData(
                    SensorUpdate.Data(
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
            sensorManager.registerListener(listener, it, SENSOR_DELAY_NORMAL,sensorListenerHandler)
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