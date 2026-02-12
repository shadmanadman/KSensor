package org.kmp.ksensor.state

import kotlinx.coroutines.flow.Flow

object KState : StateController {
    private val controller = createController()

    override fun addObserver(types: List<StateType>): Flow<StateUpdate> = controller.addObserver(types)

    override fun removeObserver(types: List<StateType>)  = controller.removeObserver(types)
}