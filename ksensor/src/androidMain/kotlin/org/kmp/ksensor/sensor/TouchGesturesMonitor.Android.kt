package org.kmp.ksensor.sensor

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.KeyboardShortcutGroup
import android.view.Menu
import android.view.MotionEvent
import android.view.Window
import androidx.annotation.RequiresApi
import org.kmp.ksensor.context.AppContext

/**
 * On Android the TouchGesturesMonitor attaches window callbacks
 * lazily to currently active Activities and future Activities.
 */
internal object TouchGesturesMonitor {
    private val context: Context by lazy { AppContext.get() }

    private val app = context.applicationContext as Application
    @Volatile
    private var observer: ((SensorUpdate) -> Unit)? = null

    /** Start tracking Activity lifecycles immediately on startup
    This ensures the first Activity is caught **/
    fun init() {
        app.registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks)
    }
    fun registerObserver(onData: (SensorUpdate) -> Unit) {
        observer = onData
    }

    fun removeObserver(){
        observer = null
        app.unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks)
    }

    private fun hookWindowCallback(activity: Activity) {
        val window = activity.window
        if (window.callback is TouchInterceptingCallback) return

        val originalCallback = window.callback
        window.callback = TouchInterceptingCallback(originalCallback) {
            observer?.invoke(it)
        }
    }
    private object ActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(
            activity: Activity,
            savedInstanceState: Bundle?
        ) {
            hookWindowCallback(activity)
        }

        override fun onActivityDestroyed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivitySaveInstanceState(
            activity: Activity,
            outState: Bundle
        ) {}
        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {}

    }

}


internal class TouchInterceptingCallback(
    private val delegate: Window.Callback,
    private val onData: (SensorUpdate) -> Unit
) : Window.Callback by delegate {

    private fun updateTouchGesture(
        x: Float,
        y: Float,
        type: TouchGestureType
    ) {
        onData(
            SensorUpdate.Data(
                type = SensorType.TOUCH_GESTURES,
                data = SensorData.TouchGestures(
                    x = x,
                    y = y,
                    type = type
                ), platformType = PlatformType.Android
            )
        )
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                updateTouchGesture(event.rawX, event.rawY, TouchGestureType.ACTION_DOWN)
            }

            MotionEvent.ACTION_MOVE -> {
                updateTouchGesture(event.rawX, event.rawY, TouchGestureType.ACTION_MOVE)
            }

            MotionEvent.ACTION_UP -> {
                updateTouchGesture(event.rawX, event.rawY, TouchGestureType.ACTION_UP)
            }

        }
        return delegate.dispatchTouchEvent(event)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPointerCaptureChanged(hasCapture: Boolean) {
        delegate.onPointerCaptureChanged(hasCapture)
    }

    override fun onProvideKeyboardShortcuts(
        data: List<KeyboardShortcutGroup?>?,
        menu: Menu?,
        deviceId: Int
    ) {
        delegate.onProvideKeyboardShortcuts(data, menu, deviceId)
    }
}