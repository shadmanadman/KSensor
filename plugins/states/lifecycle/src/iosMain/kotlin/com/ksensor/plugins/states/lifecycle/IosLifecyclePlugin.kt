package com.ksensor.plugins.states.lifecycle

import com.ksensor.core.Permission
import com.ksensor.core.model.PluginId
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.StateData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification

class IosLifecyclePlugin : LifecyclePlugin {
    override val id: PluginId = PluginId.LIFECYCLE
    override val requiredPermissions: List<Permission> = emptyList()

    override fun appVisibility(): StatePlugin<StateData.AppVisibilityStatus> = object : StatePlugin<StateData.AppVisibilityStatus> {
        override val id: PluginId = PluginId.LIFECYCLE
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.AppVisibilityStatus> = KSensorResponse(StateData.AppVisibilityStatus(true))

        override fun observe(): Flow<KSensorResponse<StateData.AppVisibilityStatus>> = callbackFlow {
            val foregroundObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                name = UIApplicationWillEnterForegroundNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue
            ) { trySend(KSensorResponse(StateData.AppVisibilityStatus(true))) }

            val backgroundObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                name = UIApplicationDidEnterBackgroundNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue
            ) { trySend(KSensorResponse(StateData.AppVisibilityStatus(false))) }

            awaitClose {
                NSNotificationCenter.defaultCenter.removeObserver(foregroundObserver)
                NSNotificationCenter.defaultCenter.removeObserver(backgroundObserver)
            }
        }
    }
}

actual fun createLifecyclePlugin(): LifecyclePlugin = IosLifecyclePlugin()
