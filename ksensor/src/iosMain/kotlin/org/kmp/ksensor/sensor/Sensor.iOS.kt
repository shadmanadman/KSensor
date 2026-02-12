package org.kmp.ksensor.sensor

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.kmp.ksensor.permission.PermissionStatus
import org.kmp.ksensor.permission.PermissionType
import org.kmp.ksensor.permission.createPermissionHandler
import org.kmp.ksensor.sensor.SensorData.Accelerometer
import org.kmp.ksensor.sensor.SensorData.Gyroscope
import org.kmp.ksensor.sensor.SensorData.LightIlluminance
import org.kmp.ksensor.sensor.SensorData.Location
import org.kmp.ksensor.sensor.SensorData.Magnetometer
import org.kmp.ksensor.sensor.SensorData.Orientation
import org.kmp.ksensor.sensor.SensorData.Proximity
import org.kmp.ksensor.sensor.SensorUpdate.Data
import org.kmp.ksensor.sensor.SensorUpdate.Error
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreMotion.CMAltimeter
import platform.CoreMotion.CMMotionManager
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSTimer
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIDeviceOrientationDidChangeNotification
import platform.UIKit.UIDeviceProximityStateDidChangeNotification
import platform.UIKit.UIScreen
import platform.darwin.NSObject

actual fun createController() :SensorController = iOSSensorController()
internal class iOSSensorController : SensorController {

    private val permissionHandler = createPermissionHandler()
    private val motionManager = CMMotionManager()
    private val altimeter = if (CMAltimeter.isRelativeAltitudeAvailable()) CMAltimeter() else null
    private val pedometer = if (CMPedometer.isStepCountingAvailable()) CMPedometer() else null

    private val locationManager = CLLocationManager()
    private val touchGesturesMonitor = TouchGesturesMonitor
    private var orientationObserver: NSObject? = null

    private var proximityObserver: NSObject? = null

    private var timer: NSTimer? = null

    @OptIn(ExperimentalForeignApi::class)
    override fun registerSensors(
        types: List<SensorType>,
        locationIntervalMillis: Long
    ): Flow<SensorUpdate> = callbackFlow {
        types.forEach { sensorType ->
            when (sensorType) {
                SensorType.ACCELEROMETER -> registerAccelerometer { trySend(it) }
                SensorType.GYROSCOPE -> registerGyroscope { trySend(it) }
                SensorType.MAGNETOMETER -> registerMagnetometer { trySend(it) }
                SensorType.BAROMETER -> registerBarometer { trySend(it) }
                SensorType.STEP_COUNTER -> registerStepCounter { trySend(it) }
                SensorType.LOCATION -> registerLocation { trySend(it) }
                SensorType.DEVICE_ORIENTATION -> registerDeviceOrientation { trySend(it) }
                SensorType.PROXIMITY -> registerProximity { trySend(it) }
                SensorType.LIGHT -> registerLight { trySend(it) }
                SensorType.TOUCH_GESTURES -> registerTouchGestures { trySend(it) }
            }.also {
                println("Sensor registered for $sensorType on iOS")
            }
        }

        awaitClose { unregisterSensors(types) }
    }

