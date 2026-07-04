package com.ksensor.plugins.states.bluetooth

import android.content.Context
import com.ksensor.core.Permission
import com.ksensor.core.model.PluginId
import com.ksensor.core.StatePlugin
import com.ksensor.core.context.KSensorContext
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.StateData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

class AndroidBluetoothPlugin : BluetoothPlugin {
    override val id: PluginId = PluginId.BLUETOOTH
    override val requiredPermissions: List<Permission> = listOf(Permission.BLUETOOTH)

    private val context: Context by lazy { KSensorContext.get() }
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val connectionsFlow by lazy {
        callbackFlow {
            val receiver = BleConnectionReceiver(context) { trySend(KSensorResponse(it)) }
            receiver.register()
            awaitClose { receiver.unregister() }
        }.shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
    }

    private val discoveriesFlow by lazy {
        callbackFlow {
            val receiver = BleDiscoversReceiver(context, { trySend(KSensorResponse(it)) }, { close(it) })
            receiver.register()
            awaitClose { receiver.unregister() }
        }.shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
    }

    private val connectionPlugin = object : StatePlugin<StateData.BleConnectionStatus> {
        override val id: PluginId = PluginId.BLUETOOTH
        override val requiredPermissions: List<Permission> = listOf(Permission.BLUETOOTH)
        override val currentState: KSensorResponse<StateData.BleConnectionStatus>
            get() = KSensorResponse(StateData.BleConnectionStatus(emptyList())) // Placeholder

        override fun observe(): Flow<KSensorResponse<StateData.BleConnectionStatus>> = connectionsFlow
    }

    private val discoveriesPlugin = object : StatePlugin<StateData.BleDiscoversStatus> {
        override val id: PluginId = PluginId.BLUETOOTH
        override val requiredPermissions: List<Permission> = listOf(Permission.BLUETOOTH)
        override val currentState: KSensorResponse<StateData.BleDiscoversStatus> = KSensorResponse(StateData.BleDiscoversStatus(emptyList()))
        
        override fun observe(): Flow<KSensorResponse<StateData.BleDiscoversStatus>> = discoveriesFlow
    }

    override fun connections(): StatePlugin<StateData.BleConnectionStatus> = connectionPlugin

    override fun discoveries(): StatePlugin<StateData.BleDiscoversStatus> = discoveriesPlugin
}

actual fun createBluetoothPlugin(): BluetoothPlugin = AndroidBluetoothPlugin()
