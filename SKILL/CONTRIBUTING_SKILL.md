# KSensor Plugin Development Skill

This guide explains how to extend KSensor by creating new plugins. Use this when you need to add support for a new hardware sensor or system state.

## 1. Plugin Types
Decide which interface your plugin should implement:
- **`SensorPlugin<T>`**: Use for continuous data streams (e.g., Gyroscope, Barometer).
- **`StatePlugin<T>`**: Use for data that has a current value and changes occasionally (e.g., Battery level, Connectivity).

## 2. Project Structure
Follow the established module hierarchy:
- `plugins/sensors/<category>/`: For hardware sensors.
- `plugins/states/<category>/`: For system states.

Each plugin should have:
- `commonMain`: Interfaces and data models.
- `androidMain`: Android-specific implementation.
- `iosMain`: iOS-specific implementation (using C-Interop/Platform APIs).
- `jvmMain` / `wasmJsMain`: Default or fallback implementations.

## 3. Step-by-Step Implementation

### Step A: Define the Data Model
Place data models in `core` or the plugin's `commonMain`. Ensure it's a `data class` or `sealed class`.

### Step B: Common Interface
```kotlin
interface MyNewPlugin : KSensorPlugin {
    fun observeData(): Flow<KSensorResponse<MyData>>
}
```

### Step C: Platform Implementation (Android)
Use `callbackFlow` to wrap Android listeners (e.g., `SensorEventListener`, `BroadcastReceiver`).
```kotlin
class AndroidMyNewPlugin(private val context: Context) : MyNewPlugin {
    override val id = PluginId.MY_NEW_ID
    override val requiredPermissions = listOf(Permission.SOME_PERMISSION)

    override fun observeData() = callbackFlow {
        // Implementation using Android APIs
        awaitClose { /* Cleanup */ }
    }
}
```

### Step D: Platform Implementation (iOS)
Use `callbackFlow` with iOS delegates or notification center.
```kotlin
class IosMyNewPlugin : MyNewPlugin {
    override fun observeData() = callbackFlow {
        // Implementation using AVFoundation, CoreMotion, etc.
        awaitClose { /* Cleanup */ }
    }
}
```

## 4. Best Practices
- **Memory Management**: Always use `awaitClose` to unregister listeners or stop hardware sessions.
- **Error Handling**: Wrap platform-specific exceptions into `KSensorResponse` if applicable, or throw meaningful exceptions.
- **Power Efficiency**: Honor `SensorConfig` (e.g., `samplingRate`) to save battery.
- **Permissions**: Always declare `requiredPermissions` so the `PermissionHandler` can manage them.

## 5. Registration
Add your new ID to `PluginId` enum in the `core` module.
Provide a factory function (e.g., `createMyNewPlugin()`) in each platform module for easy instantiation.
