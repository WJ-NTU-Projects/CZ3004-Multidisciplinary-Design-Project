package wjayteo.mdp.algorithms.algorithm

import wjayteo.mdp.algorithms.wifi.WifiMessageListener
import java.util.concurrent.atomic.AtomicInteger

abstract class Algorithm : WifiMessageListener {
    companion object {
        const val FORWARD = 0
        const val LEFT = 1
        const val RIGHT = 2
        const val REVERSE = 3
        var ACTUAL_RUN: Boolean = false
    }

    abstract override fun messageReceived(message: String)
}