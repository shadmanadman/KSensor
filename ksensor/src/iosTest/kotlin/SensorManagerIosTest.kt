import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import org.kmp.shots.k.sensor.SensorHandler
import org.kmp.shots.k.sensor.SensorType
import org.kmp.shots.k.sensor.SensorUpdate
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
            sensorType = listOf(sensorType)
        ).catch {

        }.collect { senorUpdate->
            when (senorUpdate) {
                is SensorUpdate.Data -> {
                    println(senorUpdate.data.toString())
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