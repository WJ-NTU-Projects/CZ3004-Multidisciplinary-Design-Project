package wjayteo.mdp.algorithms.wifi

import java.io.IOException
import java.io.InputStream

class WifiSocketReadThread : Thread() {
    var listener: WifiMessageListener? = null
    private val inputStream: InputStream? = WifiSocketController.socket?.getInputStream()
    private var buffer: ByteArray = ByteArray(1024)
    private var alive = true

    override fun run() {
        if (inputStream == null) {
            println("Socket input stream is null.")
            return
        }

        while (alive) {
            try {
                val result: Int = inputStream.read(buffer)
                if (result < 0) break
            } catch (e: IOException) {
                e.printStackTrace()
                break
            }

            var ret = String(buffer, Charsets.UTF_8)
            ret = ret.trim().replace("\u0000", "")
            println("Read: ${ret.trim()}")
            listener?.messageReceived(ret.trim())
            buffer = ByteArray(1024)
        }

        WifiSocketController.disconnect()
    }

    fun finish() {
        alive = false
    }
}