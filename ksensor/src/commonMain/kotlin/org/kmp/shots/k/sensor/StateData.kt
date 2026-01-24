package org.kmp.shots.k.sensor


enum class StateType {
    SCREEN,
    APP_VISIBILITY,
    CONNECTIVITY,
    ACTIVE_NETWORK,
    LOCATION,
    VOLUME
}


sealed class StateData {
    data class AppVisibilityStatus(
        val isAppVisible: Boolean,
    ) : StateData()

    data class LocationStatus(
        val isLocationOn: Boolean
    ) : StateData()

    data class ScreenStatus(
        val isScreenOn: Boolean,
    ) : StateData()

    data class CurrentActiveNetwork(val activeNetwork: ActiveNetwork) : StateData() {
        enum class ActiveNetwork {
            WIFI,
            CELLULAR,
            NONE
        }
    }

    data class VolumeStatus(val volumePercentage: Int): StateData()
    data class ConnectivityStatus(
        val isConnected: Boolean,
    ) : StateData()
}