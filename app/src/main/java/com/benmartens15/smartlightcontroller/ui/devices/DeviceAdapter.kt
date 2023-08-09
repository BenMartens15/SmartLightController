package com.benmartens15.smartlightcontroller.ui.devices

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.benmartens15.smartlightcontroller.R
import com.benmartens15.smartlightcontroller.ble.ConnectionManager
import com.google.android.material.switchmaterial.SwitchMaterial

@SuppressLint("MissingPermission") // probably figure out how to handle this properly at some point
class DeviceAdapter(private val items: List<ScanResult>, private val switchCheckedChangeListener: OnSwitchCheckedChangeListener) :
    RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.text_view_device_name)
        val type: TextView = itemView.findViewById(R.id.text_view_device_type)
        val switch: SwitchMaterial = itemView.findViewById(R.id.material_switch)

        fun bind(result: ScanResult) {
            name.text = result.device.name ?: "Unnamed"
            type.text = result.device.address
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


        // set the OnCheckedChangeListener for each SwitchMaterial
        holder.switch.setOnCheckedChangeListener { buttonView, isChecked ->
            // call the callback function when the switch state changes
            switchCheckedChangeListener.onSwitchCheckedChanged(position, isChecked)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    interface OnSwitchCheckedChangeListener {
        fun onSwitchCheckedChanged(position: Int, isChecked: Boolean)
    }
}