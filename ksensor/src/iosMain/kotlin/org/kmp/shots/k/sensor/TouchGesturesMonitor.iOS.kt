package org.kmp.shots.k.sensor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.*
import kotlin.concurrent.Volatile

internal object TouchGesturesMonitor {

    @Volatile
    private var observer: ((SensorUpdate) -> Unit)? = null

    private var window: TouchInterceptingWindow? = null

    fun registerObserver(onData: (SensorUpdate) -> Unit) {
        observer = onData
        installWindowIfNeeded()
    }

    fun removeObserver() {
        observer = null
    }

    internal fun dispatch(update: SensorUpdate) {
        observer?.invoke(update)
    }

    private fun installWindowIfNeeded() {
        if (window != null) return

        val scene = UIApplication.sharedApplication
            .connectedScenes
            .firstOrNull() as? UIWindowScene ?: return

        window = TouchInterceptingWindow(scene).apply {
            rootViewController = UIViewController()
            makeKeyAndVisible()
        }
    }
}


internal class TouchInterceptingWindow(
    scene: UIWindowScene
) : UIWindow(scene) {

    private fun updateTouchGesture(
        x: Float,
        y: Float,
        type: TouchGestureType
    ) {
        TouchGesturesMonitor.dispatch(
            SensorUpdate.Data(
                type = SensorType.TOUCH_GESTURES,
                data = SensorData.TouchGestures(
                    x = x,
                    y = y,
                    type = type
                ),
                platformType = PlatformType.iOS
            )
        )
    }

    override fun touchesBegan(
        touches: Set<*>,
        withEvent: UIEvent?
    ) {
        super.touchesBegan(touches, withEvent)
        handleTouches(touches, TouchGestureType.ACTION_DOWN)
    }

    override fun touchesMoved(
        touches: Set<*>,
        withEvent: UIEvent?
    ) {
        super.touchesMoved(touches, withEvent)
        handleTouches(touches, TouchGestureType.ACTION_MOVE)
    }

    override fun touchesEnded(
        touches: Set<*>,
        withEvent: UIEvent?
    ) {
        super.touchesEnded(touches, withEvent)
        handleTouches(touches, TouchGestureType.ACTION_UP)
    }

    override fun touchesCancelled(
        touches: Set<*>,
        withEvent: UIEvent?
    ) {
        super.touchesCancelled(touches, withEvent)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun handleTouches(
        touches: Set<*>,
        type: TouchGestureType
    ) {
        val touch = touches.firstOrNull() as? UITouch ?: return
        val point = touch.locationInView(this)

        point.useContents {
            updateTouchGesture(
                x = x.toFloat(),
                y = y.toFloat(),
                type = type
            )
        }
    }
}
