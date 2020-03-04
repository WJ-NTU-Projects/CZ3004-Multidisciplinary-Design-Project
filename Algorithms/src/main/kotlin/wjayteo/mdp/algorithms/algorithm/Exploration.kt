package wjayteo.mdp.algorithms.algorithm

import kotlinx.coroutines.*
import wjayteo.mdp.algorithms.algorithm.AStarSearch.Companion.GridNode
import wjayteo.mdp.algorithms.arena.*
import wjayteo.mdp.algorithms.uicomponent.ControlsView
import wjayteo.mdp.algorithms.uicomponent.MasterView
import wjayteo.mdp.algorithms.wifi.WifiSocketController
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs


class Exploration : Algorithm() {
    private var started: Boolean = false
    private var simulationStarted: Boolean = false
    private var wallHug: Boolean = true
    private var previousCommand: Int = FORWARD
    private var lastStartedJob: Job? = null
    private var braking: AtomicBoolean = AtomicBoolean(false)
    private val stepReference: AtomicInteger = AtomicInteger(0)
    private var delay: Long = 100L
    private var startTime: Long = 0L

    override fun messageReceived(message: String) {
        if (message.length == 2) {
            lastStartedJob = CoroutineScope(Dispatchers.Default).launch {
                try {
                    val step: Int = message[0].toString().toInt()
                    if (step > 0) Robot.moveTemp()

                    val set1: Int = message[0].toString().toInt()
                    val set2: Int = message[1].toString().toInt()

                    if (!braking.get() && set2 <= 1 && !Robot.isLeftObstructed() && previousCommand != LEFT) {
                        braking.set(true)
                        WifiSocketController.write("A", "B")
                    }

                    val str1: String = Integer.toBinaryString(set1).padStart(3, '0')
                    val str2: String = Integer.toBinaryString(set2).padStart(3, '0')
                    val x: Int = Robot.position.x
                    val y: Int = Robot.position.y
                    val facing: Int = Robot.facing
                    Sensor.updateArenaSensor1(x, y, facing, if (str1[0] == '1') 1 else 3)
                    Sensor.updateArenaSensor2(x, y, facing, if (str1[1] == '1') 1 else 3)
                    Sensor.updateArenaSensor3(x, y, facing, if (str1[2] == '1') 1 else 3)
                    Sensor.updateArenaSensor4(x, y, facing, if (str2[0] == '1') 1 else 3)
                    Sensor.updateArenaSensor5(x, y, facing, if (str2[1] == '1') 1 else 3)
                    Sensor.updateArenaSensor6(x, y, facing, if (str2[2] == '1') 1 else 3)
                } catch (e: NumberFormatException) {}
            }

            return
        }

        if (message.length == 1) {
            while (lastStartedJob != null && lastStartedJob?.isCompleted == false) {
                Thread.sleep(1)
            }

            if (started) step()

            when (message[0]) {
                'M' -> {
                    //Arena.sendArena()
                    if (Robot.position.x == Arena.start.x && Robot.position.y == Arena.start.y) wallHug = false
                }

                //'L', 'R' -> WifiSocketController.write("D", "#robotPosition:${Robot.position.x}, ${Robot.position.y}, ${Robot.facing}")
                else -> return
            }

            if (!started) MasterView.idleListener.listen()
            return
        }
    }

    fun start() {
        WifiSocketController.setListener(this)
        Arena.setAllUnknown()
        stepReference.set(0)
        wallHug = true

        if (ACTUAL_RUN) {
            started = true
            step()
        } else {
            delay = (1000.0 / ACTIONS_PER_SECOND).toLong()
            simulationStarted = true
            startTime = System.currentTimeMillis()
            simulate()
        }
    }

    fun stop() {
        if (!started && !simulationStarted) return
        started = false
        simulationStarted = false

        if (ACTUAL_RUN && !braking.get()) {
            braking.set(true)
            WifiSocketController.write("A", "B")
        }
    }

    fun testSensorReadings() {
        WifiSocketController.write("A", "I")
    }

    private fun step() {
        if (!started) return
        braking.set(false)

        if (wallHug) {
            stepReference.set(0)

            if (!Robot.isLeftObstructed() && previousCommand != LEFT) {
                previousCommand = LEFT
                WifiSocketController.write("A", "L")
                Robot.turn(-90)
                return
            }

            if (!Robot.isFrontObstructed()) {
                previousCommand = FORWARD
                WifiSocketController.write("A", "M")
                Robot.moveTemp()
                return
            }

            previousCommand = RIGHT
            WifiSocketController.write("A", "R")
            Robot.turn(90)
            return
        }

        stop()
        return
    }

