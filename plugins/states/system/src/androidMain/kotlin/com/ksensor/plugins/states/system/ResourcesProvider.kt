package com.ksensor.plugins.states.system

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.SystemClock
import com.ksensor.core.model.StateData
import java.io.RandomAccessFile

object ResourcesProvider {
    private var lastCpuTime: Long = 0
    private var lastAppTime: Long = 0

    fun getCurrentStatus(context: Context): StateData.ResourcesStatus {
        // Memory
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalMem = memoryInfo.totalMem
        val freeMem = memoryInfo.availMem
        val usedMem = totalMem - freeMem

        // Memory Pressure
        val pressureLevel = when {
            memoryInfo.lowMemory -> StateData.ResourcesStatus.MemoryPressureLevel.CRITICAL
            else -> {
                val processInfo = ActivityManager.RunningAppProcessInfo()
                ActivityManager.getMyMemoryState(processInfo)
                when (processInfo.lastTrimLevel) {
                    ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> StateData.ResourcesStatus.MemoryPressureLevel.CRITICAL
                    ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> StateData.ResourcesStatus.MemoryPressureLevel.MODERATE
                    ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> StateData.ResourcesStatus.MemoryPressureLevel.MODERATE
                    else -> StateData.ResourcesStatus.MemoryPressureLevel.NORMAL
                }
            }
        }

        // App CPU
        val appCpu = getAppCpuUsage()

        // System Load Average
        val loadAvg = getSystemLoadAverage()

        return StateData.ResourcesStatus(
            cpuUsagePercent = 0.0, // Restricted on modern Android
            appCpuUsagePercent = appCpu,
            systemLoadAverage = loadAvg,
            memoryUsage = StateData.ResourcesStatus.MemoryUsage(
                totalBytes = totalMem,
                usedBytes = usedMem,
                freeBytes = freeMem
            ),
            memoryPressure = pressureLevel
        )
    }

    private fun getSystemLoadAverage(): List<Double> {
        return try {
            val reader = RandomAccessFile("/proc/loadavg", "r")
            val line = reader.readLine()
            reader.close()
            if (line == null) return emptyList()
            val tokens = line.split(" ")
            listOf(tokens[0].toDouble(), tokens[1].toDouble(), tokens[2].toDouble())
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getAppCpuUsage(): Double {
        return try {
            val reader = RandomAccessFile("/proc/self/stat", "r")
            val line = reader.readLine()
            reader.close()
            if (line == null) return 0.0
            val tokens = line.split(" ")

            // utime = 14th, stime = 15th, cutime = 16th, cstime = 17th
            // Tokens are 0-indexed, so index 13, 14, 15, 16
            val cpuTime = tokens[13].toLong() + tokens[14].toLong() +
                    tokens[15].toLong() + tokens[16].toLong()
            val appTime = SystemClock.elapsedRealtime()

            val usage = if (lastAppTime != 0L) {
                val timeDiff = appTime - lastAppTime
                val cpuDiff = cpuTime - lastCpuTime
                // Rough calculation assuming 100 ticks per second (10ms per tick)
                if (timeDiff > 0) {
                    (cpuDiff * 1000.0 / timeDiff) / 10.0 
                } else {
                    0.0
                }
            } else {
                0.0
            }

            lastCpuTime = cpuTime
            lastAppTime = appTime
            usage
        } catch (e: Exception) {
            0.0
        }
    }
}
