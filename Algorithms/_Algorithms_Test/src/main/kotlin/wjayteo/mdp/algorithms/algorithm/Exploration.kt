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
    private var moveCount: Int = 0
    private var hugRightStartX = 1
    private var hugRightStartY = 1
    private var commandBeforeHugRight = LEFT
    private var imagesFound = 0
    private val imagesCount = 5
    private var counter = 3
    private var hugRight = false
    private var justStartedHugRight = false

    override fun messageReceived(message: String) {
        if (!started) {
            if (message.contains(":")) {
                val s: List<String> = message.split(":")
                if (s[0] == "waypoint") {
                    val coords: List<String> = s[1].split(", ")

                    try {
                        val x: Int = coords[0].toInt()
                        val y: Int = coords[1].toInt()
                        Arena.setWaypoint(x, y)
                        Arena.refreshPoints()
                    } catch (e: NumberFormatException) {}
                }
            }
            if (message == "exs") {
                started = true
                startTime = System.currentTimeMillis()
                step()
            }

            return
        }

        if (!message.contains("#")) return
        val messages: List<String> = message.split("#")
        val moved: Int = messages[6].toInt()
        if (moved >= 1) for (i in 0 until moveCount) Robot.moveTemp()

        val x = Robot.position.x
        val y = Robot.position.y
        val facing = Robot.facing
        var imageX: Int = x
        var imageY: Int = y

        when (facing) {
            0 -> {
                imageX = Robot.position.x - 2
                imageY = Robot.position.y
            }

            90 -> {
                imageX = Robot.position.x
                imageY = Robot.position.y + 2
            }

            180 -> {
                imageX = Robot.position.x + 2
                imageY = Robot.position.y
            }

            270 -> {
                imageX = Robot.position.x
                imageY = Robot.position.y - 2
            }
        }

        if (!Arena.isInvalidCoordinates(imageX, imageY)) {
            Thread.sleep(10)
            WifiSocketController.write("R", "P")
            Thread.sleep(10)
        }

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
        Arena.refreshPoints()
        wallHug = true

        if (!ACTUAL_RUN) {
            delay = (1000.0 / ACTIONS_PER_SECOND).toLong()
            simulationStarted = true
            startTime = System.currentTimeMillis()
            simulate()
        } else {
            Arena.reset()
            WifiSocketController.write("A", "E")
            Thread.sleep(1000)
            WifiSocketController.write("A", "S")
        }
    }

    fun stop(completed: Boolean = false) {
        if (!started && !simulationStarted) return
        started = false
        simulationStarted = false
        if (!completed) return

        val endTime: Long = System.currentTimeMillis()
        println("-------------")
        val seconds: Double = (endTime - startTime) / 1000.0
        println("TIME TAKEN: $seconds seconds")
        println("-------------")

        if (ACTUAL_RUN) WifiSocketController.write("D", "#exe")
        Arena.refreshPoints()

        if (ACTUAL_RUN && !braking.get()) {
            braking.set(true)
            Arena.endHug()
            Arena.sendArena()
        }

        Thread.sleep(1000)
        if (ACTUAL_RUN) WifiSocketController.write("A", "R")
        Robot.turn(90)
        Thread.sleep(3000)
        if (ACTUAL_RUN) WifiSocketController.write("A", "S")

        if (Arena.isInvalidCoordinates(Arena.waypoint)) {
            ControlsView.stop()
            return
        }

        ControlsView.start()
        MasterView.fastestPath.start()
    }

    private fun step() {
        if (!started) return
        braking.set(false)

        if (wallHug) {
//            if (!hugRight && imagesFound < imagesCount && Robot.checkRight() && Robot.position.x > 3 && Robot.position.x < 11) {
//                hugRight = true
//                hugRightStartX = Robot.position.x
//                hugRightStartY = Robot.position.y
//                commandBeforeHugRight = previousCommand
//                justStartedHugRight = true
//                WifiSocketController.write("A", "T")
//                Robot.turn(180)
//                return
//            }
//
//            if (hugRight && !justStartedHugRight && Robot.position.x == hugRightStartX && Robot.position.y == hugRightStartY) {
//                hugRight = false
//                previousCommand = commandBeforeHugRight
//                WifiSocketController.write("A", "T")
//                Robot.turn(180)
//                return
//            }

            if (!Robot.isLeftObstructed() && previousCommand != LEFT) {
                if (Robot.isFrontObstructed() && Robot.isLeftObstructed2()) {
                    previousCommand = RIGHT
                    WifiSocketController.write("A", "R")
                    Robot.turn(90)
                    return
                }

                previousCommand = LEFT
                WifiSocketController.write("A", "L")
                Robot.turn(-90)
                return
            }

            if (!Robot.isFrontObstructed()) {
                if (Robot.isWallFront2() && Robot.isLeftCompletelyBlocked() && Robot.isRightCompletelyBlocked()) {
                    previousCommand = RIGHT
                    WifiSocketController.write("A", "R")
                    Robot.turn(90)
                    return
                }

                previousCommand = FORWARD
                if (hugRight) justStartedHugRight = false
                val moves: Int = Robot.getContinuousMoveCount(0)

                if (moves == 0) {
                    moveCount = 1
                    WifiSocketController.write("A", "M")
                    return
                }

                var commands = ""

                for (i in 0 .. moves) {
                    commands += "M"
                }

                moveCount = moves + 1
                WifiSocketController.write("A", commands)
                return
            }

            previousCommand = RIGHT
            WifiSocketController.write("A", "R")
            Robot.turn(90)
            return
        }

        stop(completed = true)
        return
    }

    private fun simulate() {
        var hugRight = false

        CoroutineScope(Dispatchers.Default).launch {
            while (simulationStarted) {
//                if (Arena.coverageReached() || (TIME_LIMIT > 0 && System.currentTimeMillis() - startTime >= TIME_LIMIT)) {
//                    returnToStart()
//                    return@launch
//                }

                if (started && Robot.position.x == Arena.start.x && Robot.position.y == Arena.start.y) wallHug = false

                if (wallHug) {
                    if (!hugRight && imagesFound < imagesCount && Robot.checkRight() && Robot.position.x > 3 && Robot.position.x < 11) {
                        hugRight = true
                        hugRightStartX = Robot.position.x
                        hugRightStartY = Robot.position.y
                        commandBeforeHugRight = previousCommand
                        Robot.turn(180)
                        delay(delay)
                    }

                    if (!Robot.isLeftObstructed() && previousCommand != LEFT) {
                        if (Robot.isFrontObstructed() && Robot.isLeftCompletelyBlocked2()) {
                            previousCommand = RIGHT
                            Robot.turn(90)
                            delay((delay * 1.3).toLong())
                        } else {
                            previousCommand = LEFT
                            Robot.turn(-90)
                            delay((delay * 1.3).toLong())
                        }
                    } else if (!Robot.isFrontObstructed()) {
                        if (Robot.isWallFront2() && Robot.isLeftCompletelyBlocked() && Robot.isRightCompletelyBlocked()) {
                            previousCommand = RIGHT
                            Robot.turn(180)
                            delay((delay * 2).toLong())
                        } else {
                            started = true
                            previousCommand = FORWARD
                            val moves: Int = Robot.getContinuousMoveCount(imagesFound)

                            if (moves == 0) {
                                Robot.moveTemp()
                                delay(delay)
                            }

                            else {
                                for (i in 0 .. moves) {
                                    Robot.moveTemp()
                                    delay(delay / 2)
                                }

                                delay(delay / 2)
                            }
                        }
                    } else {
                        previousCommand = RIGHT
                        Robot.turn(90)
                        delay((delay * 1.3).toLong())
                    }

                    if (hugRight && Robot.position.x == hugRightStartX && Robot.position.y == hugRightStartY) {
                        hugRight = false
                        delay(delay)
                        Robot.turn(180)
                        previousCommand = commandBeforeHugRight
                    }

                    when (Robot.facing) {
                        0 -> println("Image Coordinates: ${Robot.position.x - 2}, ${Robot.position.y}")
                        90 -> println("Image Coordinates: ${Robot.position.x}, ${Robot.position.y + 2}")
                        180 -> println("Image Coordinates: ${Robot.position.x + 2}, ${Robot.position.y}")
                        270 -> println("Image Coordinates: ${Robot.position.x}, ${Robot.position.y - 2}")
                    }

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
                    stop(completed = true)
                    return@launch
                }

                val pathList: List<GridNode> = AStarSearch.run(Robot.position.x, Robot.position.y, Robot.facing, Arena.start.x, Arena.start.y)

                if (pathList.isEmpty()) {
                    stop(completed = true)
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