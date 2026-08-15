package com.ksensor.sample

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ksensor.core.KSensor
import com.ksensor.core.Permission
import com.ksensor.core.PermissionStatus
import com.ksensor.core.model.PluginId
import com.ksensor.core.model.SensorData
import com.ksensor.plugins.sensors.health.HealthPlugin
import com.ksensor.plugins.sensors.health.createHealthPlugin
import com.ksensor.plugins.sensors.motion.MotionPlugin
import com.ksensor.plugins.sensors.motion.createMotionPlugin
import com.ksensor.plugins.sensors.positioning.PositioningPlugin
import com.ksensor.plugins.sensors.positioning.createPositioningPlugin
import com.ksensor.plugins.states.bluetooth.BluetoothPlugin
import com.ksensor.plugins.states.bluetooth.createBluetoothPlugin
import com.ksensor.plugins.states.system.SystemPlugin
import com.ksensor.plugins.states.system.createSystemPlugin
import kotlin.time.Duration.Companion.seconds

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val permissions = listOf(
                Permission.BLUETOOTH,
                Permission.ACTIVITY_RECOGNITION,
                Permission.LOCATION,
                Permission.BODY_SENSORS,
                Permission.CAMERA
            )
            var grantedPermissions by remember {
                mutableStateOf(permissions.filter { KSensor.permissionHandler.hasPermission(it) }.toSet())
            }

            val remainingPermissions = permissions.filter { it !in grantedPermissions }

            if (remainingPermissions.isNotEmpty()) {
                val nextPermission = remainingPermissions.first()
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text("$nextPermission Permission Required")
                    Button(onClick = { /* Trigger permission request handled by AskPermission */ }) {
                        Text("Grant Permission")
                    }

                    KSensor.permissionHandler.AskPermission(nextPermission) { status ->
                        if (status == PermissionStatus.GRANTED) {
                            grantedPermissions = grantedPermissions + nextPermission
                        }
                    }
                }
            } else {
//                BluetoothSample()
//                OrientationSampleUsingEffect()
//                OrientationSampleUsingState()
//                MotionSampleUsingState()
//                LocationSample()
//                StorageSample()
//                HeadingSample()
                BrightnessSample()
//                HealthSample()
            }
        }
    }
}

@Composable
fun BrightnessSample() {
    val plugin = remember {
        KSensor.get<SystemPlugin>(PluginId.SYSTEM)
            ?: createSystemPlugin().also { KSensor.register(it) }
    }

    val brightnessResponse by plugin.brightness().observe().collectAsState(null)
    val brightness = brightnessResponse?.data

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Brightness Status Sample", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (brightness != null) {
            Text("Screen Brightness: ${brightness.screenBrightness}%")

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = brightness.screenBrightness.toFloat() / 100f,
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Try changing your system brightness to see it update.", style = MaterialTheme.typography.caption)
        } else {
            Text("Loading brightness data...")
        }
    }
}

@Composable
fun HealthSample() {
    val plugin = remember {
        KSensor.get<HealthPlugin>(PluginId.HEALTH)
            ?: createHealthPlugin().also { KSensor.register(it) }
    }

    val heartRateResponse by plugin.heartRate().collectAsState(null)
    
    var lastValidHeartRate by remember { mutableStateOf<SensorData.HeartRate?>(null) }
    
    LaunchedEffect(heartRateResponse) {
        val data = heartRateResponse?.data
        if (data != null && (data.heartRate > 0 || lastValidHeartRate == null)) {
            lastValidHeartRate = data
        }
    }

    val heartRate = lastValidHeartRate

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Health Plugin Sample", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (heartRate != null) {
            if (heartRate.source == SensorData.HeartRateSource.CAMERA_PPG) {
                if (heartRate.heartRate == 0f) {
                    val status = "Place your finger over the camera lens and flash"
                    Text("Camera PPG Active", style = MaterialTheme.typography.h6, color = MaterialTheme.colors.primary)
                    Text(status, style = MaterialTheme.typography.body1)
                } else {
                    val color = if (heartRate.confidence > 0.6f) MaterialTheme.colors.primary else MaterialTheme.colors.secondary
                    Text("Heart Rate: ${heartRate.heartRate.toInt()} BPM", style = MaterialTheme.typography.h4)
                    Text("Source: Camera PPG (Confidence: ${(heartRate.confidence * 100).toInt()}%)", 
                        style = MaterialTheme.typography.caption,
                        color = color
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Keep your finger on the camera.", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.secondary)
                }
            } else {
                Text("Heart Rate: ${heartRate.heartRate.toInt()} BPM", style = MaterialTheme.typography.h4)
                Text("Source: Hardware Sensor", style = MaterialTheme.typography.caption)
            }
        } else {
            Text("Waiting for heart rate data...")
            Text("(Make sure you have a heart rate sensor and permission is granted)")
        }
    }
}

