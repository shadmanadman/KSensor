package com.ksensor.core

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun nowMillis(): Double = js("Date.now()")

actual fun currentPlatform(): PlatformType = PlatformType.WebNoImp
actual fun currentTimestamp(): Long = nowMillis().toLong()
