package com.ksensor.plugins.states.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.ksensor.core.model.BleDevice
import com.ksensor.core.model.StateData

internal class BleDiscoversReceiver(
    private val context: Context,
    private val onData: (StateData.BleDiscoversStatus) -> Unit,
    private val onError: (Exception) -> Unit
) : BroadcastReceiver() {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bleScanner get() = bluetoothAdapter?.bluetoothLeScanner

    private val discoveredDevices = mutableMapOf<String, BleDevice>()
    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val bleDevice = BleDevice(
                id = device.address,
                name = device.name ?: "Unknown Device"
            )
            discoveredDevices[bleDevice.id] = bleDevice
            emitCurrentState()
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            onError(Exception("BLE Scan failed with error code: $errorCode"))
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            if (state == BluetoothAdapter.STATE_ON) {
                startScanInternal()
            } else if (state == BluetoothAdapter.STATE_OFF) {
                stopScanInternal()
                discoveredDevices.clear()
                emitCurrentState()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun register() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(this, filter)
        }
        startScanInternal()
    }

    @SuppressLint("MissingPermission")
    fun unregister() {
        try {
            context.unregisterReceiver(this)
        } catch (_: Exception) {
        }
        stopScanInternal()
    }

    @SuppressLint("MissingPermission")
    private fun startScanInternal() {
        if (isScanning) return
        
        val scanner = bleScanner
        if (scanner != null) {
            if (bluetoothAdapter?.isEnabled == true) {
                try {
                    scanner.startScan(scanCallback)
                    isScanning = true
                } catch (e: Exception) {
                    onError(e)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanInternal() {
        if (!isScanning) return
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (_: Exception) {
        }
        isScanning = false
    }

    private fun emitCurrentState() {
        onData(StateData.BleDiscoversStatus(discoveredDevices.values.toList()))
    }
}
