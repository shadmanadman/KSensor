package org.kmp.ksensor.state

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import androidx.core.text.layoutDirection

internal class LocaleReceiver(
    private val context: Context,
    val onLocaleChanged: (StateData.LocaleInfo) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_LOCALE_CHANGED) {
            context?.let {
                onLocaleChanged(getCurrentLocale())
            }
        }
    }

    fun getCurrentLocale(): StateData.LocaleInfo {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

        // Detect RTL using Android's standard API
        val isRtl = locale.layoutDirection == View.LAYOUT_DIRECTION_RTL

        return StateData.LocaleInfo(
            languageCode = locale.language,
            countryCode = locale.country,
            fullLocaleString = locale.toString(),
            displayName = locale.displayName,
            isRTL = isRtl
        )
    }
}
