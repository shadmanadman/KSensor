package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

object KState : StateController {
    private lateinit var stateController: StateController
//    private val stateController : StateController by lazy {
//        factory.create()
//    }
//
//    internal fun init(factory: StateControllerFactory) {
//        this.factory = factory
//    }

    internal fun setController(controller: StateController) {
        stateController = controller
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