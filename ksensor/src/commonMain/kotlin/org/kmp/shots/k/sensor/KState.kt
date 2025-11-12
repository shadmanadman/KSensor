package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

object KState : StateController {
    private lateinit var factory: StateControllerFactory
    private val stateController : StateController by lazy {
        factory.create()
    }

    fun init(factory: StateControllerFactory) {
        this.factory = factory
    }

    override fun addObserver(types: List<StateType>): Flow<StateUpdate>  =
        stateController.addObserver(types)

    override fun removeObserver(types: List<StateType>) =
        stateController.removeObserver(types)

    @Composable
    override fun HandelPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    ) = stateController.HandelPermissions( permission,onPermissionStatus)
}