package com.ksensor.core

import platform.Foundation.NSUserDefaults
import platform.Foundation.setValue

internal class IosPlatformStorage : PlatformStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, key)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return if (defaults.objectForKey(key) == null) defaultValue else defaults.boolForKey(key)
    }

    override fun putStringSet(key: String, values: Set<String>) {
        defaults.setObject(values.toList(), key)
    }

    override fun getStringSet(key: String, defaultValues: Set<String>): Set<String> {
        val list = defaults.objectForKey(key) as? List<String>
        return list?.toSet() ?: defaultValues
    }
}

actual fun createPlatformStorage(): PlatformStorage = IosPlatformStorage()
