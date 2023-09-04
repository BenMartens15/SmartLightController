package com.benmartens15.smartlightcontroller.ui.devices

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import android.util.Log
import com.benmartens15.smartlightcontroller.ble.ConnectionEventListener
import com.benmartens15.smartlightcontroller.ble.ConnectionManager
import com.benmartens15.smartlightcontroller.ble.toHexString
import java.util.Locale
import java.util.UUID

private val RGB_CTRL_CHARACTERISTIC_UUID = UUID.fromString("f19a2445-96fe-4d87-a476-68a7a8d0b7ba")
private val INFO_CHARACTERISTIC_UUID = UUID.fromString("f19a2446-96fe-4d87-a476-68a7a8d0b7ba")

enum class LightState {
    OFF,
    ON
}

enum class DeviceType {
    RGB_CONTROLLER,
    LIGHT_SWITCH,
    UNKNOWN
}

@SuppressLint("MissingPermission") // probably figure out how to handle this properly at some point
class LightningLightController(scanResult: ScanResult) {

    val bleDevice: BluetoothDevice = scanResult.device
    var advertisedName: String = scanResult.device.name
    var displayName: String? = null
    private val advData: ByteArray = scanResult.scanRecord!!.bytes
    val state: LightState = if (advData[25].toInt() == 0) {
        LightState.OFF
    } else {
        LightState.ON
    }
    val deviceType = if (advData[26].toInt() == 0) {
        DeviceType.RGB_CONTROLLER
    } else if (advData[26].toInt() == 1) {
        DeviceType.LIGHT_SWITCH
    } else {
        DeviceType.UNKNOWN
    }
    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(bleDevice)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }

    private val rgbControlCharacteristic: BluetoothGattCharacteristic by lazy {
        val uuid = characteristics.firstOrNull { it.uuid == RGB_CTRL_CHARACTERISTIC_UUID }
        uuid ?: throw NoSuchElementException("Control characteristic not found")
    }
    private val infoCharacteristic: BluetoothGattCharacteristic by lazy {
        val uuid = characteristics.firstOrNull { it.uuid == INFO_CHARACTERISTIC_UUID }
        uuid ?: throw NoSuchElementException("Info characteristic not found")
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            device = bleDevice
            onConnectionSetupComplete = {
                Log.i("LightningLightController", "Connection complete")
                readName()
            }

            onCharacteristicRead = { _, characteristic ->
                Log.i("LightningLightController","Read from ${characteristic.uuid}: ${characteristic.value.toHexString()}")
                displayName = String(characteristic.value)
            }

            onCharacteristicWrite = { _, characteristic ->
                Log.i("LightningLightController","Wrote to ${characteristic.uuid}")
            }
            onDisconnect = {
                Log.i("LightningLightController", "Disconnected")
                ConnectionManager.unregisterListener(this)
            }
        }
    }

    init {
        Log.i("LightningLightController", "Registering listener")
        ConnectionManager.registerListener(connectionEventListener)
    }

    fun setRGBColour(colour: String) {
        val command = "0203$colour"
        Log.d("LightningLightController", "Command: $command")

        Log.i("LightningLightController", "Setting colour to: $colour")
        ConnectionManager.writeCharacteristic(bleDevice, rgbControlCharacteristic, command.hexToBytes())
    }

    fun setLightState(state: LightState) {
        val command = "01010${state.ordinal}"
        Log.d("LightningLightController", "Command: $command")

        Log.i("LightningLightController", "Setting light state to: $state")
        ConnectionManager.writeCharacteristic(bleDevice, rgbControlCharacteristic, command.hexToBytes())
    }

    private fun readName() {
        Log.i("LightningLightController", "Reading device name...")
        ConnectionManager.readCharacteristic(bleDevice, infoCharacteristic)
    }

    private fun String.hexToBytes() =
        this.chunked(2).map { it.toUpperCase(Locale.US).toInt(16).toByte() }.toByteArray()
}