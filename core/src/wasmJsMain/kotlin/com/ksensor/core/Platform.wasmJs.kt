package com.ksensor.core

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun nowMillis(): Double = js("Date.now()")

actual fun currentPlatform(): PlatformType = PlatformType.Web
actual fun currentTimestamp(): Long = nowMillis().toLong()
