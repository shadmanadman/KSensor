package com.ksensor.plugins.states.system

import com.ksensor.core.model.StateData
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIScreen
import platform.UIKit.UIScreenBrightnessDidChangeNotification

internal class BrightnessReceiver(private val onData: (StateData.BrightnessStatus) -> Unit) {
    private var observer: Any? = null

    fun register() {
        observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIScreenBrightnessDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            onData(getCurrentStatus())
        }
        // Send initial value
        onData(getCurrentStatus())
    }

    fun unregister() {
        observer?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
        }
        observer = null
    }

    fun getCurrentStatus(): StateData.BrightnessStatus {
        val brightness = UIScreen.mainScreen.brightness
        return StateData.BrightnessStatus((brightness * 100).toInt())
    }
}
