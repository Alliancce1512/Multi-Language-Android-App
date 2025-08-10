package com.shellytask.app.bluetooth

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat

import java.util.UUID

interface BleEvents {
    fun onDeviceFound(device: BluetoothDevice)
    fun onScanFailed(errorCode: Int)
    fun onScanStopped()
    fun onConnectionStateChanged(state: Int)
    fun onCharacteristicRead(uuid: UUID, value: ByteArray)
    fun onMessage(message: String)
}

class BleClient(private val context: Context) {
    private val bluetoothManager                                = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter                                = bluetoothManager.adapter
    private val scanner             : BluetoothLeScanner? get() = bluetoothAdapter.bluetoothLeScanner
    private val mainHandler                                     = Handler(Looper.getMainLooper())

    private var scanTimeoutRunnable : Runnable?                 = null
    private var isScanning                                      = false
    private var bluetoothGatt       : BluetoothGatt?            = null

    private var events              : BleEvents?                = null
    private val seenAddresses       : MutableSet<String>        = mutableSetOf()

    fun startScan(events: BleEvents, timeoutMs: Long) {
        this.events = events

        if (isScanning) return

        val s       = scanner ?: return
        isScanning  = true
        seenAddresses.clear()

        try {
            s.startScan(scanCallback)
        } catch (_: SecurityException) {
            events.onMessage("Missing permission for scan")
            return
        }

        scanTimeoutRunnable = Runnable { stopScan() }
        mainHandler.postDelayed(scanTimeoutRunnable!!, timeoutMs)
    }

    fun stopScan() {
        if (!isScanning) return

        try {
            scanner?.stopScan(scanCallback)
        } catch (_: SecurityException) { }

        isScanning = false
        scanTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        scanTimeoutRunnable = null
        events?.onScanStopped()
    }

    private fun hasConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    fun connect(device: BluetoothDevice, events: BleEvents) {
        this.events = events

        stopScan()

        try { bluetoothGatt?.close() } catch (_: SecurityException) {}

        if (!hasConnectPermission()) {
            events.onMessage("Missing BLUETOOTH_CONNECT for connection")
            return
        }
        try {
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (_: SecurityException) {
            events.onMessage("Missing permission for connection")
        }
    }

    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (_: SecurityException) { }

        bluetoothGatt = null
    }

    fun cleanup() {
        stopScan()
        disconnect()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device  = result.device ?: return
            val address    = try { device.address } catch (_: SecurityException) { null }

            address?.let {
                if (!seenAddresses.add(it)) return
            }
            events?.onDeviceFound(device)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { res -> onScanResult(0, res) }
        }

        override fun onScanFailed(errorCode: Int) {
            events?.onScanFailed(errorCode)
            stopScan()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            events?.onConnectionStateChanged(newState)

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                ) {
                    gatt.discoverServices()
                } else {
                    events?.onMessage("Missing BLUETOOTH_CONNECT for service discovery")
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                try { bluetoothGatt?.close() } catch (_: SecurityException) {}
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return

            // Try reading battery level first, then fallback to the first available characteristic
            val batteryServiceUuid  = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
            val batteryLevelUuid    = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB")
            val characteristic      = gatt.getService(batteryServiceUuid)?.getCharacteristic(batteryLevelUuid)

            characteristic?.let {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                ) {
                    gatt.readCharacteristic(characteristic)
                }
                return
            }

            val firstChar = gatt.services.firstOrNull()?.characteristics?.firstOrNull() ?: return

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            )
                gatt.readCharacteristic(firstChar)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                @Suppress("DEPRECATION")
                val valueBytes = characteristic.value ?: ByteArray(0)
                events?.onCharacteristicRead(characteristic.uuid, valueBytes)
            }
        }
    }
}