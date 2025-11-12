package org.kmp.shots.k.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal object AndroidStateControllerFactory: StateControllerFactory{
    override fun create(): StateController = StateHandler()
}
internal class StateHandler : StateController {
    private val context: Context by lazy { AppContext.get() }
    private val lifecycleOwner = ProcessLifecycleOwner.get()
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private lateinit var connectivityMonitor: ConnectivityMonitor
    private val activeStateObservers = mutableMapOf<StateType, Any>()

    override fun addObserver(types: List<StateType>): Flow<StateUpdate> = callbackFlow {
        types.forEach { stateType ->
            if (activeStateObservers.contains(stateType)) return@forEach

            when (stateType) {
                StateType.SCREEN -> observerScreenState { trySend(it).isSuccess }
                StateType.APP_VISIBILITY -> observerAppVisibility { trySend(it).isSuccess }
                StateType.CONNECTIVITY, StateType.ACTIVE_NETWORK -> observeConnectivity { trySend(it).isSuccess }
                StateType.LOCATION -> observerLocation { trySend(it).isSuccess }
            }.also {
                println("Observer added for $stateType on Android")
            }

            awaitClose { removeObserver(types) }
        }
    }

    override fun removeObserver(types: List<StateType>) {
        types.forEach { stateType ->
            when (val listener = activeStateObservers.remove(stateType)) {
                is ScreenStateReceiver -> context.unregisterReceiver(listener)
                is LocationProviderReceiver -> context.unregisterReceiver(listener)
                is LifecycleEventObserver -> lifecycleOwner.lifecycle.removeObserver(listener)
                is ConnectivityManager -> connectivityManager.unregisterNetworkCallback(
                    connectivityMonitor
                )

                else -> println("Observer not found for $stateType on Android")
            }.also {
                println("Observer removed for $stateType on Android")
            }
        }
    }

    @Composable
    override fun HandelPermissions(
        permission: PermissionType,
        onPermissionStatus: (PermissionStatus) -> Unit
    ) = Unit

    private fun observerLocation(onData: (StateUpdate) -> Boolean) {
        val isLocationOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        onData(
            StateUpdate.Data(
                type = StateType.LOCATION,
                data= StateData.LocationStatus(isLocationOn),
                platformType = PlatformType.Android
            )
        )

        val locationReceiver = LocationProviderReceiver(onProviderChanged = {
            onData(
                StateUpdate.Data(
                    type = StateType.LOCATION,
                    data= StateData.LocationStatus(isLocationOn),
                    platformType = PlatformType.Android
                )
            )
        })

        context.registerReceiver(locationReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        activeStateObservers[StateType.LOCATION] = locationReceiver
    }

    @SuppressLint("MissingPermission")
    private fun observeConnectivity(onData: (StateUpdate) -> Boolean) {
        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityMonitor = ConnectivityMonitor(onStatusChanged = {
            onData(
                StateUpdate.Data(
                    type = StateType.CONNECTIVITY,
                    StateData.ConnectivityStatus(
                        isConnected = it
                    ),
                    PlatformType.Android
                )
            )
        }, onActiveNetworkChanged = {
            StateUpdate.Data(
                type = StateType.ACTIVE_NETWORK,
                StateData.CurrentActiveNetwork(
                    activeNetwork = it
                ),
                PlatformType.Android
            )
        })
        connectivityManager.registerNetworkCallback(networkRequest, connectivityMonitor)
        activeStateObservers[StateType.CONNECTIVITY] = connectivityManager
    }

    private fun observerAppVisibility(onData: (StateUpdate) -> Boolean) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> onData(
                    StateUpdate.Data(
                        type = StateType.APP_VISIBILITY, StateData.AppVisibilityStatus(
                            false
                        ), PlatformType.Android
                    )
                )

                Lifecycle.Event.ON_START -> onData(
                    StateUpdate.Data(
                        type = StateType.APP_VISIBILITY, StateData.AppVisibilityStatus(
                            true
                        ), PlatformType.Android
                    )
                )

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
                        StateType.SCREEN,
                        StateData.ScreenStatus(true),
                        PlatformType.Android
                    )
                )
            },
            onScreenOff = {
                onData(
                    StateUpdate.Data(
                        StateType.SCREEN,
                        StateData.ScreenStatus(false),
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
        activeStateObservers[StateType.SCREEN] = screenStateReceiver
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun getTransportStatus(): StateData.CurrentActiveNetwork.ActiveNetwork {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork
            ?: return StateData.CurrentActiveNetwork.ActiveNetwork.NONE
        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return StateData.CurrentActiveNetwork.ActiveNetwork.NONE

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            return StateData.CurrentActiveNetwork.ActiveNetwork.WIFI
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            return StateData.CurrentActiveNetwork.ActiveNetwork.CELLULAR
        return StateData.CurrentActiveNetwork.ActiveNetwork.NONE
    }
}