package com.ksensor.plugins.states.system

import com.ksensor.core.model.StateData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSFileSystemSize
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNumber

object StorageProvider {
    @OptIn(ExperimentalForeignApi::class)
    fun getCurrentStatus(): StateData.StorageStatus {
        val fileManager = NSFileManager.defaultManager
        val path = NSHomeDirectory()
        val attributes = fileManager.attributesOfFileSystemForPath(path, null) ?: return StateData.StorageStatus(0, 0, 0)
        
        val total = (attributes[NSFileSystemSize] as? NSNumber)?.longLongValue ?: 0L
        val free = (attributes[NSFileSystemFreeSize] as? NSNumber)?.longLongValue ?: 0L
        val used = total - free
        
        return StateData.StorageStatus(
            totalBytes = total,
            usedBytes = used,
            freeBytes = free
        )
    }
}
