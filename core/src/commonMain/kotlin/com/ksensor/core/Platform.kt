package com.ksensor.core

enum class PlatformType {
    iOS,
    Android,
    Desktop,
    Web
}

expect fun currentPlatform(): PlatformType
expect fun currentTimestamp(): Long
