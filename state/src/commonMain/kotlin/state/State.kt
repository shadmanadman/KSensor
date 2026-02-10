package state

import androidx.compose.runtime.Composable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class StateUpdate {
    data class Data(val type: StateType, val data: StateData, val platformType: PlatformType) :
        StateUpdate()

    data class Error(val exception: Exception) : StateUpdate()
}


interface StateController {
    fun addObserver(
        types: List<StateType>,
    ): Flow<StateUpdate>

    fun removeObserver(types: List<StateType>)
}

expect fun createController(): StateController

internal class FakeStateHandler : StateController {
    val observedStates = mutableListOf<StateType>()

    override fun addObserver(types: List<StateType>): Flow<StateUpdate> = callbackFlow {
        observedStates.addAll(types)
        awaitClose { removeObserver(types) }
    }

    override fun removeObserver(types: List<StateType>) {
        types.forEach { observedStates.remove(it) }
    }
}