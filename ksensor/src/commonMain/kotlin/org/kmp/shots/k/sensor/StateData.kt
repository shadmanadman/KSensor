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
        val platformType: PlatformType
    ): StateData()

    data class ScreenStatus(
        val screenState: ScreenState,
        val platformType: PlatformType
    ): StateData()

}