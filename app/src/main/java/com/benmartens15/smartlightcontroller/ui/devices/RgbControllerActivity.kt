package com.benmartens15.smartlightcontroller.ui.devices

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.inputmethod.InputMethodManager
import com.benmartens15.smartlightcontroller.R
import com.benmartens15.smartlightcontroller.databinding.ActivityRgbControllerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class RgbControllerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRgbControllerBinding
    private val device by lazy {
        intent.getParcelableExtra<LightningLightController>("Device")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRgbControllerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUi()
    }

    private fun setupUi() {
        title = "RGB Controller"

        binding.textViewDeviceName.text = device?.displayName

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