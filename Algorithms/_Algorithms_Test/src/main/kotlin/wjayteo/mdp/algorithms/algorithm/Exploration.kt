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
    private var braking: AtomicBoolean = AtomicBoolean(false)
    private var delay: Long = 100L
    private var startTime: Long = 0L

    override fun messageReceived(message: String) {
        if (!started && message == "exs") {
            started = true
            step()
            return
        }

        if (!message.contains("#")) return
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
        step()
        Thread.sleep(10)
        Arena.sendArena()
    }

    fun start() {
        WifiSocketController.setListener(this)
        Robot.reset()
        Arena.setAllUnknown()
        //Arena.reset()
        wallHug = true
        startTime = System.currentTimeMillis()

        if (!ACTUAL_RUN) {
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
        val endTime: Long = System.currentTimeMillis()
        if (ACTUAL_RUN) WifiSocketController.write("D", "#exe")

        if (ACTUAL_RUN && !braking.get()) {
            braking.set(true)
            Arena.endHug()
            Arena.sendArena()
        }

        if (ACTUAL_RUN) {
            Thread.sleep(1000)

            when (Robot.facing) {
                90  -> {
                    WifiSocketController.write("A", "L")
                    Robot.turn(-90)
                }

                180 -> {
                    WifiSocketController.write("A", "T")
                    Robot.turn(180)
                }

                270 -> {
                    WifiSocketController.write("A", "R")
                    Robot.turn(90)
                }
            }
        }

        println("-------------")
        val seconds: Double = (endTime - startTime) / 1000.0
        println("TIME TAKEN: $seconds seconds")
        println("-------------")

        if (Arena.isInvalidCoordinates(Arena.waypoint)) {
            return
        }

        val pathList: List<IntArray> = MasterView.fastestPath.computeFastestPath()
        if (pathList.isEmpty()) return
        val f = pathList[0][2]

        when (f - Robot.facing) {
            90 -> {
                if (ACTUAL_RUN) WifiSocketController.write("A", "R")
                Robot.turn(90)
            }


            180, -180 -> {
                if (ACTUAL_RUN) WifiSocketController.write("A", "T")
                Robot.turn(180)
            }

            -90 -> {
                if (ACTUAL_RUN) WifiSocketController.write("A", "L")
                Robot.turn(-90)
            }
        }
    }

    private fun step() {
        if (!started) return
        braking.set(false)

        if (wallHug) {
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
        var hugRight = false

        CoroutineScope(Dispatchers.Default).launch {
            var hugRightStartX = 1
            var hugRightStartY = 1
            var commandBeforeHugRight = LEFT
            var imagesFound = 0
            val imagesCount = 5

            while (simulationStarted) {
                if (Arena.coverageReached() || (TIME_LIMIT > 0 && System.currentTimeMillis() - startTime >= TIME_LIMIT)) {
                    returnToStart()
                    return@launch
                }

                if (started && Robot.position.x == Arena.start.x && Robot.position.y == Arena.start.y) wallHug = false

                if (wallHug) {
//                    if (!hugRight && imagesFound < imagesCount && Robot.checkRight()) {
//                        hugRight = true
//                        hugRightStartX = Robot.position.x
//                        hugRightStartY = Robot.position.y
//                        commandBeforeHugRight = previousCommand
//                        Robot.turn(180)
//                        delay(delay)
//                    }

                    if (!Robot.isLeftObstructed() && previousCommand != LEFT) {
                        previousCommand = LEFT
                        Robot.turn(-90)
                    } else if (!Robot.isFrontObstructed()) {
                        started = true
                        previousCommand = FORWARD
                        val moves: Int = Robot.getContinuousMoveCount(imagesFound)
                        if (moves == 0) Robot.moveTemp()
                        else {
                            for (i in 0 .. moves) {
                                Robot.moveTemp()
                                delay(delay / 2)
                            }
                        }
                    } else {
                        previousCommand = RIGHT
                        Robot.turn(90)
                    }

//                    if (hugRight && Robot.position.x == hugRightStartX && Robot.position.y == hugRightStartY) {
//                        hugRight = false
//                        delay(delay)
//                        Robot.turn(180)
//                        previousCommand = commandBeforeHugRight
//                    }

                    when (Robot.facing) {
                        0 -> println("Image Coordinates: ${Robot.position.x - 2}, ${Robot.position.y}")
                        90 -> println("Image Coordinates: ${Robot.position.x}, ${Robot.position.y + 2}")
                        180 -> println("Image Coordinates: ${Robot.position.x + 2}, ${Robot.position.y}")
                        270 -> println("Image Coordinates: ${Robot.position.x}, ${Robot.position.y - 2}")
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
                    delay(delay / 2)
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
                    delay(delay / 2)
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