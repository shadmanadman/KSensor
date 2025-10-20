import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.kmp.shots.k.sensor.StateHandler
import org.kmp.shots.k.sensor.StateType
import org.kmp.shots.k.sensor.StateUpdate
import kotlin.test.Test
import kotlin.test.assertTrue

class StateAndroidTest {

    @Test
    fun testAppVisibilityState(){
        assertStateObserver(StateType.APP_VISIBILITY)
    }
    @Test
    fun testScreenState(){
        assertStateObserver(StateType.SCREEN_STATE)
    }
    private fun assertStateObserver(stateType: StateType) = runBlocking {
        val stateHandler = StateHandler()
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

        delay(WAIT_FOR_SENSOR_DATA)

        stateHandler.removeObserver(listOf(stateType))

        assertTrue(called, "Expected $stateType data callback")
    }
}