package com.ksensor.plugins.states.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.ksensor.core.model.BleDevice
import com.ksensor.core.model.StateData

internal class BleConnectionReceiver(
    private val context: Context,
    private val onData: (StateData.BleConnectionStatus) -> Unit
) : BroadcastReceiver() {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    
    private val aclConnectedDevices = mutableMapOf<String, BluetoothDevice>()
    private val proxies = mutableMapOf<Int, BluetoothProfile>()

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            proxies[profile] = proxy
            emitCurrentState()
        }

        override fun onServiceDisconnected(profile: Int) {
            proxies.remove(profile)
            emitCurrentState()
        }
    }

    private val monitoredProfiles = listOf(
        BluetoothProfile.A2DP,
        BluetoothProfile.HEADSET,
        4 // BluetoothProfile.HID_HOST (some versions might not have the constant accessible easily)
    )

    fun register() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(this, filter)
        }

        monitoredProfiles.forEach { profile ->
            try {
                bluetoothAdapter?.getProfileProxy(context, profileListener, profile)
            } catch (_: Exception) {
            }
        }

        emitCurrentState()
    }

    fun unregister() {
        try {
            context.unregisterReceiver(this)
        } catch (_: Exception) {
        }
        
        proxies.forEach { (profile, proxy) ->
            try {
                bluetoothAdapter?.closeProfileProxy(profile, proxy)
            } catch (_: Exception) {
            }
        }
        proxies.clear()
        aclConnectedDevices.clear()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                device?.let { aclConnectedDevices[it.address] = it }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                device?.let { aclConnectedDevices.remove(it.address) }
            }
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF) {
                    aclConnectedDevices.clear()
                }
            }
        }
        emitCurrentState()
    }

    @SuppressLint("MissingPermission")
    fun emitCurrentState() {
        val connectedDevices = mutableSetOf<BluetoothDevice>()
        if (bluetoothAdapter?.isEnabled == true) {
            // 1. Check GATT and GATT_SERVER via BluetoothManager
            try {
                bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).forEach { connectedDevices.add(it) }
            } catch (_: Exception) {}
            try {
                bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER).forEach { connectedDevices.add(it) }
            } catch (_: Exception) {}

            // 2. Check all proxies
            proxies.values.forEach { proxy ->
                try {
                    proxy.connectedDevices.forEach { connectedDevices.add(it) }
                } catch (_: Exception) {}
            }

            // 3. Include devices tracked via ACL broadcasts
            connectedDevices.addAll(aclConnectedDevices.values)
        } else {
            aclConnectedDevices.clear()
        }

        val bleDevices = connectedDevices.map { device ->
            val name = try {
                device.name
            } catch (_: Exception) {
                null
            } ?: "Unknown Device"
            
            BleDevice(
                id = device.address,
                name = name
            )
        }

        onData(StateData.BleConnectionStatus(bleDevices))
    }
}
