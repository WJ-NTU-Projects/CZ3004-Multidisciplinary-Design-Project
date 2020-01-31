package ntu.mdp.android.mdptestkotlin.utils

import android.util.Log
import kotlinx.coroutines.*
import ntu.mdp.android.mdptestkotlin.App.Companion.allowDiagonalExploration
import ntu.mdp.android.mdptestkotlin.App.Companion.plotPathChosen
import ntu.mdp.android.mdptestkotlin.App.Companion.plotSearch
import ntu.mdp.android.mdptestkotlin.App.Companion.simulationDelay
import ntu.mdp.android.mdptestkotlin.MainActivityController
import ntu.mdp.android.mdptestkotlin.arena.Arena
import ntu.mdp.android.mdptestkotlin.bluetooth.BluetoothController
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 *
 * WELCOME TO THE SCRATCHPAD.
 *
 * THIS CLASS IS USED FOR TESTING WEIRD AND STUPID IDEAS AND EXPERIMENTAL STUFF.
 *
 */

class ScratchPad(activityController: MainActivityController, private val activityCallback: (stop: Boolean) -> Unit) {
    private val arenaController = activityController.arenaController
    private val arena = arenaController.arena
    private var stop = false
    private var fastestPath = false

    fun stop() {
        stop = true
    }

    fun exploration() {
        stop = false
        fastestPath = allowDiagonalExploration
        var x: Int
        var y: Int
        var r: Int
        var counter = 0
        var haveDelay = true
        findBestStartFacing()

        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                if (haveDelay) delay(simulationDelay)
                if (stop) return@launch

                haveDelay = true
                x = arena.robotCoordinates[0]
                y = arena.robotCoordinates[1]
                r = arena.robotCoordinates[2]
                if (attemptMove(x, y, r)) continue
                if (!hasUnexploredGrid()) break

                val nearestUnexplored = nearestUnexploredGrid()
                val xTarget = nearestUnexplored.first
                val yTarget = nearestUnexplored.second
                if (counter >= 15) break

                if (!arena.isValidCoordinates(xTarget, yTarget)) {
                    if (xTarget == -1) {
                        x = (yTarget % 15)
                        y = (yTarget / 15)
                        if (arena.isValidCoordinates(x, y)) arena.plot(x, y, Arena.GridType.EXPLORED)
                        haveDelay = false
                    }

                    counter++
                    continue
                }

                counter = 0
                arena.resetPathing()
                val path: List<IntArray> = findShortestPath(x, y, r, xTarget, yTarget).second

                if (path.isEmpty()) {
                    counter++
                    continue
                }

                for ((i, p) in path.withIndex()) {
                    if (i != 0) delay(simulationDelay)
                    if (stop) return@launch

                    x = arena.robotCoordinates[0]
                    y = arena.robotCoordinates[1]
                    r = arena.robotCoordinates[2]
                    if (attemptMove(x, y, r)) break

                    val requestedFacing: Int = getRequestedFacing(p[0], p[1])
                    if (requestedFacing == -1) {
                        goHome()
                        return@launch
                    } // gg

                    val robotFacing = arenaController.getRobotFacing()
                    if (plotPathChosen) arena.plot(p[0], p[1], Arena.GridType.SEARCH_PICKED)

                    if ((requestedFacing - robotFacing) % 90 == 0 && requestedFacing != robotFacing && abs(requestedFacing - robotFacing) != 180) {
                        handleMove(requestedFacing)
                        delay(simulationDelay)
                    } else if ((requestedFacing - robotFacing) % 90 != 0 && requestedFacing != robotFacing && abs(requestedFacing - robotFacing) != 180) {
                        handleMove(requestedFacing)
                        delay((simulationDelay * 0.5).toLong())
                    }

                    handleMove(requestedFacing)
                    break
                }

            }

