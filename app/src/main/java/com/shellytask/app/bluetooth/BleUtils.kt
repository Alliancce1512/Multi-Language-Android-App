package com.shellytask.app.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import com.shellytask.app.R
import java.util.UUID

object BleUtils {

    fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    fun safeDeviceName(context: Context, device: BluetoothDevice, hasConnectPermission: Boolean): String {
        return if (!hasConnectPermission) context.getString(R.string.device_no_name) else try {
            device.name ?: context.getString(R.string.device_no_name)
        } catch (_: SecurityException) {
            context.getString(R.string.device_no_name)
        }
    }

    fun safeDeviceAddress(context: Context, device: BluetoothDevice, hasConnectPermission: Boolean): String {
        return try {
            if (!hasConnectPermission) context.getString(R.string.device_no_address) else device.address
        } catch (_: SecurityException) {
            context.getString(R.string.device_no_address)
        }
    }

    fun formatCharacteristic(context: Context, uuid: UUID, value: ByteArray): String {
        val hex     = if (value.isEmpty()) context.getString(R.string.na)
        else value.joinToString(" ") { b -> String.format("%02X", b) }

        val ascii = if (value.isEmpty()) context.getString(R.string.na) else value.map { b ->
            val c = (b.toInt() and 0xFF).toChar()
            if (c.isLetterOrDigit() || c.isWhitespace() || c in ' '..'~') c else '.'
        }.joinToString("")

        val uint8   = if (value.isNotEmpty()) (value[0].toInt() and 0xFF).toString() else context.getString(R.string.na)
        val uint16  = if (value.size >= 2) {
            val v = (value[0].toInt() and 0xFF) or ((value[1].toInt() and 0xFF) shl 8)
            v.toString()
        } else
            context.getString(R.string.na)
        val uint32  = if (value.size >= 4) {
            val v =
                (value[0].toLong() and 0xFF) or
                ((value[1].toLong() and 0xFF) shl 8) or
                ((value[2].toLong() and 0xFF) shl 16) or
                ((value[3].toLong() and 0xFF) shl 24)
            v.toString()
        } else
            context.getString(R.string.na)

        val isBattery   = uuid.toString().equals("00002A19-0000-1000-8000-00805F9B34FB", ignoreCase = true)
        val label8      = if (isBattery) context.getString(R.string.label_battery) else context.getString(R.string.label_uint8)
        val label16     = context.getString(R.string.label_uint16)
        val label32     = context.getString(R.string.label_uint32)

        return context.getString(
            R.string.characteristic_detailed_value,
            uuid.toString(), value.size, hex, ascii,
            label8, uint8,
            label16, uint16,
            label32, uint32
        )
    }
}