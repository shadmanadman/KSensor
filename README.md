latest suported sensors:

    ACCELEROMETER,
    GYROSCOPE,
    MAGNETOMETER,
    BAROMETER,
    STEP_COUNTER,
    LOCATION

<!-- GETTING STARTED -->
## Getting Started
### Adding dependencies
- Add it in your `commonMain.dependencies` :
  ```
  implementation("io.github.shadmanadman:ksensor:1.9.4")
  ```

  
### Usage  
```
//Create a list of sensors that you need
val sensors = listof(
SensorType.ACCELEROMETER,
SensorType.GYROSCOPE,
SensorType.MAGNETOMETER,
SensorType.BAROMETER,
SensorType.STEP_COUNTER,
SensorType.LOCATION)

// Register sensors
KSensor.registerSensors(
    types = sensors,
    locationIntervalMillis = {optional. default is 1000L},
    onSensorData = { type, data ->
        println("Sensor: $type - Data: $data")
    },
    onSensorError = { error ->
        println("Sensor error: ${error.message}")
    }
)

// Unregister sensors when no longer needed
KSensor.unregisterSensors(sensors)
```
Each `SensorData` has a `platformType` so you know the sensor info comes from Android or iOS.

If you are using Location you need ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permissions on Android. You can handel this permissions yourself or let the library handle them for you:
```
    //Put this in AndroidManifest
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```
Inside a composable call:
```
KSensor.HandelPermissions() { status ->
    when (status) {
        PermissionStatus.Granted -> println("Permission Granted")
        PermissionStatus.Denied -> println("Permission Denied")
    }
}
```
