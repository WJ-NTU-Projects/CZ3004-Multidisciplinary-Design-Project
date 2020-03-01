package wjayteo.mdp.algorithms.algorithm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import wjayteo.mdp.algorithms.algorithm.AStarSearch.Companion.GridNode
import wjayteo.mdp.algorithms.arena.*
import wjayteo.mdp.algorithms.uicomponent.ControlsView
import wjayteo.mdp.algorithms.wifi.WifiSocketController
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs


class Exploration : Algorithm() {
    private var started: Boolean = false
    private var wallHug: Boolean = true
    private var stop: Boolean = false
    private var previousCommand: Int = FORWARD
    private var lastStartedThread: Thread? = null
    private val stepReference: AtomicInteger = AtomicInteger(0)

    override fun messageReceived(message: String) {
        if (message.contains("#")) {
            CoroutineScope(Dispatchers.Default).launch {
                val sensorReadings: List<String> = message.split("#")
                if (sensorReadings.size != 7) return@launch

                try {
                    val step: Int = sensorReadings[0].toInt()
                    val currentStep = stepReference.incrementAndGet()

                    if (step > 0) {
                        if (step != currentStep) return@launch
                        Robot.moveTemp()
                    }
                } catch (e: NumberFormatException) { return@launch }

                val facing: Int = Robot.facing
                val x: Int = Robot.position.x
                val y: Int = Robot.position.y
                try { Sensor.updateArenaSensor1(x, y, facing, sensorReadings[1].toInt()) } catch (e: NumberFormatException) {}
                try { Sensor.updateArenaSensor2(x, y, facing, sensorReadings[2].toInt()) } catch (e: NumberFormatException) {}
                try { Sensor.updateArenaSensor3(x, y, facing, sensorReadings[3].toInt()) } catch (e: NumberFormatException) {}
                try { Sensor.updateArenaSensor4(x, y, facing, sensorReadings[4].toInt()) } catch (e: NumberFormatException) {}
                try { Sensor.updateArenaSensor5(x, y, facing, sensorReadings[5].toInt()) } catch (e: NumberFormatException) {}
                try { Sensor.updateArenaSensor6(x, y, facing, sensorReadings[6].toInt()) } catch (e: NumberFormatException) {}
            }

            return
        }

        when (message) {
            "C" -> lastStartedThread?.join()
            "S" -> sendArena()

            "M" -> {
                lastStartedThread?.join()
                step()
                Thread.sleep(10)
                sendArena()
            }

            "L" -> {
                if (!started) Robot.turn(-90)
                lastStartedThread?.join()
                step()
                Thread.sleep(10)
                WifiSocketController.writeSynchronous("D", "#robotPosition:${Robot.position.x}, ${Robot.position.y}, ${Robot.facing}")
            }

            "R" -> {
                if (!started) Robot.turn(90)
                lastStartedThread?.join()
                step()
                Thread.sleep(10)
                WifiSocketController.writeSynchronous("D", "#robotPosition:${Robot.position.x}, ${Robot.position.y}, ${Robot.facing}")
            }

            else -> return
        }

        if (stop) started = false
    }

    fun init() {
        WifiSocketController.setListener(this)
        Arena.reset()
        Robot.reset()
    }

    fun start() {
        init()
        stepReference.set(0)
        wallHug = true
        stop = false

        if (ACTUAL_RUN) {
            started = true
            step()
        } else {
            simulate()
        }
    }

    fun stop() {
        stop = true
        if (ACTUAL_RUN) WifiSocketController.write("A", "B")
        else started = false
    }

    fun testSensorReadings() {
        WifiSocketController.write("A", "I")
    }

    private fun step() {
        if (stop) return

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
                WifiSocketController.write("A", "N")
                return
            }

            previousCommand = RIGHT
            WifiSocketController.write("A", "R")
            Robot.turn(90)
        }
    }

    private fun simulate() {
        CoroutineScope(Dispatchers.Default).launch {
            while (!stop) {
                delay(100)
                if (started && Robot.position.x == Arena.start.x && Robot.position.y == Arena.start.y) wallHug = false

                if (wallHug) {
                    if (!Robot.isLeftObstructed() && previousCommand != LEFT) {
                        previousCommand = LEFT
                        Robot.turn(-90)
                        continue
                    }

                    if (!Robot.isFrontObstructed()) {
                        started = true
                        previousCommand = FORWARD
                        Robot.moveTemp()
                        continue
                    }

                    previousCommand = RIGHT
                    Robot.turn(90)
                    continue
                }

                if (Arena.isEveryGridExplored()) {
                    returnToStart()
                    return@launch
                }

                if (!isGridExplored(FORWARD) && !Robot.isFrontObstructed()) {
                    previousCommand = FORWARD
                    Robot.moveTemp()
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
                    delay(100)
                    Robot.moveAdvanced(path.x, path.y)
                }
            }
        }
    }

    private fun returnToStart() {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
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
                    delay(100)
                    Robot.moveAdvanced(path.x, path.y)
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
                    shortestDistanceMovable = distance;
                    coordinatesMovable.x = x;
                    coordinatesMovable.y = y;
                    continue;
                }

                if (distance >= shortestDistanceUnmovable) continue;
                shortestDistanceUnmovable = distance;
                coordinatesUnmovable.x = x;
                coordinatesUnmovable.y = y;
            }
        }

        if (Arena.isMovable(coordinatesMovable.x, coordinatesMovable.y)) return coordinatesMovable

        val x: Int = coordinatesUnmovable.x
        val y: Int = coordinatesUnmovable.y
        val coordinates = Coordinates(-1, -1)
        var shortestDistance: Int = Int.MAX_VALUE
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
                if (found) return coordinates;
                repeat = true;
            }
        }

        if (found) return coordinates;
        coordinates.y = 15 * y + x;
        return coordinates;
    }

    private fun isGridExplored(direction: Int): Boolean {
        var x: Int = Robot.position.x
        var y: Int = Robot.position.y

        when (Robot.facing) {
            0 -> {
                when (direction) {
                    FORWARD -> y += 2
                    LEFT -> x -= 2
                    RIGHT -> x += 2
                    REVERSE -> y -= 2
                }
            }

            90 -> {
                when (direction) {
                    FORWARD -> x += 2
                    LEFT -> y += 2
                    RIGHT -> y -= 2
                    REVERSE -> x -= 2
                }
            }

            180 -> {
                when (direction) {
                    FORWARD -> y -= 2
                    LEFT -> x += 2
                    RIGHT -> x -= 2
                    REVERSE -> y += 2
                }
            }

            270 -> {
                when (direction) {
                    FORWARD -> x -= 2
                    LEFT -> y -= 2
                    RIGHT -> y += 2
                    REVERSE -> x += 2
                }
            }
        }

        if (Arena.isInvalidCoordinates(x, y)) return true
        return Arena.isExplored(x, y)
    }

    private fun sendArena() {
        val descriptor: List<String> = MapDescriptor.fromArray(Arena.exploreArray, Arena.obstacleArray, 1)
        WifiSocketController.writeSynchronous("D", "#r:${descriptor[0]},${descriptor[1]},${Robot.position.x},${Robot.position.y},${Robot.facing}")
    }
}