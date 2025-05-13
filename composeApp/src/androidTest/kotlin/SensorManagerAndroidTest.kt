import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import junit.framework.TestCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kmp.shots.k.sensor.AppContext
import org.kmp.shots.k.sensor.SensorHandler
import org.kmp.shots.k.sensor.SensorType
import kotlin.test.assertTrue


const val WAIT_FOR_SENSOR_DATA = 4000L

@RunWith(AndroidJUnit4::class)
class SensorManagerAndroidTest : TestCase() {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BODY_SENSORS
    )


    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppContext.setUp(context)
    }

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

    private fun assertSensorCallback(sensorType: SensorType) {
        val sensorHandler = SensorHandler()
        var called = false

        sensorHandler.registerSensors(
            types = listOf(sensorType),
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

        Thread.sleep(WAIT_FOR_SENSOR_DATA)

        sensorHandler.unregisterSensors(listOf(sensorType))

        assertTrue(called, "Expected $sensorType data callback")
    }
}