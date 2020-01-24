package ntu.mdp.android.mdptestkotlin.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothService.Companion.socket
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothService.Companion.uuid
import java.io.IOException

class BluetoothListener(private val callback: (status: BluetoothService.Status, message: String) -> Unit, private val connectedCallback: (connected: Boolean) -> Unit): Thread() {
    private val appName: String = "MDP"
    private val serverSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
        BluetoothAdapter.getDefaultAdapter().listenUsingInsecureRfcommWithServiceRecord(appName, uuid)
    }

    override fun run() {
        //callback(BluetoothService.Status.READY, BluetoothService.Status.READY.message)
        var loop = true

        while (loop) {
            socket = try {
                serverSocket?.accept()
            } catch (e: IOException) {
                Log.e("Bluetooth", "Accept failed or interrupted.", e)
                loop = false
                null
            }

            socket?.also {
                serverSocket?.close()
                loop = false
            }

            if (socket != null && socket!!.isConnected) {
                CoroutineScope(Dispatchers.Main).launch {
                    connectedCallback(true)
                    callback(BluetoothService.Status.CONNECTED, BluetoothService.Status.CONNECTED.message)
                }
            }
        }
    }

    fun cancel() {
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Failed to close server socket.", e)
            callback(BluetoothService.Status.ERROR, BluetoothService.Status.ERROR.message)
        }
    }
}