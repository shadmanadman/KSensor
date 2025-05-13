package org.kmp.shots.k.sensor

import androidx.compose.runtime.Composable

internal expect class PermissionsManager : PermissionHandler {
    @Composable
    override fun askPermission(
        permission: PermissionType,
        permissionStatus: (PermissionStatus) -> Unit
    )

    @Composable
    override fun launchSettings()

}

interface PermissionHandler {
    @Composable
    fun askPermission(permission: PermissionType, permissionStatus: (PermissionStatus) -> Unit)

    @Composable
    fun launchSettings()

}

enum class PermissionType {
    LOCATION
}

enum class PermissionStatus {
    GRANTED,
    DENIED,
    SHOW_RATIONAL
}