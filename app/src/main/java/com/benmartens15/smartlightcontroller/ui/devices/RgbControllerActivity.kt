package com.benmartens15.smartlightcontroller.ui.devices

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benmartens15.smartlightcontroller.R
import com.benmartens15.smartlightcontroller.databinding.ActivityRgbControllerBinding
import com.benmartens15.smartlightcontroller.ui.ui.AppTheme
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class RgbControllerActivity : ComponentActivity() {
    private lateinit var binding: ActivityRgbControllerBinding
    private val device by lazy {
        intent.getParcelableExtra<LightningLightController>("Device")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    device?.displayName?.let {
                        ColorPicker(
                            it,
                            Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
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
        nameTextInputEditText.setText(device?.displayName)
        nameTextInputEditText.selectAll()

        builder.setPositiveButton("OK") { dialog, _ ->
            val newName = nameTextInputEditText.text.toString()
//            binding.textViewDeviceName.text = newName
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ColorPicker(deviceName: String, modifier: Modifier = Modifier) {
        val controller = rememberColorPickerController()
        var name by remember {
            mutableStateOf(deviceName)
        }
        var hexColorInput by remember {
            mutableStateOf("FFFFFF")
        }
        var textClickEnabled by remember {
            mutableStateOf(true)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopAppBar(title = {
                Text("RGB Controller")
            })

            Text(
                modifier = modifier.padding(0.dp, 15.dp, 0.dp, 0.dp)
                    .clickable(enabled = textClickEnabled) {
                        textClickEnabled = false
                        showChangeNameDialog()
                                                           },
                text = name,
                textAlign = TextAlign.Center,
                fontSize = 36.sp,
            )

            HsvColorPicker(
                modifier = modifier.height(375.dp).padding(20.dp),
                controller = controller,
                onColorChanged = { colorEnvelope: ColorEnvelope ->
                    Log.d("RgbControllerActivity", colorEnvelope.hexCode)
                    hexColorInput = colorEnvelope.hexCode.substring(2)
                }
            )

            OutlinedTextField(
                value = hexColorInput,
                onValueChange = {
                    hexColorInput = it
                },
                label = { Text("Hex Code") }
            )

            Button(
                modifier = modifier.padding(20.dp),
                onClick = {
                    device?.setRGBColour(hexColorInput)
                }
            ) {
                Text("Set")
            }
        }
    }
}