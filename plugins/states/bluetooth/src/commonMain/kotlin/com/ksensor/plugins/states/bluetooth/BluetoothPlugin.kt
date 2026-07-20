package com.ksensor.plugins.states.bluetooth

import com.ksensor.core.KSensorPlugin
import com.ksensor.core.StatePlugin
import com.ksensor.core.model.StateData

interface BluetoothPlugin : KSensorPlugin {
    fun connections(): StatePlugin<StateData.BleConnectionStatus>
    fun discoveries(): StatePlugin<StateData.BleDiscoversStatus>
    fun audioDevices(): StatePlugin<StateData.BleConnectionStatus>
}

expect fun createBluetoothPlugin(): BluetoothPlugin
