package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import permission.PermissionStatus
import permission.PermissionType
import state.StateType
import state.StateUpdate
import state.createController

object KState  {
    private val controller = createController()

    suspend fun addObserver(types: List<StateType>): Flow<StateUpdate> = controller.addObserver(types)

    suspend fun removeObserver(types: List<StateType>)  = controller.removeObserver(types)
}