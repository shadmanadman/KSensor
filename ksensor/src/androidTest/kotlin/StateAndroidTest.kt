import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.kmp.shots.k.sensor.AndroidStateHandler
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
    fun testLocationState(){
        assertStateObserver(StateType.LOCATION)
    }
    private fun assertStateObserver(stateType: StateType) = runBlocking {
        val stateHandler = AndroidStateHandler()
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