            arena.resetPathing()
            goHome()
        }
    }

    fun fastestPath() {
        stop = false
        fastestPath = allowDiagonalExploration
        var startX = arena.startPointCoordinates.first
        var startY = arena.startPointCoordinates.second
        var endX = arena.wayPointCoordinates.first
        var endY = arena.wayPointCoordinates.second
        var smallestCost = 99999.0
        var bestWaypointPath: List<IntArray> = listOf()

        for (i in 0 .. 90 step 45) {
            arena.resetPathing()
            val pathToWaypoint: Pair<Double, List<IntArray>> = findShortestPath(startX, startY, i, endX, endY)
            val cost = pathToWaypoint.first

            if (cost < smallestCost) {
                smallestCost = cost
                bestWaypointPath = pathToWaypoint.second
            }
        }

        startX = endX
        startY = endY
        val startR = if (bestWaypointPath.isEmpty()) 0 else bestWaypointPath.last()[2]
        endX = arena.goalPointCoordinates.first
        endY = arena.goalPointCoordinates.second
        val goalPointPath = findShortestPath(startX, startY, startR, endX, endY).second

        CoroutineScope(Dispatchers.Main).launch {
            tracePath(bestWaypointPath)
            delay(simulationDelay)
            tracePath(goalPointPath)
            activityCallback(true)
        }
    }

    private fun findBestStartFacing() {
        val x = arena.robotCoordinates[0]
        val y = arena.robotCoordinates[1]

        // ALWAYS CHECK RIGHT FIRST
        val bestFacing = when {
            arena.isRobotMovable(x, y + 1)  -> 0
            arena.isRobotMovable(x + 1, y)  -> 90
            arena.isRobotMovable(x, y - 1)  -> 180
            else                            -> 270
        }

        arena.moveRobot(x, y, bestFacing)
    }

    private fun attemptMove(x: Int, y: Int, r: Int): Boolean {
        // CHECK IN CURRENT DIRECTION FIRST
        if (r == 0 || r == 180) {
            if (y <= 1) {
                if (checkBottom(x, y)) {
                    handleMove(180)
                    return true
                }

                if (checkUp(x, y)) {
                    handleMove(0)
                    return true
                }
            } else {
                if (checkUp(x, y)) {
                    handleMove(0)
                    return true
                }

                if (checkBottom(x, y)) {
                    handleMove(180)
                    return true
                }
            }
        }

        if (r == 90 || r == 270) {
            if (x >= 7) {
                if (checkRight(x, y)) {
                    handleMove(90)
                    return true
                }

                if (checkLeft(x, y)) {
                    handleMove(270)
                    return true
                }
            } else {
                if (checkLeft(x, y)) {
                    handleMove(270)
                    return true
                }

                if (checkRight(x, y)) {
                    handleMove(90)
                    return true
                }
            }
        }

        if (y <= 1) {
            // CHECK IN OTHER DIRECTIONS, WITH PRIORITY
            if (x >= 7) {
                if (r != 90 && checkRight(x, y)) {
                    handleMove(90)
                    return true
                }
            } else {
                if (r != 270 && checkLeft(x, y)) {
                    handleMove(270)
                    return true
                }
            }

            if (r != 180 && checkBottom(x, y)) {
                handleMove(180)
                return true
            }

            if (x < 7) {
                if (r != 90 && checkRight(x, y)) {
                    handleMove(90)
                    return true
                }
            } else {
                if (r != 270 && checkLeft(x, y)) {
                    handleMove(270)
                    return true
                }
            }

            if (r != 0 && checkUp(x, y)) {
                handleMove(0)
                return true
            }
        } else {
            if (x >= 7) {
                if (r != 90 && checkRight(x, y)) {
                    handleMove(90)
                    return true
                }
            } else {
                if (r != 270 && checkLeft(x, y)) {
                    handleMove(270)
                    return true
                }
            }

            if (r != 0 && checkUp(x, y)) {
                handleMove(0)
                return true
            }

            if (x < 7) {
                if (r != 90 && checkRight(x, y)) {
                    handleMove(90)
                    return true
                }
            } else {
                if (r != 270 && checkLeft(x, y)) {
                    handleMove(270)
                    return true
                }
            }

            if (r != 180 && checkBottom(x, y)) {
                handleMove(180)
                return true
            }
        }

        return false
    }

    private fun checkRight(x: Int, y: Int): Boolean {
        if (arena.isRobotMovable(x + 1, y) && !arena.isExplored(x + 2, y, 90, false)) {
            return true
        } else if (arena.isRobotMovable(x + 1, y) && !arena.isExplored(x + 2, y, 90, true)) {
            return true
        }

        return false
    }

    private fun checkBottom(x: Int, y: Int): Boolean {
        if (arena.isRobotMovable(x, y - 1) && !arena.isExplored(x, y - 2, 180, false)) {
            return true
        } else if (arena.isRobotMovable(x, y - 1) && !arena.isExplored(x, y - 2, 180, true)) {
            return true
        }

        return false
    }

    private fun checkUp(x: Int, y: Int): Boolean {
        if (arena.isRobotMovable(x, y + 1) && !arena.isExplored(x, y + 2, 0, false)) {
            return true
        } else if (arena.isRobotMovable(x, y + 1) && !arena.isExplored(x, y + 2, 0, true)) {
            return true
        }

        return false
    }

    private fun checkLeft(x: Int, y: Int): Boolean {
        if (arena.isRobotMovable(x - 1, y) && !arena.isExplored(x - 2, y, 270, false)) {
            return true
        } else if (arena.isRobotMovable(x - 1, y) && !arena.isExplored(x - 2, y, 270, true)) {
            return true
        }

        return false
    }

    private fun getRequestedFacing(endX: Int, endY: Int): Int {
        val startX = arena.robotCoordinates[0]
        val startY = arena.robotCoordinates[1]
        val xDiff = startX - endX
        val yDiff = startY - endY
        var requestedFacing = 0

        if (xDiff != 0 && yDiff != 0) {
            return  if (xDiff == -1 && yDiff == -1)     45
                    else if (xDiff == -1 && yDiff == 1) 135
                    else if (xDiff == 1 && yDiff == -1) 315
                    else if (xDiff == 1 && yDiff == 1)  225
                    else                                -1
        }

        if (xDiff == 1) requestedFacing = 270
        if (xDiff == -1) requestedFacing = 90
        if (yDiff == 1) requestedFacing = 180
        if (yDiff == -1) requestedFacing = 0
        return requestedFacing
    }

    private fun handleMove(r: Int) {
        Log.e("R", r.toString())
        var requestedFacing: Int = r
        val robotFacing = arenaController.getRobotFacing()
        var facingOffset: Int = abs(robotFacing - requestedFacing)

        if (Math.floorMod(facingOffset, 90) != 0) {
            val x = arena.robotCoordinates[0]
            val y = arena.robotCoordinates[1]

            if (facingOffset in 91..314) requestedFacing += 180
            requestedFacing = Math.floorMod(requestedFacing, 360)

            when (requestedFacing) {
                0 -> arena.moveRobot(x, y, requestedFacing)
                90 -> arena.moveRobot(x, y, requestedFacing)
                180 -> arena.moveRobot(x, y, requestedFacing)
                270 -> arena.moveRobot(x, y, requestedFacing)
                45  -> arena.moveRobot(x, y, requestedFacing)
                135 -> arena.moveRobot(x, y, requestedFacing)
                225 -> arena.moveRobot(x, y, requestedFacing)
                315 -> arena.moveRobot(x, y, requestedFacing)
            }

            return
        }

        if (Math.floorMod(requestedFacing, 90) != 0) {
            val x = arena.robotCoordinates[0]
            val y = arena.robotCoordinates[1]
            var facingDirection = requestedFacing
            if (facingOffset == 180)  facingDirection += 180
            facingDirection = Math.floorMod(facingDirection, 360)

            when (requestedFacing) {
                0 -> arena.moveRobot(x, y + 1, facingDirection)
                90 -> arena.moveRobot(x + 1, y, facingDirection)
                180 -> arena.moveRobot(x, y - 1, facingDirection)
                270 -> arena.moveRobot(x - 1, y, facingDirection)
                45  -> arena.moveRobot(x + 1, y + 1, facingDirection)
                135 -> arena.moveRobot(x + 1, y - 1, facingDirection)
                225 -> arena.moveRobot(x - 1, y - 1, facingDirection)
                315 -> arena.moveRobot(x - 1, y + 1, facingDirection)
            }

            return
        }

        facingOffset = robotFacing - requestedFacing
        if (requestedFacing == robotFacing)                     arenaController.moveRobot(1, BluetoothController.isSocketConnected())
        else if (abs(requestedFacing - robotFacing) == 180)     arenaController.moveRobot(-1, BluetoothController.isSocketConnected())
        else if (facingOffset == 90 || facingOffset == -270)    arenaController.turnRobot(-1, BluetoothController.isSocketConnected())
        else                                                    arenaController.turnRobot(1, BluetoothController.isSocketConnected())
    }

    private fun hasUnexploredGrid(): Boolean {
        for (y in 19 downTo 0) {
            for (x in 0..14) {
                if (arena.gridStateArray[y][x] == 0) return true
            }
        }

        return false
    }

    private fun nearestUnexploredGrid(): Pair<Int, Int> {
        var shortestDistance = 999
        var coordinates: Pair<Int, Int> = Pair(-1, -1)

        for (y in 19 downTo 0) {
            for (x in 0 .. 14) {
                if (arena.gridStateArray[y][x] != 0) continue
                val distance = abs(x - arena.robotCoordinates[0]) + abs(y - arena.robotCoordinates[1])
                if (distance >= shortestDistance) continue
                shortestDistance = distance
                coordinates = Pair(x, y)
                continue
            }
        }

        if (arena.isRobotMovable(coordinates.first, coordinates.second)) return coordinates

        val x = coordinates.first
        val y = coordinates.second
        var found = false
        shortestDistance = 999

        for (yOffset in -1..1) {
            for (xOffset in -1..1) {
                val xNew: Int = x + xOffset
                val yNew: Int = y + yOffset
                val distance = abs(xNew -  arena.robotCoordinates[0]) + abs(yNew - arena.robotCoordinates[1])

                if (arena.isRobotMovable(xNew, yNew) && distance < shortestDistance) {
                    shortestDistance = distance
                    coordinates = Pair(xNew, yNew)
                    found = true
                }
            }
        }

        if (!found) coordinates = Pair(-1, 15 * y + x)
        return coordinates
    }

    private suspend fun tracePath(path: List<IntArray>) = withContext(Dispatchers.Main) {
        for ((i, p) in path.withIndex()) {
            if (i != 0) delay(simulationDelay)
            if (stop) return@withContext
            val requestedFacing: Int = getRequestedFacing(p[0], p[1])
            if (requestedFacing == -1) return@withContext // gg

            val robotFacing = arenaController.getRobotFacing()
            if (plotPathChosen) arena.plot(p[0], p[1], Arena.GridType.SEARCH_PICKED)

            if ((requestedFacing - robotFacing) % 90 == 0 && requestedFacing != robotFacing && abs(requestedFacing - robotFacing) != 180) {
                handleMove(requestedFacing)
                delay(simulationDelay)
            } else if ((requestedFacing - robotFacing) % 90 != 0 && requestedFacing != robotFacing) {
                handleMove(requestedFacing)
                delay((simulationDelay * 0.5).toLong())
            }

            handleMove(requestedFacing)
        }
    }

    private fun goHome() {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                if (stop) return@launch
                val startX = arena.robotCoordinates[0]
                val startY = arena.robotCoordinates[1]
                val startR = arena.robotCoordinates[2]
                val endX = arena.startPointCoordinates.first
                val endY = arena.startPointCoordinates.second
                val pathHome: Pair<Double, List<IntArray>> = findShortestPath(startX, startY, startR, endX, endY)
                val list = pathHome.second
                tracePath(list)
                val currentPosition = arena.robotCoordinates
                if (currentPosition[0] != endX || currentPosition[1] != endY) continue
                break
            }

            activityCallback(true)
        }
    }

    private fun findShortestPath(startX: Int, startY: Int, startR: Int, endX: Int, endY: Int): Pair<Double, List<IntArray>> {
        if (!arena.isValidCoordinates(startX, startY)) return Pair(-1.0, listOf())
        if (!arena.isValidCoordinates(endX, endY)) return Pair(-1.0, listOf())
        val openList: ArrayList<GridNode> = arrayListOf()
        val closedList: ArrayList<GridNode> = arrayListOf()
        val successors: ArrayList<GridNode> = arrayListOf()
        var parentNode = GridNode(startX, startY, 0.0, 0.0, 0.0, -1, -1, startR, startR)
        var smallestF: Double
        var found = false
        openList.add(parentNode)

        while (openList.isNotEmpty()) {
            smallestF = 999.0
            successors.clear()

            for (node in openList) {
                if (node.f < smallestF) {
                    smallestF = node.f
                    parentNode = node
                }
            }

            if (plotSearch) arena.plot(parentNode.x, parentNode.y, Arena.GridType.SEARCH_ADJACENT)
            openList.remove(parentNode)
            closedList.add(parentNode)

            if (fastestPath) {
                for (yOffset in -1 .. 1) {
                    for (xOffset in -1 .. 1) {
                        var continueToNext = false

                        var facing = when {
                            (yOffset == xOffset && yOffset == -1)   -> 225
                            (yOffset == xOffset && yOffset == 1)    -> 45
                            (xOffset == -1 && yOffset == 1)         -> 315
                            (xOffset == 1 && yOffset == -1)         -> 135
                            (xOffset == 0 && yOffset == -1)         -> 180
                            (xOffset == 0 && yOffset == 1)          -> 0
                            (yOffset == 0 && xOffset == -1)         -> 270
                            (yOffset == 0 && xOffset == 1)          -> 90
                            else                                    -> parentNode.facing
                        }

                        val direction = facing
                        if (facing - parentNode.facing == 180) facing = Math.floorMod(facing + 180, 360)
                        val gridNode = GridNode(parentNode.x + xOffset, parentNode.y + yOffset, 0.0, 0.0, 0.0, parentNode.x, parentNode.y, facing, direction)

                        if (arena.isRobotMovable(gridNode.x, gridNode.y)) {
                            for (node in closedList) if (node.x == gridNode.x && node.y == gridNode.y) {
                                continueToNext = true
                                break
                            }

                            if (!continueToNext) successors.add(gridNode)
                        }
                    }
                }
            } else {
                for (offset in -1..1 step 2) {
                    for (count in 0..1) {
                        var continueToNext = false
                        var facing = if (offset == -1) (180 + (count * 90)) else (0 + (count * 90))
                        val direction = facing
                        if (facing - parentNode.facing == 180) facing = Math.floorMod(facing + 180, 360)
                        val gridNode =
                            if (count == 0) GridNode(parentNode.x, parentNode.y + offset, 0.0, 0.0, 0.0, parentNode.x, parentNode.y, facing, direction)
                            else            GridNode(parentNode.x + offset, parentNode.y, 0.0, 0.0, 0.0, parentNode.x, parentNode.y, facing, direction)

                        if (arena.isRobotMovable(gridNode.x, gridNode.y)) {
                            for (node in closedList) if (node.x == gridNode.x && node.y == gridNode.y) {
                                continueToNext = true
                                break
                            }

                            if (!continueToNext) successors.add(gridNode)
                        }
                    }
                }
            }

            for (successor in successors) {
                var continueToNext = false

                if (successor.x == endX && successor.y == endY) {
                    parentNode = successor
                    found = true
                    openList.clear()
                    break
                }

                val penalty: Double = when {
                    parentNode.facing == successor.facing               -> 0.0
                    abs(parentNode.facing - successor.facing) <= 45     -> 2.0
                    abs(parentNode.facing - successor.facing) >= 315    -> 2.0
                    abs(parentNode.facing - successor.facing) == 90     -> 3.5
                    abs(parentNode.facing - successor.facing) == 270    -> 3.5
                    else                                                -> 3.5
                }

                if (fastestPath) {
                    successor.g = sqrt((1.0 * abs(successor.x - successor.parentX) + 1.0 * abs(successor.y - successor.parentY))) + parentNode.g + penalty
                    successor.h = max(abs(successor.x - endX), abs(successor.y - endY)).toDouble()
                    //successor.h = sqrt((1.0 * successor.x - endX).pow(2) + (1.0 * successor.y - endY).pow(2))
                } else {
                    successor.g = abs(successor.x - successor.parentX) + abs(successor.y - successor.parentY) + parentNode.g + penalty
                    successor.h = 1.0 * (abs(successor.x - endX) + abs(successor.y - endY))
                }

                successor.f = successor.g + successor.h

                for (node in openList) {
                    if (node.x == successor.x && node.y == successor.y) {
                        if (successor.f < node.f) {
                            node.f = successor.f
                            node.g = successor.g
                            node.h = successor.h
                            node.parentX = successor.parentX
                            node.parentY = successor.parentY
                            continueToNext = true
                        }
                    }
                }

                if (continueToNext) continue
                openList.add(successor)
            }
        }

        val pathList: ArrayList<IntArray> = arrayListOf()
        var totalCost = 0.0

        if (found) {
            while (true) {
                val x = parentNode.x
                val y = parentNode.y
                if (plotPathChosen) arena.plot(x, y, Arena.GridType.SEARCH_PICKED)

                if (parentNode.parentX == -1 || parentNode.parentY == -1) break
                pathList.add(intArrayOf(x, y, parentNode.facing, parentNode.direction))
                totalCost += parentNode.f

                for (node in closedList) {
                    if (node.x == parentNode.parentX && node.y == parentNode.parentY) {
                        parentNode = node
                        break
                    }
                }
            }
        }

        val ret: List<IntArray> = pathList.reversed()
        return Pair(totalCost, ret)
    }
}



