package org.kmp.shots.k.sensor


enum class StateType{
    SCREEN,
    APP_VISIBILITY,
    CONNECTIVITY,
    ACTIVE_NETWORK
}







sealed class StateData {
    data class AppVisibilityStatus(
        val isAppVisible: Boolean,
    ) : StateData()

    data class ScreenStatus(
        val isScreenOn: Boolean,
    ) : StateData()

    data class CurrentActiveNetwork(val activeNetwork: ActiveNetwork) : StateData(){
        enum class ActiveNetwork{
            WIFI,
            CELLULAR,
            NONE
        }
    }
    data class ConnectivityStatus(
        val isConnected: Boolean,
    ) : StateData()
}