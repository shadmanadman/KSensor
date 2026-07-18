package com.ksensor.plugins.states.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

internal const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
internal const val STREAM_TYPE = AudioManager.STREAM_MUSIC

internal class VolumeReceiver(
    private val audioManager: AudioManager,
    private val onVolumeChange: (Int) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == VOLUME_CHANGED_ACTION) {
            onVolumeChange(audioManager.getVolumePercentage(STREAM_TYPE))
        }
    }
}

internal fun AudioManager.getVolumePercentage(streamType: Int): Int {
    val current = getStreamVolume(streamType)
    val max = getStreamMaxVolume(streamType)
    return if (max != 0) (current * 100) / max else 0
}
