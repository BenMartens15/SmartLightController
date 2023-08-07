package com.benmartens15.smartlightcontroller.ui.devices

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.benmartens15.smartlightcontroller.R
import java.util.UUID

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val SCAN_DURATION_MS = 5000
private const val LIGHT_CONTROLLER_NAME = "LIGHTNING-LC2444"

@SuppressLint("MissingPermission") // probably figure out how to handle this properly at some point
class DevicesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView

    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: DeviceAdapter by lazy {
        DeviceAdapter(scanResults)
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val scanSettings =
        ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

    private val scanFilter =
        ScanFilter.Builder().setDeviceName(LIGHT_CONTROLLER_NAME).build()

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        permissions.entries.forEach {
            Log.d("DEBUG", "${it.key} = ${it.value}")
        }
    }

    private var isScanning = false

    /*******************************************
     * Activity function overrides
     *******************************************/
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_devices, container, false)
        recyclerView = view.findViewById(R.id.recycler_view_devices)

        return view
    }

    override fun onResume() {
        super.onResume()
        setupRecyclerView()

        if (!checkPermissions()) {
            Log.i("DevicesFragment", "Required permissions not granted - requesting them")
            requestPermissions()
        } else {
            if (!bluetoothAdapter.isEnabled) {
                Log.i("DevicesFragment", "Bluetooth not enabled, prompting the user to enable it")
                promptEnableBluetooth()
            } else {
                startBleScan()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ENABLE_BLUETOOTH_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) { // if the user declined enabling bluetooth
                promptEnableBluetooth()
            }
        }
    }

    /*******************************************
     * Private functions
     *******************************************/
    private fun setupRecyclerView() {
        recyclerView.apply {
            adapter = scanResultAdapter
            layoutManager = LinearLayoutManager(
                context,
                RecyclerView.VERTICAL,
                false
            )
        }
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    private fun startBleScan() {
        if (!checkPermissions()) {
            Log.i("DevicesFragment", "Required permissions not granted - requesting them")
            requestPermissions()
        } else {
            Log.i("DevicesFragment", "Starting BLE scan")
            scanResults.clear()
            scanResultAdapter.notifyDataSetChanged()
            isScanning = true
            bleScanner.startScan(listOf(scanFilter), scanSettings, scanCallback)

            val scanHandler = Handler(Looper.getMainLooper())
            scanHandler.postDelayed({ // stop the scan after the specified time
                stopBleScan()
            }, SCAN_DURATION_MS.toLong())
        }
    }

    private fun stopBleScan() {
        Log.i("DevicesFragment", "Stopping BLE scan")
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val fineLocationPermission =
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            val bleScanPermission =
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED

            val bleConnectPermission =
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED

            fineLocationPermission && bleScanPermission && bleConnectPermission
        } else {
            true // Prior to Android 6.0, permissions are granted at install time
        }
    }

    private fun requestPermissions() {
        Log.i("DevicesFragment", "Requesting permissions")
        requestMultiplePermissions.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        )
    }

    /*******************************************
     * Callback bodies
     *******************************************/
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery == -1) { // if there isn't already a scan result with the same address
                with(result.device) {
                    Log.i("ScanCallback", "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                }
                scanResults.add(result)
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
            }
        }
    }
}