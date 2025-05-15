import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import junit.framework.TestCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kmp.shots.k.sensor.AppContext
import org.kmp.shots.k.sensor.PermissionType
import org.kmp.shots.k.sensor.PermissionsManager
import org.kmp.shots.k.sensor.SensorHandler
import org.kmp.shots.k.sensor.SensorType
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


    private fun assertSensorCallback(sensorType: SensorType) {
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

        Thread.sleep(WAIT_FOR_SENSOR_DATA)

        sensorHandler.unregisterSensors(listOf(sensorType))

        assertTrue(called, "Expected $sensorType data callback")
    }

    private fun checkLocationPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
    }
}