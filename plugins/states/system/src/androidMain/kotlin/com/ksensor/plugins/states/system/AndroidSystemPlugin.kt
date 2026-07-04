package com.ksensor.plugins.states.system

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.PowerManager
import com.ksensor.core.Permission
import com.ksensor.core.PluginId
import com.ksensor.core.StatePlugin
import com.ksensor.core.context.KSensorContext
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.StateData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn

class AndroidSystemPlugin : SystemPlugin {
    override val id: PluginId = PluginId.SYSTEM
    override val requiredPermissions: List<Permission> = emptyList()

    private val context: Context by lazy { KSensorContext.get() }
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val batteryFlow by lazy {
        callbackFlow {
            val receiver = BatteryStateReceiver { trySend(KSensorResponse(it)) }
            context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            awaitClose { context.unregisterReceiver(receiver) }
        }.shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
    }

    private val batteryPlugin = object : StatePlugin<StateData.BatteryStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.BatteryStatus>
            get() = KSensorResponse(BatteryStateReceiver.getCurrentStatus(context))

        override fun observe(): Flow<KSensorResponse<StateData.BatteryStatus>> = batteryFlow
    }

    override fun battery(): StatePlugin<StateData.BatteryStatus> = batteryPlugin

    private val volumeFlow by lazy {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        callbackFlow {
            val receiver = VolumeReceiver(audioManager) { trySend(KSensorResponse(StateData.VolumeStatus(it))) }
            context.registerReceiver(receiver, IntentFilter(VOLUME_CHANGED_ACTION))
            awaitClose { context.unregisterReceiver(receiver) }
        }.shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
    }

    private val volumePlugin = object : StatePlugin<StateData.VolumeStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        override val currentState: KSensorResponse<StateData.VolumeStatus>
            get() = KSensorResponse(StateData.VolumeStatus(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)))

        override fun observe(): Flow<KSensorResponse<StateData.VolumeStatus>> = volumeFlow
    }

    override fun volume(): StatePlugin<StateData.VolumeStatus> = volumePlugin

    private val localeFlow by lazy {
        callbackFlow {
            val obs = LocaleReceiver(context) { trySend(KSensorResponse(it)) }
            context.registerReceiver(obs, IntentFilter(Intent.ACTION_LOCALE_CHANGED))
            awaitClose { context.unregisterReceiver(obs) }
        }.shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
    }

    private val localePlugin = object : StatePlugin<StateData.LocaleStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        private val receiver = LocaleReceiver(context) {}
        override val currentState: KSensorResponse<StateData.LocaleStatus>
            get() = KSensorResponse(receiver.getCurrentLocale())

        override fun observe(): Flow<KSensorResponse<StateData.LocaleStatus>> = localeFlow
    }

    override fun locale(): StatePlugin<StateData.LocaleStatus> = localePlugin

    private val screenFlow by lazy {
        callbackFlow {
            val receiver = ScreenStateReceiver(
                onScreenOn = { trySend(KSensorResponse(StateData.ScreenStatus(true))) },
                onScreenOff = { trySend(KSensorResponse(StateData.ScreenStatus(false))) }
            )
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            context.registerReceiver(receiver, filter)
            awaitClose { context.unregisterReceiver(receiver) }
        }.shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
    }

    private val screenPlugin = object : StatePlugin<StateData.ScreenStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.ScreenStatus>
            get() = KSensorResponse(StateData.ScreenStatus(true)) // Approximate

        override fun observe(): Flow<KSensorResponse<StateData.ScreenStatus>> = screenFlow
    }

    override fun screen(): StatePlugin<StateData.ScreenStatus> = screenPlugin

    private val lockFlow by lazy {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        callbackFlow {
            val receiver = ScreenStateReceiver(
                onScreenOff = { trySend(KSensorResponse(StateData.LockStatus(keyguardManager.isDeviceSecure))) },
                onUserPresent = { trySend(KSensorResponse(StateData.LockStatus(false))) }
            )
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            context.registerReceiver(receiver, filter)
            awaitClose { context.unregisterReceiver(receiver) }
        }.shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
    }

    private val lockPlugin = object : StatePlugin<StateData.LockStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        override val currentState: KSensorResponse<StateData.LockStatus>
            get() = KSensorResponse(StateData.LockStatus(keyguardManager.isDeviceLocked))

        override fun observe(): Flow<KSensorResponse<StateData.LockStatus>> = lockFlow
    }

    override fun lock(): StatePlugin<StateData.LockStatus> = lockPlugin

    private val powerSaveFlow by lazy {
        callbackFlow {
            val receiver = PowerSaveReceiver { trySend(KSensorResponse(it)) }
            context.registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
            trySend(KSensorResponse(PowerSaveReceiver.getCurrentStatus(context))) // initial value
            awaitClose { context.unregisterReceiver(receiver) }
        }.shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
    }

    private val powerSavePlugin = object : StatePlugin<StateData.PowerSaveStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.PowerSaveStatus>
            get() = KSensorResponse(PowerSaveReceiver.getCurrentStatus(context))

        override fun observe(): Flow<KSensorResponse<StateData.PowerSaveStatus>> = powerSaveFlow
    }

    override fun powerSave(): StatePlugin<StateData.PowerSaveStatus> = powerSavePlugin
}

actual fun createSystemPlugin(): SystemPlugin = AndroidSystemPlugin()
