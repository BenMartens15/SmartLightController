package com.benmartens15.smartlightcontroller.ui.devices

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.benmartens15.smartlightcontroller.R
import com.benmartens15.smartlightcontroller.ble.ConnectionManager
import com.benmartens15.smartlightcontroller.databinding.ActivityLightSwitchBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
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
                ConnectionManager.writeCharacteristic(
                    device,
                    rgbControlCharacteristic,
                    "010101".hexToBytes()
                )
            } else {
                ConnectionManager.writeCharacteristic(
                    device,
                    rgbControlCharacteristic,
                    "010100".hexToBytes()
                )
            }
        }

        binding.switchMotionDetection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ConnectionManager.writeCharacteristic(
                    device,
                    rgbControlCharacteristic,
                    "060101".hexToBytes()
                )
            } else {
                ConnectionManager.writeCharacteristic(
                    device,
                    rgbControlCharacteristic,
                    "060100".hexToBytes()
                )
            }
        }

        binding.buttonSet.setOnClickListener {
            var seconds = binding.editTextShutoffTime.text.toString().toInt() * 60
            if (seconds == 0) {
                seconds = 300
            }
            ConnectionManager.writeCharacteristic(
                device,
                rgbControlCharacteristic,
                "0502${Integer.toHexString(seconds).padStart(4, '0')}".hexToBytes()
            )
        }

        binding.textViewDeviceName.setOnClickListener {
            showChangeNameDialog()
        }
    }

    private fun showChangeNameDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_rename_device, null)
        builder.setView(dialogLayout)

        val nameTextInputEditText: TextInputEditText =
            dialogLayout.findViewById(R.id.text_input_edit_text_device_name)
        nameTextInputEditText.setText(binding.textViewDeviceName.text.toString())
        nameTextInputEditText.selectAll()

        builder.setPositiveButton("OK") { dialog, which ->
            val newName = nameTextInputEditText.text.toString()
            val newNameAscii = stringToHexAscii(newName)
            binding.textViewDeviceName.text = newName

            Log.i("LightSwitchActivity", "Setting name to $newName")
            ConnectionManager.writeCharacteristic(
                device,
                rgbControlCharacteristic,
                "04${
                    Integer.toHexString(newName.length).padStart(2, '0')
                }${newNameAscii}".hexToBytes()
            )
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }

        val dialog = builder.create()

        // Request focus on the EditText and show the keyboard
        nameTextInputEditText.requestFocus()
        // Use a Handler to show the keyboard after a short delay
        Handler().postDelayed({
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(
                nameTextInputEditText,
                InputMethodManager.SHOW_IMPLICIT
            )
        }, 200) // Delay in milliseconds

        dialog.show()
    }

    private fun String.hexToBytes() =
        this.chunked(2).map { it.toUpperCase(Locale.US).toInt(16).toByte() }.toByteArray()

    private fun stringToHexAscii(input: String): String {
        val hexStringBuilder = StringBuilder()

        for (char in input) {
            val asciiHex = char.toInt().toString(16)
            hexStringBuilder.append(asciiHex)
        }

        return hexStringBuilder.toString()
    }
}