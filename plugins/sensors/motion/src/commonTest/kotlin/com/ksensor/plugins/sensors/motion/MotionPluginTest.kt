package com.ksensor.plugins.sensors.motion

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

class FakeMotionPlugin : MotionPlugin {
    override val id: PluginId = PluginId.MOTION
    override val requiredPermissions: List<Permission> = emptyList()

    val activeObservers = mutableSetOf<String>()

    override fun accelerometer(config: SensorConfig): Flow<KSensorResponse<SensorData.Accelerometer>> =
        MutableSharedFlow<KSensorResponse<SensorData.Accelerometer>>().asTrackedFlow("accelerometer")

    override fun gyroscope(config: SensorConfig): Flow<KSensorResponse<SensorData.Gyroscope>> =
        MutableSharedFlow<KSensorResponse<SensorData.Gyroscope>>().asTrackedFlow("gyroscope")

    override fun stepCounter(config: SensorConfig): Flow<KSensorResponse<SensorData.StepCounter>> =
        MutableSharedFlow<KSensorResponse<SensorData.StepCounter>>().asTrackedFlow("stepCounter")

    override fun stepDetector(config: SensorConfig): Flow<KSensorResponse<SensorData.StepDetector>> =
        MutableSharedFlow<KSensorResponse<SensorData.StepDetector>>().asTrackedFlow("stepDetector")

    private fun <T> Flow<T>.asTrackedFlow(name: String): Flow<T> {
        return this.onStart { activeObservers.add(name) }
            .onCompletion { activeObservers.remove(name) }
    }
}

class MotionPluginTest {

    @Test
    fun testAccelerometer() = runTest {
        val fake = FakeMotionPlugin()
        val job = launch { fake.accelerometer().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("accelerometer"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("accelerometer"))
    }

    @Test
    fun testGyroscope() = runTest {
        val fake = FakeMotionPlugin()
        val job = launch { fake.gyroscope().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("gyroscope"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("gyroscope"))
    }

    @Test
    fun testStepCounter() = runTest {
        val fake = FakeMotionPlugin()
        val job = launch { fake.stepCounter().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("stepCounter"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("stepCounter"))
    }

    @Test
    fun testStepDetector() = runTest {
        val fake = FakeMotionPlugin()
        val job = launch { fake.stepDetector().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("stepDetector"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("stepDetector"))
    }
}
