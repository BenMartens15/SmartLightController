package com.benmartens15.smartlightcontroller.ui.devices

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.benmartens15.smartlightcontroller.R
import com.benmartens15.smartlightcontroller.ble.ConnectionManager
import com.benmartens15.smartlightcontroller.databinding.ActivityLightSwitchBinding
import java.util.Locale
import java.util.UUID

private val RGB_CTRL_CHARACTERISTIC_UUID = UUID.fromString("f19a2445-96fe-4d87-a476-68a7a8d0b7ba")

class LightSwitchActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLightSwitchBinding
    private val deviceName by lazy {
        intent.getStringExtra("DeviceName")
    }
    private val device by lazy {
        val deviceAddress = intent.getStringExtra("DeviceAddress")
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter.getRemoteDevice(deviceAddress)
    }
    private val state by lazy {
        intent.getSerializableExtra("LightState") as LightState
    }
    private val motionEnabled by lazy {
        intent.getBooleanExtra("MotionEnabled", false)
    }
    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }
    private val rgbControlCharacteristic: BluetoothGattCharacteristic by lazy {
        val uuid = characteristics.firstOrNull { it.uuid == RGB_CTRL_CHARACTERISTIC_UUID }
        uuid ?: throw NoSuchElementException("Control characteristic not found")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLightSwitchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUi()
    }

    private fun setupUi() {
        title = "Light Switch"

        binding.textViewDeviceName.text = deviceName
        binding.editTextShutoffTime.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        binding.switchLight.isChecked = state == LightState.ON
        binding.switchMotionDetection.isChecked = motionEnabled

        binding.switchLight.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ConnectionManager.writeCharacteristic(device, rgbControlCharacteristic, "010101".hexToBytes())
            } else {
                ConnectionManager.writeCharacteristic(device, rgbControlCharacteristic, "010100".hexToBytes())
            }
        }

        binding.switchMotionDetection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ConnectionManager.writeCharacteristic(device, rgbControlCharacteristic, "060101".hexToBytes())
            } else {
                ConnectionManager.writeCharacteristic(device, rgbControlCharacteristic, "060100".hexToBytes())
            }
        }

        binding.buttonSet.setOnClickListener {
            var seconds = binding.editTextShutoffTime.text.toString().toInt() * 60
            if (seconds == 0) {
                seconds = 300
            }
            ConnectionManager.writeCharacteristic(device, rgbControlCharacteristic, "0502${Integer.toHexString(seconds).padStart(4, '0')}".hexToBytes())
        }
    }

    private fun String.hexToBytes() =
        this.chunked(2).map { it.toUpperCase(Locale.US).toInt(16).toByte() }.toByteArray()
}