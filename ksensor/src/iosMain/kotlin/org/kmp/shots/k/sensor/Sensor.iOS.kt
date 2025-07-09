package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.kmp.shots.k.sensor.SensorData.*
import org.kmp.shots.k.sensor.SensorUpdate.*
import platform.CoreMotion.*
import platform.CoreLocation.*
import platform.Foundation.*
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIDeviceOrientationDidChangeNotification
import platform.darwin.*
import kotlin.time.Clock.System

internal actual class SensorHandler : SensorController {

    private val motionManager = CMMotionManager()
    private val altimeter = if (CMAltimeter.isRelativeAltitudeAvailable()) CMAltimeter() else null
    private val pedometer = if (CMPedometer.isStepCountingAvailable()) CMPedometer() else null
    private val locationManager = CLLocationManager()
    private var orientationObserver: NSObject? = null

    @OptIn(ExperimentalForeignApi::class)
    actual override fun registerSensors(
        sensorType: List<SensorType>,
        locationIntervalMillis: Long
    ): Flow<SensorUpdate> = callbackFlow {
        sensorType.forEach { sensorType ->
            when (sensorType) {
                SensorType.ACCELEROMETER -> {
                    if (motionManager.accelerometerAvailable) {
                        motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
                            data?.let {
                                it.acceleration.useContents {
                                    trySend(
                                        Data(
                                            SensorType.ACCELEROMETER,
                                            Accelerometer(
                                                this.x.toFloat(),
                                                this.y.toFloat(),
                                                this.z.toFloat(),
                                                PlatformType.iOS
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    } else
                        println("Accelerometer not available")
                }

                SensorType.GYROSCOPE -> {
                    if (motionManager.gyroAvailable) {
                        motionManager.startGyroUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
                            data?.let {
                                it.rotationRate.useContents {
                                    trySend(
                                        Data(
                                            SensorType.GYROSCOPE,
                                            Gyroscope(
                                                this.x.toFloat(),
                                                this.y.toFloat(),
                                                this.z.toFloat(),
                                                PlatformType.iOS
                                            )
                                        )
                                    )
                                }
                            }
                        }

                    } else
                        println("Gyroscope not available")
                }

                SensorType.MAGNETOMETER -> {
                    if (motionManager.magnetometerAvailable) {
                        motionManager.startMagnetometerUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
                            data?.let {
                                it.magneticField.useContents {
                                    trySend(
                                        Data(
                                            SensorType.MAGNETOMETER,
                                            Magnetometer(
                                                this.x.toFloat(),
                                                this.y.toFloat(),
                                                this.z.toFloat(),
                                                PlatformType.iOS
                                            )
                                        )
                                    )
                                }

                            }

                        }
                    } else
                        println("Magnetometer not available")
                }

                SensorType.BAROMETER -> {
                    altimeter?.startRelativeAltitudeUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
                        data?.let {
                            val pressure = it.pressure.doubleValue.toFloat()
                            trySend(
                                Data(
                                    SensorType.BAROMETER,
                                    Barometer(pressure, PlatformType.iOS)
                                )
                            )
                        }
                    }
                }

                SensorType.STEP_COUNTER -> {
                    pedometer?.startPedometerUpdatesFromDate(NSDate()) { data, _ ->
                        data?.let {
                            val steps = it.numberOfSteps.intValue
                            trySend(
                                Data(
                                    SensorType.STEP_COUNTER,
                                    StepCounter(steps, PlatformType.iOS)
                                )
                            )
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

                                    trySend(
                                        Data(
                                            SensorType.LOCATION,
                                            Location(
                                                latitude = latitude,
                                                longitude = longitude,
                                                altitude = it.altitude,
                                                platformType = PlatformType.iOS
                                            )
                                        )
                                    )
                                }
                            }

                            override fun locationManager(
                                manager: CLLocationManager,
                                didFailWithError: NSError
                            ) {
                                trySend(Error(Exception(didFailWithError.description)))
                            }
                        }

                    locationManager.requestWhenInUseAuthorization()
                    locationManager.startUpdatingLocation()
                }

                SensorType.DEVICE_ORIENTATION -> {
                    UIDevice.currentDevice.beginGeneratingDeviceOrientationNotifications()

                    // Send current orientation immediately
                    val initialOrientation =
                        UIDevice.currentDevice.orientation.toDeviceOrientation()
                    trySend(
                        element = Data(
                            type = SensorType.DEVICE_ORIENTATION,
                            Orientation(orientation = initialOrientation)
                        )
                    ).isSuccess

                    orientationObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                        name = UIDeviceOrientationDidChangeNotification,
                        `object` = null,
                        queue = NSOperationQueue.mainQueue()
                    ) {
                        val orientation = UIDevice.currentDevice.orientation
                        val mapped = when (orientation) {
                            UIDeviceOrientation.UIDeviceOrientationPortrait,
                            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> DeviceOrientation.PORTRAIT

                            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft,
                            UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> DeviceOrientation.LANDSCAPE

                            else -> DeviceOrientation.UNKNOWN
                        }
                        trySend(
                            element = Data(
                                type = SensorType.DEVICE_ORIENTATION,
                                data = Orientation(orientation = mapped)
                            )
                        ).isSuccess
                    } as NSObject?
                }
            }
        }

        awaitClose {
            unregisterSensors(sensorType)
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
                SensorType.DEVICE_ORIENTATION -> {
                    orientationObserver?.let {
                        NSNotificationCenter.defaultCenter.removeObserver(it)
                        UIDevice.currentDevice.endGeneratingDeviceOrientationNotifications()
                    }
                }
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