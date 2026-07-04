package com.ksensor.core.model

import com.ksensor.core.PlatformType

enum class StateType {
    SCREEN,
    APP_VISIBILITY,
    CONNECTIVITY,
    ACTIVE_NETWORK,
    LOCATION,
    VOLUME,
    LOCALE,
    BATTERY,
    LOCK,
    POWER_SAVE,
    BLE_CONNECTIONS,
    BLE_DISCOVERS
}

data class BleDevice(val id: String, val name: String)

sealed class StateData {
    data class AppVisibilityStatus(
        val isAppVisible: Boolean,
    ) : StateData()

    data class LocationStatus(
        val isLocationOn: Boolean
    ) : StateData()

    data class ScreenStatus(
        val isScreenOn: Boolean,
    ) : StateData()

    data class LockStatus(
        val isDeviceLocked:Boolean
    ):StateData()

    data class PowerSaveStatus(
        val isPowerSaveMode: Boolean,
    ) : StateData()

    data class CurrentActiveNetwork(val activeNetwork: ActiveNetwork) : StateData() {
        enum class ActiveNetwork {
            WIFI,
            CELLULAR,
            NONE
        }
    }

    data class VolumeStatus(val volumePercentage: Int): StateData()
    data class ConnectivityStatus(
        val isConnected: Boolean,
    ) : StateData()

    data class LocaleStatus(
        val languageCode: String,
        val countryCode: String,
        val fullLocaleString: String,
        val displayName: String,
        val isRTL: Boolean
    ) : StateData()

    data class BatteryStatus(
        val levelPercent: Int?,
        val chargingState: ChargingState,
        val health: BatteryHealth?,
        val temperatureC: Float?
    ) : StateData(){
        enum class ChargingState { UNKNOWN, DISCHARGING, CHARGING, FULL }
        enum class BatteryHealth { UNKNOWN, GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, UNSPECIFIED_FAILURE, COLD }
    }

    data class BleConnectionStatus(val connectedDevices: List<BleDevice>) : StateData()

    data class BleDiscoversStatus(val discoveredDevices: List<BleDevice>) : StateData()
}