package com.ksensor.plugins.sensors.motion

import com.ksensor.core.Permission
import com.ksensor.core.model.PluginId
import com.ksensor.core.SensorConfig
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import com.ksensor.core.model.Vector3
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreMotion.CMMotionActivityManager
import platform.CoreMotion.CMMotionManager
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSDate
import platform.Foundation.NSOperationQueue

class IosMotionPlugin : MotionPlugin {
    override val id: PluginId = PluginId.MOTION
    override val requiredPermissions: List<Permission> = listOf(Permission.ACTIVITY_RECOGNITION)

    private val motionManager = CMMotionManager()
    private val pedometer = if (CMPedometer.isStepCountingAvailable()) CMPedometer() else null
    private val activityManager = CMMotionActivityManager()

    @OptIn(ExperimentalForeignApi::class)
    override fun accelerometer(config: SensorConfig): Flow<KSensorResponse<SensorData.Accelerometer>> = callbackFlow {
        if (!motionManager.accelerometerAvailable) {
            close()
            return@callbackFlow
        }

        motionManager.accelerometerUpdateInterval = config.samplingIntervalMs / 1000.0
        motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
            data?.acceleration?.useContents {
                val sensorData = SensorData.Accelerometer(Vector3(x.toFloat(), y.toFloat(), z.toFloat()))
                trySend(KSensorResponse(sensorData))
            }
        }

        awaitClose { motionManager.stopAccelerometerUpdates() }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun gyroscope(config: SensorConfig): Flow<KSensorResponse<SensorData.Gyroscope>> = callbackFlow {
        if (!motionManager.gyroAvailable) {
            close()
            return@callbackFlow
        }

        motionManager.gyroUpdateInterval = config.samplingIntervalMs / 1000.0
        motionManager.startGyroUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
            data?.rotationRate?.useContents {
                val sensorData = SensorData.Gyroscope(Vector3(x.toFloat(), y.toFloat(), z.toFloat()))
                trySend(KSensorResponse(sensorData))
            }
        }

        awaitClose { motionManager.stopGyroUpdates() }
    }

    override fun stepCounter(config: SensorConfig): Flow<KSensorResponse<SensorData.StepCounter>> = callbackFlow {
        if (pedometer == null) {
            close()
            return@callbackFlow
        }

        pedometer.startPedometerUpdatesFromDate(NSDate()) { data, _ ->
            data?.let {
                val sensorData = SensorData.StepCounter(it.numberOfSteps.intValue)
                trySend(KSensorResponse(sensorData))
            }
        }

        awaitClose { pedometer.stopPedometerUpdates() }
    }

    override fun motionDetector(config: SensorConfig): Flow<KSensorResponse<SensorData.MotionDetector>> = callbackFlow {
        if (!CMMotionActivityManager.isActivityAvailable()) {
            close()
            return@callbackFlow
        }

        activityManager.startActivityUpdatesToQueue(NSOperationQueue.mainQueue()) { activity ->
            activity?.let {
                val type = when {
                    it.running -> SensorData.MotionType.RUNNING
                    it.cycling -> SensorData.MotionType.CYCLING
                    it.walking -> SensorData.MotionType.WALKING
                    it.automotive -> SensorData.MotionType.AUTOMOTIVE
                    it.stationary -> SensorData.MotionType.STATIONARY
                    else -> SensorData.MotionType.UNKNOWN
                }
                trySend(KSensorResponse(SensorData.MotionDetector(type)))
            }
        }

        awaitClose { activityManager.stopActivityUpdates() }
    }
}

actual fun createMotionPlugin(): MotionPlugin = IosMotionPlugin()
