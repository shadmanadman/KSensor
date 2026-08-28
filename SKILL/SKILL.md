# KSensor LLM Skill

This document provides a comprehensive technical reference for KSensor, a Kotlin Multiplatform (KMP) library designed for observing device sensors and system states on Android and iOS.

## 1. Overview
KSensor is built on a modular, plugin-based architecture. It allows developers to integrate only the necessary sensor or state monitoring capabilities, minimizing app size and permission requirements.

### Key Features:
- **Kotlin Multiplatform**: Supports Android and iOS with a unified API.
- **Plugin-Based**: Each sensor/state is a separate module.
- **Reactive API**: Exposes data via Kotlin Coroutines `Flow`.
- **Start on Boot**: Infrastructure for background observation across reboots.
- **Robust Health Monitoring**: Hardware Heart Rate support with Camera PPG fallback.

---

## 2. Core Architecture

### KSensor Registry (`KSensor` object)
The central entry point for managing plugins and permissions.
- `register(plugin, startOnBoot)`: Registers a plugin.
- `get<T>(id)`: Retrieves a plugin instance.
- `permissionHandler`: Cross-platform utility for checking/requesting permissions.
- `startOnBoot`: Global toggle for boot-time activation.

### Data Wrapper (`KSensorResponse<T>`)
Every event is wrapped in this model:
- `data: T`: The payload (e.g., `Accelerometer`, `HeartRate`).
- `platform: PlatformType`: `ANDROID` or `IOS`.
- `timestamp: Long`: Collection time (epoch millis).

### Plugin Interfaces
- `SensorPlugin<T>`: For continuous streams (e.g., Accelerometer).
- `StatePlugin<T>`: For states with a current value (e.g., Battery level).

### Configuration (`SensorConfig`)
Most sensor observations accept a `SensorConfig` to control:
- `intervalMs: Long`: Desired sampling interval (default: 1000ms).
- `accuracy: Accuracy`: `POWER_SAVE`, `BALANCED`, or `HIGH_PRECISION`.

### Permission Handling (`PermissionHandler`)
Utility to check and request permissions in a KMP-friendly way.
```kotlin
val handler = KSensor.permissionHandler
if (!handler.hasPermission(Permission.LOCATION)) {
    val granted = handler.requestPermission(Permission.LOCATION)
    // ...
}
```
**Compose Support**:
```kotlin
KSensor.permissionHandler.AskPermission(Permission.CAMERA) { status ->
    when(status) {
        PermissionStatus.GRANTED -> // Start PPG
        else -> // Show error
    }
}
```

---

## 3. Sensor Plugins Reference

### Motion Sensors (`MOTION`)
Tracks device movement and physical activity.
- **Accelerometer**: 3-axis acceleration (`Vector3`).
- **Gyroscope**: 3-axis rotation rate (`Vector3`).
- **Step Counter**: Cumulative step count.
- **Motion Detector**: Activity classification (Walking, Running, Cycling, etc.).
- **Permissions**: `ACTIVITY_RECOGNITION` (Android), `NSMotionUsageDescription` (iOS).

### Environment Sensors (`ENVIRONMENT`)
Monitors ambient conditions.
- **Barometer**: Atmospheric pressure (hPa).
- **Light**: Ambient illuminance (lux).
- **Proximity**: Object proximity detection.

### Positioning Sensors (`POSITIONING`)
Location and spatial orientation.
- **Location**: Lat/Long/Alt (requires `LOCATION` permissions).
- **Magnetometer**: Magnetic field strength (`Vector3`).
- **Orientation**: Device orientation (Portrait, Landscape, etc.).
- **Heading**: Magnetic and True heading.
- **Location Status**: GPS on/off state.

### Health Sensors (`HEALTH`)
Vital signs and biological data.
- **Heart Rate**: BPM with confidence and quality metrics.
- **Fallback Strategy**: 
    1. Hardware Sensor (Wear/Mobile API).
    2. HealthKit (iOS Apple Watch data).
    3. **Camera PPG**: Analyzes finger blood flow via camera and flash if no hardware is present.
- **Permissions**: `BODY_SENSORS`, `CAMERA`.

### Interaction Sensors (`INTERACTION`)
- **Touch Gestures**: Detects screen interactions (Taps, Swipes).

---

## 4. State Plugins Reference

### Network (`NETWORK`)
- **Connectivity**: Online/Offline status.
- **Active Network**: Type detection (WiFi, Cellular, None).

### System (`SYSTEM`)
- **Battery**: Level, charging state, health, temperature.
- **Volume**: System output volume percentage.
- **Locale**: Language, country, and RTL detection.
- **Screen**: Screen on/off state.
- **Brightness**: Screen brightness percentage.
- **Lock**: Device lock status.
- **Power Save**: Battery saver mode status.
- **Storage**: Total and available disk space.
- **Resources**: CPU usage (System & App), system load average, and memory metrics.

### Bluetooth (`BLUETOOTH`)
- **Connections**: List of connected BLE devices.
- **Discoveries**: Real-time discovery of nearby BLE devices.
- **Permissions**: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`.

### Lifecycle (`LIFECYCLE`)
- **App Visibility**: Tracks if the app is in foreground or background.

---

## 5. Integration Guide

### Android Setup
1. Add dependencies to `build.gradle.kts`.
2. Declare permissions in `AndroidManifest.xml`.
3. (Optional) For "Start on Boot", register plugins and call `KSensor.start()` in `Application.onCreate()`.

### iOS Setup
1. Add dependencies.
2. Configure `Info.plist` with required usage descriptions (e.g., `NSCameraUsageDescription`, `NSHealthUpdateUsageDescription`).
3. For "Start on Boot", call `KSensor.start()` in `AppDelegate.didFinishLaunchingWithOptions`.

---

## 6. Coding Patterns & Examples

### Basic Observation
```kotlin
val motionPlugin = KSensor.get<MotionPlugin>(PluginId.MOTION)
    ?: createMotionPlugin().also { KSensor.register(it) }

scope.launch {
    motionPlugin.accelerometer().collect { response ->
        val x = response.data.values.x
        // ...
    }
}
```

### Jetpack Compose Integration
```kotlin
val battery by systemPlugin.battery().collectAsState(initial = null)
Text("Battery: ${battery?.data?.levelPercent}%")
```

### Permission Handling
```kotlin
val granted = KSensor.permissionHandler.requestPermission(Permission.LOCATION)
if (granted) {
    locationPlugin.observe().collect { ... }
}
```

---

## 7. LLM Injection Instructions
To use this knowledge in an LLM:
1. **Claude/GPT**: Copy the entire content of this `SKILL.md` and paste it into the "System Prompt" or "Context" section.
2. **Cursor/IDE Agents**: Add this file to the project indexing or reference it via `@SKILL.md`.
3. **RAG Systems**: Index this file in your knowledge base for KSensor-related queries.
