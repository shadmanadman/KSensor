package org.kmp.ksensor.state

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.outputVolume
import platform.AVFAudio.setActive
import platform.Foundation.NSKeyValueChangeNewKey
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.darwin.NSObject
import platform.foundation.NSKeyValueObservingProtocol

@OptIn(ExperimentalForeignApi::class)
class VolumeReceiver {
    private val session = AVAudioSession.sharedInstance()
    private var observer: VolumeObserver? = null
    private val volumeKey = "outputVolume"

    private inner class VolumeObserver(val onVolumeChange: (Int) -> Unit) : NSObject(),
        NSKeyValueObservingProtocol {
        override fun observeValueForKeyPath(
            keyPath: String?,
            ofObject: Any?,
            change: Map<Any?, *>?,
            context: COpaquePointer?
        ) {
            if (keyPath == volumeKey) {
                val volume = change?.get(NSKeyValueChangeNewKey) as? Float ?: return
                onVolumeChange((volume * 100).toInt())
            }
        }
    }

    init {
        session.setCategory(
            AVAudioSessionCategoryAmbient,
            error = null
        )
        session.setActive(true, error = null)
    }

    fun registerObserver(onVolumeChange: (Int) -> Unit) {
        val newObserver = VolumeObserver(onVolumeChange)
        observer = newObserver

        session.addObserver(
            observer = newObserver,
            forKeyPath = volumeKey,
            options = NSKeyValueObservingOptionNew,
            context = null
        )
    }

    fun removeObserver() {
        observer?.let {
            session.removeObserver(it, volumeKey)
            observer = null
        }
    }

    fun getCurrentVolume(): Int {
        return (session.outputVolume * 100).toInt()
    }
}