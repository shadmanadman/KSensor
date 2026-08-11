package com.ksensor.plugins.sensors.health.heart

import kotlin.math.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class PPGAlgorithmTest {

    @Test
    fun testBpmAccuracy() {
        val bpms = listOf(50.0, 60.0, 75.0, 90.0, 120.0, 150.0)
        
        for (targetBpm in bpms) {
            val algo = PPGAlgorithm(targetSamplingRate = 30.0, windowSeconds = 12)
            var lastResult: PPGAlgorithm.AnalysisResult? = null
            
            // Simulate 15 seconds of signal
            val durationSec = 15.0
            val fs = 30.0
            val numSamples = (durationSec * fs).toInt()
            
            for (i in 0 until numSamples) {
                val timeSec = i / fs
                // Synthetic PPG: basic sine wave + DC offset + small noise
                val signal = 100.0 + 5.0 * sin(2.0 * PI * (targetBpm / 60.0) * timeSec) + 0.1 * cos(20.0 * timeSec)
                
                val result = algo.processSample(signal, (timeSec * 1000).toLong())
                if (result != null) {
                    lastResult = result
                }
            }
            
            assertTrue(lastResult != null, "Should return a result for $targetBpm BPM")
            assertTrue(lastResult.confidence > 0.5f, "Confidence should be high for clean signal at $targetBpm BPM")
            
            val error = abs(lastResult.bpm - targetBpm)
            assertTrue(error < 2.0, "BPM error should be < 2. Actual: ${lastResult.bpm}, Expected: $targetBpm")
        }
    }

    @Test
    fun testNoisySignalRecovery() {
        val targetBpm = 75.0
        val algo = PPGAlgorithm(targetSamplingRate = 30.0, windowSeconds = 10)
        var lastResult: PPGAlgorithm.AnalysisResult? = null
        
        val fs = 30.0
        // 20 seconds of signal
        for (i in 0 until 600) {
            val timeSec = i / fs
            var signal = 100.0 + 5.0 * sin(2.0 * PI * (targetBpm / 60.0) * timeSec)
            
            // Add significant noise every 5 seconds
            if (i % 150 in 0..10) {
                signal += 20.0 // spike
            }
            
            val result = algo.processSample(signal, (timeSec * 1000).toLong())
            if (result != null) {
                lastResult = result
            }
        }
        
        assertTrue(lastResult != null)
        val error = abs(lastResult.bpm - targetBpm)
        assertTrue(error < 5.0, "Should recover BPM even with noise. Actual: ${lastResult.bpm}")
    }

    @Test
    fun testFilterStateStability() {
        val algo = PPGAlgorithm(targetSamplingRate = 30.0, windowSeconds = 5)
        
        // Feed constant signal
        for (i in 0 until 100) {
            algo.processSample(100.0, (i * 33).toLong())
        }
        
        // After stabilization, a constant input should lead to a near-zero filtered output (due to bandpass)
        // We can't easily inspect the internal buffer without reflection, but we can verify it doesn't crash 
        // and produces consistent confidence for a non-periodic signal
        val result = algo.processSample(100.0, 100 * 33L)
        assertTrue(result == null || result.confidence == 0f, "Constant signal should not result in high confidence BPM")
    }
    
    @Test
    fun testNonUniformSamplingResampling() {
        val targetBpm = 80.0
        val algo = PPGAlgorithm(targetSamplingRate = 30.0, windowSeconds = 10)
        var lastResult: PPGAlgorithm.AnalysisResult? = null
        
        // Feed samples with jitter in timestamps (25ms to 45ms instead of 33ms)
        var currentTime = 0L
        for (i in 0 until 400) {
            val timeSec = currentTime / 1000.0
            val signal = 100.0 + 5.0 * sin(2.0 * PI * (targetBpm / 60.0) * timeSec)
            
            val result = algo.processSample(signal, currentTime)
            if (result != null) {
                lastResult = result
            }
            
            // Jitter: 33ms +/- 8ms
            currentTime += (25 + (i % 20)).toLong() 
        }
        
        assertTrue(lastResult != null, "Should work with jittery timestamps")
        val error = abs(lastResult.bpm - targetBpm)
        assertTrue(error < 5.0, "Resampling should maintain BPM accuracy under jitter. Actual: ${lastResult.bpm}")
    }

    @Test
    fun testStreamingConsistency() {
        // Test that processing samples one by one is consistent and doesn't accumulate errors
        // compared to a hypothetical window-based filtering (which we moved away from).
        val targetBpm = 60.0
        val algo = PPGAlgorithm(targetSamplingRate = 30.0, windowSeconds = 10)
        
        val fs = 30.0
        val results = mutableListOf<Float>()
        
        for (i in 0 until 600) {
            val timeSec = i / fs
            val signal = 100.0 + 5.0 * sin(2.0 * PI * (targetBpm / 60.0) * timeSec)
            val result = algo.processSample(signal, (timeSec * 1000).toLong())
            if (result != null && result.confidence > 0.8f) {
                results.add(result.bpm)
            }
        }
        
        // Results should be very stable after initial window
        assertTrue(results.size > 20)
        // Skip first few to allow stabilization
        val stableResults = results.drop(10)
        val variance = stableResults.map { (it - targetBpm).pow(2) }.average()
        assertTrue(variance < 0.5, "BPM should be stable in clean streaming signal. Variance: $variance")
    }
}
