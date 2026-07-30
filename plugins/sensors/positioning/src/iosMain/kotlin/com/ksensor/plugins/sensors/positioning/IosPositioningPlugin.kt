package com.ksensor.plugins.sensors.positioning

import com.ksensor.core.Permission
import com.ksensor.core.model.PluginId
import com.ksensor.core.SensorConfig
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.DeviceOrientation
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import com.ksensor.core.model.StateData
import com.ksensor.core.model.Vector3
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSError
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIDeviceOrientationDidChangeNotification
import platform.darwin.NSObject

class IosPositioningPlugin : PositioningPlugin {
    override val id: PluginId = PluginId.POSITIONING
    override val requiredPermissions: List<Permission> = listOf(Permission.LOCATION)

    private val locationManager = CLLocationManager()
    private val motionManager = CMMotionManager()

    @OptIn(ExperimentalForeignApi::class)
    override fun location(config: SensorConfig): Flow<KSensorResponse<SensorData.Location>> = callbackFlow {
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val loc = didUpdateLocations.lastOrNull() as? CLLocation
                loc?.let {
                    it.coordinate.useContents {
                        val sensorData = SensorData.Location(latitude, longitude, it.altitude)
                        trySend(KSensorResponse(sensorData))
                    }
                }
            }
            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {}
        }
        locationManager.delegate = delegate
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
        awaitClose { locationManager.stopUpdatingLocation() }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun magnetometer(config: SensorConfig): Flow<KSensorResponse<SensorData.Magnetometer>> = callbackFlow {
        if (!motionManager.magnetometerAvailable) {
            close()
            return@callbackFlow
        }
        motionManager.magnetometerUpdateInterval = config.intervalMs / 1000.0
        motionManager.startMagnetometerUpdatesToQueue(NSOperationQueue.mainQueue()) { data, _ ->
            data?.magneticField?.useContents {
                val sensorData = SensorData.Magnetometer(Vector3(x.toFloat(), y.toFloat(), z.toFloat()))
                trySend(KSensorResponse(sensorData))
            }
        }
        awaitClose { motionManager.stopMagnetometerUpdates() }
    }

    override fun orientation(config: SensorConfig): Flow<KSensorResponse<SensorData.Orientation>> = callbackFlow {
        UIDevice.currentDevice.beginGeneratingDeviceOrientationNotifications()
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIDeviceOrientationDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) {
            val orientation = UIDevice.currentDevice.orientation
            val mapped = when (orientation) {
                UIDeviceOrientation.UIDeviceOrientationPortrait,
                UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> DeviceOrientation.PORTRAIT
                UIDeviceOrientation.UIDeviceOrientationLandscapeLeft,
                UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> DeviceOrientation.LANDSCAPE
                else -> DeviceOrientation.UNKNOWN
            }
            val sensorData = SensorData.Orientation(mapped, orientation.value.toInt())
            trySend(KSensorResponse(sensorData))
        }
        awaitClose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
            UIDevice.currentDevice.endGeneratingDeviceOrientationNotifications()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun heading(config: SensorConfig): Flow<KSensorResponse<SensorData.Heading>> = callbackFlow {
        val headingManager = CLLocationManager()
        var lastMagneticHeading = 0.0
        var lastTrueHeading = 0.0
        var lastDeviceHeading = 0.0
        var lastCourseOverGround = 0.0

        fun sendHeading() {
            trySend(KSensorResponse(SensorData.Heading(
                magneticHeading = lastMagneticHeading,
                trueHeading = lastTrueHeading,
                deviceHeading = lastDeviceHeading,
                courseOverGround = lastCourseOverGround
            )))
        }

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
                lastMagneticHeading = didUpdateHeading.magneticHeading
                lastTrueHeading = didUpdateHeading.trueHeading
                lastDeviceHeading = didUpdateHeading.magneticHeading // Azimuth
                sendHeading()
            }

            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val loc = didUpdateLocations.lastOrNull() as? CLLocation
                loc?.let {
                    if (it.course >= 0) {
                        lastCourseOverGround = it.course
                        sendHeading()
                    }
                }
            }
        }

        headingManager.delegate = delegate
        headingManager.startUpdatingLocation()
        headingManager.startUpdatingHeading()

        awaitClose {
            headingManager.stopUpdatingLocation()
            headingManager.stopUpdatingHeading()
        }
    }

    override fun locationStatus(): StatePlugin<StateData.LocationStatus> = object : StatePlugin<StateData.LocationStatus> {
        override val id: PluginId = PluginId.POSITIONING
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.LocationStatus>
            get() = KSensorResponse(StateData.LocationStatus(isLocationCurrentlyEnabled()))

        override fun observe(): Flow<KSensorResponse<StateData.LocationStatus>> = callbackFlow {
            val receiver = LocationProviderReceiver {
                trySend(KSensorResponse(StateData.LocationStatus(it)))
            }
            awaitClose { receiver.dispose() }
        }

        private fun isLocationCurrentlyEnabled(): Boolean {
            return CLLocationManager.locationServicesEnabled() &&
                    CLLocationManager.authorizationStatus() != kCLAuthorizationStatusDenied &&
                    CLLocationManager.authorizationStatus() != kCLAuthorizationStatusRestricted
        }
    }
}

actual fun createPositioningPlugin(): PositioningPlugin = IosPositioningPlugin()
