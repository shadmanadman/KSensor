package com.ksensor.plugins.states.system

import com.ksensor.core.KSensorPlugin
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.StateData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface SystemPlugin : KSensorPlugin {
    fun battery(): StatePlugin<StateData.BatteryStatus>
    fun volume(): StatePlugin<StateData.VolumeStatus>
    fun locale(): StatePlugin<StateData.LocaleStatus>
    fun screen(): StatePlugin<StateData.ScreenStatus>
    fun lock(): StatePlugin<StateData.LockStatus>
    fun powerSave(): StatePlugin<StateData.PowerSaveStatus>
    fun storage(interval: Duration = 3.seconds): StatePlugin<StateData.StorageStatus>
}

expect fun createSystemPlugin(): SystemPlugin
