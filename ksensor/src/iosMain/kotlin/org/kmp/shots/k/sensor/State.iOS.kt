package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.kmp.shots.k.sensor.StateUpdate.Data
import org.kmp.shots.k.sensor.StateUpdate.Error
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Network.nw_path_monitor_create
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.darwin.NSObject

actual fun createController(): StateController = IOSStateHandler()
internal class IOSStateHandler : StateController {
    private var foregroundObserver: NSObject? = null
    private var backgroundObserver: NSObject? = null
    private val monitor = nw_path_monitor_create()

    private lateinit var locationProviderReceiver: LocationProviderReceiver
    private val connectivityMonitor = ConnectivityMonitor
    private val volumeReceiver = VolumeReceiver()
    override fun addObserver(types: List<StateType>): Flow<StateUpdate> = callbackFlow {
        types.forEach { stateType ->
            when (stateType) {
                StateType.SCREEN -> trySend(Error(exception = Exception("iOS dos not have a convince way to check screen state")))
                StateType.APP_VISIBILITY -> observerAppVisibility { trySend(it).isSuccess }
                StateType.CONNECTIVITY, StateType.ACTIVE_NETWORK -> observeConnectivity { trySend(it).isSuccess }
                StateType.LOCATION -> observeLocation { trySend(it).isSuccess }
                StateType.VOLUME -> observeVolume { trySend(it).isSuccess }
            }.also {
                println("Observer added for $stateType on iOS")
            }
        }
        awaitClose { removeObserver(types) }
    }

    override fun removeObserver(types: List<StateType>) {
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

                StateType.CONNECTIVITY, StateType.ACTIVE_NETWORK -> connectivityMonitor.unregister(
                    monitor
                )

                StateType.LOCATION -> locationProviderReceiver.dispose()
                StateType.VOLUME -> volumeReceiver.removeObserver()
            }.also {
                println("Observer removed for $stateType on iOS")
            }
        }
    }

    @Composable
    override fun HandelPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    ) = Unit

    private fun observeLocation(onData: (StateUpdate) -> Boolean) {
        locationProviderReceiver = LocationProviderReceiver {
            onData(
                Data(
                    type = StateType.LOCATION, data = StateData.LocationStatus(it),
                    PlatformType.iOS
                )
            )
        }
    }

    private fun observeVolume(onData: (StateUpdate) -> Unit) {
        onData(
            Data(
                type = StateType.VOLUME,
                data = StateData.VolumeStatus(volumeReceiver.getCurrentVolume()),
                platformType = PlatformType.iOS
            )
        )

        volumeReceiver.registerObserver {
            Data(
                type = StateType.VOLUME,
                data = StateData.VolumeStatus(it),
                platformType = PlatformType.iOS
            )
        }
    }

    private fun observeConnectivity(onData: (StateUpdate) -> Boolean) {
        connectivityMonitor.register(monitor, isConnected = {
            onData(
                Data(
                    type = StateType.CONNECTIVITY,
                    data = StateData.ConnectivityStatus(isConnected = it),
                    platformType = PlatformType.iOS
                )
            )
        }, currentActiveNetwork = {
            onData(
                Data(
                    type = StateType.ACTIVE_NETWORK,
                    data = StateData.CurrentActiveNetwork(it),
                    platformType = PlatformType.iOS
                )
            )
        })
    }

    private fun observerAppVisibility(onData: (StateUpdate) -> Boolean) {
        onData(
            Data(
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
                Data(
                    type = StateType.APP_VISIBILITY, StateData.AppVisibilityStatus(
                        false
                    ),
                    PlatformType.iOS
                )
            )
        } as NSObject?
    }
}