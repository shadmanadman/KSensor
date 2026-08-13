package com.ksensor.core

internal class JvmPlatformStorage : PlatformStorage {
    override fun putBoolean(key: String, value: Boolean) {}
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = defaultValue
    override fun putStringSet(key: String, values: Set<String>) {}
    override fun getStringSet(key: String, defaultValues: Set<String>): Set<String> = defaultValues
}

actual fun createPlatformStorage(): PlatformStorage = JvmPlatformStorage()
