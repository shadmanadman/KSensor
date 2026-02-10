import kotlinx.coroutines.runBlocking
import state.FakeStateHandler
import state.StateType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StateHandlerTest {

    @Test
    fun testAppVisibilityState(){
        assertObservingState(StateType.APP_VISIBILITY)
        assertObserverRemoved(StateType.APP_VISIBILITY)
    }
    @Test
    fun testScreenState(){
        assertObservingState(StateType.SCREEN)
        assertObserverRemoved(StateType.SCREEN)
    }

    @Test
    fun testConnectivityState(){
        assertObservingState(StateType.CONNECTIVITY)
        assertObserverRemoved(StateType.CONNECTIVITY)
    }
    @Test
    fun testActiveNetworkState(){
        assertObservingState(StateType.ACTIVE_NETWORK)
        assertObserverRemoved(StateType.ACTIVE_NETWORK)
    }
    @Test
    fun testLocationSate(){
        assertObservingState(StateType.LOCATION)
        assertObserverRemoved(StateType.LOCATION)
    }
    private fun assertObservingState(stateType: StateType) = runBlocking{
        val fake = FakeStateHandler()
        fake.addObserver(listOf(stateType)).collect{}
        assertTrue(fake.observedStates.contains(stateType))
    }

    private fun assertObserverRemoved(stateType: StateType) = runBlocking{
        val fake = FakeStateHandler()
        fake.addObserver(listOf(stateType)).collect{}
        fake.removeObserver(listOf(stateType))
        assertFalse(fake.observedStates.contains(stateType))
    }
}