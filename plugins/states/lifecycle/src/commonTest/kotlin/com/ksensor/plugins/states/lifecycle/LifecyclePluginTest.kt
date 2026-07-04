package com.ksensor.plugins.states.lifecycle

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

class FakeLifecyclePlugin : LifecyclePlugin {
    override val id: PluginId = PluginId.LIFECYCLE
    override val requiredPermissions: List<Permission> = emptyList()

    val activeObservers = mutableSetOf<String>()

    override fun appVisibility(): StatePlugin<StateData.AppVisibilityStatus> = object : StatePlugin<StateData.AppVisibilityStatus> {
        override val id: PluginId = PluginId.LIFECYCLE
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.AppVisibilityStatus> get() = TODO()
        override fun observe(): Flow<KSensorResponse<StateData.AppVisibilityStatus>> = 
            MutableSharedFlow<KSensorResponse<StateData.AppVisibilityStatus>>().asTrackedFlow("appVisibility")
    }

    private fun <T> Flow<T>.asTrackedFlow(name: String): Flow<T> {
        return this.onStart { activeObservers.add(name) }
            .onCompletion { activeObservers.remove(name) }
    }
}

class LifecyclePluginTest {

    @Test
    fun testAppVisibility() = runTest {
        val fake = FakeLifecyclePlugin()
        val job = launch { fake.appVisibility().observe().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("appVisibility"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("appVisibility"))
    }
}
