package com.ksensor.plugins.sensors.positioning

import com.ksensor.core.Permission
import com.ksensor.core.PluginId
import com.ksensor.core.SensorConfig
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import com.ksensor.core.model.StateData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FakePositioningPlugin : PositioningPlugin {
    override val id: PluginId = PluginId.POSITIONING
    override val requiredPermissions: List<Permission> = emptyList()

    val activeObservers = mutableSetOf<String>()

    override fun location(config: SensorConfig): Flow<KSensorResponse<SensorData.Location>> = 
        MutableSharedFlow<KSensorResponse<SensorData.Location>>().asTrackedFlow("location")

    override fun magnetometer(config: SensorConfig): Flow<KSensorResponse<SensorData.Magnetometer>> = 
        MutableSharedFlow<KSensorResponse<SensorData.Magnetometer>>().asTrackedFlow("magnetometer")

    override fun orientation(config: SensorConfig): Flow<KSensorResponse<SensorData.Orientation>> = 
        MutableSharedFlow<KSensorResponse<SensorData.Orientation>>().asTrackedFlow("orientation")

    override fun locationStatus(): StatePlugin<StateData.LocationStatus> = object : StatePlugin<StateData.LocationStatus> {
        override val id: PluginId = PluginId.POSITIONING
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.LocationStatus> get() = TODO()
        override fun observe(): Flow<KSensorResponse<StateData.LocationStatus>> = 
            MutableSharedFlow<KSensorResponse<StateData.LocationStatus>>().asTrackedFlow("locationStatus")
    }

    private fun <T> Flow<T>.asTrackedFlow(name: String): Flow<T> {
        return this.onStart { activeObservers.add(name) }
            .onCompletion { activeObservers.remove(name) }
    }
}

class PositioningPluginTest {

    @Test
    fun testLocation() = runTest {
        val fake = FakePositioningPlugin()
        val job = launch { fake.location().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("location"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("location"))
    }

    @Test
    fun testMagnetometer() = runTest {
        val fake = FakePositioningPlugin()
        val job = launch { fake.magnetometer().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("magnetometer"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("magnetometer"))
    }

    @Test
    fun testOrientation() = runTest {
        val fake = FakePositioningPlugin()
        val job = launch { fake.orientation().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("orientation"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("orientation"))
    }

    @Test
    fun testLocationStatus() = runTest {
        val fake = FakePositioningPlugin()
        val job = launch { fake.locationStatus().observe().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("locationStatus"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("locationStatus"))
    }
}
