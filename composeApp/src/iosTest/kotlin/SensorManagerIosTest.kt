import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.kmp.shots.k.sensor.SensorHandler
import org.kmp.shots.k.sensor.SensorType
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

    private fun assertSensorCallback(sensorType: SensorType) = runBlocking {
        val sensorHandler = SensorHandler()
        var called = false

        sensorHandler.registerSensors(
            sensorType = listOf(sensorType),
            onSensorData = { type, data ->
                if (type == sensorType) {
                    println("$sensorType data received: $data")
                    called = true
                }
            },
            onSensorError = {
                println("Error for $sensorType: ${it.message}")
            }
        )

        delay(WAIT_FOR_SENSOR_DATA)

        sensorHandler.unregisterSensors(listOf(sensorType))

        assertTrue(called, "Expected $sensorType data callback")
    }
}