package ntu.mdp.android.mdptestkotlin.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ntu.mdp.android.mdptestkotlin.App
import java.io.IOException
import java.util.*

class BluetoothServer(private val connectedCallback: (Boolean) -> Unit): Thread() {
    private val serverSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
        BluetoothAdapter.getDefaultAdapter().listenUsingInsecureRfcommWithServiceRecord("MDP", UUID.fromString(App.BLUETOOTH_UUID))
    }

    override fun run() {
        var loop = true

        while (loop) {
            App.socket = try {
                serverSocket?.accept()
            } catch (e: IOException) {
                loop = false
                null
            }

            App.socket?.also {
                serverSocket?.close()
                loop = false

                CoroutineScope(Dispatchers.Main).launch {
                    connectedCallback(true)
                }
            }
        }
    }

    fun cancel() {
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e(this::class.simpleName ?: "-", "Failed to cancel server socket.", e)
            BluetoothController.broadcastStatus(BluetoothController.Status.ERROR)
        }
    }
}