package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.darwin.NSObject

internal actual class StateHandler : StateController {
    private var foregroundObserver: NSObject? = null
    private var backgroundObserver: NSObject? = null
    actual override fun addObserver(types: List<StateType>): Flow<StateUpdate> = callbackFlow {
        types.forEach { stateType ->
            when (stateType) {
                StateType.SCREEN_STATE -> trySend(StateUpdate.Error(exception = Exception("iOS dos not have a convince way to check screen state")))
                StateType.APP_VISIBILITY -> observerAppVisibility { trySend(it).isSuccess }
            }.also {
                println("Observer added for $stateType on iOS")
            }
        }
    }

    actual override fun removeObserver(types: List<StateType>) {
        types.forEach { stateType ->
            when (stateType) {
                StateType.SCREEN_STATE -> println("iOS dos not have a convince way to check screen state")
                StateType.APP_VISIBILITY -> {
                    foregroundObserver?.let {
                        NSNotificationCenter.defaultCenter.removeObserver(it)
                    }
                    backgroundObserver?.let {
                        NSNotificationCenter.defaultCenter.removeObserver(it)
                    }
                }
            }.also {
                println("Observer removed for $stateType on iOS")
            }
        }
    }

    @Composable
    actual override fun HandelPermissions(onPermissionStatus: (PermissionStatus) -> Unit) = Unit

    private fun observerAppVisibility(onData: (StateUpdate) -> Boolean) {
        onData(
            StateUpdate.Data(
                type = StateType.APP_VISIBILITY, StateData.AppVisibilityStatus(
                    AppVisibility.INVISIBLE, PlatformType.iOS
                )
            )
        )

        foregroundObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue()
        ) {
            onData(
                StateUpdate.Data(
                    type = StateType.APP_VISIBILITY, StateData.AppVisibilityStatus(
                        AppVisibility.VISIBLE, PlatformType.iOS
                    )
                )
            )
        } as NSObject?

        backgroundObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue()
        ) {
            onData(
                StateUpdate.Data(
                    type = StateType.APP_VISIBILITY, StateData.AppVisibilityStatus(
                        AppVisibility.INVISIBLE, PlatformType.iOS
                    )
                )
            )
        } as NSObject?
    }
}