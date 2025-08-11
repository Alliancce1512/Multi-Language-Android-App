package com.shellytask.app.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.shellytask.app.R
import java.util.UUID

class BluetoothActivity : AppCompatActivity() {

    private lateinit var statusTextView     : TextView
    private lateinit var toggleScanButton   : Button
    private lateinit var enableBtButton     : Button
    private lateinit var clearButton        : Button
    private lateinit var disconnectButton   : Button
    private lateinit var devicesListView    : ListView
    private lateinit var gattValueTextView  : TextView
    private lateinit var progressScan       : ProgressBar
    private var toolbar                     : Toolbar? = null

    private val discoveredDevices           : MutableList<BluetoothDevice> = mutableListOf()
    private lateinit var devicesAdapter     : ArrayAdapter<String>

    private val bluetoothManager            by lazy { getSystemService(BLUETOOTH_SERVICE) as BluetoothManager }
    private val bluetoothAdapter            by lazy { bluetoothManager.adapter }
    private val bleScanner                  : BluetoothLeScanner? get() = bluetoothAdapter.bluetoothLeScanner
    private val bleClient                   by lazy { BleClient(this) }

    private var isScanning                  : Boolean = false
    private val scanTimeoutMs               : Long = 10_000

    private val enableBtLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK)
            showStatus(getString(R.string.status_bluetooth_enabled))
        else
            showStatus(getString(R.string.status_bluetooth_not_enabled))
    }

    private val permissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.values.all { it }
        showStatus(if (allGranted) getString(R.string.permissions_granted) else getString(R.string.permissions_denied))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        title                   = getString(R.string.bluetooth)
        toolbar                 = findViewById(R.id.topAppBar)
        toolbar?.let {
            setSupportActionBar(it)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        statusTextView          = findViewById(R.id.txtStatus)
        toggleScanButton        = findViewById(R.id.btnToggleScan)
        enableBtButton          = findViewById(R.id.btnEnableBt)
        clearButton             = findViewById(R.id.btnClear)
        disconnectButton        = findViewById(R.id.btnDisconnect)
        devicesListView         = findViewById(R.id.listDevices)
        gattValueTextView       = findViewById(R.id.txtGattValue)
        progressScan            = findViewById(R.id.progressScan)

        devicesAdapter          = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        devicesListView.adapter = devicesAdapter

        toggleScanButton.setOnClickListener {
            if (isScanning) stopScan() else startScan()
        }

        enableBtButton.setOnClickListener { requestEnableBluetooth() }
        clearButton.setOnClickListener {
            discoveredDevices.clear()
            devicesAdapter.clear()
            gattValueTextView.text = getString(R.string.characteristic_value)
        }
        disconnectButton.setOnClickListener { disconnectGatt() }

        devicesListView.setOnItemClickListener { _, _, position, _ ->
            val device = discoveredDevices.getOrNull(position) ?: return@setOnItemClickListener
            connectToDevice(device)
        }

        requestRuntimePermissionsIfNeeded()

        if (!hasLocationPermission()) {
            requestLocationPermission()
        }
        registerBluetoothStateReceiver()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
        stopScan()
        bleClient.cleanup()
    }

    private fun requestEnableBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            showStatus(getString(R.string.status_bluetooth_already_enabled))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the missing permission first to avoid SecurityException
            permissionsLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
            showStatus(getString(R.string.status_missing_permissions_requesting))
            return
        }

        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        try {
            enableBtLauncher.launch(enableBtIntent)
        } catch (_: SecurityException) {
            showStatus(getString(R.string.status_missing_permissions_requesting))
        }
    }

    private fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requestRuntimePermissionsIfNeeded() {
        val missing = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionsLauncher.launch(missing.toTypedArray())
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        else
            true
    }

    private fun hasScanPermission(): Boolean {
        return requiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startScan() {
        if (!hasScanPermission()) {
            showStatus(getString(R.string.status_missing_permissions_requesting))
            requestRuntimePermissionsIfNeeded()
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            showStatus(getString(R.string.status_bluetooth_disabled_prompt))
            requestEnableBluetooth()
            return
        }
        if (!hasLocationPermission()) {
            showStatus(getString(R.string.status_requesting_location_permission))
            requestLocationPermission()
            return
        }
        if (!isLocationEnabled()) {
            showStatus(getString(R.string.status_location_disabled_prompt))

            try {
                startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } catch (_: Exception) { }
            return
        }
        if (bleScanner == null) {
            showStatus(getString(R.string.status_ble_not_supported))
            return
        }

        discoveredDevices.clear()
        devicesAdapter.clear()

        showStatus(getString(R.string.status_scanning))

        isScanning              = true
        toggleScanButton.text   = getString(R.string.stop_scan)
        progressScan.visibility = View.VISIBLE

        bleClient.startScan(object : BleEvents {
            override fun onDeviceFound(device: BluetoothDevice) {
                val address = safeDeviceAddress(device)

                if (address != getString(R.string.device_no_address)) {
                    if (discoveredDevices.any { safeDeviceAddress(it) == address }) return
                }
                discoveredDevices.add(device)

                val name = BleUtils.safeDeviceName(this@BluetoothActivity, device, hasConnectPermission())
                runOnUiThread { devicesAdapter.add("$name\n$address") }
            }
            override fun onScanFailed(errorCode: Int) { runOnUiThread { showStatus(getString(R.string.status_scan_failed, errorCode)) } }
            override fun onScanStopped() { runOnUiThread { stopScanUi() } }
            override fun onConnectionStateChanged(state: Int) {}
            override fun onCharacteristicRead(uuid: UUID, value: ByteArray) {}
            override fun onMessage(message: String) { runOnUiThread { showStatus(message) } }
        }, scanTimeoutMs)
    }

    private fun stopScan() {
        if (!isScanning) return

        bleClient.stopScan()
        stopScanUi()
    }

    private fun stopScanUi() {
        isScanning              = false
        toggleScanButton.text   = getString(R.string.start_scan)
        showStatus(getString(R.string.status_scan_stopped))
        progressScan.visibility = View.GONE
    }

    private fun isLocationEnabled(): Boolean = BleUtils.isLocationEnabled(this)

    private fun hasLocationPermission(): Boolean {
        // Some OEMs still gate BLE scan behind location even on Android 12+
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        permissionsLauncher.launch(permissions)
    }

    private fun safeDeviceAddress(device: BluetoothDevice): String {
        return BleUtils.safeDeviceAddress(this, device, hasConnectPermission())
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (!hasScanPermission() || !hasConnectPermission()) {
            showStatus(getString(R.string.status_missing_permissions_requesting))
            return
        }
        stopScan()

        // Reset previous characteristic output when initiating a new connection
        gattValueTextView.text  = getString(R.string.characteristic_value)
        val displayName         = try {
            if (hasConnectPermission()) device.name ?: device.address else device.address
        } catch (_: SecurityException) {
            device.address
        }

        showStatus(getString(R.string.status_connecting_to, displayName))

        bleClient.connect(device, object : BleEvents {
            override fun onDeviceFound(device: BluetoothDevice) {}
            override fun onScanFailed(errorCode: Int) {}
            override fun onScanStopped() {}
            override fun onConnectionStateChanged(state: Int) {
                runOnUiThread {
                    when (state) {
                        BluetoothProfile.STATE_CONNECTING   -> showStatus(getString(R.string.status_connecting))
                        BluetoothProfile.STATE_CONNECTED    -> {
                            showStatus(getString(R.string.status_connected_discovering))
                            disconnectButton.visibility = View.VISIBLE
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            showStatus(getString(R.string.status_disconnected))
                            disconnectButton.visibility = View.GONE
                            gattValueTextView.text      = getString(R.string.characteristic_value)
                        }
                    }
                }
            }
            override fun onCharacteristicRead(uuid: UUID, value: ByteArray) {
                runOnUiThread {
                    gattValueTextView.text = buildCharacteristicDisplay(uuid, value)
                    showStatus(getString(R.string.status_read_ok_from, uuid.toString()))
                }
            }
            override fun onMessage(message: String) { runOnUiThread { showStatus(message) } }
        })
    }

    private fun registerBluetoothStateReceiver() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state   = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                val msg     = when (state) {
                    BluetoothAdapter.STATE_ON           -> getString(R.string.bt_state_on)
                    BluetoothAdapter.STATE_OFF          -> getString(R.string.bt_state_off)
                    BluetoothAdapter.STATE_TURNING_ON   -> getString(R.string.bt_state_turning_on)
                    BluetoothAdapter.STATE_TURNING_OFF  -> getString(R.string.bt_state_turning_off)
                    else                                -> getString(R.string.bt_state_other, state)
                }
                showStatus(msg)
            }
        }
    }

    private fun showStatus(message: String) {
        statusTextView.text = message
    }

    private fun disconnectGatt() {
        bleClient.disconnect()
        gattValueTextView.text      = getString(R.string.characteristic_value)
        disconnectButton.visibility = View.GONE
    }

    private fun buildCharacteristicDisplay(uuid: UUID, value: ByteArray): String =
        BleUtils.formatCharacteristic(this, uuid, value)
}