@Composable
fun BluetoothSample() {
    val bluetoothPlugin = remember {
        KSensor.get<BluetoothPlugin>(PluginId.BLUETOOTH)
            ?: createBluetoothPlugin().also { KSensor.register(it) }
    }

    val connectionsResponse by (bluetoothPlugin.connections().observe()).collectAsState(null)
    val discoveriesResponse by (bluetoothPlugin.discoveries().observe()).collectAsState(null)

    val connections = connectionsResponse?.data
    val discoveries = discoveriesResponse?.data

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Bluetooth Plugin Sample", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Connected Devices:", style = MaterialTheme.typography.h6)
        LazyColumn(modifier = Modifier.height(100.dp)) {
            items(connections?.connectedDevices ?: emptyList()) { device ->
                Text("${device.name} (${device.id})")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Discovered Devices:", style = MaterialTheme.typography.h6)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(discoveries?.discoveredDevices ?: emptyList()) { device ->
                Text("${device.name} (${device.id})")
            }
        }
    }
}

@Composable
fun OrientationSampleUsingEffect() {
    val plugin = remember {
        KSensor.get<PositioningPlugin>(PluginId.POSITIONING)
            ?: createPositioningPlugin().also { KSensor.register(it) }
    }

    // Use effect
    LaunchedEffect(plugin) {
        plugin.orientation().collect {
            println("OrientationData in effect: ${it.data}")
        }
    }
}

@Composable
fun OrientationSampleUsingState() {
    val plugin = remember {
        KSensor.get<PositioningPlugin>(PluginId.POSITIONING)
            ?: createPositioningPlugin().also { KSensor.register(it) }
    }

    // Use state
    val orientation by plugin.orientation().collectAsState(null)

    println("OrientationData as state: ${orientation?.data}")
}

@Composable
fun MotionSampleUsingState() {
    val plugin = remember {
        KSensor.get<MotionPlugin>(PluginId.MOTION)
            ?: createMotionPlugin().also { KSensor.register(it) }
    }

    // Use state
    val motion by plugin.motionDetector().collectAsState(null)

    println("MotionDetectionData as state: ${motion?.data}")
}

@Composable
fun LocationSample() {
    val plugin = remember {
        KSensor.get<PositioningPlugin>(PluginId.POSITIONING)
            ?: createPositioningPlugin().also { KSensor.register(it) }
    }

    // Use state
    val location by plugin.location().collectAsState(null)

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Location Plugin Sample", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Current Location:", style = MaterialTheme.typography.h6)
        Text("Latitude: ${location?.data?.latitude ?: "N/A"}")
        Text("Longitude: ${location?.data?.longitude ?: "N/A"}")
        Text("Altitude: ${location?.data?.altitude ?: "N/A"}")
    }
}

@Composable
fun StorageSample() {
    val plugin = remember {
        KSensor.get<SystemPlugin>(PluginId.SYSTEM)
            ?: createSystemPlugin().also { KSensor.register(it) }
    }

    val storageResponse by plugin.storage(interval = 2.seconds).observe().collectAsState(null)
    val storage = storageResponse?.data

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Storage Status Sample", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (storage != null) {
            val totalGB = storage.totalBytes / (1024.0 * 1024.0 * 1024.0)
            val usedGB = storage.usedBytes / (1024.0 * 1024.0 * 1024.0)
            val freeGB = storage.freeBytes / (1024.0 * 1024.0 * 1024.0)

            Text("Total Storage: ${totalGB.toTwoDecimalString()} GB")
            Text("Used Storage: ${usedGB.toTwoDecimalString()} GB")
            Text("Free Storage: ${freeGB.toTwoDecimalString()} GB")
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = if (storage.totalBytes > 0) storage.usedBytes.toFloat() / storage.totalBytes else 0f,
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
        } else {
            Text("Loading storage data...")
        }
    }
}

@Composable
fun HeadingSample() {
    val plugin = remember {
        KSensor.get<PositioningPlugin>(PluginId.POSITIONING)
            ?: createPositioningPlugin().also { KSensor.register(it) }
    }

    val headingResponse by plugin.heading().collectAsState(null)
    val heading = headingResponse?.data

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Heading Plugin Sample", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))

        if (heading != null) {
            Text("Magnetic Heading: ${heading.magneticHeading.toTwoDecimalString()}°")
            Text("True Heading: ${heading.trueHeading.toTwoDecimalString()}°")
            Text("Device Heading: ${heading.deviceHeading.toTwoDecimalString()}°")
            Text("Course Over Ground: ${heading.courseOverGround.toTwoDecimalString()}°")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Basic visualization: showing the values is usually enough for a sample, 
            // but we can add more if needed.
        } else {
            Text("Waiting for heading data...")
        }
    }
}

private fun Double.toTwoDecimalString(): String {
    val integerPart = this.toLong()
    val decimalPart = ((this - integerPart) * 100).toLong()
    return "$integerPart.${decimalPart.toString().padStart(2, '0')}"
}


