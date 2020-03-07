package wjayteo.mdp.android.bluetooth

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import wjayteo.mdp.android.App

class BluetoothController {
    enum class Status {
        ERROR,
        CONNECTING,
        CONNECT_FAILED,
        CONNECTED,
        DISCONNECTED,
        WRITE_FAILED,
        WRITE_SUCCESS,
        READ;
    }

    companion object {
        lateinit var callback: (status: Status, message: String) -> Unit

        @Synchronized
        fun startServer(callback: (status: Status, message: String) -> Unit) {
            this.callback = callback
            App.bluetoothServerThread = BluetoothServer { connected -> if (connected) manageConnection() }

            CoroutineScope(Dispatchers.Default).launch {
                App.bluetoothServerThread?.start()
            }
        }

        @Synchronized
        fun startClient(device: BluetoothDevice, callback: (status: Status, message: String) -> Unit) {
            this.callback = callback
            App.bluetoothClientThread = BluetoothClient(device) { connected -> if (connected) manageConnection() }

            CoroutineScope(Dispatchers.Default).launch {
                App.bluetoothClientThread?.start()
            }
        }

        @Synchronized
        private fun manageConnection() {
            App.bluetoothConnectionManagerThread = BluetoothConnectionManager()

            CoroutineScope(Dispatchers.Default).launch {
                App.bluetoothConnectionManagerThread?.start()
            }
        }

        @Synchronized
        fun disconnect() {
            App.bluetoothConnectionManagerThread?.cancel()
        }

        fun isSocketConnected(): Boolean {
            if (App.socket == null) return false
            return App.socket!!.isConnected
        }

        fun write(output: String) {
            App.bluetoothConnectionManagerThread?.write(output.toByteArray())
        }

        fun broadcastStatus(status: Status, extra: String = "") {
            CoroutineScope(Dispatchers.Main).launch {
                val message: String = when (status) {
                    Status.ERROR -> "Unexpected error occurred."
                    Status.CONNECTING -> "Attempting to connect to $extra."
                    Status.CONNECTED -> "Successfully connected to ${App.BLUETOOTH_CONNECTED_DEVICE}."
                    Status.DISCONNECTED -> "Disconnected from ${App.BLUETOOTH_CONNECTED_DEVICE}."
                    Status.WRITE_FAILED -> "Failed to write data to ${App.BLUETOOTH_CONNECTED_DEVICE}."
                    Status.WRITE_SUCCESS -> "Successfully written data to ${App.BLUETOOTH_CONNECTED_DEVICE}."
                    Status.CONNECT_FAILED -> "Failed to connect to $extra."
                    else -> ""
                }

                callback(status, message)
            }
        }
    }
}