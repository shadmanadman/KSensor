package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class StateUpdate {
    data class Data(val type: StateType, val data: StateData) : StateUpdate()
    data class Error(val exception: Exception) : StateUpdate()
}


internal interface StateController {
    fun addObserver(
        types: List<StateType>,
    ): Flow<StateUpdate>

    fun removeObserver(types: List<StateType>)

    @Composable
    fun HandelPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    )
}

internal expect class StateHandler() : StateController {
    override fun addObserver(types: List<StateType>): Flow<StateUpdate>
    override fun removeObserver(types: List<StateType>)
    @Composable
    override fun HandelPermissions(permission: PermissionType,onPermissionStatus: (PermissionStatus) -> Unit)

}

internal class FakeStateHandler : StateController {
    val observedStates = mutableListOf<StateType>()

    override fun addObserver(types: List<StateType>): Flow<StateUpdate> = callbackFlow {
        observedStates.addAll(types)
        awaitClose { removeObserver(types)}
    }

    override fun removeObserver(types: List<StateType>) {
        types.forEach { observedStates.remove(it) }
    }

    @Composable
    override fun HandelPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    ) {
    }
}