package com.ksensor.plugins.states.network

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

class FakeNetworkPlugin : NetworkPlugin {
    override val id: PluginId = PluginId.NETWORK
    override val requiredPermissions: List<Permission> = emptyList()

    val activeObservers = mutableSetOf<String>()

    override fun connectivity(): StatePlugin<StateData.ConnectivityStatus> = object : StatePlugin<StateData.ConnectivityStatus> {
        override val id: PluginId = PluginId.NETWORK
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.ConnectivityStatus> get() = TODO()
        override fun observe(): Flow<KSensorResponse<StateData.ConnectivityStatus>> = 
            MutableSharedFlow<KSensorResponse<StateData.ConnectivityStatus>>().asTrackedFlow("connectivity")
    }

    override fun activeNetwork(): StatePlugin<StateData.CurrentActiveNetwork> = object : StatePlugin<StateData.CurrentActiveNetwork> {
        override val id: PluginId = PluginId.NETWORK
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.CurrentActiveNetwork> get() = TODO()
        override fun observe(): Flow<KSensorResponse<StateData.CurrentActiveNetwork>> = 
            MutableSharedFlow<KSensorResponse<StateData.CurrentActiveNetwork>>().asTrackedFlow("activeNetwork")
    }

    private fun <T> Flow<T>.asTrackedFlow(name: String): Flow<T> {
        return this.onStart { activeObservers.add(name) }
            .onCompletion { activeObservers.remove(name) }
    }
}

class NetworkPluginTest {

    @Test
    fun testConnectivity() = runTest {
        val fake = FakeNetworkPlugin()
        val job = launch { fake.connectivity().observe().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("connectivity"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("connectivity"))
    }

    @Test
    fun testActiveNetwork() = runTest {
        val fake = FakeNetworkPlugin()
        val job = launch { fake.activeNetwork().observe().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("activeNetwork"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("activeNetwork"))
    }
}
