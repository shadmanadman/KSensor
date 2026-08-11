package com.ksensor.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.AVFoundation.*
import platform.HealthKit.*
import platform.darwin.NSObject
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import kotlin.coroutines.resume

internal class IosPermissionHandler : PermissionHandler {
    private val locationManager = CLLocationManager()

    override fun hasPermission(permission: Permission): Boolean {
        return when (permission) {
            Permission.LOCATION -> {
                val status = locationManager.authorizationStatus()
                status == kCLAuthorizationStatusAuthorizedAlways || status == kCLAuthorizationStatusAuthorizedWhenInUse
            }
            Permission.BLUETOOTH -> true // Placeholder
            Permission.CAMERA -> {
                val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
                status == AVAuthorizationStatusAuthorized
            }
            Permission.BODY_SENSORS -> {
                if (HKHealthStore.isHealthDataAvailable()) {
                    val store = HKHealthStore()
                    val type = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierHeartRate)!!
                    store.authorizationStatusForType(type) == HKAuthorizationStatusSharingAuthorized
                } else false
            }
            else -> true
        }
    }

    override suspend fun requestPermission(permission: Permission): Boolean = suspendCancellableCoroutine { continuation ->
        when (permission) {
            Permission.LOCATION -> {
                val status = locationManager.authorizationStatus()
                if (status != kCLAuthorizationStatusNotDetermined) {
                    continuation.resume(hasPermission(permission))
                    return@suspendCancellableCoroutine
                }

                val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                    override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
                        if (didChangeAuthorizationStatus != kCLAuthorizationStatusNotDetermined) {
                            manager.delegate = null
                            continuation.resume(
                                didChangeAuthorizationStatus == kCLAuthorizationStatusAuthorizedAlways ||
                                        didChangeAuthorizationStatus == kCLAuthorizationStatusAuthorizedWhenInUse
                            )
                        }
                    }
                }
                locationManager.delegate = delegate
                locationManager.requestWhenInUseAuthorization()
                continuation.invokeOnCancellation {
                    locationManager.delegate = null
                }
            }
            Permission.CAMERA -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    continuation.resume(granted)
                }
            }
            Permission.BODY_SENSORS -> {
                if (HKHealthStore.isHealthDataAvailable()) {
                    val store = HKHealthStore()
                    val type = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierHeartRate)!!
                    val types = setOf(type)
                    store.requestAuthorizationToShareTypes(null, types) { success, _ ->
                        continuation.resume(success)
                    }
                } else {
                    continuation.resume(false)
                }
            }
            else -> continuation.resume(true)
        }
    }

    @Composable
    override fun AskPermission(permission: Permission, onStatus: (PermissionStatus) -> Unit) {
        when (permission) {
            Permission.LOCATION -> {
                val status = remember { locationManager.authorizationStatus() }
                when (status) {
                    kCLAuthorizationStatusAuthorizedAlways,
                    kCLAuthorizationStatusAuthorizedWhenInUse -> {
                        onStatus(PermissionStatus.GRANTED)
                    }
                    kCLAuthorizationStatusNotDetermined -> {
                        val delegate = remember {
                            object : NSObject(), CLLocationManagerDelegateProtocol {
                                override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
                                    when (didChangeAuthorizationStatus) {
                                        kCLAuthorizationStatusAuthorizedAlways,
                                        kCLAuthorizationStatusAuthorizedWhenInUse -> onStatus(PermissionStatus.GRANTED)
                                        kCLAuthorizationStatusDenied,
                                        kCLAuthorizationStatusRestricted -> onStatus(PermissionStatus.DENIED)
                                        else -> Unit
                                    }
                                }
                            }
                        }
                        locationManager.delegate = delegate
                        locationManager.requestWhenInUseAuthorization()
                    }
                    kCLAuthorizationStatusDenied,
                    kCLAuthorizationStatusRestricted -> {
                        onStatus(PermissionStatus.DENIED)
                    }
                    else -> onStatus(PermissionStatus.UNKNOWN)
                }
            }
            Permission.BLUETOOTH -> {
                onStatus(PermissionStatus.GRANTED)
            }
            Permission.CAMERA -> {
                val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
                when (status) {
                    AVAuthorizationStatusAuthorized -> onStatus(PermissionStatus.GRANTED)
                    AVAuthorizationStatusNotDetermined -> {
                        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                            if (granted) onStatus(PermissionStatus.GRANTED) else onStatus(PermissionStatus.DENIED)
                        }
                    }
                    AVAuthorizationStatusDenied,
                    AVAuthorizationStatusRestricted -> onStatus(PermissionStatus.DENIED)
                    else -> onStatus(PermissionStatus.UNKNOWN)
                }
            }
            Permission.BODY_SENSORS -> {
                if (HKHealthStore.isHealthDataAvailable()) {
                    val store = HKHealthStore()
                    val type = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierHeartRate)!!
                    val types = setOf(type)
                    store.requestAuthorizationToShareTypes(null, types) { success, _ ->
                        if (success) onStatus(PermissionStatus.GRANTED) else onStatus(PermissionStatus.DENIED)
                    }
                } else {
                    onStatus(PermissionStatus.DENIED)
                }
            }
            else -> onStatus(PermissionStatus.GRANTED)
        }
    }
}

actual fun createPermissionHandler(): PermissionHandler = IosPermissionHandler()
