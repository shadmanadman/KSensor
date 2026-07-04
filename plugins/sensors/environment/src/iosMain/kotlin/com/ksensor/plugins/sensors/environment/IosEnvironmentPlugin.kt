package com.ksensor.plugins.sensors.environment

import com.ksensor.core.Permission
import com.ksensor.core.model.PluginId
import com.ksensor.core.SensorConfig
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreMotion.CMAltimeter
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSTimer
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceProximityStateDidChangeNotification
import platform.UIKit.UIScreen

class IosEnvironmentPlugin : EnvironmentPlugin {
    override val id: PluginId = PluginId.ENVIRONMENT
    override val requiredPermissions: List<Permission> = emptyList()

    private val altimeter = if (CMAltimeter.isRelativeAltitudeAvailable()) CMAltimeter() else null

    override fun barometer(config: SensorConfig): Flow<KSensorResponse<SensorData.Barometer>> = callbackFlow {
        if (altimeter == null) {
            close()
            return@callbackFlow
        }
        altimeter.startRelativeAltitudeUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
            data?.let {
                val sensorData = SensorData.Barometer(it.pressure.doubleValue.toFloat())
                trySend(KSensorResponse(sensorData))
            }
        }
        awaitClose { altimeter.stopRelativeAltitudeUpdates() }
    }

    override fun light(config: SensorConfig): Flow<KSensorResponse<SensorData.LightIlluminance>> = callbackFlow {
        val timer = NSTimer.scheduledTimerWithTimeInterval(
            config.samplingIntervalMs / 1000.0,
            repeats = true,
            block = {
                val brightness = UIScreen.mainScreen.brightness.toFloat()
                val sensorData = SensorData.LightIlluminance(brightness * 1000f)
                trySend(KSensorResponse(sensorData))
            }
        )
        NSRunLoop.mainRunLoop.addTimer(timer, NSRunLoopCommonModes)
        awaitClose { timer.invalidate() }
    }

    override fun proximity(config: SensorConfig): Flow<KSensorResponse<SensorData.Proximity>> = callbackFlow {
        val device = UIDevice.currentDevice
        device.proximityMonitoringEnabled = true
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIDeviceProximityStateDidChangeNotification,
            `object` = device,
            queue = NSOperationQueue.mainQueue,
            usingBlock = {
                val isNear = device.proximityState
                val sensorData = SensorData.Proximity(if (isNear) 0f else -1f, isNear)
                trySend(KSensorResponse(sensorData))
            }
        )
        awaitClose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
            device.proximityMonitoringEnabled = false
        }
    }
}

actual fun createEnvironmentPlugin(): EnvironmentPlugin = IosEnvironmentPlugin()
