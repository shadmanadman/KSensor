package com.ksensor.plugins.states.system

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.ksensor.core.model.StateData

internal class BrightnessObserver(
    private val context: Context,
    private val onData: (StateData.BrightnessStatus) -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        onData(getCurrentStatus(context))
    }

    companion object {
        fun getCurrentStatus(context: Context): StateData.BrightnessStatus {
            val brightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                -1
            )
            val percentage = if (brightness != -1) (brightness * 100) / 255 else -1
            return StateData.BrightnessStatus(percentage)
        }
    }
}
