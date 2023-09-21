package com.benmartens15.smartlightcontroller.ui.devices

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.inputmethod.InputMethodManager
import com.benmartens15.smartlightcontroller.R
import com.benmartens15.smartlightcontroller.databinding.ActivityLightSwitchBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class LightSwitchActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLightSwitchBinding
    private val device by lazy {
        intent.getParcelableExtra<LightningLightController>("Device")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLightSwitchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUi()
    }

    private fun setupUi() {
        title = "Light Switch"

        binding.textViewDeviceName.text = device?.displayName
        binding.editTextShutoffTime.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        binding.switchLight.isChecked = device?.state  == LightState.ON
        binding.switchMotionDetection.isChecked = device?.motionEnabled == true

        binding.switchLight.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                device?.setLightState(LightState.ON)
            } else {
                device?.setLightState(LightState.OFF)
            }
        }

        binding.switchMotionDetection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                device?.enableMotionDetection(true)
            } else {
                device?.enableMotionDetection(false)
            }
        }

        binding.buttonSet.setOnClickListener {
            var seconds = binding.editTextShutoffTime.text.toString().toInt() * 60
            if (seconds == 0) {
                seconds = 300
            }
            device?.setMotionTimeout(seconds)
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

        builder.setPositiveButton("OK") { dialog, _ ->
            val newName = nameTextInputEditText.text.toString()
            binding.textViewDeviceName.text = newName
            device?.setName(newName)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
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
}