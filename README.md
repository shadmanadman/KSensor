[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-blue.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-green.svg?style=flat-square&logo=gradle)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

![](KSensor.jpg)

### latest supported sensors:

    ACCELEROMETER : accelerometer
    GYROSCOPE : gyroscope
    MAGNETOMETER : compass
    BAROMETER : barometer
    STEP_COUNTER : step counter    
    LOCATION : live location
    DEVICE_ORIENTATION : device orientation
    PROXIMITY : proximity sensor
    LIGHT : light sensor
    SCREEN_STATE : whether the screen is on or off / only Android, no direct way for iOS
    APP_STATE : whether the app is in the foreground or background

<!-- GETTING STARTED -->
## Getting Started
### Adding dependencies
Add it in your `commonMain.dependencies` :

  ```
  implementation("io.github.shadmanadman:KSensor:1.2.22")
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
SensorType.DEVICE_ORIENTATION,
SensorType.PROXIMITY,
SensorType.LIGHT,
SensorType.SCREEN_STATE
SensorType.APP_STATE)

// Register sensors
KSensor.registerSensors(
    types = sensors,
    // Optional
    locationIntervalMillis = 1000L
).collect {sensorUpdate ->
    when (sensorUpdate) {
        is SensorUpdate.Data -> println(sensorUpdate.data)
        is SensorUpdate.Error -> println(sensorUpdate.exception)
    }
}

// Unregister sensors when no longer needed
KSensor.unregisterSensors(sensors)
```
Each `SensorData` has a `platformType` so you know the sensor data comes from Android or iOS.

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