/*
IF DIAGONAL MOVEMENT IS ALLOWED, REPLACE WITH THIS

for (yOffset in -1 .. 1) {
    for (xOffset in -1 .. 1) {
        var con2 = false
        val gridNode = GridNode(parentNode.x + xOffset, parentNode.y + yOffset, 0.0, 0.0, 0, parentNode.x, parentNode.y)
        if (arena.isRobotMovable(gridNode.x, gridNode.y)) {
            for (node in closedList) {
                if (node.x == gridNode.x && node.y == gridNode.y) con2 = true
            }

            if (!con2) successors.add(gridNode)
        }

        con2 = false
        val gridNode2 = GridNode(parentNode.x + yOffset, parentNode.y + xOffset, 0.0, 0.0, 0, parentNode.x, parentNode.y)
        if (arena.isRobotMovable(gridNode2.x, gridNode2.y)) {
            for (node in closedList) {
                if (node.x == gridNode2.x && node.y == gridNode2.y) con2 = true
            }

            if (!con2) successors.add(gridNode2)
        }
    }
}


//successor.g = sqrt(1.0 * abs(successor.x - successor.parentX) + 1.0 * abs(successor.y - successor.parentY)) + parentNode.g
//successor.h = max(abs(successor.x - goalX), abs(successor.y - goalY))

 */