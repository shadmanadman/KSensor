package org.kmp.ksensor

import kotlinx.coroutines.flow.Flow
import state.StateController
import state.StateType
import state.StateUpdate
import state.createController

object KState : StateController  {
    private val controller = createController()

    override fun addObserver(types: List<StateType>): Flow<StateUpdate> = controller.addObserver(types)

    override fun removeObserver(types: List<StateType>)  = controller.removeObserver(types)
}