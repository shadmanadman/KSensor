package com.ksensor.core

interface PlatformStorage {
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putStringSet(key: String, values: Set<String>)
    fun getStringSet(key: String, defaultValues: Set<String>): Set<String>
}

expect fun createPlatformStorage(): PlatformStorage
