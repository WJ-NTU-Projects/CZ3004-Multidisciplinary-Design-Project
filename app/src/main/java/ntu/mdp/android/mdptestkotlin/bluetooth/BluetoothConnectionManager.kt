package ntu.mdp.android.mdptestkotlin.bluetooth

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ntu.mdp.android.mdptestkotlin.App
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class BluetoothConnectionManager: Thread() {
    private val inputStream: InputStream? = App.socket?.inputStream
    private val outputStream: OutputStream? = App.socket?.outputStream
    private var buffer: ByteArray = ByteArray(1024)

    override fun run() {
        if (inputStream == null || outputStream == null) {
            BluetoothController.broadcastStatus(BluetoothController.Status.ERROR)
            return
        }

        App.BLUETOOTH_CONNECTED_DEVICE = App.socket?.remoteDevice?.name!!
        BluetoothController.broadcastStatus(BluetoothController.Status.CONNECTED)

        while (true) {
            try {
                inputStream.read(buffer)
            } catch (e: IOException) {
                cancel()

                CoroutineScope(Dispatchers.Main).launch {
                    BluetoothController.broadcastStatus(BluetoothController.Status.DISCONNECTED)
                }

                break
            }

            val ret = buffer.toString(Charsets.UTF_8).trim().replace("\u0000", "")
            buffer = ByteArray(1024)

            CoroutineScope(Dispatchers.Main).launch {
                BluetoothController.callback(BluetoothController.Status.READ, ret)
            }
        }
    }

    fun write(output: ByteArray) {
        try {
            outputStream?.write(output)
        } catch (e: IOException) {
            Log.d("Bluetooth", "Failed to write data.", e)
            BluetoothController.broadcastStatus(BluetoothController.Status.WRITE_FAILED)
            return
        }

        BluetoothController.broadcastStatus(BluetoothController.Status.WRITE_SUCCESS)
    }

    fun cancel() {
        try {
            App.socket?.close()
            App.socket = null
        } catch (e: IOException) {
            Log.e(this::class.simpleName ?: "-", "Unable to close connection.", e)
        }
    }
}