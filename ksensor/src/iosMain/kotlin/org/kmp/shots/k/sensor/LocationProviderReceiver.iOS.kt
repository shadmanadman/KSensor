package org.kmp.shots.k.sensor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.darwin.NSObject

class LocationProviderReceiver(private val isLocationOn:(Boolean)-> Unit) : NSObject(), CLLocationManagerDelegateProtocol {

    private val locationManager = CLLocationManager()
    private val _isLocationEnabled = MutableStateFlow(isLocationCurrentlyEnabled())
    val isLocationEnabled: StateFlow<Boolean> = _isLocationEnabled

    init {
        locationManager.delegate = this
        locationManager.requestWhenInUseAuthorization()
        isLocationOn(isLocationCurrentlyEnabled())
    }

    private fun isLocationCurrentlyEnabled(): Boolean {
        return CLLocationManager.locationServicesEnabled() &&
                CLLocationManager.authorizationStatus() != kCLAuthorizationStatusDenied &&
                CLLocationManager.authorizationStatus() != kCLAuthorizationStatusRestricted
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        isLocationOn(isLocationCurrentlyEnabled())
    }

    fun dispose(){
        locationManager.delegate = null
    }
}