    private fun simulate() {
        CoroutineScope(Dispatchers.Default).launch {
            while (simulationStarted) {
                if (Arena.coverageReached() || (TIME_LIMIT > 0 && System.currentTimeMillis() - startTime >= TIME_LIMIT)) {
                    returnToStart()
                    return@launch
                }

                if (started && Robot.position.x == Arena.start.x && Robot.position.y == Arena.start.y) wallHug = false

                if (wallHug) {
                    if (!Robot.isLeftObstructed() && previousCommand != LEFT) {
                        previousCommand = LEFT
                        Robot.turn(-90)
                    } else if (!Robot.isFrontObstructed()) {
                        started = true
                        previousCommand = FORWARD
                        Robot.moveTemp()
                    } else {
                        previousCommand = RIGHT
                        Robot.turn(90)
                    }

                    delay(delay)
                    continue
                }

                if (Arena.isEveryGridExplored()) {
                    returnToStart()
                    return@launch
                }

                if (!isGridExploredFront() && !Robot.isFrontObstructed()) {
                    previousCommand = FORWARD
                    Robot.moveTemp()
                    delay(delay)
                    continue
                }

                val nearest: Coordinates = findNearestUnexploredGrid()

                if (Arena.isInvalidCoordinates(nearest, true)) {
                    returnToStart()
                    return@launch
                }

                val pathList: List<GridNode> = AStarSearch.run(Robot.position.x, Robot.position.y, Robot.facing, nearest.x, nearest.y)

                if (pathList.isEmpty()) {
                    returnToStart()
                    return@launch
                }

                for (path in pathList) {
                    if (!simulationStarted) return@launch

                    if (Arena.coverageReached() || (TIME_LIMIT > 0 && System.currentTimeMillis() - startTime >= TIME_LIMIT)) {
                        returnToStart()
                        return@launch
                    }

                    Robot.moveAdvanced(path.x, path.y)
                    delay(delay)
                }
            }
        }
    }

    private fun returnToStart() {
        CoroutineScope(Dispatchers.Default).launch {
            while (simulationStarted) {
                if (Robot.position.x == Arena.start.x && Robot.position.y == Arena.start.y) {
                    stop()
                    ControlsView.stop()
                    return@launch
                }

                val pathList: List<GridNode> = AStarSearch.run(Robot.position.x, Robot.position.y, Robot.facing, Arena.start.x, Arena.start.y)

                if (pathList.isEmpty()) {
                    stop()
                    ControlsView.stop()
                    return@launch
                }

                for (path in pathList) {
                    if (!simulationStarted) return@launch
                    Robot.moveAdvanced(path.x, path.y)
                    delay(delay)
                }
            }
        }
    }

    private fun findNearestUnexploredGrid(): Coordinates {
        val coordinatesMovable = Coordinates(-1, -1)
        val coordinatesUnmovable = Coordinates(-1, -1)
        var shortestDistanceMovable = Int.MAX_VALUE
        var shortestDistanceUnmovable = Int.MAX_VALUE

        for (y in 19 downTo 0) {
            for (x in 14 downTo 0) {
                if (Arena.isExplored(x, y)) continue
                val distance: Int = abs(x - Robot.position.x) + abs(y - Robot.position.y)
                if (distance >= shortestDistanceMovable) continue

                if (Arena.isMovable(x, y)) {
                    shortestDistanceMovable = distance
                    coordinatesMovable.x = x
                    coordinatesMovable.y = y
                    continue
                }

                if (distance >= shortestDistanceUnmovable) continue
                shortestDistanceUnmovable = distance
                coordinatesUnmovable.x = x
                coordinatesUnmovable.y = y
            }
        }

        if (Arena.isMovable(coordinatesMovable.x, coordinatesMovable.y)) return coordinatesMovable

        val x: Int = coordinatesUnmovable.x
        val y: Int = coordinatesUnmovable.y
        val coordinates = Coordinates(-1, -1)
        var shortestDistance: Int
        var found = false
        var repeat = false

        for (i in 0 until 2) {
            shortestDistance = Int.MAX_VALUE
            val bound: Int = if (repeat) 2 else 1

            for (yOffset in (bound * -1)..bound) {
                if (found) break

                for (xOffset in (bound * -1)..bound) {
                    if (repeat && xOffset != 0 && yOffset != 0) continue
                    val x1: Int = x + xOffset
                    val y1: Int = y + yOffset
                    val distance = abs(x1 - Robot.position.x) + abs(y1 - Robot.position.y)
                    if (!Arena.isMovable(x1, y1) || distance >= shortestDistance) continue

                    shortestDistance = distance
                    coordinates.x = x1
                    coordinates.y = y1
                    found = true
                    break
                }
            }

            if (!repeat) {
                if (found) return coordinates
                repeat = true
            }
        }

        if (found) return coordinates
        coordinates.y = 15 * y + x
        return coordinates
    }

    private fun isGridExploredFront(): Boolean {
        var x: Int = Robot.position.x
        var y: Int = Robot.position.y

        when (Robot.facing) {
            0   -> y += 2
            90  -> x += 2
            180 -> y -= 2
            270 -> x -= 2
        }

        if (Arena.isInvalidCoordinates(x, y)) return true
        return Arena.isExplored(x, y)
    }
}