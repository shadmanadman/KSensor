package org.kmp.ksensor.state

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager

class LocationProviderReceiver(private val onProviderChanged:()-> Unit): BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == LocationManager.MODE_CHANGED_ACTION)
            onProviderChanged()
    }
}