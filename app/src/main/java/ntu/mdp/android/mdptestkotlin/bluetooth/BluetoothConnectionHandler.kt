package ntu.mdp.android.mdptestkotlin.bluetooth

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothService.Companion.socket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

class BluetoothConnectionHandler(private var callback: (status: BluetoothService.Status, message: String) -> Unit): Thread() {
    private val inputStream: InputStream? = socket?.inputStream
    private val outputStream: OutputStream? = socket?.outputStream
    private var buffer: ByteArray = ByteArray(1024)

    @Synchronized
    fun setCallback(callback: (status: BluetoothService.Status, message: String) -> Unit) {
        this.callback = callback
    }

    override fun run() {
        if (inputStream == null) {
            CoroutineScope(Dispatchers.Main).launch {
                callback(BluetoothService.Status.DISCONNECTED, BluetoothService.Status.DISCONNECTED.message)
            }
            return
        }

        while (true) {
            try {
                inputStream.read(buffer)
            } catch (e: IOException) {
                Log.d("Bluetooth", "InputStream has been disconnected.", e)
                cancel()
                CoroutineScope(Dispatchers.Main).launch {
                    callback(BluetoothService.Status.DISCONNECTED, BluetoothService.Status.DISCONNECTED.message)
                }
                break
            }

            val s: String = buffer.toString(Charsets.UTF_8).trim().replace("\u0000", "")
            buffer = ByteArray(1024)

            CoroutineScope(Dispatchers.Main).launch {
                callback(BluetoothService.Status.READ, s)
            }
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
            callback(BluetoothService.Status.WRITE_FAILED, BluetoothService.Status.WRITE_FAILED.message)
            return
        }

        callback(BluetoothService.Status.WRITE_SUCCESS, BluetoothService.Status.WRITE_SUCCESS.message)
    }

    fun cancel() {
        try {
            socket?.close()
            socket = null
        } catch (e: IOException) {
            Log.e("Bluetooth", "Failed to close socket.", e)
        }
    }
}