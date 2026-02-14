import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.kmp.ksensor.state.IOSStateHandler
import org.kmp.ksensor.state.StateType
import org.kmp.ksensor.state.StateUpdate
import kotlin.test.Test
import kotlin.test.assertTrue

const val WAIT_FOR_STATE_DATA = 2000L

class StateIOSTest {

    @Test
    fun testAppVisibilityState(){
        assertStateObserver(StateType.APP_VISIBILITY)
    }
    @Test
    fun testScreenState(){
        assertStateObserver(StateType.SCREEN)
    }
    @Test
    fun testConnectivityState(){
        assertStateObserver(StateType.CONNECTIVITY)
    }
    @Test
    fun testActiveNetworkState(){
        assertStateObserver(StateType.ACTIVE_NETWORK)
    }

    @Test
    fun testLocalState(){
        assertStateObserver(StateType.LOCALE)
    }

    private fun assertStateObserver(stateType: StateType) = runBlocking {
        val stateHandler = IOSStateHandler()
        var called = false

        stateHandler.addObserver(
            types = listOf(stateType)
        ).collect { senorUpdate ->
            when (senorUpdate) {
                is StateUpdate.Data -> {
                    println("SensorUpdate : ${senorUpdate.data}")
                    called = true
                }

                is StateUpdate.Error ->{
                        println("SensorError : ${senorUpdate.exception}")
                        called = true
                }
            }
        }

        delay(WAIT_FOR_STATE_DATA)

        stateHandler.removeObserver(listOf(stateType))

        assertTrue(called, "Expected $stateType data callback")
    }
}