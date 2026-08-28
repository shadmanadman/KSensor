package com.ksensor.plugins.states.system

import com.ksensor.core.Permission
import com.ksensor.core.model.PluginId
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.StateData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationProtectedDataDidBecomeAvailable
import platform.UIKit.UIApplicationProtectedDataWillBecomeUnavailable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class IosSystemPlugin : SystemPlugin {
    override val id: PluginId = PluginId.SYSTEM
    override val requiredPermissions: List<Permission> = emptyList()

    override fun battery(): StatePlugin<StateData.BatteryStatus> = object : StatePlugin<StateData.BatteryStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        private val receiver = BatteryStateReceiver {}
        override val currentState: KSensorResponse<StateData.BatteryStatus> 
            get() = KSensorResponse(receiver.getCurrentStatus())

        override fun observe(): Flow<KSensorResponse<StateData.BatteryStatus>> = callbackFlow {
            val obs = BatteryStateReceiver { trySend(KSensorResponse(it)) }
            obs.register()
            awaitClose { obs.unregister() }
        }
    }

    override fun volume(): StatePlugin<StateData.VolumeStatus> = object : StatePlugin<StateData.VolumeStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        private val receiver = VolumeReceiver {}
        override val currentState: KSensorResponse<StateData.VolumeStatus> 
            get() = KSensorResponse(StateData.VolumeStatus(receiver.getCurrentVolume()))

        override fun observe(): Flow<KSensorResponse<StateData.VolumeStatus>> = callbackFlow {
            val obs = VolumeReceiver { trySend(KSensorResponse(StateData.VolumeStatus(it))) }
            obs.register()
            awaitClose { obs.unregister() }
        }
    }

    override fun locale(): StatePlugin<StateData.LocaleStatus> = object : StatePlugin<StateData.LocaleStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        private val receiver = LocaleReceiver {}
        override val currentState: KSensorResponse<StateData.LocaleStatus> 
            get() = KSensorResponse(receiver.getCurrentLocale())

        override fun observe(): Flow<KSensorResponse<StateData.LocaleStatus>> = callbackFlow {
            val obs = LocaleReceiver { trySend(KSensorResponse(it)) }
            obs.register()
            awaitClose { obs.unregister() }
        }
    }

    override fun screen(): StatePlugin<StateData.ScreenStatus> = object : StatePlugin<StateData.ScreenStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.ScreenStatus> 
            get() = KSensorResponse(StateData.ScreenStatus(true))

        override fun observe(): Flow<KSensorResponse<StateData.ScreenStatus>> = callbackFlow {
            val obs = ScreenStateReceiver { trySend(KSensorResponse(StateData.ScreenStatus(it))) }
            obs.register()
            awaitClose { obs.unregister() }
        }
    }

    override fun brightness(): StatePlugin<StateData.BrightnessStatus> = object : StatePlugin<StateData.BrightnessStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        private val receiver = BrightnessReceiver {}
        override val currentState: KSensorResponse<StateData.BrightnessStatus>
            get() = KSensorResponse(receiver.getCurrentStatus())

        override fun observe(): Flow<KSensorResponse<StateData.BrightnessStatus>> = callbackFlow {
            val obs = BrightnessReceiver { trySend(KSensorResponse(it)) }
            obs.register()
            awaitClose { obs.unregister() }
        }
    }

    override fun lock(): StatePlugin<StateData.LockStatus> = object : StatePlugin<StateData.LockStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.LockStatus> 
            get() = KSensorResponse(StateData.LockStatus(!UIApplication.sharedApplication.isProtectedDataAvailable()))

        override fun observe(): Flow<KSensorResponse<StateData.LockStatus>> = callbackFlow {
            val lockObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                name = UIApplicationProtectedDataWillBecomeUnavailable,
                `object` = null,
                queue = NSOperationQueue.mainQueue
            ) { trySend(KSensorResponse(StateData.LockStatus(true))) }

            val unlockObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                name = UIApplicationProtectedDataDidBecomeAvailable,
                `object` = null,
                queue = NSOperationQueue.mainQueue
            ) { trySend(KSensorResponse(StateData.LockStatus(false))) }

            awaitClose {
                NSNotificationCenter.defaultCenter.removeObserver(lockObserver)
                NSNotificationCenter.defaultCenter.removeObserver(unlockObserver)
            }
        }
    }

    override fun powerSave(): StatePlugin<StateData.PowerSaveStatus> = object : StatePlugin<StateData.PowerSaveStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        private val receiver = PowerSaveReceiver {}
        override val currentState: KSensorResponse<StateData.PowerSaveStatus>
            get() = KSensorResponse(receiver.getCurrentStatus())

        override fun observe(): Flow<KSensorResponse<StateData.PowerSaveStatus>> = callbackFlow {
            val obs = PowerSaveReceiver { trySend(KSensorResponse(it)) }
            obs.register()
            awaitClose { obs.unregister() }
        }
    }

    override fun storage(interval: Duration): StatePlugin<StateData.StorageStatus> = object : StatePlugin<StateData.StorageStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.StorageStatus>
            get() = KSensorResponse(StorageProvider.getCurrentStatus())

        override fun observe(): Flow<KSensorResponse<StateData.StorageStatus>> = flow {
            while (true) {
                emit(KSensorResponse(StorageProvider.getCurrentStatus()))
                delay(interval)
            }
        }
    }

    override fun resources(interval: Duration): StatePlugin<StateData.ResourcesStatus> = object : StatePlugin<StateData.ResourcesStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.ResourcesStatus>
            get() = KSensorResponse(ResourcesProvider.getCurrentStatus())

        override fun observe(): Flow<KSensorResponse<StateData.ResourcesStatus>> = flow {
            while (true) {
                emit(KSensorResponse(ResourcesProvider.getCurrentStatus()))
                delay(interval)
            }
        }
    }
}

actual fun createSystemPlugin(): SystemPlugin = IosSystemPlugin()
