package wjayteo.mdp.algorithms.wifi

import wjayteo.mdp.algorithms.algorithm.Algorithm
import wjayteo.mdp.algorithms.uicomponent.MenuBar
import java.io.IOException
import java.net.Socket

class WifiSocketController {
    companion object {
        var socket: Socket? = null
        private var readThread: WifiSocketReadThread? = null

        @Synchronized
        fun connect(address: String, port: Int): Boolean {
            if (socket != null) return false

            try {
                socket = Socket(address, port)
            } catch (e: IOException) {
                println(e.message)
                return false
            }

            readThread = WifiSocketReadThread()
            readThread?.start()
            return true
        }

        @Synchronized
        fun disconnect(): Boolean {
            if (socket == null) return false

            try {
                socket?.close()
                socket = null
            } catch (e: IOException) {
                e.printStackTrace()
                return false
            }

            readThread?.finish()
            readThread = null
            MenuBar.connectionChanged(false)
            return true
        }

        @Synchronized
        fun setListener(listener: WifiMessageListener) {
            readThread?.listener = listener
        }

        fun isConnected(): Boolean {
            return (socket != null && socket?.isConnected == true)
        }

        fun write(destination: String, message: String) {
            WifiSocketWriteThread(destination, message).start()
        }
    }
}