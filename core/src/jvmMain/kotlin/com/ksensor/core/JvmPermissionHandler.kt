package com.ksensor.core

import androidx.compose.runtime.Composable

/**
 * Desktop (JVM) has no runtime permission model for these sensors, so every
 * permission is treated as granted.
 */
internal class JvmPermissionHandler : PermissionHandler {
    override fun hasPermission(permission: Permission): Boolean = true

    override suspend fun requestPermission(permission: Permission): Boolean = true

    @Composable
    override fun AskPermission(permission: Permission, onStatus: (PermissionStatus) -> Unit) {
        onStatus(PermissionStatus.GRANTED)
    }
}

actual fun createPermissionHandler(): PermissionHandler = JvmPermissionHandler()
