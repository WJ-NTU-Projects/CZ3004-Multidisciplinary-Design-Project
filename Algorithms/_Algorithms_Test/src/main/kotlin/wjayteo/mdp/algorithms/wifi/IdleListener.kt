package wjayteo.mdp.algorithms.wifi

import wjayteo.mdp.algorithms.arena.Arena
import wjayteo.mdp.algorithms.arena.Robot

class IdleListener : WifiMessageListener {
    override fun messageReceived(message: String) {
        when (message) {
            "S" -> Arena.sendArena()

            "M" -> {
                Robot.moveTemp()
                Arena.sendArena()
            }

            "L" -> {
                Robot.turn(-90)
                WifiSocketController.write("D", "#robotPosition:${Robot.position.x}, ${Robot.position.y}, ${Robot.facing}")
            }

            "R" -> {
                Robot.turn(90)
                WifiSocketController.write("D", "#robotPosition:${Robot.position.x}, ${Robot.position.y}, ${Robot.facing}")
            }
        }
    }

    fun listen() {
        WifiSocketController.setListener(this)
    }
}