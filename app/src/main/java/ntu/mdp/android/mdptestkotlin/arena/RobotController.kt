package ntu.mdp.android.mdptestkotlin.arena

import android.content.Context
import kotlinx.coroutines.*

class RobotController(context: Context, callback: (callback: Callback, message: String) -> Unit) : ArenaV2(context, callback) {
    fun turnRobot(direction: Direction) {
        CoroutineScope(Dispatchers.Main).launch {
            when (direction) {
                Direction.RIGHT -> turnRobot(Math.floorMod(getRobotFacing() + 90, 360))
                Direction.LEFT -> turnRobot(Math.floorMod(getRobotFacing() - 90, 360))
                else -> return@launch
            }
        }
    }

    fun moveRobot(array: IntArray) {
        CoroutineScope(Dispatchers.Main).launch {
            moveRobot(array[0], array[1])
        }
    }

    fun moveRobot(direction: Direction) {val robotPosition = getRobotPosition()
        var x = robotPosition[0]
        var y = robotPosition[1]

        when (robotPosition[2]) {
            0 -> {
                when (direction) {
                    Direction.FORWARD -> y += 1
                    Direction.LEFT -> x -= 1
                    Direction.RIGHT -> x += 1
                    else -> y -= 1
                }
            }

            90 -> {
                when (direction) {
                    Direction.FORWARD -> x += 1
                    Direction.LEFT -> y += 1
                    Direction.RIGHT -> y -= 1
                    else -> x -= 1
                }
            }

            180 -> {
                when (direction) {
                    Direction.FORWARD -> y -= 1
                    Direction.LEFT -> x += 1
                    Direction.RIGHT -> x -= 1
                    else -> y += 1
                }
            }

            270 -> {
                when (direction) {
                    Direction.FORWARD -> x -= 1
                    Direction.LEFT -> y -= 1
                    Direction.RIGHT -> y += 1
                    else -> x += 1
                }
            }

            else -> return
        }

        if (!isValidCoordinates(x, y)) return

        CoroutineScope(Dispatchers.Main).launch {
            moveRobot(x, y)
        }
    }

    fun isGridExplored(direction: Direction): Boolean {
        val robotPosition = getRobotPosition()
        var x = robotPosition[0]
        var y = robotPosition[1]

        when (robotPosition[2]) {
            0 -> {
                when (direction) {
                    Direction.FORWARD -> y += 2
                    Direction.LEFT -> x -= 2
                    Direction.RIGHT -> x += 2
                    else -> y -= 2
                }
            }

            90 -> {
                when (direction) {
                    Direction.FORWARD -> x += 2
                    Direction.LEFT -> y += 2
                    Direction.RIGHT -> y -= 2
                    else -> x -= 2
                }
            }

            180 -> {
                when (direction) {
                    Direction.FORWARD -> y -= 2
                    Direction.LEFT -> x += 2
                    Direction.RIGHT -> x -= 2
                    else -> y += 2
                }
            }

            270 -> {
                when (direction) {
                    Direction.FORWARD -> x -= 2
                    Direction.LEFT -> y -= 2
                    Direction.RIGHT -> y += 2
                    else -> x += 2
                }
            }

            else -> return true
        }

        if (!isValidCoordinates(x, y)) return true
        return isGridExplored(x, y)
    }

    fun getInitialSurrounding(): Boolean {
        val robotPosition: IntArray = getRobotPosition()
        val sensorData: BooleanArray = scan(robotPosition[0], robotPosition[1], robotPosition[2])
        val frontObstructed = sensorData[0]
        val rightObstructed = sensorData[1]

        if (!rightObstructed) {
            turnRobot(Direction.RIGHT)
            return false
        }

        if (frontObstructed) {
            turnRobot(Direction.LEFT)
            return false
        }

        CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            broadcast(Broadcast.MOVE_COMPLETE, sensorData)
        }

        return true
    }
}