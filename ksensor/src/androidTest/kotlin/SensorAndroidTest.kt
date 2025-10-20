import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kmp.shots.k.sensor.AppContext
import org.kmp.shots.k.sensor.SensorHandler
import org.kmp.shots.k.sensor.SensorType
import org.kmp.shots.k.sensor.SensorUpdate
import kotlin.test.assertTrue


const val WAIT_FOR_SENSOR_DATA = 2000L

@RunWith(AndroidJUnit4::class)
class SensorManagerAndroidTest : TestCase() {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BODY_SENSORS
    )
    private val context = ApplicationProvider.getApplicationContext<Context>()


    @Before
    fun setup() {
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
    fun testOrientation() {
        assertSensorCallback(SensorType.DEVICE_ORIENTATION)
    }

    @Test
    fun testProximity() {
        assertSensorCallback(SensorType.PROXIMITY)
    }

    @Test
    fun testLightSensor() {
        assertSensorCallback(SensorType.LIGHT)
    }

    private fun assertSensorCallback(sensorType: SensorType) = runBlocking {
        val sensorHandler = SensorHandler()
        var called = false

        sensorHandler.registerSensors(
            types = listOf(sensorType)
        ).collect { senorUpdate ->
            when (senorUpdate) {
                is SensorUpdate.Data -> {
                    println(senorUpdate.data.toString())
                    called = true
                }

                is SensorUpdate.Error -> {
                }
            }
        }

        Thread.sleep(WAIT_FOR_SENSOR_DATA)

        sensorHandler.unregisterSensors(listOf(sensorType))

        assertTrue(called, "Expected $sensorType data callback")
    }
}