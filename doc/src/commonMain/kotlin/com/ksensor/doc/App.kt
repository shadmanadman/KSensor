package com.ksensor.doc

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ksensorlib.doc.generated.resources.Res
import ksensorlib.doc.generated.resources.ksensor
import org.jetbrains.compose.resources.painterResource

@Composable
fun App() {
    var currentPage by remember { mutableStateOf<DocPage>(DocPage.Intro) }

    MaterialTheme {
        Row(modifier = Modifier.fillMaxSize()) {
            Sidebar(onPageSelected = { currentPage = it })
            Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
            Box(modifier = Modifier.fillMaxHeight().weight(1f).padding(32.dp).verticalScroll(rememberScrollState())) {
                Content(currentPage)
            }
        }
    }
}

sealed class DocPage {
    object Intro : DocPage()
    data class Plugin(
        val category: String,
        val name: String,
        val description: String,
        val data: String,
        val code: String,
        val androidConfig: String? = null,
        val iosConfig: String? = null
    ) : DocPage()
}

@Composable
fun Sidebar(onPageSelected: (DocPage) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color(0xFFF8F9FA))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "KSensor",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF3F51B5),
            modifier = Modifier.clickable { onPageSelected(DocPage.Intro) }.padding(bottom = 24.dp)
        )

        SidebarSection("Sensors", Icons.Default.Sensors) {
            SidebarSubSection("Environment") {
                SidebarItem("Barometer") { onPageSelected(DocPage.Plugin("Sensors", "Barometer", "Measures the ambient air pressure in hPa (millibars).", "Pressure (Float)", "KSensor.get<EnvironmentPlugin>(PluginId.ENVIRONMENT)?.barometer()?.collect { response -> \n    val pressure = response.data.pressure\n}")) }
                SidebarItem("Light") { onPageSelected(DocPage.Plugin("Sensors", "Light", "Measures the ambient light level (illuminance) in lx.", "Illuminance (Float)", "KSensor.get<EnvironmentPlugin>(PluginId.ENVIRONMENT)?.light()?.collect { response -> \n    val lux = response.data.illuminance\n}")) }
                SidebarItem("Proximity") { onPageSelected(DocPage.Plugin("Sensors", "Proximity", "Measures the proximity of an object in cm relative to the view screen of a device.", "Distance (Float)", "KSensor.get<EnvironmentPlugin>(PluginId.ENVIRONMENT)?.proximity()?.collect { response -> \n    val distance = response.data.distance\n}")) }
            }
            SidebarSubSection("Motion") {
                SidebarItem("Accelerometer") { onPageSelected(DocPage.Plugin("Sensors", "Accelerometer", "Measures the acceleration force in m/s² that is applied to a device on all three physical axes (x, y, and z).", "X, Y, Z (Float)", "KSensor.get<MotionPlugin>(PluginId.MOTION)?.accelerometer()?.collect { response -> \n    val x = response.data.values.x\n}")) }
                SidebarItem("Gyroscope") { onPageSelected(DocPage.Plugin("Sensors", "Gyroscope", "Measures a device's rate of rotation in rad/s around each of the three physical axes (x, y, and z).", "X, Y, Z (Float)", "KSensor.get<MotionPlugin>(PluginId.MOTION)?.gyroscope()?.collect { response -> \n    val rotationX = response.data.values.x\n}")) }
                SidebarItem("Step Counter") { onPageSelected(DocPage.Plugin("Sensors", "Step Counter", "Measures the number of steps taken by the user since the last reboot while the sensor was activated.", "Steps (Int)", "KSensor.get<MotionPlugin>(PluginId.MOTION)?.stepCounter()?.collect { response -> \n    val steps = response.data.steps\n}", "android.permission.ACTIVITY_RECOGNITION", "NSMotionUsageDescription")) }
                SidebarItem("Motion Detector") { onPageSelected(DocPage.Plugin("Sensors", "Motion Detector", "Detects the user's current activity (Walking, Running, Cycling, etc.).", "Motion Type (Enum)", "KSensor.get<MotionPlugin>(PluginId.MOTION)?.motionDetector()?.collect { response -> \n    val activity = response.data.type\n}", "android.permission.ACTIVITY_RECOGNITION", "NSMotionUsageDescription")) }
            }
            SidebarSubSection("Positioning") {
                SidebarItem("Location") { onPageSelected(DocPage.Plugin("Sensors", "Location", "Provides geographic location coordinates.", "Latitude, Longitude, Altitude (Double)", "KSensor.get<PositioningPlugin>(PluginId.POSITIONING)?.location()?.collect { response -> \n    val lat = response.data.latitude\n}", "android.permission.ACCESS_FINE_LOCATION\nandroid.permission.ACCESS_COARSE_LOCATION")) }
                SidebarItem("Magnetometer") { onPageSelected(DocPage.Plugin("Sensors", "Magnetometer", "Measures the ambient geomagnetic field for all three physical axes (x, y, z) in μT.", "X, Y, Z (Float)", "KSensor.get<PositioningPlugin>(PluginId.POSITIONING)?.magnetometer()?.collect { response -> \n    val x = response.data.values.x\n}")) }
                SidebarItem("Orientation") { onPageSelected(DocPage.Plugin("Sensors", "Orientation", "Calculates the device's orientation based on the accelerometer and magnetometer.", "Azimuth, Pitch, Roll (Float)", "KSensor.get<PositioningPlugin>(PluginId.POSITIONING)?.orientation()?.collect { response -> \n    val azimuth = response.data.azimuth\n}")) }
                SidebarItem("Heading") { onPageSelected(DocPage.Plugin("Sensors", "Heading", "Provides device heading information including magnetic heading, true heading, and course over ground.", "Magnetic, True, Device, Course (Double)", "KSensor.get<PositioningPlugin>(PluginId.POSITIONING)?.heading()?.collect { response -> \n    val trueHeading = response.data.trueHeading\n}")) }
            }
            SidebarSubSection("Interaction") {
                SidebarItem("Touch Gestures") { onPageSelected(DocPage.Plugin("Sensors", "Touch Gestures", "Monitors touch events on the screen.", "X, Y, Type (Enum)", "KSensor.get<InteractionPlugin>(PluginId.INTERACTION)?.touchGestures()?.collect { response -> \n    val x = response.data.x\n}")) }
            }
            SidebarSubSection("Health") {
                SidebarItem("Heart Rate") { onPageSelected(DocPage.Plugin("Sensors", "Heart Rate", "Measures heart rate using either dedicated hardware sensors or Camera PPG (Photoplethysmography).", "BPM (Float), Source (Enum), Confidence (Float)", "KSensor.get<HealthPlugin>(PluginId.HEALTH)?.heartRate()?.collect { response -> \n    val bpm = response.data.heartRate\n    val source = response.data.source\n}", "android.permission.BODY_SENSORS\nandroid.permission.CAMERA\nandroid.permission.health.READ_HEART_RATE", "NSCameraUsageDescription\nNSHealthUpdateUsageDescription\nNSHealthShareUsageDescription")) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SidebarSection("States", Icons.Default.Info) {
            SidebarSubSection("Network") {
                SidebarItem("Connectivity") { onPageSelected(DocPage.Plugin("States", "Connectivity", "Monitors the network connectivity status.", "Status (Boolean)", "KSensor.get<NetworkPlugin>(PluginId.NETWORK)?.connectivity()?.observe()?.collect { status -> \n    val isConnected = status.isConnected\n}")) }
                SidebarItem("Active Network") { onPageSelected(DocPage.Plugin("States", "Active Network", "Provides information about the currently active network (WiFi, Cellular, etc.).", "Network Type (Enum)", "KSensor.get<NetworkPlugin>(PluginId.NETWORK)?.activeNetwork()?.observe()?.collect { network -> \n    val type = network.type\n}")) }
            }
            SidebarSubSection("Bluetooth") {
                SidebarItem("Connections") { onPageSelected(DocPage.Plugin("States", "Connections", "Monitors connected BLE devices.", "List of Devices", "KSensor.get<BluetoothPlugin>(PluginId.BLUETOOTH)?.connections()?.observe()?.collect { status -> \n    val devices = status.connectedDevices\n}", "android.permission.BLUETOOTH_CONNECT")) }
                SidebarItem("Audio Devices") { onPageSelected(DocPage.Plugin("States", "Audio Devices", "Monitors connected Bluetooth audio devices.", "List of Devices", "KSensor.get<BluetoothPlugin>(PluginId.BLUETOOTH)?.audioDevices()?.observe()?.collect { status -> \n    val devices = status.connectedDevices\n}")) }
                SidebarItem("Discoveries") { onPageSelected(DocPage.Plugin("States", "Discoveries", "Monitors discovered BLE devices.", "List of Devices", "KSensor.get<BluetoothPlugin>(PluginId.BLUETOOTH)?.discoveries()?.observe()?.collect { status -> \n    val devices = status.discoveredDevices\n}", "android.permission.BLUETOOTH_SCAN\nandroid.permission.ACCESS_FINE_LOCATION")) }
            }
            SidebarSubSection("System") {
                SidebarItem("Battery") { onPageSelected(DocPage.Plugin("States", "Battery", "Monitors battery level and state.", "Level, IsCharging", "KSensor.get<SystemPlugin>(PluginId.SYSTEM)?.battery()?.observe()?.collect { status -> \n    val level = status.level\n}")) }
                SidebarItem("Volume") { onPageSelected(DocPage.Plugin("States", "Volume", "Monitors system volume changes.", "Volume Level", "KSensor.get<SystemPlugin>(PluginId.SYSTEM)?.volume()?.observe()?.collect { status -> \n    val volume = status.volume\n}")) }
                SidebarItem("Screen") { onPageSelected(DocPage.Plugin("States", "Screen", "Monitors screen on/off state.", "IsOn (Boolean)", "KSensor.get<SystemPlugin>(PluginId.SYSTEM)?.screen()?.observe()?.collect { status -> \n    val isOn = status.isOn\n}")) }
                SidebarItem("Storage") { onPageSelected(DocPage.Plugin("States", "Storage", "Monitors device storage capacity and usage.", "Total, Used, Free (Long)", "KSensor.get<SystemPlugin>(PluginId.SYSTEM)?.storage()?.observe()?.collect { response -> \n    val freeSpace = response.data.freeBytes\n}")) }
            }
            SidebarSubSection("Lifecycle") {
                SidebarItem("App Visibility") { onPageSelected(DocPage.Plugin("States", "App Visibility", "Monitors whether the app is in the foreground or background.", "Status (Enum)", "KSensor.get<LifecyclePlugin>(PluginId.LIFECYCLE)?.appVisibility()?.observe()?.collect { status -> \n    // handle visibility\n}")) }
            }
        }
    }
}

@Composable
fun SidebarSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
        }
        Column(modifier = Modifier.padding(start = 12.dp), content = content)
    }
}

