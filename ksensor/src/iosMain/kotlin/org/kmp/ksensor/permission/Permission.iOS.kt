package org.kmp.ksensor.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject

actual fun createPermissionHandler() : PermissionHandler = iOSPermissionHanlder()
private val locationManager = CLLocationManager()

internal class iOSPermissionHanlder : PermissionHandler {
    @Composable
    override fun AskPermission(
        permission: PermissionType,
        permissionStatus: (PermissionStatus) -> Unit
    ) {
        when(permission) {
            PermissionType.LOCATION -> {
                val status =
                    remember { locationManager.authorizationStatus() }
                askLocationPermission(status, permissionStatus)
            }
        }
    }

    @Composable
    override fun OpenSettingsForPermission() {
        NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
            UIApplication.sharedApplication.openURL(it)
        }
    }

}



private fun askLocationPermission(
    status:  CLAuthorizationStatus,
    onPermissionStatus: (PermissionStatus) -> Unit
) {
    when (status) {
        kCLAuthorizationStatusAuthorizedAlways -> {
            onPermissionStatus(PermissionStatus.GRANTED)
        }

        kCLAuthorizationStatusAuthorizedWhenInUse -> {
            onPermissionStatus(PermissionStatus.GRANTED)
        }

        kCLAuthorizationStatusNotDetermined -> {
            requestLocationPermission(onPermissionStatus)
        }
        kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> {
            onPermissionStatus(PermissionStatus.DENIED)
        }
        else -> error("unknown gallery status $status")
    }
}


private fun requestLocationPermission(
    onPermissionStatus: (PermissionStatus) -> Unit
) {
    val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(
            manager: CLLocationManager,
            didChangeAuthorizationStatus: CLAuthorizationStatus
        ) {
            when (didChangeAuthorizationStatus) {
                kCLAuthorizationStatusAuthorizedAlways,
                kCLAuthorizationStatusAuthorizedWhenInUse -> {
                    onPermissionStatus(PermissionStatus.GRANTED)
                }

                kCLAuthorizationStatusDenied,
                kCLAuthorizationStatusRestricted -> {
                    onPermissionStatus(PermissionStatus.DENIED)
                }

                else -> Unit
            }
        }
    }

    locationManager.delegate = delegate
    locationManager.requestWhenInUseAuthorization()
}