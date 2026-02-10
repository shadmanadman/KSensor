package state

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
private const val streamType = AudioManager.STREAM_MUSIC

class VolumeReceiver(val audioManager: AudioManager, val onVolumeChange: (Int) -> Unit) :
    BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == VOLUME_CHANGED_ACTION)
            onVolumeChange(audioManager.getStreamVolume(streamType))
    }

    fun getCurrentVolume():Int{
        return audioManager.getStreamVolume(streamType)
    }
}