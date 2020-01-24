package ntu.mdp.android.mdptestkotlin.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class BluetoothService2 private constructor() {
    /*
    companion object {
        const val STATE_DISCONNECTED = -1
        const val STATE_READY = 0
        const val STATE_CONNECTION_FAILED = 1
        const val STATE_CONNECTED = 2
        const val STATE_READ = 3
        const val STATE_WRITE_FAILED = 4
        const val STATE_WRITE = 5

        private const val appName: String = "MDP"
        private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        private var bluetoothSocket: BluetoothSocket? = null
        private var acceptThread: AcceptThread? = null
        private var connectedThread: ConnectedThread? = null
        private var connectThread: ConnectThread? = null
        private lateinit var callback: (Int, String) -> Unit

        @Synchronized
        fun startServer() {
            reset()

            CoroutineScope(Dispatchers.Default).launch {
                if (acceptThread != null) acceptThread!!.cancel()
                acceptThread = AcceptThread()
                acceptThread!!.start()
            }
        }

        @Synchronized
        fun connect(device: BluetoothDevice) {
            stop()

            CoroutineScope(Dispatchers.Default).launch {
                if (connectThread != null) connectThread!!.cancel()
                connectThread = ConnectThread(device)
                connectThread!!.start()
            }
        }

        @Synchronized
        fun connected() {
            CoroutineScope(Dispatchers.IO).launch {
                if (connectedThread != null) connectedThread!!.cancel()
                connectedThread = ConnectedThread()
                connectedThread!!.start()
            }

            callback(STATE_CONNECTED, "Bluetooth connection has been established.")
        }

        @Synchronized
        fun setCallback(callback: (status: Int, message: String) -> Unit) {
            this.callback = callback
            callback(STATE_READY, "Bluetooth service is ready.")
        }

        @Synchronized
        fun stop() {
            reset()
        }

        @Synchronized
        private fun reset() {
            cancelDiscovery()

            if (connectedThread != null) {
                connectedThread!!.cancel()
                connectedThread = null
            }

            if (connectThread != null) {
                connectThread!!.cancel()
                connectThread = null
            }

            if (acceptThread != null) {
                acceptThread!!.cancel()
                acceptThread = null
            }
        }

        @Synchronized
        fun startDiscovery() {
            cancelDiscovery()
            bluetoothAdapter?.startDiscovery()
        }

        @Synchronized
        fun cancelDiscovery() {
            if (bluetoothAdapter?.isDiscovering == true) bluetoothAdapter.cancelDiscovery()
        }

        @Synchronized
        fun write(bytes: ByteArray) {
            CoroutineScope(Dispatchers.IO).launch {
                connectedThread!!.write(bytes)
            }
        }

        fun isConnected(): Boolean? {
            return (bluetoothSocket?.isConnected)
        }

        fun getBondedDevices(): Set<BluetoothDevice>? {
            return bluetoothAdapter?.bondedDevices
        }

        private class AcceptThread: Thread() {
            private val serverSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
                bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(appName, uuid)
            }

            override fun run() {
                var loop = true

                while (loop) {
                    bluetoothSocket = try {
                        serverSocket?.accept()
                    } catch (e: IOException) {
                        Log.e("Bluetooth", "Failed to accept.", e)
                        loop = false
                        null
                    }

                    bluetoothSocket?.also {
                        serverSocket?.close()
                        loop = false
                    }
                }

                if (bluetoothSocket != null && bluetoothSocket!!.isConnected) {
                    connected()
                }
            }

            fun cancel() {
                try {
                    serverSocket?.close()
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Could not close the connect socket", e)
                }
            }
        }

        private class ConnectedThread: Thread() {
            private val inputStream: InputStream? = bluetoothSocket?.inputStream
            private val outputStream: OutputStream? = bluetoothSocket?.outputStream
            private val buffer: ByteArray = ByteArray(1024)

            override fun run() {
                if (inputStream == null) {
                    connectionLost()
                    return
                }

                while (true) {
                    try {
                        inputStream.read(buffer)
                    } catch (e: IOException) {
                        Log.d("Bluetooth", "Input stream was disconnected", e)
                        connectionLost()
                        break
                    }

                    val s: String = buffer.toString(Charsets.UTF_8).trim().replace("\u0000", "")
                    callback(STATE_READ, s)
                }
            }

            fun write(bytes: ByteArray) {
                if (outputStream == null) {
                    return
                }

                try {
                    outputStream.write(bytes)
                } catch (e: IOException) {
                    Log.d("Bluetooth", "Failed to write data.", e)
                    callback(STATE_WRITE_FAILED, "Failed to write data.")
                    return
                }

                callback(STATE_WRITE, "Data successfully written.")
            }

            fun cancel() {
                try {
                    bluetoothSocket?.close()
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Cannot close socket.", e)
                }
            }
        }

        private class ConnectThread(private val device: BluetoothDevice) : Thread() {
            override fun run() {
                cancelDiscovery()

                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    device.createBond()
                    return
                }

                bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                bluetoothSocket.use { socket ->
                    try {
                        socket?.connect()
                        connected()
                    } catch (e: IOException) {
                        Log.e("Bluetooth", "Failed to connect.", e)
                        callback(STATE_CONNECTION_FAILED, "Connection failed.")
                    }
                }
            }

            fun cancel() {
                try {
                    bluetoothSocket?.close()
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Could not close the client socket", e)
                }
            }
        }

        private fun connectionLost() {
            callback(STATE_DISCONNECTED, "Bluetooth connection has been terminated!")
            startServer()
        }
    }

     */
}