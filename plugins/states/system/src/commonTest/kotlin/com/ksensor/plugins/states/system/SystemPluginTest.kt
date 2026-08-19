package com.ksensor.plugins.states.system

import com.ksensor.core.Permission
import com.ksensor.core.model.PluginId
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.StateData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlin.time.Duration
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FakeSystemPlugin : SystemPlugin {
    override val id: PluginId = PluginId.SYSTEM
    override val requiredPermissions: List<Permission> = emptyList()

    val activeObservers = mutableSetOf<String>()

    override fun battery(): StatePlugin<StateData.BatteryStatus> = object : StatePlugin<StateData.BatteryStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.BatteryStatus> get() = KSensorResponse(StateData.BatteryStatus(100, StateData.BatteryStatus.ChargingState.FULL, StateData.BatteryStatus.BatteryHealth.GOOD, 30f))
        override fun observe(): Flow<KSensorResponse<StateData.BatteryStatus>> = 
            MutableSharedFlow<KSensorResponse<StateData.BatteryStatus>>().asTrackedFlow("battery")
    }

    override fun volume(): StatePlugin<StateData.VolumeStatus> = object : StatePlugin<StateData.VolumeStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.VolumeStatus> get() = KSensorResponse(StateData.VolumeStatus(50))
        override fun observe(): Flow<KSensorResponse<StateData.VolumeStatus>> = 
            MutableSharedFlow<KSensorResponse<StateData.VolumeStatus>>().asTrackedFlow("volume")
    }

    override fun locale(): StatePlugin<StateData.LocaleStatus> = object : StatePlugin<StateData.LocaleStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.LocaleStatus> get() = KSensorResponse(StateData.LocaleStatus("en", "US", "en_US", "English", false))
        override fun observe(): Flow<KSensorResponse<StateData.LocaleStatus>> = 
            MutableSharedFlow<KSensorResponse<StateData.LocaleStatus>>().asTrackedFlow("locale")
    }

    override fun screen(): StatePlugin<StateData.ScreenStatus> = object : StatePlugin<StateData.ScreenStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.ScreenStatus> get() = KSensorResponse(StateData.ScreenStatus(true))
        override fun observe(): Flow<KSensorResponse<StateData.ScreenStatus>> = 
            MutableSharedFlow<KSensorResponse<StateData.ScreenStatus>>().asTrackedFlow("screen")
    }

    override fun brightness(): StatePlugin<StateData.BrightnessStatus> = object : StatePlugin<StateData.BrightnessStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.BrightnessStatus> get() = KSensorResponse(StateData.BrightnessStatus(100))
        override fun observe(): Flow<KSensorResponse<StateData.BrightnessStatus>> =
            MutableSharedFlow<KSensorResponse<StateData.BrightnessStatus>>().asTrackedFlow("brightness")
    }

    override fun lock(): StatePlugin<StateData.LockStatus> = object : StatePlugin<StateData.LockStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.LockStatus> get() = KSensorResponse(StateData.LockStatus(false))
        override fun observe(): Flow<KSensorResponse<StateData.LockStatus>> = 
            MutableSharedFlow<KSensorResponse<StateData.LockStatus>>().asTrackedFlow("lock")
    }

    override fun powerSave(): StatePlugin<StateData.PowerSaveStatus> = object : StatePlugin<StateData.PowerSaveStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.PowerSaveStatus> get() = KSensorResponse(StateData.PowerSaveStatus(false))
        override fun observe(): Flow<KSensorResponse<StateData.PowerSaveStatus>> =
            MutableSharedFlow<KSensorResponse<StateData.PowerSaveStatus>>().asTrackedFlow("powerSave")
    }

    override fun storage(interval: Duration): StatePlugin<StateData.StorageStatus> = object : StatePlugin<StateData.StorageStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.StorageStatus> get() = KSensorResponse(StateData.StorageStatus(0, 0, 0))
        override fun observe(): Flow<KSensorResponse<StateData.StorageStatus>> =
            MutableSharedFlow<KSensorResponse<StateData.StorageStatus>>().asTrackedFlow("storage")
    }

    override fun resources(interval: Duration): StatePlugin<StateData.ResourcesStatus> = object : StatePlugin<StateData.ResourcesStatus> {
        override val id: PluginId = PluginId.SYSTEM
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.ResourcesStatus> get() = KSensorResponse(
            StateData.ResourcesStatus(
                cpuUsagePercent = 0.0,
                appCpuUsagePercent = 0.0,
                systemLoadAverage = emptyList(),
                memoryPressure = StateData.ResourcesStatus.MemoryPressureLevel.UNKNOWN,
                memoryUsage = StateData.ResourcesStatus.MemoryUsage(0, 0, 0)
            )
        )
        override fun observe(): Flow<KSensorResponse<StateData.ResourcesStatus>> =
            MutableSharedFlow<KSensorResponse<StateData.ResourcesStatus>>().asTrackedFlow("resources")
    }

    private fun <T> Flow<T>.asTrackedFlow(name: String): Flow<T> {
        return this.onStart { activeObservers.add(name) }
            .onCompletion { activeObservers.remove(name) }
    }
}

class SystemPluginTest {

    @Test
    fun testBattery() = runTest {
        val fake = FakeSystemPlugin()
        val job = launch { fake.battery().observe().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("battery"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("battery"))
    }

    @Test
    fun testVolume() = runTest {
        val fake = FakeSystemPlugin()
        val job = launch { fake.volume().observe().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("volume"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("volume"))
    }

    @Test
    fun testLocale() = runTest {
        val fake = FakeSystemPlugin()
        val job = launch { fake.locale().observe().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("locale"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("locale"))
    }

    @Test
    fun testScreen() = runTest {
        val fake = FakeSystemPlugin()
        val job = launch { fake.screen().observe().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("screen"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("screen"))
    }

    @Test
    fun testLock() = runTest {
        val fake = FakeSystemPlugin()
        val job = launch { fake.lock().observe().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("lock"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("lock"))
    }

    @Test
    fun testPowerSave() = runTest {
        val fake = FakeSystemPlugin()
        val job = launch { fake.powerSave().observe().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("powerSave"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("powerSave"))
    }

    @Test
    fun testStorage() = runTest {
        val fake = FakeSystemPlugin()
        val job = launch { fake.storage().observe().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("storage"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("storage"))
    }

    @Test
    fun testResources() = runTest {
        val fake = FakeSystemPlugin()
        val job = launch { fake.resources().observe().collect {} }
        runCurrent()
        assertTrue(fake.activeObservers.contains("resources"))
        job.cancelAndJoin()
        assertFalse(fake.activeObservers.contains("resources"))
    }
}
