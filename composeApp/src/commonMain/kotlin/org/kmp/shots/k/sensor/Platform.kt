package org.kmp.shots.k.sensor

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform