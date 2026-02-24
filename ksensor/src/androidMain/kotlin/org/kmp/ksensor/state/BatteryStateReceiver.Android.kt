package org.kmp.ksensor.state

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import org.kmp.ksensor.state.StateData.BatteryStatus

internal  class BatteryStateReceiver(val onData: (StateUpdate)-> Unit): BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return

        try {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percent: Int? = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else null

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val chargingState = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.ChargingState.CHARGING
                BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.ChargingState.FULL
                BatteryManager.BATTERY_STATUS_DISCHARGING, BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.ChargingState.DISCHARGING
                else -> BatteryStatus.ChargingState.UNKNOWN
            }

            val healthInt = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)
            val health = when (healthInt) {
                BatteryManager.BATTERY_HEALTH_GOOD -> BatteryStatus.BatteryHealth.GOOD
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryStatus.BatteryHealth.OVERHEAT
                BatteryManager.BATTERY_HEALTH_DEAD -> BatteryStatus.BatteryHealth.DEAD
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryStatus.BatteryHealth.OVER_VOLTAGE
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryStatus.BatteryHealth.UNSPECIFIED_FAILURE
                BatteryManager.BATTERY_HEALTH_COLD -> BatteryStatus.BatteryHealth.COLD
                else -> BatteryStatus.BatteryHealth.UNKNOWN
            }

            val tempDeciC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            val temperatureC = if (tempDeciC != Int.MIN_VALUE) tempDeciC / 10f else null

            onData(
                StateUpdate.Data(
                    StateType.BATTERY,
                    BatteryStatus(
                        levelPercent = percent,
                        chargingState = chargingState,
                        health = health,
                        temperatureC = temperatureC,
                    ),
                    platformType = PlatformType.Android

                )
            )
        }catch (e: Exception){
            onData(StateUpdate.Error(e))
        }
    }

}