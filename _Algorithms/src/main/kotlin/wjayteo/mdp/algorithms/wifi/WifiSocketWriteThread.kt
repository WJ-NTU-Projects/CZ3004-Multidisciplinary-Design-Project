package wjayteo.mdp.algorithms.wifi

import java.io.IOException
import java.io.OutputStream


class WifiSocketWriteThread(private val destination: String, private val message: String) : Thread() {
    private val outputStream: OutputStream? = WifiSocketController.socket?.getOutputStream()

    override fun run() {
        if (outputStream == null) {
            println("Socket output stream is null.")
            return
        }

        val output: ByteArray = (destination + message).toByteArray()

        try {
            outputStream.write(output)
            println("Write: $message")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}