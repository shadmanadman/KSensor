package permission

import androidx.compose.runtime.Composable

interface PermissionHandler {
    @Composable
    fun AskPermission(permission: PermissionType, permissionStatus: (PermissionStatus) -> Unit)

    @Composable
    fun OpenSettingsForPermission()

}

expect fun createPermissionHandler(): PermissionHandler

enum class PermissionType {
    LOCATION
}

enum class PermissionStatus {
    GRANTED,
    DENIED,
    SHOW_RATIONAL,
    UNKNOWN
}