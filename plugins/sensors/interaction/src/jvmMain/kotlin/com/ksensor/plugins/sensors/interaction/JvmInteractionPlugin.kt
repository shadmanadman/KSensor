package com.ksensor.plugins.sensors.interaction

import com.ksensor.core.Permission
import com.ksensor.core.PluginId
import com.ksensor.core.SensorConfig
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.SensorData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * No-op InteractionPlugin for the desktop (JVM) target. KSensor has no desktop (JVM) sensor
 * backend yet, so observations emit nothing and state values are placeholders.
 */
class JvmInteractionPlugin : InteractionPlugin {
    override val id: PluginId = PluginId.INTERACTION
    override val requiredPermissions: List<Permission> = emptyList()

    override fun touchGestures(config: SensorConfig): Flow<KSensorResponse<SensorData.TouchGestures>> = emptyFlow()
}

actual fun createInteractionPlugin(): InteractionPlugin = JvmInteractionPlugin()
