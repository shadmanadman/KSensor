package com.ksensor.plugins.sensors.motion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.ksensor.core.model.SensorData
import kotlinx.coroutines.flow.MutableSharedFlow

class ActivityRecognitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            result?.let {
                val mostProbableActivity = it.mostProbableActivity
                val motionType = when (mostProbableActivity.type) {
                    DetectedActivity.WALKING -> SensorData.MotionType.WALKING
                    DetectedActivity.RUNNING -> SensorData.MotionType.RUNNING
                    DetectedActivity.ON_BICYCLE -> SensorData.MotionType.CYCLING
                    DetectedActivity.IN_VEHICLE -> SensorData.MotionType.AUTOMOTIVE
                    DetectedActivity.STILL -> SensorData.MotionType.STATIONARY
                    else -> SensorData.MotionType.UNKNOWN
                }
                motionEvents.tryEmit(motionType)
            }
        }
    }

    companion object {
        val motionEvents = MutableSharedFlow<SensorData.MotionType>(extraBufferCapacity = 1)
    }
}
