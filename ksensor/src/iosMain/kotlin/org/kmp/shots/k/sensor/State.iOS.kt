package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.kmp.shots.k.sensor.StateUpdate.Data
import org.kmp.shots.k.sensor.StateUpdate.Error
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Network.nw_interface_type_cellular
import platform.Network.nw_interface_type_wifi
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_uses_interface_type
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.darwin.NSObject
import platform.darwin.dispatch_queue_create

internal actual class StateHandler : StateController {
    private var foregroundObserver: NSObject? = null
    private var backgroundObserver: NSObject? = null
    private val monitor = nw_path_monitor_create()
    private val queue = dispatch_queue_create("NetworkMonitor", null)

    actual override fun addObserver(types: List<StateType>): Flow<StateUpdate> = callbackFlow {
        types.forEach { stateType ->
            when (stateType) {
                StateType.SCREEN -> trySend(Error(exception = Exception("iOS dos not have a convince way to check screen state")))
                StateType.APP_VISIBILITY -> observerAppVisibility { trySend(it).isSuccess }
                StateType.CONNECTIVITY, StateType.ACTIVE_NETWORK -> observeConnectivity { trySend(it).isSuccess }
            }.also {
                println("Observer added for $stateType on iOS")
            }
        }
    }

    actual override fun removeObserver(types: List<StateType>) {
        types.forEach { stateType ->
            when (stateType) {
                StateType.SCREEN -> println("iOS dos not have a convince way to check screen state")
                StateType.APP_VISIBILITY -> {
                    foregroundObserver?.let {
                        NSNotificationCenter.defaultCenter.removeObserver(it)
                    }
                    backgroundObserver?.let {
                        NSNotificationCenter.defaultCenter.removeObserver(it)
                    }
                }

                StateType.CONNECTIVITY, StateType.ACTIVE_NETWORK -> nw_path_monitor_cancel(monitor)
            }.also {
                println("Observer removed for $stateType on iOS")
            }
        }
    }

    @Composable
    actual override fun HandelPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    ) = Unit

    private fun observeConnectivity(onData: (StateUpdate) -> Boolean) {
        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = nw_path_get_status(path)
            val isConnected = status == nw_path_status_satisfied
            onData(
                Data(
                    type = StateType.CONNECTIVITY,
                    StateData.ConnectivityStatus(
                        isConnected = isConnected
                    ),
                    PlatformType.Android
                )
            )

            val activeNetwork = if (nw_path_uses_interface_type(path, nw_interface_type_wifi))
                StateData.CurrentActiveNetwork.ActiveNetwork.WIFI
            else if (nw_path_uses_interface_type(path, nw_interface_type_cellular))
                StateData.CurrentActiveNetwork.ActiveNetwork.CELLULAR
            else
                StateData.CurrentActiveNetwork.ActiveNetwork.NONE

            onData(
                Data(
                    type = StateType.ACTIVE_NETWORK,
                    StateData.CurrentActiveNetwork(
                        activeNetwork = activeNetwork
                    ),
                    PlatformType.Android
                )
            )
        }

        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_start(monitor)
    }

    private fun observerAppVisibility(onData: (StateUpdate) -> Boolean) {
        onData(
            StateUpdate.Data(
                type = StateType.APP_VISIBILITY, StateData.AppVisibilityStatus(
                    false
                ),
                PlatformType.iOS
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
                        true
                    ),
                    PlatformType.iOS
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
                        false
                    ),
                    PlatformType.iOS
                )
            )
        } as NSObject?
    }
}