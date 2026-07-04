package com.ksensor.plugins.states.bluetooth

import com.ksensor.core.Permission
import com.ksensor.core.PluginId
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.StateData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FakeBluetoothPlugin : BluetoothPlugin {
    override val id: PluginId = PluginId.BLUETOOTH
    override val requiredPermissions: List<Permission> = emptyList()

    val activeObservers = mutableSetOf<String>()

    override fun connections(): StatePlugin<StateData.BleConnectionStatus> = object : StatePlugin<StateData.BleConnectionStatus> {
        override val id: PluginId = PluginId.BLUETOOTH
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.BleConnectionStatus> get() = TODO()
        override fun observe(): Flow<KSensorResponse<StateData.BleConnectionStatus>> = 
            MutableSharedFlow<KSensorResponse<StateData.BleConnectionStatus>>().asTrackedFlow("connections")
    }

    override fun discoveries(): StatePlugin<StateData.BleDiscoversStatus> = object : StatePlugin<StateData.BleDiscoversStatus> {
        override val id: PluginId = PluginId.BLUETOOTH
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.BleDiscoversStatus> get() = TODO()
        override fun observe(): Flow<KSensorResponse<StateData.BleDiscoversStatus>> = 
            MutableSharedFlow<KSensorResponse<StateData.BleDiscoversStatus>>().asTrackedFlow("discoveries")
    }

    private fun <T> Flow<T>.asTrackedFlow(name: String): Flow<T> {
        return this.onStart { activeObservers.add(name) }
            .onCompletion { activeObservers.remove(name) }
    }
}

class BluetoothPluginTest {

    @Test
    fun testConnections() = runTest {
        val fake = FakeBluetoothPlugin()
        val job = launch { fake.connections().observe().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("connections"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("connections"))
    }

    @Test
    fun testDiscoveries() = runTest {
        val fake = FakeBluetoothPlugin()
        val job = launch { fake.discoveries().observe().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("discoveries"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("discoveries"))
    }
}
