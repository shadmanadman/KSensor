package org.kmp.shots.k.sensor


enum class StateType{
    SCREEN_STATE,
    APP_VISIBILITY
}

enum class AppVisibility{
    VISIBLE,
    INVISIBLE
}

enum class ScreenState{
    ON, OFF
}


sealed class StateData{
    data class AppVisibilityStatus(
        val appVisibility: AppVisibility,
    ): StateData()

    data class ScreenStatus(
        val screenState: ScreenState,
    ): StateData()

    data class ConnectivityState(
        val isWiFiOn: Boolean,
        val isMobileDataOn: Boolean,
        val isConnected: Boolean
    )
}