package org.kmp.ksensor.state

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIDevice

internal class BatteryStateReceiver {
    val device = UIDevice.currentDevice
    private var batteryLevelObserver: platform.darwin.NSObjectProtocol? = null
    private var batteryStateObserver: platform.darwin.NSObjectProtocol? = null

    private fun emitData(onData: (StateUpdate) -> Unit) {
        val levelRaw = device.batteryLevel // -1.0 if unknown
        val percent: Int? = if (levelRaw < 0f) null else (levelRaw * 100f).toInt()
        val state = when (device.batteryState) {
            platform.UIKit.UIDeviceBatteryState.UIDeviceBatteryStateCharging -> StateData.BatteryStatus.ChargingState.CHARGING
            platform.UIKit.UIDeviceBatteryState.UIDeviceBatteryStateFull -> StateData.BatteryStatus.ChargingState.FULL
            platform.UIKit.UIDeviceBatteryState.UIDeviceBatteryStateUnplugged -> StateData.BatteryStatus.ChargingState.DISCHARGING
            else -> StateData.BatteryStatus.ChargingState.UNKNOWN
        }
        onData(
            StateUpdate.Data(
                StateType.BATTERY,
                StateData.BatteryStatus(
                    levelPercent = percent,
                    chargingState = state,
                    health = null, // iOS does not expose health
                    temperatureC = null, // iOS does not expose battery temperature via public API

                ),
                platformType = PlatformType.iOS
            )
        )
    }

    fun registerObserver(onData: (StateUpdate) -> Unit) {
        device.batteryMonitoringEnabled = true

        val center = NSNotificationCenter.defaultCenter
        batteryLevelObserver = center.addObserverForName(
            name = platform.UIKit.UIDeviceBatteryLevelDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ -> emitData(onData) }

        batteryStateObserver = center.addObserverForName(
            name = platform.UIKit.UIDeviceBatteryStateDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ -> emitData(onData) }

        emitData(onData)
    }

    fun removeObserver(){
        device.batteryMonitoringEnabled = false
    }
}