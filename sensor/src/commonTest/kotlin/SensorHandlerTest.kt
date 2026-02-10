import kotlinx.coroutines.runBlocking
import sensor.FakeSensorController
import sensor.SensorType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SensorHandlerTest {

    @Test
    fun testAccelerometer() {
        assertRegisterSensor(SensorType.ACCELEROMETER)
        assertUnregisterSensor(SensorType.ACCELEROMETER)
    }

    @Test
    fun testGyroscope() {
        assertRegisterSensor(SensorType.GYROSCOPE)
        assertUnregisterSensor(SensorType.GYROSCOPE)
    }

    @Test
    fun testMagnetometer() {
        assertRegisterSensor(SensorType.MAGNETOMETER)
        assertUnregisterSensor(SensorType.MAGNETOMETER)
    }

    @Test
    fun testBarometer() {
        assertRegisterSensor(SensorType.BAROMETER)
        assertUnregisterSensor(SensorType.BAROMETER)
    }

    @Test
    fun testStepCounter() {
        assertRegisterSensor(SensorType.STEP_COUNTER)
        assertUnregisterSensor(SensorType.STEP_COUNTER)
    }

    @Test
    fun testOrientation() {
        assertRegisterSensor(SensorType.DEVICE_ORIENTATION)
        assertUnregisterSensor(SensorType.DEVICE_ORIENTATION)
    }

    @Test
    fun testProximity() {
        assertRegisterSensor(SensorType.PROXIMITY)
        assertUnregisterSensor(SensorType.PROXIMITY)
    }

    @Test
    fun testLightSensor() {
        assertRegisterSensor(SensorType.LIGHT)
        assertUnregisterSensor(SensorType.LIGHT)
    }

    private fun assertRegisterSensor(sensorType: SensorType) = runBlocking{
        val fake = FakeSensorController()
        fake.registerSensors(listOf(sensorType)).collect{}
        assertTrue(fake.registeredSensors.contains(sensorType))
    }

    private fun assertUnregisterSensor(sensorType: SensorType) = runBlocking{
        val fake = FakeSensorController()
        fake.registerSensors(listOf(sensorType)).collect{}
        fake.unregisterSensors(listOf(sensorType))
        assertFalse(fake.registeredSensors.contains(sensorType))
    }
}