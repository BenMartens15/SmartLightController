package com.benmartens15.smartlightcontroller.ui.devices

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.benmartens15.smartlightcontroller.R

class DeviceAdapter(private val dataList: List<Pair<String, String>>) :
    RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.text_view_device_name)
        val type: TextView = itemView.findViewById(R.id.text_view_device_type)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val (name, type) = dataList[position]
        holder.name.text = name
        holder.type.text = type
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}