package wjayteo.mdp.android.bluetooth

import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import wjayteo.mdp.android.App
import java.io.IOException
import java.util.*

class BluetoothClient(private val device: BluetoothDevice, private val connectedCallback: (Boolean) -> Unit): Thread() {
    override fun run() {
        BluetoothController.broadcastStatus(BluetoothController.Status.CONNECTING, device.name)

        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            device.createBond()
            return
        }

        App.socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(App.BLUETOOTH_UUID))
        App.socket?.let { socket ->
            try {
                socket.connect()

                CoroutineScope(Dispatchers.Main).launch {
                    connectedCallback(true)
                }
            } catch (e: IOException) {
                Log.e(this::class.simpleName ?: "-", "Connection failed.", e)
                BluetoothController.broadcastStatus(BluetoothController.Status.CONNECT_FAILED, device.name)
            }
        }
    }
}