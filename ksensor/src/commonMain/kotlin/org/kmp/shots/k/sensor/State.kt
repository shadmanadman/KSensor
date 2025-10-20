package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

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
        onPermissionStatus: (PermissionStatus) -> Unit
    )
}

internal expect class StateHandler() : StateController {
    override fun addObserver(types: List<StateType>): Flow<StateUpdate>
    override fun removeObserver(types: List<StateType>)
    @Composable
    override fun HandelPermissions(onPermissionStatus: (PermissionStatus) -> Unit)

}