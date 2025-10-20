import kotlinx.coroutines.runBlocking
import org.kmp.shots.k.sensor.FakeStateHandler
import org.kmp.shots.k.sensor.StateType
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
        assertObservingState(StateType.SCREEN_STATE)
        assertObserverRemoved(StateType.SCREEN_STATE)
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