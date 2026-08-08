package com.ksensor.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.ksensor.core.context.KSensorContext

internal class AndroidPermissionHandler : PermissionHandler {
    private val context: Context get() = KSensorContext.get()

    override fun hasPermission(permission: Permission): Boolean {
        val androidPermissions = getAndroidPermissions(permission)
        return androidPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override suspend fun requestPermission(permission: Permission): Boolean {
        return hasPermission(permission)
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    override fun AskPermission(permission: Permission, onStatus: (PermissionStatus) -> Unit) {
        val androidPermissions = getAndroidPermissions(permission)
        
        if (androidPermissions.isEmpty()) {
            onStatus(PermissionStatus.GRANTED)
            return
        }

        if (androidPermissions.size == 1) {
            val permissionState = rememberPermissionState(androidPermissions[0])
            LaunchedEffect(permissionState.status) {
                if (permissionState.status.isGranted) {
                    onStatus(PermissionStatus.GRANTED)
                } else {
                    permissionState.launchPermissionRequest()
                }
            }
        } else {
            val multiplePermissionsState = rememberMultiplePermissionsState(androidPermissions)
            LaunchedEffect(multiplePermissionsState.allPermissionsGranted) {
                if (multiplePermissionsState.allPermissionsGranted) {
                    onStatus(PermissionStatus.GRANTED)
                } else {
                    multiplePermissionsState.launchMultiplePermissionRequest()
                }
            }
        }
    }

    private fun getAndroidPermissions(permission: Permission): List<String> {
        return when (permission) {
            Permission.LOCATION -> listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
            Permission.BLUETOOTH -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    listOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                    )
                } else {
                    listOf(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            Permission.ACTIVITY_RECOGNITION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    listOf(Manifest.permission.ACTIVITY_RECOGNITION)
                } else {
                    emptyList()
                }
            }
            Permission.BODY_SENSORS -> {
                if (Build.VERSION.SDK_INT >= 36) { // API 36+ (Baklava)
                    listOf("android.permission.health.READ_HEART_RATE")
                } else {
                    listOf(Manifest.permission.BODY_SENSORS)
                }
            }
            Permission.CAMERA -> listOf(Manifest.permission.CAMERA)
        }
    }
}

actual fun createPermissionHandler(): PermissionHandler = AndroidPermissionHandler()
