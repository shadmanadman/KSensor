package com.ksensor.plugins.states.system

import com.ksensor.core.Permission
import com.ksensor.core.PluginId
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.StateData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * No-op SystemPlugin for the desktop (JVM) target. KSensor has no desktop (JVM) sensor
 * backend yet, so observations emit nothing and state values are placeholders.
 */
class JvmSystemPlugin : SystemPlugin {
    override val id: PluginId = PluginId.SYSTEM
    override val requiredPermissions: List<Permission> = emptyList()

    override fun battery(): StatePlugin<StateData.BatteryStatus> = noopState(StateData.BatteryStatus(null, StateData.BatteryStatus.ChargingState.UNKNOWN, null, null))
    override fun volume(): StatePlugin<StateData.VolumeStatus> = noopState(StateData.VolumeStatus(0))
    override fun locale(): StatePlugin<StateData.LocaleStatus> = noopState(StateData.LocaleStatus("", "", "", "", false))
    override fun screen(): StatePlugin<StateData.ScreenStatus> = noopState(StateData.ScreenStatus(true))
    override fun lock(): StatePlugin<StateData.LockStatus> = noopState(StateData.LockStatus(false))
}

private fun <T> noopState(value: T): StatePlugin<T> = object : StatePlugin<T> {
    override val id: PluginId = PluginId.SYSTEM
    override val requiredPermissions: List<Permission> = emptyList()
    override val currentState: KSensorResponse<T> = KSensorResponse(value)
    override fun observe(): Flow<KSensorResponse<T>> = emptyFlow()
}

actual fun createSystemPlugin(): SystemPlugin = JvmSystemPlugin()
