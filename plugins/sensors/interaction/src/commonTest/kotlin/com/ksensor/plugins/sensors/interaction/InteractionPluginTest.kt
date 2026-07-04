package com.ksensor.plugins.sensors.interaction

import com.ksensor.core.Permission
import com.ksensor.core.PluginId
import com.ksensor.core.SensorConfig
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FakeInteractionPlugin : InteractionPlugin {
    override val id: PluginId = PluginId.INTERACTION
    override val requiredPermissions: List<Permission> = emptyList()

    val activeObservers = mutableSetOf<String>()

    override fun touchGestures(config: SensorConfig): Flow<KSensorResponse<SensorData.TouchGestures>> = 
        MutableSharedFlow<KSensorResponse<SensorData.TouchGestures>>().asTrackedFlow("touchGestures")

    private fun <T> Flow<T>.asTrackedFlow(name: String): Flow<T> {
        return this.onStart { activeObservers.add(name) }
            .onCompletion { activeObservers.remove(name) }
    }
}

class InteractionPluginTest {

    @Test
    fun testTouchGestures() = runTest {
        val fake = FakeInteractionPlugin()
        val job = launch { fake.touchGestures().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("touchGestures"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("touchGestures"))
    }
}
