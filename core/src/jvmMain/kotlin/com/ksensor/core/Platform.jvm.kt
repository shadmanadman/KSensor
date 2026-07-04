package com.ksensor.core

actual fun currentPlatform(): PlatformType = PlatformType.DesktopNoImp
actual fun currentTimestamp(): Long = System.currentTimeMillis()
