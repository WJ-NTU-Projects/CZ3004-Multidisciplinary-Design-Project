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
        val messages: List<String> = message.split("#")
        val moved: Int = messages[6].toInt()
        if (moved >= 1) Robot.moveTemp()
        val x = Robot.position.x
        val y = Robot.position.y
        val facing = Robot.facing
        val sensor1: Int = messages[0].toInt()
        val sensor2: Int = messages[1].toInt()
        val sensor3: Int = messages[2].toInt()
        val sensor4: Int = messages[3].toInt()
        val sensor5: Int = messages[4].toInt()
        val sensor6: Int = messages[5].toInt()
        Sensor.updateArenaSensor1(x, y, facing, sensor1)
        Sensor.updateArenaSensor2(x, y, facing, sensor2)
        Sensor.updateArenaSensor3(x, y, facing, sensor3)
        Sensor.updateArenaSensor4(x, y, facing, sensor4)
        Sensor.updateArenaSensor5(x, y, facing, sensor5)
        Sensor.updateArenaSensor6(x, y, facing, sensor6)
        if (x == Arena.start.x && y == Arena.start.y) wallHug = false

        if (moved != 1) {
            step()
            Arena.sendArena()
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