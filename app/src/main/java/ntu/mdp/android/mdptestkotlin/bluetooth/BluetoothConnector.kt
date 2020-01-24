package ntu.mdp.android.mdptestkotlin.bluetooth

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothService.Companion.socket
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothService.Companion.uuid
import java.io.IOException

class BluetoothConnector(private val device: BluetoothDevice, private val callback: (status: BluetoothService.Status, message: String) -> Unit): Thread() {
    override fun run() {
        callback(BluetoothService.Status.CONNECTING, BluetoothService.Status.CONNECTING.message)

        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            device.createBond()
            return
        }

        socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
        socket.use { socket ->
            try {
                socket?.connect()
                CoroutineScope(Dispatchers.Main).launch {
                    callback(BluetoothService.Status.CONNECTED, BluetoothService.Status.CONNECTED.message)
                }
            } catch (e: IOException) {
                Log.e("Bluetooth", "Connection failed.", e)
                callback(BluetoothService.Status.CONNECT_FAILED, BluetoothService.Status.CONNECT_FAILED.message)
            }
        }
    }

    fun cancel() {
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Failed to close the connecting socket.", e)
            callback(BluetoothService.Status.ERROR, BluetoothService.Status.ERROR.message)
        }
    }
}