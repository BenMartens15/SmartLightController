package com.benmartens15.smartlightcontroller.ui.devices

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.benmartens15.smartlightcontroller.R
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.UUID

private val RGB_CTRL_CHARACTERISTIC_UUID = UUID.fromString("f19a2445-96fe-4d87-a476-68a7a8d0b7ba")

@SuppressLint("MissingPermission") // probably figure out how to handle this properly at some point
class DeviceAdapter(private val items: List<LightningLightController>) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.text_view_device_name)
        val type: TextView = itemView.findViewById(R.id.text_view_device_type)
        val switch: SwitchMaterial = itemView.findViewById(R.id.material_switch)

        fun bind(controller: LightningLightController) {
            name.text = controller.displayName ?: "Unnamed"
            type.text = controller.bleDevice.address
            if (controller.deviceType == DeviceType.RGB_CONTROLLER) {
                type.text = "RGB Controller"
            } else if (controller.deviceType == DeviceType.LIGHT_SWITCH){
                type.text = "Light Switch"
            } else {
                type.text = "Unknown Device"
            }
            switch.isChecked = controller.state == LightState.ON
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

            if (isChecked) {
                if (item.deviceType == DeviceType.RGB_CONTROLLER) {
                    item.setRGBColour("ff1300")
                } else if (item.deviceType == DeviceType.LIGHT_SWITCH) {
                    item.setLightState(LightState.ON)
                }
            } else {
                if (item.deviceType == DeviceType.RGB_CONTROLLER) {
                    item.setRGBColour("000000")
                } else if (item.deviceType == DeviceType.LIGHT_SWITCH) {
                    item.setLightState(LightState.OFF)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}