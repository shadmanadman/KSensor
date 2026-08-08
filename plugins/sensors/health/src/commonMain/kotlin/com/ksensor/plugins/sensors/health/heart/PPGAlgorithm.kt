package com.ksensor.plugins.sensors.health.heart

import kotlin.math.*

class PPGAlgorithm(
    private val targetSamplingRate: Double = 30.0,
    windowSeconds: Int = 10
) {
    private val windowSize = (targetSamplingRate * windowSeconds).toInt()
    
    // Buffers for resampled, filtered data
    private val filteredBuffer = mutableListOf<Double>()
    private val resampledTimestamps = mutableListOf<Long>()
    
    // Resampling state
    private var lastInputTimestamp: Long = -1
    private var lastInputValue: Double = 0.0
    private var nextResampleTime: Long = -1
    private val resampleIntervalMs = (1000.0 / targetSamplingRate).toLong()

    // Filter state
    private val filter = ButterworthBandpass(0.7, 4.0, targetSamplingRate)
    
    // BPM stabilization
    private var smoothedBpm = 0.0f
    private val bpmAlpha = 0.15f
    private var lastValidBpmTime = 0L

    data class AnalysisResult(
        val bpm: Float,
        val confidence: Float,
        val quality: Float,
        val fingerDetected: Boolean
    )

    fun processSample(value: Double, timestamp: Long): AnalysisResult? {
        // 0. Finger detection (Basic)
        // Red channel in PPG should be high, and not too dark/saturated
        // Assuming value is avg of Y + 1.4*(V-128)
        val fingerDetected = value > 10.0 && value < 250.0

        if (!fingerDetected) {
            resetState()
            return AnalysisResult(0f, 0f, 0f, false)
        }
        if (lastInputTimestamp == -1L) {
            lastInputTimestamp = timestamp
            lastInputValue = value
            nextResampleTime = timestamp
            return null
        }

        while (nextResampleTime <= timestamp) {
            if (nextResampleTime > lastInputTimestamp) {
                // Linear interpolation
                val t = (nextResampleTime - lastInputTimestamp).toDouble() / (timestamp - lastInputTimestamp)
                val interpolatedValue = lastInputValue + t * (value - lastInputValue)
                
                // 2. Streaming Filtering
                val filteredValue = filter.process(interpolatedValue)
                
                filteredBuffer.add(filteredValue)
                resampledTimestamps.add(nextResampleTime)
                
                if (filteredBuffer.size > windowSize) {
                    filteredBuffer.removeAt(0)
                    resampledTimestamps.removeAt(0)
                }
            }
            nextResampleTime += resampleIntervalMs
        }

        lastInputTimestamp = timestamp
        lastInputValue = value

        // 3. Analysis (need enough data)
        if (filteredBuffer.size < windowSize * 0.5) { // Lower threshold for initial data
            return AnalysisResult(0f, 0f, calculateBaseQuality(), true)
        }

        return analyzeWindow()
    }

    private fun resetState() {
        filteredBuffer.clear()
        resampledTimestamps.clear()
        lastInputTimestamp = -1
        nextResampleTime = -1
    }

    private fun analyzeWindow(): AnalysisResult {
        // Detect peaks in the filtered signal
        val peaks = detectPeaks(filteredBuffer)
        
        // Physiological validation: 40 - 200 BPM
        // 40 BPM -> 1500ms, 200 BPM -> 300ms
        val validIntervals = mutableListOf<Long>()
        for (i in 1 until peaks.size) {
            val interval = resampledTimestamps[peaks[i]] - resampledTimestamps[peaks[i - 1]]
            if (interval in 300..1500) {
                validIntervals.add(interval)
            }
        }

        // Require at least 3 intervals (4 beats) for a valid estimate
        if (validIntervals.size < 3) {
            return AnalysisResult(0f, 0f, calculateBaseQuality(), true)
        }

        // Robust BPM calculation using median IBI
        val sortedIntervals = validIntervals.sorted()
        val medianInterval = sortedIntervals[sortedIntervals.size / 2]
        val rawBpm = 60000.0f / medianInterval

        // Quality and Confidence assessment
        val quality = calculateQuality(validIntervals, filteredBuffer)
        // More lenient confidence for displaying
        val confidence = if (quality > 0.2) 0.4f + (quality * 0.6f) else 0f
        
        val now = resampledTimestamps.last()
        // Stabilization
        if (confidence > 0.4f) {
            smoothedBpm = if (smoothedBpm == 0f) rawBpm else smoothedBpm * (1 - bpmAlpha) + rawBpm * bpmAlpha
            lastValidBpmTime = now
        }

        return AnalysisResult(smoothedBpm, confidence, quality, true)
    }

    private fun detectPeaks(data: List<Double>): List<Int> {
        val peaks = mutableListOf<Int>()
        val minPeakDistance = (targetSamplingRate * 0.25).toInt() // ~240 BPM limit
        
        // Use a moving window for local thresholding to avoid global noise spikes
        val windowSize = (targetSamplingRate * 2).toInt() // 2 sec local window
        
        for (i in 1 until data.size - 1) {
            if (data[i] > data[i - 1] && data[i] > data[i + 1]) {
                // Local threshold check
                val start = max(0, i - windowSize / 2)
                val end = min(data.size, i + windowSize / 2)
                val localData = data.subList(start, end)
                
                val localMean = localData.average()
                val localStd = sqrt(localData.map { (it - localMean).pow(2) }.average())
                
                // Peak must be significantly above local mean
                if (data[i] > localMean + localStd * 0.5 && data[i] > 0.01) {
                    if (peaks.isEmpty() || i - peaks.last() >= minPeakDistance) {
                        peaks.add(i)
                    }
                }
            }
        }
        return peaks
    }

    private fun calculateBaseQuality(): Float {
        if (filteredBuffer.isEmpty()) return 0f
        val mean = filteredBuffer.average()
        val std = sqrt(filteredBuffer.map { (it - mean).pow(2) }.average())
        // If signal is flat or too noisy (standardized red channel)
        return if (std < 0.001) 0f else 0.1f
    }

    private fun calculateQuality(intervals: List<Long>, signal: List<Double>): Float {
        if (intervals.isEmpty()) return 0f
        
        // 1. Consistency of intervals (Coefficient of Variation)
        val avgInterval = intervals.average()
        val stdInterval = sqrt(intervals.map { (it - avgInterval).pow(2) }.average())
        val cv = stdInterval / avgInterval
        val consistency = (1.0 - cv * 3.0).coerceIn(0.0, 1.0)
        
        // 2. Peak prominence (Signal strength relative to noise)
        val mean = signal.average()
        val std = sqrt(signal.map { (it - mean).pow(2) }.average())
        // A good PPG signal should have clear peaks
        val peakQuality = if (std > 0.01) 1.0 else std / 0.01
        
        return (consistency * peakQuality).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Proper Butterworth Bandpass Filter (Cascade of 2nd order LP and HP).
     * This provides a flatter passband and better roll-off than a simple biquad.
     */
    private class ButterworthBandpass(lowCutoff: Double, highCutoff: Double, fs: Double) {
        private val lp = LowPassFilter(highCutoff, fs)
        private val hp = HighPassFilter(lowCutoff, fs)

        fun process(sample: Double): Double {
            return hp.process(lp.process(sample))
        }

        private class LowPassFilter(cutoff: Double, fs: Double) {
            private val b = DoubleArray(3)
            private val a = DoubleArray(3)
            private val x = DoubleArray(3)
            private val y = DoubleArray(3)

            init {
                val ff = cutoff / fs
                val ita = 1.0 / tan(PI * ff)
                val q = sqrt(2.0)
                b[0] = 1.0 / (1.0 + q * ita + ita * ita)
                b[1] = 2.0 * b[0]
                b[2] = b[0]
                a[1] = 2.0 * (ita * ita - 1.0) * b[0]
                a[2] = -(1.0 - q * ita + ita * ita) * b[0]
            }

            fun process(sample: Double): Double {
                x[2] = x[1]; x[1] = x[0]; x[0] = sample
                y[2] = y[1]; y[1] = y[0]
                y[0] = b[0] * x[0] + b[1] * x[1] + b[2] * x[2] + a[1] * y[1] + a[2] * y[2]
                return y[0]
            }
        }

        private class HighPassFilter(cutoff: Double, fs: Double) {
            private val b = DoubleArray(3)
            private val a = DoubleArray(3)
            private val x = DoubleArray(3)
            private val y = DoubleArray(3)

            init {
                val ff = cutoff / fs
                val ita = 1.0 / tan(PI * ff)
                val q = sqrt(2.0)
                b[0] = 1.0 / (1.0 + q * ita + ita * ita)
                b[1] = -2.0 * b[0]
                b[2] = b[0]
                a[1] = 2.0 * (ita * ita - 1.0) * b[0]
                a[2] = -(1.0 - q * ita + ita * ita) * b[0]
                
                // Adjustment for HP
                b[0] *= ita * ita
                b[1] *= ita * ita
                b[2] *= ita * ita
            }

            fun process(sample: Double): Double {
                x[2] = x[1]; x[1] = x[0]; x[0] = sample
                y[2] = y[1]; y[1] = y[0]
                y[0] = b[0] * x[0] + b[1] * x[1] + b[2] * x[2] + a[1] * y[1] + a[2] * y[2]
                return y[0]
            }
        }
    }
}
