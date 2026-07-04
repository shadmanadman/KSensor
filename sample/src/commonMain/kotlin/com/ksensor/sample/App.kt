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
import com.ksensor.plugins.sensors.positioning.PositioningPlugin
import com.ksensor.plugins.sensors.positioning.createPositioningPlugin
import com.ksensor.plugins.states.bluetooth.BluetoothPlugin
import com.ksensor.plugins.states.bluetooth.createBluetoothPlugin

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            var permissionGranted by remember {
                mutableStateOf(
                    KSensor.permissionHandler.hasPermission(
                        Permission.BLUETOOTH
                    )
                )
            }

            if (!permissionGranted) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text("Bluetooth Permission Required")
                    Button(onClick = { /* Trigger permission request handled by AskPermission */ }) {
                        Text("Grant Permission")
                    }

                    KSensor.permissionHandler.AskPermission(Permission.BLUETOOTH) { status ->
                        if (status == PermissionStatus.GRANTED) {
                            permissionGranted = true
                        }
                    }
                }
            } else {
                BluetoothSample()
                OrientationSampleUsingEffect()
                OrientationSampleUsingState()
            }
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
