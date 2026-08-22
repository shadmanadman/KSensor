package com.ksensor.plugins.states.system

import com.ksensor.core.model.StateData
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.*
import platform.darwin.*

@OptIn(ExperimentalForeignApi::class)
object ResourcesProvider {
    private var lastCpuTicks: List<Long>? = null

    fun getCurrentStatus(): StateData.ResourcesStatus {
        val appCpu = getAppCpuUsage()
        val loadAvg = getSystemLoadAvg()
        val memUsage = getMemoryUsage()
        val systemCpu = getSystemCpuUsage()
        
        return StateData.ResourcesStatus(
            cpuUsagePercent = systemCpu,
            appCpuUsagePercent = appCpu,
            systemLoadAverage = loadAvg,
            memoryPressure = getMemoryPressure(),
            memoryUsage = memUsage
        )
    }

    private fun getSystemCpuUsage(): Double = memScoped {
        val count = alloc<mach_msg_type_number_tVar>()
        count.value = (sizeOf<host_cpu_load_info_data_t>() / sizeOf<integer_tVar>()).convert()
        val info = alloc<host_cpu_load_info_data_t>()
        
        val kr = host_statistics(mach_host_self(), HOST_CPU_LOAD_INFO, info.ptr.reinterpret(), count.ptr)
        if (kr != KERN_SUCCESS) return 0.0
        
        val cpuTicks = info.cpu_ticks
        val user = cpuTicks[CPU_STATE_USER].convert<Long>()
        val system = cpuTicks[CPU_STATE_SYSTEM].convert<Long>()
        val idle = cpuTicks[CPU_STATE_IDLE].convert<Long>()
        val nice = cpuTicks[CPU_STATE_NICE].convert<Long>()
        
        val currentTicks = listOf(user, system, idle, nice)
        val last = lastCpuTicks
        lastCpuTicks = currentTicks
        
        if (last == null) return 0.0
        
        val diffUser = user - last[0]
        val diffSystem = system - last[1]
        val diffIdle = idle - last[2]
        val diffNice = nice - last[3]
        
        val total = diffUser + diffSystem + diffIdle + diffNice
        if (total == 0L) return 0.0
        
        return (diffUser + diffSystem + diffNice).toDouble() / total.toDouble() * 100.0
    }

    private fun getSystemLoadAvg(): List<Double> {
        val load = DoubleArray(3)
        val result = load.usePinned { 
            getloadavg(it.addressOf(0), 3)
        }
        return if (result != -1) load.toList() else emptyList()
    }

    private fun getAppCpuUsage(): Double = memScoped {
        var totalUsage = 0.0
        val threadList = alloc<thread_array_tVar>()
        val threadCount = alloc<mach_msg_type_number_tVar>()

        val kr = task_threads(mach_task_self(), threadList.ptr, threadCount.ptr)
        if (kr != KERN_SUCCESS) return 0.0

        val threads = threadList.value!!
        val count = threadCount.value.toInt()

        for (i in 0 until count) {
            val threadInfo = alloc<thread_basic_info_data_t>()
            val threadInfoCount = alloc<mach_msg_type_number_tVar>()
            threadInfoCount.value = THREAD_BASIC_INFO_COUNT.convert()

            val krThread = thread_info(
                threads[i],
                THREAD_BASIC_INFO.convert(),
                threadInfo.ptr.reinterpret(),
                threadInfoCount.ptr
            )

            if (krThread == KERN_SUCCESS) {
                if ((threadInfo.flags.toInt() and TH_FLAGS_IDLE) == 0) {
                    totalUsage += threadInfo.cpu_usage.toDouble() / TH_USAGE_SCALE.toDouble() * 100.0
                }
            }
        }

        vm_deallocate(mach_task_self(), threads.toLong().convert(), (count * sizeOf<thread_t>()).convert())
        
        totalUsage
    }

    private fun getMemoryUsage(): StateData.ResourcesStatus.MemoryUsage = memScoped {
        val info = alloc<task_vm_info_data_t>()
        val count = alloc<mach_msg_type_number_tVar>()
        count.value = (sizeOf<task_vm_info_data_t>() / sizeOf<integer_tVar>()).convert()

        val kr = task_info(
            mach_task_self(),
            TASK_VM_INFO.convert(),
            info.ptr.reinterpret(),
            count.ptr
        )

        val used = if (kr == KERN_SUCCESS) info.phys_footprint.convert<Long>() else 0L
        val total = NSProcessInfo.processInfo.physicalMemory.convert<Long>()
        
        StateData.ResourcesStatus.MemoryUsage(
            totalBytes = total,
            usedBytes = used,
            freeBytes = total - used
        )
    }

    private fun getMemoryPressure(): StateData.ResourcesStatus.MemoryPressureLevel {
        // Simple heuristic for now. In a real app we'd listen to notifications.
        return if (NSProcessInfo.processInfo.lowPowerModeEnabled) {
            StateData.ResourcesStatus.MemoryPressureLevel.MODERATE
        } else {
            StateData.ResourcesStatus.MemoryPressureLevel.NORMAL
        }
    }
}
