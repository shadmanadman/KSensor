package com.ksensor.core

import android.content.Context
import com.ksensor.core.context.KSensorContext

internal class AndroidPlatformStorage : PlatformStorage {
    private val context: Context get() = KSensorContext.get()
    private val prefs by lazy {
        context.getSharedPreferences("ksensor_prefs", Context.MODE_PRIVATE)
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    override fun putStringSet(key: String, values: Set<String>) {
        prefs.edit().putStringSet(key, values).apply()
    }

    override fun getStringSet(key: String, defaultValues: Set<String>): Set<String> {
        return prefs.getStringSet(key, defaultValues) ?: defaultValues
    }
}

actual fun createPlatformStorage(): PlatformStorage = AndroidPlatformStorage()
