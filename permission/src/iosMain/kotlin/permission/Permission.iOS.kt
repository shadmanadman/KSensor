package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import permission.PermissionHandler
import permission.PermissionStatus
import permission.PermissionType
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSURL
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject

expect fun createHanlder() = iOSPermissionHanlder()
private val locationManager = CLLocationManager()

internal actual class iOSPermissionHanlder : PermissionHandler {
    @Composable
    actual override fun askPermission(
        permission: PermissionType,
        permissionStatus: (PermissionStatus) -> Unit
    ) {
        when(permission) {
            permission.PermissionType.LOCATION -> {
                val status =
                    remember { locationManager.authorizationStatus() }
                askLocationPermission(status, permissionStatus)
            }
        }
    }

    @Composable
    actual override fun launchSettings() {
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