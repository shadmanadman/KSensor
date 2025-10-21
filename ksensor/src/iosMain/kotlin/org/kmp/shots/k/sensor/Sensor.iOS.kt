package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.kmp.shots.k.sensor.SensorData.Accelerometer
import org.kmp.shots.k.sensor.SensorData.Barometer
import org.kmp.shots.k.sensor.SensorData.Gyroscope
import org.kmp.shots.k.sensor.SensorData.LightIlluminance
import org.kmp.shots.k.sensor.SensorData.Location
import org.kmp.shots.k.sensor.SensorData.Magnetometer
import org.kmp.shots.k.sensor.SensorData.Orientation
import org.kmp.shots.k.sensor.SensorData.Proximity
import org.kmp.shots.k.sensor.SensorData.StepCounter
import org.kmp.shots.k.sensor.SensorUpdate.Data
import org.kmp.shots.k.sensor.SensorUpdate.Error
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

internal actual class SensorHandler : SensorController {

    private val motionManager = CMMotionManager()
    private val altimeter = if (CMAltimeter.isRelativeAltitudeAvailable()) CMAltimeter() else null
    private val pedometer = if (CMPedometer.isStepCountingAvailable()) CMPedometer() else null

    private val locationManager = CLLocationManager()
    private var orientationObserver: NSObject? = null

    private var proximityObserver: NSObject? = null

    private var batteryLevelObserver: platform.darwin.NSObjectProtocol? = null
    private var batteryStateObserver: platform.darwin.NSObjectProtocol? = null

    private var timer: NSTimer? = null

    private fun registerBattery(onData: (SensorUpdate) -> Boolean) {
        val device = UIDevice.currentDevice
        device.batteryMonitoringEnabled = true

        fun emitBattery() {
            val levelRaw = device.batteryLevel // -1.0 if unknown
            val percent: Int? = if (levelRaw < 0f) null else (levelRaw * 100f).toInt()
            val state = when (device.batteryState) {
                platform.UIKit.UIDeviceBatteryState.UIDeviceBatteryStateCharging -> SensorData.ChargingState.CHARGING
                platform.UIKit.UIDeviceBatteryState.UIDeviceBatteryStateFull -> SensorData.ChargingState.FULL
                platform.UIKit.UIDeviceBatteryState.UIDeviceBatteryStateUnplugged -> SensorData.ChargingState.DISCHARGING
                else -> SensorData.ChargingState.UNKNOWN
            }
            onData(
                Data(
                    SensorType.BATTERY,
                    SensorData.BatteryStatus(
                        levelPercent = percent,
                        chargingState = state,
                        health = null, // iOS does not expose health
                        temperatureC = null, // iOS does not expose battery temperature via public API
                        platformType = PlatformType.iOS
                    )
                )
            )
        }

        val center = NSNotificationCenter.defaultCenter
        batteryLevelObserver = center.addObserverForName(
            name = platform.UIKit.UIDeviceBatteryLevelDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ -> emitBattery() }

        batteryStateObserver = center.addObserverForName(
            name = platform.UIKit.UIDeviceBatteryStateDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ -> emitBattery() }

        emitBattery()
    }

    @OptIn(ExperimentalForeignApi::class)
    actual override fun registerSensors(
        types: List<SensorType>,
        locationIntervalMillis: Long
    ): Flow<SensorUpdate> = callbackFlow {
        types.forEach { sensorType ->
            when (sensorType) {
                SensorType.ACCELEROMETER -> registerAccelerometer { trySend(it).isSuccess }
                SensorType.GYROSCOPE -> registerGyroscope { trySend(it).isSuccess }
                SensorType.MAGNETOMETER -> registerMagnetometer { trySend(it).isSuccess }
                SensorType.BAROMETER -> registerBarometer { trySend(it).isSuccess }
                SensorType.STEP_COUNTER -> registerStepCounter { trySend(it).isSuccess }
                SensorType.LOCATION -> registerLocation { trySend(it).isSuccess }
                SensorType.DEVICE_ORIENTATION -> registerDeviceOrientation { trySend(it).isSuccess }
                SensorType.PROXIMITY -> registerProximity { trySend(it).isSuccess }
                SensorType.LIGHT -> registerLight { trySend(it).isSuccess }
                SensorType.BATTERY -> registerBattery { trySend(it).isSuccess }
            }.also {
                println("Sensor registered for $sensorType on iOS")
            }
        }

        awaitClose {
            unregisterSensors(types)
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
                SensorType.BATTERY -> {
                    batteryLevelObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
                    batteryStateObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
                    batteryLevelObserver = null
                    batteryStateObserver = null
                    UIDevice.currentDevice.batteryMonitoringEnabled = false
                }
            }.also {
                println("Sensor unregistered for $types on iOS")
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

    @OptIn(ExperimentalForeignApi::class)
    private fun registerAccelerometer(onData: (SensorUpdate) -> Boolean) {
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

    @OptIn(ExperimentalForeignApi::class)
    private fun registerGyroscope(onData: (SensorUpdate) -> Boolean) {
        if (motionManager.gyroAvailable) {
            motionManager.startGyroUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
                data?.let {
                    it.rotationRate.useContents {
                        onData(
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

    @OptIn(ExperimentalForeignApi::class)
    private fun registerMagnetometer(onData: (SensorUpdate) -> Boolean) {
        if (motionManager.magnetometerAvailable) {
            motionManager.startMagnetometerUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
                data?.let {
                    it.magneticField.useContents {
                        onData(
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

    private fun registerBarometer(onData: (SensorUpdate) -> Boolean) {
        altimeter?.startRelativeAltitudeUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
            data?.let {
                val pressure = it.pressure.doubleValue.toFloat()
                onData(
                    Data(
                        SensorType.BAROMETER,
                        Barometer(pressure, PlatformType.iOS)
                    )
                )
            }
        }
    }

    private fun registerStepCounter(onData: (SensorUpdate) -> Boolean) {
        pedometer?.startPedometerUpdatesFromDate(NSDate()) { data, _ ->
            data?.let {
                val steps = it.numberOfSteps.intValue
                onData(
                    Data(
                        SensorType.STEP_COUNTER,
                        StepCounter(steps, PlatformType.iOS)
                    )
                )
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun registerLocation(onData: (SensorUpdate) -> Boolean) {
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
                    onData(Error(Exception(didFailWithError.description)))
                }
            }

        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
    }

    private fun registerDeviceOrientation(onData: (SensorUpdate) -> Boolean) {
        UIDevice.currentDevice.beginGeneratingDeviceOrientationNotifications()

        // Send current orientation immediately
        val initialOrientation =
            UIDevice.currentDevice.orientation.toDeviceOrientation()
        onData(
            Data(
                type = SensorType.DEVICE_ORIENTATION,
                Orientation(
                    orientation = initialOrientation,
                    platformType = PlatformType.iOS
                )
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
                        orientation = mapped,
                        platformType = PlatformType.iOS
                    )
                )
            )
        } as NSObject?
    }

    private fun registerProximity(onData: (SensorUpdate) -> Boolean) {
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
                            isNear = isNear,
                            platformType = PlatformType.iOS
                        )
                    )
                )
            }
        ) as NSObject?
    }

    private fun registerLight(onData: (SensorUpdate) -> Boolean) {
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
                            illuminance = lux,
                            platformType = PlatformType.iOS
                        )
                    )
                )
            }
        )
        NSRunLoop.mainRunLoop.addTimer(timer!!, NSRunLoopCommonModes)
    }
}