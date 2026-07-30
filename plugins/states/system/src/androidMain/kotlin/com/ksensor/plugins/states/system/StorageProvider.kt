package com.ksensor.plugins.states.system

import android.os.Environment
import android.os.StatFs
import com.ksensor.core.model.StateData

object StorageProvider {
    fun getCurrentStatus(): StateData.StorageStatus {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        
        val total = totalBlocks * blockSize
        val free = availableBlocks * blockSize
        val used = total - free
        
        return StateData.StorageStatus(
            totalBytes = total,
            usedBytes = used,
            freeBytes = free
        )
    }
}
