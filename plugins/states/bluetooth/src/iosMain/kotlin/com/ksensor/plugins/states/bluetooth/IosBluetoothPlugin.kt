package com.ksensor.plugins.states.bluetooth

import com.ksensor.core.Permission
import com.ksensor.core.model.PluginId
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.StateData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

class IosBluetoothPlugin : BluetoothPlugin {
    override val id: PluginId = PluginId.BLUETOOTH
    override val requiredPermissions: List<Permission> = listOf(Permission.BLUETOOTH)

    override fun connections(): StatePlugin<StateData.BleConnectionStatus> = object : StatePlugin<StateData.BleConnectionStatus> {
        override val id: PluginId = PluginId.BLUETOOTH
        override val requiredPermissions: List<Permission> = listOf(Permission.BLUETOOTH)
        override val currentState: KSensorResponse<StateData.BleConnectionStatus> = KSensorResponse(StateData.BleConnectionStatus(emptyList()))

        override fun observe(): Flow<KSensorResponse<StateData.BleConnectionStatus>> = callbackFlow {
            val receiver = BleConnectionReceiver { trySend(KSensorResponse(it)) }
            receiver.register()
            awaitClose { receiver.unregister() }
        }
    }

    override fun discoveries(): StatePlugin<StateData.BleDiscoversStatus> = object : StatePlugin<StateData.BleDiscoversStatus> {
        override val id: PluginId = PluginId.BLUETOOTH
        override val requiredPermissions: List<Permission> = listOf(Permission.BLUETOOTH)
        override val currentState: KSensorResponse<StateData.BleDiscoversStatus> = KSensorResponse(StateData.BleDiscoversStatus(emptyList()))

        override fun observe(): Flow<KSensorResponse<StateData.BleDiscoversStatus>> = callbackFlow {
            val receiver = BleDiscoversReceiver({ trySend(KSensorResponse(it)) }, { close(it) })
            receiver.register()
            awaitClose { receiver.unregister() }
        }
    }

    override fun audioDevices(): StatePlugin<StateData.BleConnectionStatus> = object : StatePlugin<StateData.BleConnectionStatus> {
        override val id: PluginId = PluginId.BLUETOOTH
        override val requiredPermissions: List<Permission> = listOf(Permission.BLUETOOTH)
        override val currentState: KSensorResponse<StateData.BleConnectionStatus> = KSensorResponse(StateData.BleConnectionStatus(emptyList()))

        override fun observe(): Flow<KSensorResponse<StateData.BleConnectionStatus>> =
            connections().observe().map { response ->
                KSensorResponse(StateData.BleConnectionStatus(response.data.connectedDevices.filter { it.isAudio }))
            }
    }
}

actual fun createBluetoothPlugin(): BluetoothPlugin = IosBluetoothPlugin()
