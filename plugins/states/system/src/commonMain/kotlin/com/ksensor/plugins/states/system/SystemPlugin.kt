package com.ksensor.plugins.states.system

import com.ksensor.core.KSensorPlugin
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.StateData

interface SystemPlugin : KSensorPlugin {
    fun battery(): StatePlugin<StateData.BatteryStatus>
    fun volume(): StatePlugin<StateData.VolumeStatus>
    fun locale(): StatePlugin<StateData.LocaleStatus>
    fun screen(): StatePlugin<StateData.ScreenStatus>
    fun lock(): StatePlugin<StateData.LockStatus>
    fun powerSave(): StatePlugin<StateData.PowerSaveStatus>
}

expect fun createSystemPlugin(): SystemPlugin
