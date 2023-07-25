package com.benmartens15.smartlightcontroller.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.benmartens15.smartlightcontroller.R

class DevicesFragment : Fragment() {

    private val dataList = mutableListOf<Pair<String, String>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_devices, container, false)

        dataList.clear()

        dataList.add("All" to "")
        dataList.add("Desk Backlight" to "RGB Strip")
        dataList.add("Main Light" to "Light Switch")

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_devices)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = DeviceAdapter(dataList)
        recyclerView.adapter = adapter

        return view
    }
}