package com.ksensor.plugins.states.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.ksensor.core.Permission
import com.ksensor.core.model.PluginId
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.KSensorResponse
import com.ksensor.core.model.StateData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn

class AndroidLifecyclePlugin : LifecyclePlugin {
    override val id: PluginId = PluginId.LIFECYCLE
    override val requiredPermissions: List<Permission> = emptyList()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val appVisibilityFlow by lazy {
        callbackFlow {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> trySend(KSensorResponse(StateData.AppVisibilityStatus(true)))
                    Lifecycle.Event.ON_STOP -> trySend(KSensorResponse(StateData.AppVisibilityStatus(false)))
                    else -> Unit
                }
            }
            ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
            awaitClose { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
        }.shareIn(scope, SharingStarted.WhileSubscribed(5000), 1)
    }

    private val appVisibilityPlugin = object : StatePlugin<StateData.AppVisibilityStatus> {
        override val id: PluginId = PluginId.LIFECYCLE
        override val requiredPermissions: List<Permission> = emptyList()
        override val currentState: KSensorResponse<StateData.AppVisibilityStatus>
            get() = KSensorResponse(StateData.AppVisibilityStatus(true)) // Placeholder

        override fun observe(): Flow<KSensorResponse<StateData.AppVisibilityStatus>> = appVisibilityFlow
    }

    override fun appVisibility(): StatePlugin<StateData.AppVisibilityStatus> = appVisibilityPlugin
}

actual fun createLifecyclePlugin(): LifecyclePlugin = AndroidLifecyclePlugin()
