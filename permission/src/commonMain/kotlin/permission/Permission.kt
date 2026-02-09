package permission

import androidx.compose.runtime.Composable

interface PermissionHandler {
    @Composable
    fun askPermission(permission: PermissionType, permissionStatus: (PermissionStatus) -> Unit)

    @Composable
    fun launchSettings()

}

expect fun createHandler(): PermissionHandler

enum class PermissionType {
    LOCATION
}

enum class PermissionStatus {
    GRANTED,
    DENIED,
    SHOW_RATIONAL
}