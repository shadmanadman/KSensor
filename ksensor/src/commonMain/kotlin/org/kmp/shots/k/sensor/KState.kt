package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

object KState : StateController {
    private val stateHandler  = StateHandler()


    override fun addObserver(types: List<StateType>): Flow<StateUpdate>  =
        stateHandler.addObserver(types)

    override fun removeObserver(types: List<StateType>) =
        stateHandler.removeObserver(types)

    @Composable
    override fun HandelPermissions(
        onPermissionStatus: (PermissionStatus) -> Unit
    ) = stateHandler.HandelPermissions( onPermissionStatus)
}