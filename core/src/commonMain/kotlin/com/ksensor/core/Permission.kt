package com.ksensor.core

import androidx.compose.runtime.Composable

/**
 * Abstraction for permissions required by sensors and state plugins.
 */
enum class Permission {
    LOCATION,
    BLUETOOTH,
    ACTIVITY_RECOGNITION,
    BODY_SENSORS,
    CAMERA
}

enum class PermissionStatus {
    GRANTED,
    DENIED,
    SHOW_RATIONAL,
    UNKNOWN
}

/**
 * Interface for handling permission requests across platforms.
 */
interface PermissionHandler {
    fun hasPermission(permission: Permission): Boolean
    suspend fun requestPermission(permission: Permission): Boolean

    @Composable
    fun AskPermission(permission: Permission, onStatus: (PermissionStatus) -> Unit)
}

expect fun createPermissionHandler(): PermissionHandler
