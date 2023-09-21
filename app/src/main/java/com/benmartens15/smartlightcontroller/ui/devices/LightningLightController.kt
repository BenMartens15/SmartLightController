package com.benmartens15.smartlightcontroller.ui.devices

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.annotation.RequiresApi
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
class LightningLightController: Parcelable {

    lateinit var bleDevice: BluetoothDevice
    private var advData: ByteArray = byteArrayOf()
    var displayName: String? = null
    var state: LightState
    var motionEnabled: Boolean = false
    var deviceType: DeviceType

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

    constructor(scanResult: ScanResult) {
        this.bleDevice = scanResult.device
        this.advData = scanResult.scanRecord!!.bytes
        this.state = if (advData[25].toInt() == 0) {
            LightState.OFF
        } else {
            LightState.ON
        }
        this.motionEnabled = advData[26].toInt() != 0
        this.deviceType = if (advData[27].toInt() == 0) {
            DeviceType.RGB_CONTROLLER
        } else if (advData[27].toInt() == 1) {
            DeviceType.LIGHT_SWITCH
        } else {
            DeviceType.UNKNOWN
        }

        Log.i("LightningLightController", "Registering listener")
        ConnectionManager.registerListener(connectionEventListener)
    }
    constructor(bleDevice: BluetoothDevice?, advData: ByteArray?) {
        if (bleDevice != null) {
            this.bleDevice = bleDevice
        }
        if (advData != null) {
            this.advData = advData
        }
        this.state = if (advData?.get(25)?.toInt()  == 0) {
            LightState.OFF
        } else {
            LightState.ON
        }
        this.motionEnabled = advData?.get(26)?.toInt() != 0
        this.deviceType = if (advData?.get(27)?.toInt() == 0) {
            DeviceType.RGB_CONTROLLER
        } else if (advData?.get(27)?.toInt() == 1) {
            DeviceType.LIGHT_SWITCH
        } else {
            DeviceType.UNKNOWN
        }

        Log.i("LightningLightController", "Registering listener")
        ConnectionManager.registerListener(connectionEventListener)
    }

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(BluetoothDevice::class.java.classLoader),
        parcel.createByteArray()
    ) {
        this.displayName = parcel.readString()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(bleDevice, flags)
        parcel.writeByteArray(advData)
        parcel.writeString(displayName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LightningLightController> {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun createFromParcel(parcel: Parcel): LightningLightController {
            return LightningLightController(parcel)
        }

        override fun newArray(size: Int): Array<LightningLightController?> {
            return arrayOfNulls(size)
        }
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

    fun enableMotionDetection(state: Boolean) {
        val command = "06010${state.toInt()}"
        Log.d("LightningLightController", "Command: $command")

        if (state) {
            Log.i("LightningLightController", "Enabling motion detection")
        } else {
            Log.i("LightningLightController", "Disabling motion detection")
        }
        ConnectionManager.writeCharacteristic(bleDevice, rgbControlCharacteristic, command.hexToBytes())
    }

    fun setMotionTimeout(timeout: Int) {
        val command = "0502${Integer.toHexString(timeout).padStart(4, '0')}"
        Log.d("LightningLightController", "Command: $command")

        Log.i("LightningLightController", "Setting motion timeout to: $timeout seconds")
        ConnectionManager.writeCharacteristic(bleDevice, rgbControlCharacteristic, command.hexToBytes())
    }

    fun setName(name: String) {
        val nameAscii = stringToHexAscii(name)
        val command = "04${Integer.toHexString(name.length).padStart(2, '0')}${nameAscii}"
        Log.d("LightningLightController", "Command: $command")

        Log.i("LightningLightController", "Setting device name to: $name")
        ConnectionManager.writeCharacteristic(bleDevice, rgbControlCharacteristic, command.hexToBytes())
    }

    private fun readName() {
        Log.i("LightningLightController", "Reading device name...")
        ConnectionManager.readCharacteristic(bleDevice, infoCharacteristic)
    }

    private fun String.hexToBytes() =
        this.chunked(2).map { it.toUpperCase(Locale.US).toInt(16).toByte() }.toByteArray()

    private fun Boolean.toInt() = if (this) 1 else 0

    private fun stringToHexAscii(input: String): String {
        val hexStringBuilder = StringBuilder()

        for (char in input) {
            val asciiHex = char.toInt().toString(16)
            hexStringBuilder.append(asciiHex)
        }

        return hexStringBuilder.toString()
    }
}