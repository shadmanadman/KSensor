import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import sensor.SensorType
import sensor.SensorUpdate
import sensor.iOSSensorController
import kotlin.test.Test
import kotlin.test.assertTrue

const val WAIT_FOR_SENSOR_DATA = 2000L

class SensorManagerIosTest {

    @Test
    fun testAccelerometer() {
        assertSensorCallback(SensorType.ACCELEROMETER)
    }

    @Test
    fun testGyroscope() {
        assertSensorCallback(SensorType.GYROSCOPE)
    }

    @Test
    fun testMagnetometer() {
        assertSensorCallback(SensorType.MAGNETOMETER)
    }

    @Test
    fun testBarometer() {
        assertSensorCallback(SensorType.BAROMETER)
    }

    @Test
    fun testStepCounter() {
        assertSensorCallback(SensorType.STEP_COUNTER)
    }

    @Test
    fun testLocation() {
        assertSensorCallback(SensorType.LOCATION)
    }

    @Test
    fun testProximity() {
        assertSensorCallback(SensorType.PROXIMITY)
    }

    @Test
    fun testLightSensor() {
        assertSensorCallback(SensorType.LIGHT)
    }

    @Test
    fun testTouchGestures(){
        assertSensorCallback(SensorType.TOUCH_GESTURES)
    }

    private fun assertSensorCallback(sensorType: SensorType) = runBlocking {
        val sensorHandler = iOSSensorController()
        var called = false

        sensorHandler.registerSensors(
            types = listOf(sensorType)
        ).collect { senorUpdate ->
            when (senorUpdate) {
                is SensorUpdate.Data -> {
                    println("SensorUpdate : ${senorUpdate.data}")
                    called = true
                }

                is SensorUpdate.Error -> {
                }
            }

        }

        delay(WAIT_FOR_SENSOR_DATA)

        sensorHandler.unregisterSensors(listOf(sensorType))

        assertTrue(called, "Expected $sensorType data callback")
    }
}