package com.ksensor.core

enum class PlatformType {
    iOS,
    Android,
    DesktopNoImp,
    WebNoImp
}

expect fun currentPlatform(): PlatformType
expect fun currentTimestamp(): Long
