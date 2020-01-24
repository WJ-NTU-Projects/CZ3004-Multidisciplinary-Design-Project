package ntu.mdp.android.mdptestkotlin.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ntu.mdp.android.mdptestkotlin.R
import ntu.mdp.android.mdptestkotlin.utils.ActivityUtil
import java.util.*

class BluetoothService private constructor() {
    enum class State {
        SERVER,
        CLIENT
    }

    enum class Status(val message: String) {
        ERROR("Unexpected error has occurred."),
        READY("Bluetooth service is ready and listening for connection."),
        CONNECTING("Connecting to device..."),
        CONNECT_FAILED("Failed to connect to bluetooth device."),
        CONNECTED("Bluetooth device has been connected successfully."),
        DISCONNECTED("Bluetooth device has been disconnected"),
        WRITE_FAILED("Failed to transfer data to device."),
        WRITE_SUCCESS("Data transfer is successful."),
        READ("Incoming data from device.");
    }

    companion object {
        val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var socket: BluetoothSocket? = null
        private var listener: BluetoothListener? = null
        private var connector: BluetoothConnector? = null
        private var handler: BluetoothConnectionHandler? = null
        private lateinit var callback: (status: Status, message: String) -> Unit

        @Synchronized
        fun init(state: State, callback: (status: Status, message: String) -> Unit, device: BluetoothDevice? = null) {
            setCallback(callback)
            //reset()

            when (state) {
                State.SERVER -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        listener = BluetoothListener(callback, { connected -> if (connected) connected() })
                        listener!!.start()
                    }
                }

                State.CLIENT -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        if (device != null) {
                            connector = BluetoothConnector(device, callback, { connected -> if (connected) connected() })
                            connector!!.start()
                        } else {
                            Log.e("Bluetooth", "Invalid device.")
                            callback(Status.ERROR, Status.ERROR.message)
                        }
                    }
                }
            }
        }

        @Synchronized
        fun reset() {
            val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()

            if (listener != null) {
                listener!!.cancel()
                listener = null
            }

            if (connector != null) {
                connector!!.cancel()
                connector = null
            }

            if (handler != null) {
                handler!!.cancel()
                handler = null
            }

            if (isConnected() != true) socket = null
        }

        @Synchronized
        fun connected() {
            //reset()

            if (socket != null) {
                CoroutineScope(Dispatchers.Default).launch {
                    handler = BluetoothConnectionHandler(callback)
                    handler!!.start()
                }
            } else {
                Log.e("Bluetooth", "Connected socket is null?")
                callback(Status.ERROR, Status.ERROR.message)
            }
        }

        @Synchronized
        fun isConnected(): Boolean? {
            return (socket?.isConnected)
        }

        @Synchronized
        fun writeToDevice(command: String) {
            val output: ByteArray = command.toByteArray()
            handler?.write(output)
        }

        @Synchronized
        fun setCallback(callback: (status: Status, message: String) -> Unit) {
            this.callback = callback
            handler?.setCallback(callback)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when(intent.action ?: "") {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state: Int = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                        val previousState: Int = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF)

                        if (state == BluetoothAdapter.STATE_OFF && previousState != BluetoothAdapter.STATE_OFF) {
                            ActivityUtil(context).sendDialog(context.getString(R.string.error_bluetooth_off))
                        }
                    }
                }
            }
        }
    }
}