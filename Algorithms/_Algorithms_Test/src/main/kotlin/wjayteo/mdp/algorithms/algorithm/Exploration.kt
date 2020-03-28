package wjayteo.mdp.algorithms.algorithm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import wjayteo.mdp.algorithms.algorithm.AStarSearch.Companion.GridNode
import wjayteo.mdp.algorithms.arena.Arena
import wjayteo.mdp.algorithms.arena.Coordinates
import wjayteo.mdp.algorithms.arena.Robot
import wjayteo.mdp.algorithms.arena.Sensor
import wjayteo.mdp.algorithms.uicomponent.ControlsView
import wjayteo.mdp.algorithms.uicomponent.MasterView
import wjayteo.mdp.algorithms.wifi.WifiSocketController
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
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
    private var hugRight = false
    private var justStartedHugRight = false
    private var imageCoordinatesList: ArrayList<Coordinates> = arrayListOf()
    private var facingList: ArrayList<Int> = arrayListOf()
    private var waitingForFP = false
    private var uTurn = false
    private var uTurnLeft = false
    private var forceMove = false

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

                        if (waitingForFP) {
                            ControlsView.start()
                            MasterView.fastestPath.start()
                        }
                    } catch (e: NumberFormatException) {}
                }

                return
            }

            if (message == "exs" && !waitingForFP) {
                started = true
                startTime = System.currentTimeMillis()
                step()
                return
            }

            if (waitingForFP) return
            if (!message.contains("#")) return
            val messages: List<String> = message.split("#")
            val x = Robot.position.x
            val y = Robot.position.y
            val facing = Robot.facing
            val sensor1: Int = messages[0].toInt()
            val sensor2: Int = messages[1].toInt()
            val sensor3: Int = messages[2].toInt()
            val sensor4: Int = messages[3].toInt()
            val sensor5: Int = messages[4].toInt()
            val sensor6: Int = messages[5].toInt()
            Sensor.updateArenaSensor1(x, y, facing, sensor1.coerceIn(0, 2))
            Sensor.updateArenaSensor2(x, y, facing, sensor2.coerceIn(0, 2))
            Sensor.updateArenaSensor3(x, y, facing, sensor3.coerceIn(0, 2))
            Sensor.updateArenaSensor4(x, y, facing, sensor4.coerceIn(0, 2))
            Sensor.updateArenaSensor5(x, y, facing, sensor5.coerceIn(0, 2))
            Sensor.updateArenaSensor6(x, y, facing, sensor6.coerceIn(0, 6))
            return
        }

        if (message == "terminate") {
            stop()
            return
        }

        if (!message.contains("#")) return

        if (uTurn) {
            uTurn = false
            if (uTurnLeft) {
                WifiSocketController.write("A", "L")
                Robot.turn(-90)
            } else {
                WifiSocketController.write("A", "R")
                Robot.turn(90)
            }
            return
        }

        val messages: List<String> = message.split("#")

        if (!forceMove) {
            val moved: Int = messages[6].toInt()
            if (moved >= 1) for (i in 0 until moved) Robot.moveTemp()
        }

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

        if (!forceMove && Robot.checkLeft()) { //!Arena.isInvalidCoordinates(imageX, imageY)) {
            //Thread.sleep(500)
            imageCoordinatesList.add(Coordinates(imageX, imageY))
            facingList.add(Robot.facing)
            WifiSocketController.write("R", "P")
            Thread.sleep(200)
        }

        val sensor1: Int = messages[0].toInt()
        val sensor2: Int = messages[1].toInt()
        val sensor3: Int = messages[2].toInt()
        val sensor4: Int = messages[3].toInt()
        val sensor5: Int = messages[4].toInt()
        val sensor6: Int = messages[5].toInt()
        Sensor.updateArenaSensor1(x, y, facing, sensor1.coerceIn(0, 2))
        Sensor.updateArenaSensor2(x, y, facing, sensor2.coerceIn(0, 2))
        Sensor.updateArenaSensor3(x, y, facing, sensor3.coerceIn(0, 2))
        Sensor.updateArenaSensor4(x, y, facing, sensor4.coerceIn(0, 2))
        Sensor.updateArenaSensor5(x, y, facing, sensor5.coerceIn(0, 2))
        Sensor.updateArenaSensor6(x, y, facing, sensor6.coerceIn(0, 6))

        if (!forceMove && Robot.isFrontCompletelyBlocked()) {
            forceMove = true
            WifiSocketController.write("A", "M")
            Thread.sleep(10)
            Arena.sendArena()
            return
        }

        if (x == Arena.start.x && y == Arena.start.y) wallHug = false
        forceMove = false
        step()
        Thread.sleep(10)
        Arena.sendArena()
    }

    fun getImages() {
        val content = StringBuilder()
        try {
            val url = URL("http://192.168.16.133:8123/end")
            val con: HttpURLConnection = url.openConnection() as HttpURLConnection
            con.requestMethod = "GET"
            val `in` = BufferedReader(InputStreamReader(con.inputStream))
            var inputLine: String?
            while (`in`.readLine().also { inputLine = it } != null) {
                content.append(inputLine)
            }
            `in`.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val s = content.toString()

        val arr1: List<String> = s.split(";")
        val arr: ArrayList<String> = arrayListOf()
        arr.addAll(arr1)
        arr[0] = arr[0].replace("\\[|\\]".toRegex(), "")
        arr[1] = arr[1].replace("\\[|\\]|\"".toRegex(), "")

        val rawIndex = arr[0].split(", ").toTypedArray()
        val classIndex = ArrayList<Int>()
        val coords = ArrayList<Coordinates>()
        val facings = ArrayList<Int>()
        for ((i, raw) in rawIndex.withIndex()) {
            val index = raw.toInt()

            if (index != -1) {
                classIndex.add(index)
                coords.add(imageCoordinatesList[i])
                facings.add(facingList[i])
            }
        }

        if (classIndex.isEmpty()) return
        val positions = arr[1].split(", ").toTypedArray()

        for (i in 0 until classIndex.size) {
            val p = positions[i].toLowerCase()

            when (p) {
                "l" -> {
                    when (facings[i]) {
                        0 -> coords[i].y -= 1
                        90 -> coords[i].x -= 1
                        180 -> coords[i].y += 1
                        270 -> coords[i].x += 1
                    }
                }

                "r" -> {
                    when (facings[i]) {
                        0 -> coords[i].y += 1
                        90 -> coords[i].x += 1
                        180 -> coords[i].y -= 1
                        270 -> coords[i].x -= 1
                    }
                }
            }
        }

        println(classIndex)
        println(coords)

        var ss = "#im:"
        for (i in 0 until classIndex.size) ss += "(${classIndex[i]},${coords[i].x},${coords[i].y})"
        WifiSocketController.write("D", ss)
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
            Thread.sleep(100)
            WifiSocketController.write("A", "S")
        }
    }

    fun stop(completed: Boolean = false) {
        if (!started && !simulationStarted) return
        started = false
        simulationStarted = false

        val endTime: Long = System.currentTimeMillis()
        println("-------------")
        val seconds: Double = (endTime - startTime) / 1000.0
        println("TIME TAKEN: $seconds seconds")
        println("-------------")

        if (ACTUAL_RUN) WifiSocketController.write("D", "exe")
        Arena.refreshPoints()

        if (ACTUAL_RUN && !braking.get()) {
            braking.set(true)
            Arena.endHug()
            Arena.sendArena()
        }

        var ss = ""

        for ((index, coordinates) in imageCoordinatesList.withIndex()) {
            ss += "(${index}: ${coordinates.x}, ${coordinates.y}) "
            if (index != imageCoordinatesList.size - 1) ss += "\n"
        }

        ss = ss.trim()
        println("-------------")
        println(ss)
        println("-------------")

        try {
            getImages()
        } catch (e: Exception) {}

        if (!completed) return
        Thread.sleep(1000)
        if (ACTUAL_RUN) WifiSocketController.write("A", "R")
        Robot.turn(90)
        Thread.sleep(3000)
        if (ACTUAL_RUN) WifiSocketController.write("A", "S")

        if (Arena.isInvalidCoordinates(Arena.waypoint)) {
            if (ACTUAL_RUN) waitingForFP = true
            else ControlsView.stop()
            return
        }

        ControlsView.start()
        MasterView.fastestPath.start()
    }

    private fun step() {
        if (!started) return
        braking.set(false)

        if (wallHug) {
            // Math.floorMod(Robot.facing, 180) == 0
//            if (!hugRight && Robot.checkRight() && Robot.position.x >= 3 && Robot.position.x <= 11) {
//                hugRight = true
//                hugRightStartX = Robot.position.x
//                hugRightStartY = Robot.position.y
//                commandBeforeHugRight = previousCommand
//                justStartedHugRight = true
//                WifiSocketController.write("A", "R")
//                Robot.turn(90)
//                uTurn = true
//                uTurnLeft = false
//                return
//            }
//
//            if (hugRight && !justStartedHugRight && Robot.position.x == hugRightStartX && Robot.position.y == hugRightStartY) {
//                hugRight = false
//                previousCommand = commandBeforeHugRight
//                WifiSocketController.write("A", "L")
//                Robot.turn(-90)
//                uTurn = true
//                uTurnLeft = true
//                return
//            }

//            if (!checked1 && Robot.position.x == 9 && Robot.position.y == 5 && Robot.facing == 180) {
//                WifiSocketController.write("A", "R")
//                Robot.turn(90)
//                uTurn = true
//                uTurnLeft = false
//                testttt = true
//                checked1 = true
//                return
//            }
//
//            if (testttt) {
//                WifiSocketController.write("A", "L")
//                Robot.turn(90)
//                uTurn = true
//                uTurnLeft = true
//                testttt = false
//                return
//            }

            if (!Robot.isLeftObstructed() && previousCommand != LEFT) {
                if (Robot.isFrontObstructed() && Robot.isLeftCompletelyBlocked2()) {
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
//                if (Robot.isWallFront2() && Robot.isLeftCompletelyBlocked() && Robot.isRightCompletelyBlocked()) {
//                    previousCommand = RIGHT
//                    WifiSocketController.write("A", "R")
//                    Robot.turn(90)
//                    return
//                }

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

                returnToStart()

//                if (Arena.isEveryGridExplored()) {
//                    returnToStart()
//                    return@launch
//                }
//
//                if (!isGridExploredFront() && !Robot.isFrontObstructed()) {
//                    previousCommand = FORWARD
//                    Robot.moveTemp()
//                    delay(delay)
//                    continue
//                }
//
//                val nearest: Coordinates = findNearestUnexploredGrid()
//
//                if (Arena.isInvalidCoordinates(nearest, true)) {
//                    returnToStart()
//                    return@launch
//                }
//
//                val pathList: List<GridNode> = AStarSearch.run(Robot.position.x, Robot.position.y, Robot.facing, nearest.x, nearest.y)
//
//                if (pathList.isEmpty()) {
//                    returnToStart()
//                    return@launch
//                }
//
//                for (path in pathList) {
//                    if (!simulationStarted) return@launch
//
//                    if (Arena.coverageReached() || (TIME_LIMIT > 0 && System.currentTimeMillis() - startTime >= TIME_LIMIT)) {
//                        returnToStart()
//                        return@launch
//                    }
//
//                    Robot.moveAdvanced(path.x, path.y)
//                    delay(delay / 2)
//                }
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