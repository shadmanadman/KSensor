[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-blue.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-green.svg?style=flat-square&logo=gradle)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

![](KSensor.png)

### latest supported sensors:

    ACCELEROMETER
    GYROSCOPE
    MAGNETOMETER
    BAROMETER
    STEP_COUNTER
    LOCATION
    DEVICE_ORIENTATION

<!-- GETTING STARTED -->
## Getting Started
### Adding dependencies
Add it in your `commonMain.dependencies` :

  ```
  implementation("io.github.shadmanadman:KSensor:0.80.11")
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
SensorType.LOCATION,
SensorType.DEVICE_ORIENTATION)

// Register sensors
KSensor.registerSensors(
    types = sensors,
    locationIntervalMillis = {optional. default is 1000L}
).collect {sensorUpdate ->
    when (sensorUpdate) {
        is SensorUpdate.Data -> println(sensorUpdate.data)
        is SensorUpdate.Error -> println(sensorUpdate.exception)
    }

}.catch {
    //Catch any other throwable
}

// Unregister sensors when no longer needed
KSensor.unregisterSensors(sensors)
```
Each `SensorData` has a `platformType` so you know the sensor info comes from Android or iOS.

#### Permissions
If you are using Location you need `FINE_LOCATION` and `COARSE_LOCATION` permissions on Android. You can handel this permissions yourself or let the library handle them for you:
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
The iOS location permission is handled by the library itself.



