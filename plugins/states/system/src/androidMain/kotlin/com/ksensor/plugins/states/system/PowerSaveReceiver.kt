package com.ksensor.plugins.states.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import com.ksensor.core.model.StateData

internal class PowerSaveReceiver(private val onData: (StateData.PowerSaveStatus) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        onData(getCurrentStatus(context))
    }

    companion object {
        fun getCurrentStatus(context: Context): StateData.PowerSaveStatus {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return StateData.PowerSaveStatus(pm.isPowerSaveMode)
        }
    }
}
