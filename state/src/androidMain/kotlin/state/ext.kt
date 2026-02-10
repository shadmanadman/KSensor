package state

import platform.UIKit.UIDeviceOrientation

internal fun UIDeviceOrientation.toDeviceOrientation(): DeviceOrientation = when (this) {
    UIDeviceOrientation.UIDeviceOrientationPortrait,
    UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> DeviceOrientation.PORTRAIT

    UIDeviceOrientation.UIDeviceOrientationLandscapeLeft,
    UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> DeviceOrientation.LANDSCAPE

    else -> DeviceOrientation.UNKNOWN
}