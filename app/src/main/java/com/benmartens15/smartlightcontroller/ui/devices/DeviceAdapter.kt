package com.benmartens15.smartlightcontroller.ui.devices

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.benmartens15.smartlightcontroller.R
import com.benmartens15.smartlightcontroller.ble.ConnectionManager
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Locale
import java.util.UUID

private val RGB_CTRL_CHARACTERISTIC_UUID = UUID.fromString("f19a2445-96fe-4d87-a476-68a7a8d0b7ba")

@SuppressLint("MissingPermission") // probably figure out how to handle this properly at some point
class DeviceAdapter(private val items: List<BluetoothDevice>) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.text_view_device_name)
        val type: TextView = itemView.findViewById(R.id.text_view_device_type)
        val switch: SwitchMaterial = itemView.findViewById(R.id.material_switch)

        fun bind(device: BluetoothDevice) {
            name.text = device.name ?: "Unnamed"
            type.text = device.address
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        val characteristics by lazy {
            ConnectionManager.servicesOnDevice(item)?.flatMap { service ->
                service.characteristics ?: listOf()
            } ?: listOf()
        }

        // set the OnCheckedChangeListener for each SwitchMaterial
        holder.switch.setOnCheckedChangeListener { buttonView, isChecked ->
            lateinit var rgbControlCharacteristic: BluetoothGattCharacteristic
            for (char in characteristics) {
                if (char.uuid == RGB_CTRL_CHARACTERISTIC_UUID) {
                    rgbControlCharacteristic = char
                }
            }

            if (isChecked) {
                ConnectionManager.writeCharacteristic(item, rgbControlCharacteristic, "ff1300".hexToBytes())
            } else {
                ConnectionManager.writeCharacteristic(item, rgbControlCharacteristic, "000000".hexToBytes())
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun String.hexToBytes() =
        this.chunked(2).map { it.toUpperCase(Locale.US).toInt(16).toByte() }.toByteArray()
}