@Composable
fun SidebarSubSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp, horizontal = 8.dp)
        )
        if (expanded) {
            Column(modifier = Modifier.padding(start = 12.dp), content = content)
        }
    }
}

@Composable
fun SidebarItem(title: String, onClick: () -> Unit) {
    Text(
        title,
        fontSize = 13.sp,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp, horizontal = 8.dp),
        color = Color(0xFF555555)
    )
}

@Composable
fun Content(page: DocPage) {
    when (page) {
        is DocPage.Intro -> IntroPage()
        is DocPage.Plugin -> PluginPage(page)
    }
}

@Composable
fun IntroPage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Welcome to KSensor", fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Image(
            painter = painterResource(Res.drawable.ksensor),
            contentDescription = "KSensor Poster",
            modifier = Modifier.fillMaxWidth(0.6f).aspectRatio(16f/9f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "KSensor is a Kotlin Multiplatform library that provides a unified API for accessing device sensors and states across Android and iOS (and soon more!).",
            fontSize = 18.sp,
            lineHeight = 28.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Library Configuration", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            elevation = 0.dp,
            backgroundColor = Color(0xFF2B2B2B),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "// Enable start on boot for the library\nKSensor.startOnBoot = true\n\n// Register a plugin to start on boot\nKSensor.register(plugin, startOnBoot = true)\n\n// Or enable it later\nKSensor.setStartOnBoot(PluginId.MOTION, true)",
                color = Color(0xFFA9B7C6),
                modifier = Modifier.padding(16.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun PluginPage(page: DocPage.Plugin) {
    Column {
        Text("${page.category} > ${page.name}", color = Color.Gray, fontSize = 14.sp)
        Text(page.name, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Description", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(page.description, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        
        if (page.androidConfig != null || page.iosConfig != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Configuration", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                if (page.androidConfig != null) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Android (Manifest)", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.Gray)
                        Card(elevation = 0.dp, backgroundColor = Color(0xFFF0F0F0), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Text(page.androidConfig, modifier = Modifier.padding(12.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                }
                if (page.iosConfig != null) {
                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        Text("iOS (Info.plist)", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.Gray)
                        Card(elevation = 0.dp, backgroundColor = Color(0xFFF0F0F0), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Text(page.iosConfig, modifier = Modifier.padding(12.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Data Provided", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Card(elevation = 0.dp, backgroundColor = Color(0xFFF0F0F0), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(page.data, modifier = Modifier.padding(16.dp), fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Sample Code", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Card(
            elevation = 0.dp,
            backgroundColor = Color(0xFF2B2B2B),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(
                page.code,
                color = Color(0xFFA9B7C6),
                modifier = Modifier.padding(16.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
    }
}
