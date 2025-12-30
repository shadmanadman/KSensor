package org.kmp.shots.k.sensor

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch

internal actual class PermissionsManager : PermissionHandler {
    @Composable
    actual override fun askPermission(
        permission: PermissionType,
        permissionStatus: (PermissionStatus) -> Unit
    ) {
        when (permission) {
            PermissionType.LOCATION -> LocationPermission(permissionStatus)
        }
    }

    @Composable
    actual override fun launchSettings() {
        val context = LocalContext.current
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).also {
            context.startActivity(it)
        }
    }

}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LocationPermission(onPermissionStatus: (PermissionStatus) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val coarseLocationPermissionState =
        rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)

    LaunchedEffect(locationPermissionState, coarseLocationPermissionState) {
        checkPermission(
            locationPermissionState,
            coarseLocationPermissionState,
            lifecycleOwner,
            onPermissionStatus
        )
    }
}


@OptIn(ExperimentalPermissionsApi::class)
private fun checkPermission(
    locationPermissionState: PermissionState,
    coarseLocationPermissionState: PermissionState,
    lifecycleOwner: LifecycleOwner,
    onPermissionStatus: (PermissionStatus) -> Unit
) {
    if (checkPermissionIsNotGranted(
            locationPermissionState,
            coarseLocationPermissionState
        )
    ) {
        if (checkRationaleShouldBePresentedToUser(
                locationPermissionState,
                coarseLocationPermissionState
            )
        ) {
            onPermissionStatus(PermissionStatus.SHOW_RATIONAL)
        } else {
            lifecycleOwner.lifecycleScope.launch {
                locationPermissionState.launchPermissionRequest()
                coarseLocationPermissionState.launchPermissionRequest()
            }
        }
    } else {
        onPermissionStatus(
            PermissionStatus.GRANTED
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun checkPermissionIsNotGranted(
    locationPermissionState: PermissionState,
    coarseLocationPermissionState: PermissionState
): Boolean {
    return locationPermissionState.status.isGranted.not() || coarseLocationPermissionState.status.isGranted.not()
}

@OptIn(ExperimentalPermissionsApi::class)
private fun checkRationaleShouldBePresentedToUser(
    locationPermissionState: PermissionState,
    coarseLocationPermissionState: PermissionState
): Boolean {
    return locationPermissionState.status.shouldShowRationale || coarseLocationPermissionState.status.shouldShowRationale
}