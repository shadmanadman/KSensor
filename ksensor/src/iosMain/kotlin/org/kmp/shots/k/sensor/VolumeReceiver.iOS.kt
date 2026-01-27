package org.kmp.shots.k.sensor

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.outputVolume
import platform.AVFAudio.setActive
import platform.Foundation.NSNotificationCenter

@OptIn(ExperimentalForeignApi::class)
class VolumeReceiver {
    val session = AVAudioSession.sharedInstance()

    fun registerObserver(onVolumeChange: (Int) -> Unit) {
        session.setActive(true, null)

        NSNotificationCenter.defaultCenter.addObserverForName(
            name = "SystemVolumeDidChange",
            `object` = null,
            queue = null
        ) { notification ->
            val volume =
                notification?.userInfo?.get("AVSystemController_AudioVolumeNotificationParameter") as? Float
            volume?.let { onVolumeChange(it.toInt()) }
        }
    }

    fun removeObserver() {
        session.setActive(false, null)
        NSNotificationCenter.defaultCenter.removeObserver(
            observer = {},
            name = "SystemVolumeDidChange",
            `object` = null
        )
    }

    fun getCurrentVolume(): Int {
        return AVAudioSession.sharedInstance().outputVolume.toInt()
    }
}