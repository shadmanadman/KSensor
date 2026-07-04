package com.ksensor.plugins.sensors.environment

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

class FakeEnvironmentPlugin : EnvironmentPlugin {
    override val id: PluginId = PluginId.ENVIRONMENT
    override val requiredPermissions: List<Permission> = emptyList()

    val activeObservers = mutableSetOf<String>()

    override fun barometer(config: SensorConfig): Flow<KSensorResponse<SensorData.Barometer>> =
        MutableSharedFlow<KSensorResponse<SensorData.Barometer>>().asTrackedFlow("barometer")

    override fun light(config: SensorConfig): Flow<KSensorResponse<SensorData.LightIlluminance>> =
        MutableSharedFlow<KSensorResponse<SensorData.LightIlluminance>>().asTrackedFlow("light")

    override fun proximity(config: SensorConfig): Flow<KSensorResponse<SensorData.Proximity>> =
        MutableSharedFlow<KSensorResponse<SensorData.Proximity>>().asTrackedFlow("proximity")

    private fun <T> Flow<T>.asTrackedFlow(name: String): Flow<T> {
        return this.onStart { activeObservers.add(name) }
            .onCompletion { activeObservers.remove(name) }
    }
}

class EnvironmentPluginTest {

    @Test
    fun testBarometer() = runTest {
        val fake = FakeEnvironmentPlugin()
        val job = launch { fake.barometer().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("barometer"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("barometer"))
    }

    @Test
    fun testLight() = runTest {
        val fake = FakeEnvironmentPlugin()
        val job = launch { fake.light().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("light"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("light"))
    }

    @Test
    fun testProximity() = runTest {
        val fake = FakeEnvironmentPlugin()
        val job = launch { fake.proximity().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("proximity"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("proximity"))
    }
}
