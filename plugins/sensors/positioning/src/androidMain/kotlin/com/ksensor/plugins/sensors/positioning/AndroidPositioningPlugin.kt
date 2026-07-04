package com.ksensor.plugins.sensors.positioning

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.view.OrientationEventListener
import com.ksensor.core.Permission
import com.ksensor.core.model.PluginId
import com.ksensor.core.SensorConfig
import com.ksensor.core.StatePlugin
import com.ksensor.core.context.KSensorContext
import com.ksensor.core.model.DeviceOrientation
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import com.ksensor.core.model.StateData
import com.ksensor.core.model.Vector3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import java.util.concurrent.ConcurrentHashMap

class AndroidPositioningPlugin : PositioningPlugin {
    override val id: PluginId = PluginId.POSITIONING
    override val requiredPermissions: List<Permission> = listOf(Permission.LOCATION)

    private val context: Context by lazy { KSensorContext.get() }
    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val locationFlows = ConcurrentHashMap<SensorConfig, Flow<KSensorResponse<SensorData.Location>>>()
    private val magnetometerFlows = ConcurrentHashMap<SensorConfig, Flow<KSensorResponse<SensorData.Magnetometer>>>()
    private val orientationFlows = ConcurrentHashMap<SensorConfig, Flow<KSensorResponse<SensorData.Orientation>>>()

    @SuppressLint("MissingPermission")
    override fun location(config: SensorConfig): Flow<KSensorResponse<SensorData.Location>> =
        locationFlows.getOrPut(config) {
            callbackFlow {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val data = SensorData.Location(location.latitude, location.longitude, location.altitude)
                        trySend(KSensorResponse(data))
                    }
                }
                try {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        config.samplingIntervalMs,
                        1f,
                        listener
                    )
                } catch (e: SecurityException) {
                    close(e)
                }
                awaitClose { locationManager.removeUpdates(listener) }
            }.shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
        }

    override fun magnetometer(config: SensorConfig): Flow<KSensorResponse<SensorData.Magnetometer>> =
        magnetometerFlows.getOrPut(config) {
            callbackFlow {
                val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                if (sensor == null) {
                    close()
                    return@callbackFlow
                }
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val data = SensorData.Magnetometer(Vector3(event.values[0], event.values[1], event.values[2]))
                        trySend(KSensorResponse(data))
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                awaitClose { sensorManager.unregisterListener(listener) }
            }.shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
        }

    override fun orientation(config: SensorConfig): Flow<KSensorResponse<SensorData.Orientation>> =
        orientationFlows.getOrPut(config) {
            callbackFlow {
                val listener = object : OrientationEventListener(context) {
                    override fun onOrientationChanged(orientation: Int) {
                        val mapped = when (orientation) {
                            in 45..134 -> DeviceOrientation.LANDSCAPE
                            in 135..224 -> DeviceOrientation.PORTRAIT
                            in 225..314 -> DeviceOrientation.LANDSCAPE
                            in 315..360, in 0..44 -> DeviceOrientation.PORTRAIT
                            else -> DeviceOrientation.UNKNOWN
                        }
                        val data = SensorData.Orientation(mapped, orientation)
                        trySend(KSensorResponse(data))
                    }
                }
                listener.enable()
                awaitClose { listener.disable() }
            }.shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
        }

    override fun locationStatus(): StatePlugin<StateData.LocationStatus> = object : StatePlugin<StateData.LocationStatus> {
        override val id: PluginId = PluginId.POSITIONING
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.LocationStatus>
            get() = KSensorResponse(StateData.LocationStatus(locationManager.isLocationEnabled))

        override fun observe(): Flow<KSensorResponse<StateData.LocationStatus>> = callbackFlow {
            val receiver = LocationProviderReceiver {
                trySend(KSensorResponse(StateData.LocationStatus(locationManager.isLocationEnabled)))
            }
            context.registerReceiver(receiver, IntentFilter(LocationManager.MODE_CHANGED_ACTION))
            awaitClose { context.unregisterReceiver(receiver) }
        }
    }
}

actual fun createPositioningPlugin(): PositioningPlugin = AndroidPositioningPlugin()
