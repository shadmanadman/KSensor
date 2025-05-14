package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.*
import platform.CoreLocation.*
import platform.Foundation.*
import platform.darwin.*
import kotlin.time.Clock.System

internal actual class SensorHandler : SensorController {

    private val motionManager = CMMotionManager()
    private val altimeter = if (CMAltimeter.isRelativeAltitudeAvailable()) CMAltimeter() else null
    private val pedometer = if (CMPedometer.isStepCountingAvailable()) CMPedometer() else null
    private val locationManager = CLLocationManager()

    private val lastEmitTimestamps = mutableMapOf<SensorType, SensorTimeInterval>()

    @OptIn(ExperimentalForeignApi::class)
    actual override fun registerSensors(
        sensorTypesWithIntervals: Map<SensorType, SensorTimeInterval?>,
        defaultIntervalMillis: Long,
        onSensorData: (SensorType, SensorData) -> Unit,
        onSensorError: (Exception) -> Unit
    ) {
        sensorTypesWithIntervals.forEach { (sensorType, sensorTimeIntervals) ->
            when (sensorType) {
                SensorType.ACCELEROMETER -> {
                    if (motionManager.accelerometerAvailable) {
                        motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
                            val now = (NSDate().timeIntervalSince1970 * 1000).toLong()
                            val lastTime = lastEmitTimestamps[sensorType] ?: 0L
                            if (now - lastTime >= (sensorTimeIntervals ?: defaultIntervalMillis)) {
                                lastEmitTimestamps[sensorType] = now
                                data?.let {
                                    it.acceleration.useContents {
                                        onSensorData(
                                            SensorType.ACCELEROMETER,
                                            SensorData.Accelerometer(
                                                this.x.toFloat(),
                                                this.y.toFloat(),
                                                this.z.toFloat(),
                                                PlatformType.iOS
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SensorType.GYROSCOPE -> {
                    if (motionManager.gyroAvailable) {
                        motionManager.startGyroUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
                            val now = (NSDate().timeIntervalSince1970 * 1000).toLong()
                            val lastTime = lastEmitTimestamps[sensorType] ?: 0L
                            if (now - lastTime >= (sensorTimeIntervals ?: defaultIntervalMillis)) {
                                lastEmitTimestamps[sensorType] = now
                                data?.let {
                                    it.rotationRate.useContents {
                                        onSensorData(
                                            SensorType.GYROSCOPE,
                                            SensorData.Gyroscope(
                                                this.x.toFloat(),
                                                this.y.toFloat(),
                                                this.z.toFloat(),
                                                PlatformType.iOS
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SensorType.MAGNETOMETER -> {
                    if (motionManager.magnetometerAvailable) {
                        motionManager.startMagnetometerUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
                            val now = (NSDate().timeIntervalSince1970 * 1000).toLong()
                            val lastTime = lastEmitTimestamps[sensorType] ?: 0L
                            if (now - lastTime >= (sensorTimeIntervals ?: defaultIntervalMillis)) {
                                lastEmitTimestamps[sensorType] = now
                                data?.let {
                                    it.magneticField.useContents {
                                        onSensorData(
                                            SensorType.MAGNETOMETER,
                                            SensorData.Magnetometer(
                                                this.x.toFloat(),
                                                this.y.toFloat(),
                                                this.z.toFloat(),
                                                PlatformType.iOS
                                            )
                                        )
                                    }

                                }
                            }
                        }
                    }
                }

                SensorType.BAROMETER -> {
                    altimeter?.startRelativeAltitudeUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
                        val now = (NSDate().timeIntervalSince1970 * 1000).toLong()
                        val lastTime = lastEmitTimestamps[sensorType] ?: 0L
                        if (now - lastTime >= (sensorTimeIntervals ?: defaultIntervalMillis)) {
                            lastEmitTimestamps[sensorType] = now
                            data?.let {
                                val pressure = it.pressure.doubleValue.toFloat()
                                onSensorData(
                                    SensorType.BAROMETER,
                                    SensorData.Barometer(pressure, PlatformType.iOS)
                                )
                            }
                        }
                    }
                }

                SensorType.STEP_COUNTER -> {
                    pedometer?.startPedometerUpdatesFromDate(NSDate()) { data, _ ->
                        val now = (NSDate().timeIntervalSince1970 * 1000).toLong()
                        val lastTime = lastEmitTimestamps[sensorType] ?: 0L
                        if (now - lastTime >= (sensorTimeIntervals ?: defaultIntervalMillis)) {
                            lastEmitTimestamps[sensorType] = now
                            data?.let {
                                val steps = it.numberOfSteps.intValue
                                onSensorData(
                                    SensorType.STEP_COUNTER,
                                    SensorData.StepCounter(steps, PlatformType.iOS)
                                )
                            }
                        }
                    }
                }

                SensorType.LOCATION -> {
                    locationManager.delegate =
                        object : NSObject(), CLLocationManagerDelegateProtocol {
                            override fun locationManager(
                                manager: CLLocationManager,
                                didUpdateLocations: List<*>
                            ) {
                                val loc = didUpdateLocations.lastOrNull() as? CLLocation
                                loc?.let {
                                    val latitude: Double
                                    val longitude: Double

                                    it.coordinate.useContents {
                                        latitude = this.latitude
                                        longitude = this.longitude
                                    }

                                    onSensorData(
                                        SensorType.LOCATION,
                                        SensorData.Location(
                                            latitude = latitude,
                                            longitude = longitude,
                                            altitude = it.altitude,
                                            platformType = PlatformType.iOS
                                        )
                                    )
                                }
                            }

                            override fun locationManager(
                                manager: CLLocationManager,
                                didFailWithError: NSError
                            ) {
                                onSensorError(Exception(didFailWithError.description))
                            }
                        }

                    locationManager.requestWhenInUseAuthorization()
                    locationManager.startUpdatingLocation()
                }
            }
        }
    }

    actual override fun unregisterSensors(types: List<SensorType>) {
        types.forEach { type ->
            when (type) {
                SensorType.ACCELEROMETER -> motionManager.stopAccelerometerUpdates()
                SensorType.GYROSCOPE -> motionManager.stopGyroUpdates()
                SensorType.MAGNETOMETER -> motionManager.stopMagnetometerUpdates()
                SensorType.BAROMETER -> altimeter?.stopRelativeAltitudeUpdates()
                SensorType.STEP_COUNTER -> pedometer?.stopPedometerUpdates()
                SensorType.LOCATION -> locationManager.stopUpdatingLocation()
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