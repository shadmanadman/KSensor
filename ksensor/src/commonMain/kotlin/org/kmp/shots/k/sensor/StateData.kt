package org.kmp.shots.k.sensor


enum class StateType{
    SCREEN,
    APP_VISIBILITY,
    CONNECTIVITY,
    ACTIVE_NETWORK
}







sealed class StateData {
    data class AppVisibilityStatus(
        val appVisibility: AppVisibility,
    ) : StateData() {
        enum class AppVisibility {
            VISIBLE,
            INVISIBLE
        }
    }

    data class ScreenStatus(
        val screenState: ScreenState,
    ) : StateData() {
        enum class ScreenState {
            ON, OFF
        }
    }

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