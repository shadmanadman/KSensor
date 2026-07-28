package com.ksensor.core

/**
 * Configuration for sensor observations.
 */
data class SensorConfig(
    val intervalMs: Long = 1000L,
    val accuracy: Accuracy = Accuracy.BALANCED
) {
    enum class Accuracy {
        POWER_SAVE,
        BALANCED,
        HIGH_PRECISION
    }

    companion object {
        val Default = SensorConfig()
    }
}
