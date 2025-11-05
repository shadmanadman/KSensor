package org.kmp.shots.k.sensor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal actual class StateHandler : StateController{
    private val context: Context by lazy { AppContext.get() }
    private val lifecycleOwner = ProcessLifecycleOwner.get()

    private val activeStateObservers = mutableMapOf<StateType, Any>()

    actual override fun addObserver(types: List<StateType>): Flow<StateUpdate> = callbackFlow {
        types.forEach { stateType ->
            if(activeStateObservers.contains(stateType))return@forEach

            when(stateType){
                StateType.SCREEN_STATE -> observerScreenState { trySend(it).isSuccess }
                StateType.APP_VISIBILITY -> observerAppVisibility { trySend(it).isSuccess }
            }.also {
                println("Observer added for $stateType on Android")
            }

            awaitClose { removeObserver(types) }
        }
    }

    actual override fun removeObserver(types: List<StateType>) {
        types.forEach { stateType ->
            when (val listener = activeStateObservers.remove(stateType)) {
                is ScreenStateReceiver -> context.unregisterReceiver(listener)
                is LifecycleEventObserver -> lifecycleOwner.lifecycle.removeObserver(listener)
                else -> println("Observer not found for $stateType on Android")
            }.also {
                println("Observer removed for $stateType on Android")
            }
        }
    }

    @Composable
    actual override fun HandelPermissions(permission: PermissionType, onPermissionStatus: (PermissionStatus) -> Unit) = Unit


    private fun observerAppVisibility(onData: (StateUpdate) -> Boolean) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> onData(StateUpdate.Data(type = StateType.APP_VISIBILITY, StateData.AppVisibilityStatus(
                    AppVisibility.INVISIBLE),PlatformType.Android))
                Lifecycle.Event.ON_START -> onData(StateUpdate.Data(type = StateType.APP_VISIBILITY, StateData.AppVisibilityStatus(
                    AppVisibility.VISIBLE),PlatformType.Android))
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        activeStateObservers[StateType.APP_VISIBILITY] = observer
    }


    private fun observerScreenState(onData: (StateUpdate) -> Boolean) {
        val screenStateReceiver = ScreenStateReceiver(
            onScreenOn = {
                onData(
                    StateUpdate.Data(
                        StateType.SCREEN_STATE,
                        StateData.ScreenStatus(ScreenState.ON),
                        PlatformType.Android
                    )
                )
            },
            onScreenOff = {
                onData(
                    StateUpdate.Data(
                        StateType.SCREEN_STATE,
                        StateData.ScreenStatus(ScreenState.OFF),
                        PlatformType.Android
                    )
                )
            }
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(screenStateReceiver, filter)
        activeStateObservers[StateType.SCREEN_STATE] = screenStateReceiver
    }
}