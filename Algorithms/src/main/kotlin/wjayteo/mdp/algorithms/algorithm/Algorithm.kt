package wjayteo.mdp.algorithms.algorithm

import wjayteo.mdp.algorithms.wifi.WifiMessageListener
import java.util.concurrent.atomic.AtomicInteger

abstract class Algorithm : WifiMessageListener {
    companion object {
        const val COMMAND_FORWARD = 0
        const val COMMAND_LEFT = 1
        const val COMMAND_RIGHT = 2
        const val COMMAND_REVERSE = 3
        var ACTUAL_RUN: Boolean = false
    }

    abstract override fun messageReceived(message: String)
}