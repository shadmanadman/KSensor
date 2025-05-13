import org.kmp.shots.k.sensor.FakeSensorManager
import org.kmp.shots.k.sensor.SensorData
import org.kmp.shots.k.sensor.SensorType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SensorHandlerTest {
    @Test
    fun testRegisterSensors() {
        val fake = FakeSensorManager()
        fake.registerSensors(listOf(SensorType.ACCELEROMETER), onSensorData = {_, _ -> } , onSensorError = {})

        assertTrue(fake.registeredSensors.contains(SensorType.ACCELEROMETER))
    }

    @Test
    fun testUnregisterSensors() {
        val fake = FakeSensorManager()
        fake.registerSensors(listOf(SensorType.ACCELEROMETER), onSensorData = {_, _ -> } , onSensorError = {})
        fake.unregisterSensors(listOf(SensorType.ACCELEROMETER))

        assertFalse(fake.registeredSensors.contains(SensorType.ACCELEROMETER))
    }
}