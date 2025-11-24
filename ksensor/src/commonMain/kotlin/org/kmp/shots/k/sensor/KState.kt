package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

object KState  {
    private val controller = createController()

    suspend fun addObserver(types: List<StateType>): Flow<StateUpdate> = controller.addObserver(types)

    suspend fun removeObserver(types: List<StateType>)  = controller.removeObserver(types)

    @Composable
   fun HandelPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    ) =  controller.HandelPermissions(permission,onPermissionStatus)
}