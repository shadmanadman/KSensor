package com.ksensor.plugins.sensors.health

import android.content.Context
import android.util.Log
import com.ksensor.core.Permission
import com.ksensor.core.SensorConfig
import com.ksensor.core.context.KSensorContext
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.PluginId
import com.ksensor.core.model.SensorData
import com.ksensor.plugins.sensors.health.heart.CameraHeartRateMonitor
import com.ksensor.plugins.sensors.health.heart.SensorManagerHeartRateMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

class AndroidHealthPlugin : HealthPlugin {
    override val id: PluginId = PluginId.HEALTH
    override val requiredPermissions: List<Permission> = listOf(
        Permission.BODY_SENSORS,
        Permission.CAMERA
    )

    private val context: Context by lazy { KSensorContext.get() }
    
    private val sensorManagerMonitor by lazy { SensorManagerHeartRateMonitor(context) }
    private val cameraMonitor by lazy { CameraHeartRateMonitor(context) }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val heartRateFlows = ConcurrentHashMap<SensorConfig, Flow<KSensorResponse<SensorData.HeartRate>>>()

    override fun heartRate(config: SensorConfig): Flow<KSensorResponse<SensorData.HeartRate>> =
        heartRateFlows.getOrPut(config) {
            flow {
                if (sensorManagerMonitor.isSupported()) {
                    Log.i("AndroidHealthPlugin", "Using Hardware Sensor for heart rate")
                    emitAll(sensorManagerMonitor.start())
                } else if (cameraMonitor.isSupported()) {
                    Log.i("AndroidHealthPlugin", "Falling back to Camera PPG for heart rate")
                    // Emit an initial value with 0 BPM to signal the UI that Camera PPG is active
                    emit(KSensorResponse(SensorData.HeartRate(0f, SensorData.HeartRateSource.CAMERA_PPG)))
                    emitAll(cameraMonitor.start())
                } else {
                    Log.w("AndroidHealthPlugin", "Heart rate monitoring is not supported on this device")
                }
            }.shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
        }
}

actual fun createHealthPlugin(): HealthPlugin = AndroidHealthPlugin()