     override fun unregisterSensors(types: List<SensorType>) {
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

                SensorType.PROXIMITY -> {
                    proximityObserver?.let {
                        NSNotificationCenter.defaultCenter.removeObserver(it)
                        UIDevice.currentDevice.proximityMonitoringEnabled = false
                    }
                }

                SensorType.LIGHT -> {
                    timer?.invalidate()
                    timer = null
                }

                SensorType.TOUCH_GESTURES -> touchGesturesMonitor.removeObserver()
            }.also {
                println("Sensor unregistered for $types on iOS")
            }
        }
    }

    @Composable
    override fun AskPermission(
        permissionType: PermissionType,
        permissionStatus: (PermissionStatus) -> Unit
    ) = permissionHandler.AskPermission(permissionType, permissionStatus)


    @Composable
    override fun OpenSettingsForPermission()  = permissionHandler.OpenSettingsForPermission()

    @OptIn(ExperimentalForeignApi::class)
    private fun registerAccelerometer(onData: (SensorUpdate) -> Unit) {
        if (motionManager.accelerometerAvailable) {
            motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
                data?.let {
                    it.acceleration.useContents {
                        onData(
                            Data(
                                SensorType.ACCELEROMETER,
                                Accelerometer(
                                    this.x.toFloat(),
                                    this.y.toFloat(),
                                    this.z.toFloat()
                                ),
                                PlatformType.iOS
                            )
                        )
                    }
                }
            }
        } else
            println("Accelerometer not available")
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun registerGyroscope(onData: (SensorUpdate) -> Unit) {
        if (motionManager.gyroAvailable) {
            motionManager.startGyroUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
                data?.let {
                    it.rotationRate.useContents {
                        onData(
                            SensorUpdate.Data(
                                SensorType.GYROSCOPE,
                                Gyroscope(
                                    this.x.toFloat(),
                                    this.y.toFloat(),
                                    this.z.toFloat()
                                ),
                                PlatformType.iOS
                            )
                        )
                    }
                }
            }
        } else
            println("Gyroscope not available")
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun registerMagnetometer(onData: (SensorUpdate) -> Unit) {
        if (motionManager.magnetometerAvailable) {
            motionManager.startMagnetometerUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
                data?.let {
                    it.magneticField.useContents {
                        onData(
                            SensorUpdate.Data(
                                SensorType.MAGNETOMETER,
                                Magnetometer(
                                    this.x.toFloat(),
                                    this.y.toFloat(),
                                    this.z.toFloat()
                                ),
                                PlatformType.iOS
                            )
                        )
                    }
                }
            }
        } else
            println("Magnetometer not available")
    }

    private fun registerBarometer(onData: (SensorUpdate) -> Unit) {
        altimeter?.startRelativeAltitudeUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
            data?.let {
                val pressure = it.pressure.doubleValue.toFloat()
                onData(
                    SensorUpdate.Data(
                        SensorType.BAROMETER,
                        SensorData.Barometer(pressure),
                        PlatformType.iOS
                    )
                )
            }
        }
    }

    private fun registerStepCounter(onData: (SensorUpdate) -> Unit) {
        pedometer?.startPedometerUpdatesFromDate(NSDate()) { data, _ ->
            data?.let {
                val steps = it.numberOfSteps.intValue
                onData(
                    SensorUpdate.Data(
                        SensorType.STEP_COUNTER,
                        SensorData.StepCounter(steps),
                        PlatformType.iOS
                    )
                )
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun registerLocation(onData: (SensorUpdate) -> Unit) {
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

                        onData(
                            Data(
                                SensorType.LOCATION,
                                Location(
                                    latitude = latitude,
                                    longitude = longitude,
                                    altitude = it.altitude
                                ),
                                PlatformType.iOS
                            )
                        )
                    }
                }

                override fun locationManager(
                    manager: CLLocationManager,
                    didFailWithError: NSError
                ) {
                    onData(Error(Exception(didFailWithError.description)))
                }
            }

        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
    }

    private fun registerDeviceOrientation(onData: (SensorUpdate) -> Unit) {
        UIDevice.currentDevice.beginGeneratingDeviceOrientationNotifications()

        // Send current orientation immediately
        val initialOrientation =
            UIDevice.currentDevice.orientation.toDeviceOrientation()
        onData(
            Data(
                type = SensorType.DEVICE_ORIENTATION,
                Orientation(
                    orientation = initialOrientation
                ),
                PlatformType.iOS
            )
        )

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
            onData(
                Data(
                    type = SensorType.DEVICE_ORIENTATION,
                    data = Orientation(
                        orientation = mapped
                    ),
                    PlatformType.iOS
                )
            )
        } as NSObject?
    }

    private fun registerProximity(onData: (SensorUpdate) -> Unit) {
        val device = UIDevice.currentDevice
        device.proximityMonitoringEnabled = true
        proximityObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIDeviceProximityStateDidChangeNotification,
            `object` = device,
            queue = NSOperationQueue.mainQueue,
            usingBlock = {
                val isNear = device.proximityState
                onData(
                    Data(
                        type = SensorType.PROXIMITY,
                        // In ios the proximity sensor is restricted
                        data = Proximity(
                            distanceInCM = if (isNear) 0f else -1f,
                            isNear = isNear
                        ),
                        PlatformType.iOS
                    )
                )
            }
        ) as NSObject?
    }

    private fun registerLight(onData: (SensorUpdate) -> Unit) {
        timer = NSTimer.scheduledTimerWithTimeInterval(
            0.5,
            repeats = true,
            block = {
                val brightness = UIScreen.mainScreen.brightness.toFloat()
                // Scale to lux like value (0–1000)
                val lux = brightness * 1000f
                onData(
                    Data(
                        type = SensorType.LIGHT,
                        data = LightIlluminance(
                            illuminance = lux
                        ),
                        PlatformType.iOS
                    )
                )
            }
        )
        NSRunLoop.mainRunLoop.addTimer(timer!!, NSRunLoopCommonModes)
    }

    private fun registerTouchGestures(onData: (SensorUpdate) -> Unit) {
        touchGesturesMonitor.registerObserver(onData)
    }
}