package com.ksensor.core

actual fun currentPlatform(): PlatformType = PlatformType.Desktop
actual fun currentTimestamp(): Long = System.currentTimeMillis()
