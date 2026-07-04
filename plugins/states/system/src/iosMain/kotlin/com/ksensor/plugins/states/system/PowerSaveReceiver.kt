package com.ksensor.plugins.states.system

import com.ksensor.core.model.StateData
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSProcessInfoPowerStateDidChangeNotification
import platform.Foundation.lowPowerModeEnabled

internal class PowerSaveReceiver(private val onData: (StateData.PowerSaveStatus) -> Unit) {
    private var observer: platform.darwin.NSObjectProtocol? = null

    private fun current() = StateData.PowerSaveStatus(NSProcessInfo.processInfo.lowPowerModeEnabled)

    fun register() {
        observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = NSProcessInfoPowerStateDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ -> onData(current()) }
        onData(current()) // initial value
    }

    fun unregister() {
        observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
    }

    fun getCurrentStatus(): StateData.PowerSaveStatus = current()
}
