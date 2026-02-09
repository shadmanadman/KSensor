package sensor

import androidx.compose.runtime.Composable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow

const val DEFAULT_INTERVAL_MILLIS = 1000L
typealias SensorTimeInterval = Long

sealed class SensorUpdate {
    data class Data(val type: SensorType, val data: SensorData,val platformType: PlatformType) : SensorUpdate()
    data class Error(val exception: Exception) : SensorUpdate()
}

interface SensorController {

    fun registerSensors(
        types: List<SensorType>,
        locationIntervalMillis: SensorTimeInterval = DEFAULT_INTERVAL_MILLIS
    ): Flow<SensorUpdate>

    fun unregisterSensors(types: List<SensorType>)
}

expect fun createController() : SensorController

internal class FakeSensorController : SensorController {
    val registeredSensors = mutableListOf<SensorType>()

    override fun registerSensors(
        types: List<SensorType>,
        locationIntervalMillis: SensorTimeInterval
    ): Flow<SensorUpdate> = callbackFlow {
        registeredSensors.addAll(types)
        awaitClose { unregisterSensors(types)}
    }

    override fun unregisterSensors(types: List<SensorType>) {
        types.forEach { registeredSensors.remove(it) }
    }
}